# GameServerBot Deployment Pipeline

Ansible-orchestrated local deployment pipeline that builds the app, bumps the version,
publishes Docker images, provisions a Proxmox LXC container via Terraform, and configures
the full application stack (app + cron + Hawser agent).

## Pipeline Overview

```
Play 1 — localhost
  build           ktlintCheck → test → build (fails fast on errors)
  version         semver bump → build.gradle.kts → CHANGELOG.md → git tag
  docker_publish  make publish (app image only — cron uses hectorm/supercronic directly)

Play 2 — localhost
  terraform       destroy existing LXC → provision new LXC → add to inventory

Play 3 — LXC host (via SSH)
  base            apt upgrade + unattended-upgrades
  docker_install  Docker CE + compose plugin + registry auth
  app_deploy      .env + docker-compose.yml + docker compose up -d + health check
  hawser          verify Hawser (Dockhand agent) container is running
```

---

## Prerequisites

### One-time software setup

```bash
# Ansible
pip install ansible
ansible-galaxy collection install -r pipeline/ansible/requirements.yml

# Terraform >= 1.0 must be on PATH
terraform --version

# Docker daemon must be running locally (for image builds and push)
docker info
```

### Infrastructure prerequisites

| Requirement | Notes |
|---|---|
| Proxmox host with API access | API token with LXC create/delete permissions |
| Debian 13 LXC template on Proxmox | See "Download Debian 13 template" below |
| Docker registry | ghcr.io, Docker Hub, or self-hosted |
| Dockhand account | Required for Hawser agent — https://dockhand.dev |
| SSH key pair | Used to bootstrap and manage the LXC |

#### Proxmox — Terraform API user setup

Run on the Proxmox host shell:

```bash
# 1. Create the Terraform API user
pveum user add terraform@pve --comment "Terraform API user"

# 2. Create an API token (--privsep 1 = token has its own ACLs, not inherited from user)
pveum user token add terraform@pve terraform --privsep 1

# 3. Create a role with the minimum required privileges
# SDN.Use is required by Proxmox 8 for network interface creation on SDN-managed bridges
pveum role add TerraformLXC \
  --privs "VM.Allocate VM.Clone VM.Config.CDROM VM.Config.Cloudinit VM.Config.CPU VM.Config.Disk VM.Config.HWType VM.Config.Memory VM.Config.Network VM.Config.Options VM.Migrate VM.PowerMgmt VM.Audit VM.Console Datastore.AllocateSpace Datastore.Audit Pool.Allocate Sys.Audit Sys.Console Sys.Modify SDN.Use"

# 4. Grant the role on the required scopes
# With privsep=1 the effective permissions = intersection(user perms, token perms)
# so both the user AND the token must be granted the role on each path.

# Grant to the user
pveum acl modify /nodes/mule                        --users terraform@pve --roles TerraformLXC --propagate 1
pveum acl modify /vms                               --users terraform@pve --roles TerraformLXC --propagate 1
pveum acl modify /storage/local                     --users terraform@pve --roles TerraformLXC --propagate 1
pveum acl modify /storage/local-lvm                 --users terraform@pve --roles TerraformLXC --propagate 1
pveum acl modify /sdn/zones/localnetwork/vmbr0      --users terraform@pve --roles TerraformLXC

# Grant to the token (pveum acl modify does not support --tokens, use pvesh instead)
pvesh set /access/acl --path /nodes/mule                   --tokens 'terraform@pve!terraform' --roles TerraformLXC --propagate 1
pvesh set /access/acl --path /vms                          --tokens 'terraform@pve!terraform' --roles TerraformLXC --propagate 1
pvesh set /access/acl --path /storage/local                --tokens 'terraform@pve!terraform' --roles TerraformLXC --propagate 1
pvesh set /access/acl --path /storage/local-lvm            --tokens 'terraform@pve!terraform' --roles TerraformLXC --propagate 1
pvesh set /access/acl --path /sdn/zones/localnetwork/vmbr0 --tokens 'terraform@pve!terraform' --roles TerraformLXC

# Verify — should list all granted privileges
pveum user token permissions terraform@pve terraform
```

The token secret is shown once on creation — copy it into `secrets.yml` as `proxmox_api_token_secret`.

#### Download Debian 13 template on Proxmox host

```bash
# Run on the Proxmox host
pveam update
pveam download local debian-13-standard_13.1-2_amd64.tar.zst
```

---

## TODO — Required Before First Run

### 1. Create and fill in your secrets file

