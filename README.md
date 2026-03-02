# GameServerBot

Monitors and controls game servers via the Nitrado API. Syncs server state on a schedule, sends Discord notifications on status changes, and exposes REST endpoints and Discord slash commands for manual control.

## Features

**Done**
- Start / stop / restart / delete servers via REST and Discord slash commands
- Periodic Nitrado sync (supercronic → `POST /api/jobs/sync-servers`)
- Discord status-change notifications
- Player whitelist and banlist management (`/server-whitelist`, `/server-banlist`)
- Role-based HTTP Basic Auth (`ADMIN`, `CRON_JOB`)
- Resilience4j circuit breaker, rate limiter, retry + Caffeine cache on Nitrado client

**Planned**
- Persistent database (currently in-memory — state is lost on restart)
- Discord notifications for whitelist / banlist changes

## Local Development

```bash
# Start WireMock (stubs Nitrado API) and supercronic via Docker
./gradlew startDependencies

# Run the application (with WireMock profile)
./gradlew bootRun

# Run without Docker
./gradlew bootRun --args='--spring.profiles.active=local'

# Full build: format check + tests + architecture tests
./gradlew build
```

## Deploy with Docker Compose

**`.env`**
```dotenv
ADMIN_PASSWORD=changeme
CRON_JOB_PASSWORD=changeme
NITRADO_API_TOKEN=your-token
NITRADO_SERVER_ID=12345678

# Discord (optional)
DISCORD_ENABLED=false
DISCORD_BOT_TOKEN=
DISCORD_GUILD_ID=
DISCORD_NOTIFICATION_CHANNEL_ID=
DISCORD_ADMIN_ROLE_ID=
DISCORD_ALLOWED_ROLE_ID=
```

**`crontab`**
```
*/30 * * * * * * curl -sf -X POST -u game-server-bot-job:${JOB_PASSWORD} http://game-server-bot:8080/api/jobs/sync-servers
*/15 * * * * * * curl -sf -X POST -u game-server-bot-job:${JOB_PASSWORD} http://game-server-bot:8080/api/jobs/update-discord-presence
```

**`docker-compose.yaml`**
```yaml
services:
  game-server-bot:
    image: ghcr.io/xenexes/game-server-bot:latest
    restart: unless-stopped
    env_file: .env
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      start_period: 90s

  cron:
    image: hectorm/supercronic
    restart: unless-stopped
    environment:
      JOB_PASSWORD: ${CRON_JOB_PASSWORD}
    volumes:
      - ./crontab:/etc/crontab:ro
    depends_on:
      game-server-bot:
        condition: service_healthy
```

```bash
docker compose up -d
```

## Deployment Pipeline (Proxmox)

The `pipeline/` directory contains an Ansible-orchestrated pipeline that builds, versions, and deploys to a Proxmox LXC container via Terraform.

**What it does:**
1. `ktlintCheck → test → build` (fails fast)
2. Semver bump in `build.gradle.kts` + git tag
3. Build and push Docker image
4. Terraform: destroy old LXC → provision new Debian 13 LXC on Proxmox
5. Ansible: install Docker, deploy app stack, verify Hawser agent

```bash
# Fill in secrets first
cp pipeline/ansible/vars/secrets.yml.example pipeline/ansible/vars/secrets.yml

# Run full pipeline
make deploy
```

See [`pipeline/README.md`](pipeline/README.md) for prerequisites, Proxmox API setup, and operational notes.