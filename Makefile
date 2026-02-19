IMAGE_NAME := game-server-bot
REGISTRY   := $(DOCKER_REGISTRY)
VERSION    := $(shell grep '^version' build.gradle.kts | head -n1 | cut -d'"' -f2)
FULL_IMAGE := $(REGISTRY)/$(IMAGE_NAME):$(VERSION)

.DEFAULT_GOAL := help
.PHONY: help all build test format build-image publish clean start stop destroy check-registry deploy

help: ## Show available targets
	@awk 'BEGIN {FS = ":.*##"; printf "%-20s %s\n%-20s %s\n", "Target", "Description", "------", "-----------"} \
	     /^[a-zA-Z_-]+:.*?##/ { printf "%-20s %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "Image: $(FULL_IMAGE)"

# -- Application --------------------------------------------------------------

all: format build ## Format, build and test (gate check)

build: ## Full build including tests and linting
	./gradlew build

test: ## Run tests only
	./gradlew test

format: ## Auto-format code with ktlint
	./gradlew ktlintFormat

# -- Docker -------------------------------------------------------------------

check-registry:
ifndef DOCKER_REGISTRY
	$(error DOCKER_REGISTRY environment variable is not set)
endif

build-image: ## Build Docker image to local Docker daemon (via Jib)
	./gradlew jibDockerBuild
	@echo "Built locally: $(IMAGE_NAME):$(VERSION)"

publish: build check-registry ## Build and push Docker image to the registry (via Jib, no Docker daemon required)
	./gradlew jib
	@echo "Published: $(FULL_IMAGE)"

clean: check-registry ## Remove local Docker image
	-docker rmi $(FULL_IMAGE)

# -- Local dev ----------------------------------------------------------------

start: ## Start local infrastructure (WireMock)
	./gradlew startDependencies

stop: ## Stop local infrastructure
	./gradlew stopDependencies

destroy: ## Stop local infrastructure and remove volumes
	./gradlew destroyDependencies

# -- Pipeline -----------------------------------------------------------------

ANSIBLE_PLAYBOOK := pipeline/ansible/site.yml
ANSIBLE_SECRETS  := pipeline/ansible/vars/secrets.yml

deploy: ## Build, tag, and deploy (version is read from build.gradle.kts)
	ansible-playbook $(ANSIBLE_PLAYBOOK) -e @$(ANSIBLE_SECRETS)