```bash
cp pipeline/ansible/vars/secrets.yml.example pipeline/ansible/vars/secrets.yml
# Edit pipeline/ansible/vars/secrets.yml — fill in every value
```

`secrets.yml` is gitignored and never committed. See
`pipeline/ansible/vars/secrets.yml.example` for all required fields.

### 2. Verify the Makefile has a `publish` target

The `docker_publish` role runs `make publish` from the project root.
Ensure your `Makefile` has a `publish` target that builds and pushes the app image.
It must read `DOCKER_REGISTRY` from the environment, e.g.:

```makefile
publish:
    docker build -t $(DOCKER_REGISTRY)/game-server-bot:$(VERSION) .
    docker push $(DOCKER_REGISTRY)/game-server-bot:$(VERSION)
```

---

## Versioning

The version is set **manually** in `build.gradle.kts`:

```kotlin
version = "1.2.3"        // release
version = "1.2.3-SNAPSHOT" // SNAPSHOT suffix is stripped automatically for the tag
```

The pipeline reads this version, then:
- If git tag `v1.2.3` **does not exist** → creates the tag and pushes it to the remote
- If git tag `v1.2.3` **already exists** → skips tagging, deploys the existing version

To release a new version: bump the version in `build.gradle.kts`, commit, then run `make deploy`.

## Running the Pipeline

```bash
# Set version in build.gradle.kts first, then:
make deploy

# Or directly:
ansible-playbook pipeline/ansible/site.yml -e @pipeline/ansible/vars/secrets.yml
```

---

## Verification

```bash
# Application health
curl http://<LXC_IP>:8080/actuator/health

# Running containers
ssh root@<LXC_IP> 'docker ps'

# Application logs
ssh root@<LXC_IP> 'docker logs -f game-server-bot'

# Hawser agent
curl http://<LXC_IP>:2376/health

# Check version tag was created
git tag -l | tail -5
```

---

## Operational Notes

- **In-memory data loss on redeploy** — `GameServer` data is stored in-memory only.
  All registered servers are lost when the container restarts. Re-register via the REST
  API after each deployment.

- **Clean-slate deploys** — The pipeline destroys the old LXC and creates a new one.
  There are no rolling updates. Expect downtime during deployment.

- **Do NOT set `SPRING_PROFILES_ACTIVE=prod`** — `application-prod.yaml` forces
  `discord.enabled: true`, which causes startup to fail if Discord credentials are absent.
  Leave the profile unset; the `.env` file controls feature flags directly.

- **`/actuator/health` is public** — The health check endpoint requires no authentication.
  This is intentional (`permitAll()` in `SecurityConfiguration.kt`).

- **Git tag is local until pushed** — The pipeline does not push to the remote.
  Run `git push && git push --tags` after the pipeline finishes.

- **Hawser requires a Dockhand server** — The Hawser container runs but is only useful
  when connected to a configured Dockhand server. See https://dockhand.dev for setup.

---

## Directory Structure

```
pipeline/
├── README.md                           # This file
├── ansible/
│   ├── site.yml                        # Master playbook
│   ├── requirements.yml                # Ansible Galaxy collections
│   ├── group_vars/
│   │   └── all.yml                     # Non-sensitive defaults
│   ├── vars/
│   │   ├── secrets.yml.example         # Secrets template (committed)
│   │   └── secrets.yml                 # GITIGNORED — real secrets
│   ├── roles/
│   │   ├── build/tasks/main.yml        # Gradle build gate
│   │   ├── version/tasks/main.yml      # Semver bump + git tag
│   │   ├── docker_publish/tasks/main.yml
│   │   ├── terraform/tasks/main.yml    # Proxmox LXC lifecycle
│   │   ├── base/tasks/main.yml         # Base OS configuration
│   │   ├── docker_install/tasks/main.yml
│   │   ├── app_deploy/tasks/main.yml   # Deploy app stack
│   │   └── hawser/tasks/main.yml       # Verify Hawser agent
│   └── templates/
│       ├── docker-compose.yml.j2       # App + cron + hawser services
│       ├── env.j2                      # Runtime secrets (mode 0600)
│       └── crontab.j2                  # supercronic schedule
├── cron/
│   └── crontab.local                   # supercronic schedule for local dev (docker-compose.yaml)
└── terraform/
    ├── main.tf                         # Proxmox LXC resource
    ├── variables.tf
    ├── outputs.tf
    ├── terraform.tfvars.example        # Template for manual use
    └── .gitignore                      # State files excluded
```
