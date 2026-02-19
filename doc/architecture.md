# Architecture Overview

GameServerBot is a Spring Boot / Kotlin service that manages game servers via the **Nitrado API**. It periodically syncs external server state into its own store and triggers notifications (Discord) based on state changes. Scheduling is driven by an **external cron container** calling HTTP endpoints instead of Spring `@Scheduled`.

---

## Technology Stack

| Concern | Technology |
|---------|-----------|
| Language | Kotlin |
| Web framework | Spring Boot, Spring WebFlux |
| HTTP client | Ktor Client |
| Error handling | Arrow-kt (`Either`, Raise DSL) |
| Discord bot | Kord |
| Resilience | Resilience4j (circuit breaker, rate limiter, retry) |
| Caching | Caffeine |
| Security | Spring Security WebFlux, HTTP Basic Auth, BCrypt |
| Serialization | kotlinx.serialization |
| Logging | kotlin-logging (oshai) |
| Testing | JUnit 5, MockK, AssertK, WireMock, Konsist |
| Linting | ktlint, Konsist (architecture tests) |

---

## Hexagonal Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  Driving Adapters (api/)                                        │
│  ┌──────────────────┐  ┌────────────────┐  ┌────────────────┐  │
│  │  REST Controllers│  │  Job Controllers│  │  Discord Bot   │  │
│  │  /api/game-      │  │  /api/jobs/    │  │  (Kord)        │  │
│  │  servers/**      │  │  sync-servers  │  │  slash commands│  │
│  │                  │  │  update-discord│  │  autocomplete  │  │
│  └────────┬─────────┘  └───────┬────────┘  └───────┬────────┘  │
│           │                    │                    │           │
│  ┌────────▼────────────────────▼────────────────────▼────────┐  │
│  │                     Use Cases (usecases/)                  │  │
│  │  GameServerUseCases  PlayerUseCases  DiscordBotUseCases    │  │
│  │  DiscordPresenceUseCases  NotificationUseCases             │  │
│  └────────┬─────────────────────────────────────────────────-┘  │
│           │  context(UserContext)  UseCaseRaise                  │
└───────────┼─────────────────────────────────────────────────────┘
            │
┌───────────▼─────────────────────────────────────────────────────┐
│  Domain (domain/)  — no external dependencies                    │
│  GameServer aggregate  ·  Player aggregate  ·  Discord domain    │
│  Domain events  ·  Value objects  ·  Domain failures             │
└───────────┬─────────────────────────────────────────────────────┘
            │
┌───────────▼─────────────────────────────────────────────────────┐
│  Outbound Ports (ports/outbound/)                                │
│  GameServerRepository  NitradoGateway  PlayerGateway             │
│  NotificationGateway   DiscordClientGateway  DomainEventPublisher│
│  UserRepository                                                  │
└───────────┬─────────────────────────────────────────────────────┘
            │
┌───────────▼─────────────────────────────────────────────────────┐
│  Driven Adapters (infrastructure/)                               │
│  ┌────────────────┐  ┌─────────────────┐  ┌───────────────────┐ │
│  │ InMemory       │  │ KtorNitrado     │  │ KordDiscord       │ │
│  │ GameServer     │  │ Gateway         │  │ ClientGateway     │ │
│  │ Repository     │  │ (+ Caching)     │  │ DiscordNotification│
│  └────────────────┘  └─────────────────┘  │ Adapter           │ │
│  ┌────────────────┐  ┌─────────────────┐  └───────────────────┘ │
│  │ ConfigUser     │  │ Spring          │                         │
│  │ Repository     │  │ DomainEvent     │                         │
│  └────────────────┘  │ Publisher       │                         │
│                      └─────────────────┘                         │
└─────────────────────────────────────────────────────────────────┘
```

### Layer Dependency Rules

| Layer | May depend on |
|-------|--------------|
| `api` | `domain`, `usecases` |
| `domain` | nothing |
| `usecases` | `domain`, `ports` |
| `ports` | `domain` |
| `infrastructure` | `domain`, `ports`, `usecases` |
| `config` | anything |

Enforced at build time by **Konsist** architecture tests (`ArchitectureTest.kt`).

---

## Package Layout

```
src/main/kotlin/de/xenexes/gameserverbot/
├── api/
│   ├── rest/                    # REST controllers, ControllerHandler, DTOs
│   ├── jobs/                    # Cron job HTTP endpoints + CronJobHandler
│   ├── events/                  # Spring event listeners (domain → notifications)
│   └── discord/                 # Discord bot service, command/autocomplete handlers
├── usecases/                    # Application services, UseCase DSL, UseCaseError
├── domain/
│   ├── gameserver/              # GameServer aggregate
│   ├── player/                  # Player aggregate
│   ├── discord/                 # Discord domain (roles, permissions, presence)
│   └── shared/                  # UserContext, CallerPrincipal, UserId, DomainEvent
├── ports/outbound/              # Port interfaces + failure types
├── infrastructure/
│   ├── persistence/             # InMemoryGameServerRepository
│   ├── security/                # ConfigUserRepository
│   ├── http/nitrado/            # KtorNitradoGateway, CachingNitradoGateway
│   ├── player/                  # NitradoPlayerGateway
│   ├── discord/                 # KordDiscordClientGateway, DiscordNotificationAdapter
│   └── events/                  # SpringDomainEventPublisher
└── config/                      # Spring configuration and properties
```

---

## Domain Model

### GameServer (Aggregate Root)

```
GameServer
  id: GameServerId           — @JvmInline UUID
  nitradoId: NitradoServerId — @JvmInline Long
  name: String
  status: GameServerStatus   — STARTED | STOPPED | RESTARTING | STOPPING |
                               SUSPENDED | GUARDIAN_LOCKED | BACKUP_RESTORE |
                               BACKUP_CREATION | CHUNKFIX | OVERVIEWMAP_RENDER |
                               GS_INSTALLATION | UNKNOWN
  ip, port                   — connection info (nullable)
  gameCode, gameName         — active game (nullable)
  playerSlots: Int?
  location: String?
  createdAt, updatedAt: Instant
  pendingEvents              — collected until consumeEvents() is called
```

**State transitions triggered by**:
- User actions (`startServer`, `stopServer`, `restartServer`) — immediate status update
- Sync from Nitrado (`syncServer`) — status updated to match external state, emits `GameServerStatusChangedEvent`

### Events

```
GameServerEvent (sealed)
  ├── GameServerCreatedEvent       — on create
  ├── GameServerStatusChangedEvent — on any status change; carries isStatusImprovement() / isStatusDegradation()
  └── GameServerDeletedEvent       — on delete

PlayerEvent (sealed)
  ├── PlayerAddedToWhitelist / PlayerRemovedFromWhitelist
  └── PlayerAddedToBanlist   / PlayerRemovedFromBanlist
```

Events are published via `DomainEventPublisher` → Spring Application Events → `GameServerEventListener` / `PlayerEventListener` → `NotificationUseCases` → Discord.

### Request Context

```
UserContext
  userId: UserId
  principal: CallerPrincipal
    ├── ApiUser(username)      — HTTP Basic Auth user
    ├── DiscordUser(id)        — Discord slash command caller
    ├── System                 — internal operations
    └── CronJob                — called from /api/jobs endpoints

// Pre-built instances
UserContext.api(username)      — created by UserContextProvider from Spring Security
UserContext.discord(id)        — created by Discord command handlers
UserContext.system             — for internal use
UserContext.cron               — used by CronJobHandler
```

---

## Use Cases

### GameServerUseCases
| Operation | Description |
|-----------|-------------|
| `findById(id)` | Fetch single server |
| `findAll()` | Fetch all servers |
| `create(command)` | Register new server, publishes `GameServerCreatedEvent` |
| `startServer(id)` | Calls Nitrado start, sets status to RESTARTING |
| `stopServer(id, message?)` | Calls Nitrado stop, sets status to STOPPING |
| `restartServer(id, message?)` | Calls Nitrado restart, sets status to RESTARTING |
| `syncServer(id)` | Fetches Nitrado info, updates status if changed, publishes event |
| `syncAllServers()` | Syncs all registered servers (batch) |
| `deleteServer(id)` | Deletes server, publishes `GameServerDeletedEvent` |
| `fetchGameList(id)` | List available games from Nitrado |
| `fetchPlayers(id)` | Online players from Nitrado |
| `switchGame(id, gameId)` | Switch active game via Nitrado |

### PlayerUseCases

Delegates player list operations to `PlayerGateway`, publishes `PlayerEvent` on changes.

| Operation | Description |
|-----------|-------------|
| `fetchWhitelist(id)` | Fetch whitelist entries |
| `addToWhitelist(id, player)` | Add player to whitelist |
| `removeFromWhitelist(id, player)` | Remove player from whitelist |
| `fetchBanlist(id)` | Fetch banlist entries |
| `addToBanlist(id, player)` | Add player to banlist |
| `removeFromBanlist(id, player)` | Remove player from banlist |

### DiscordBotUseCases
Manages Kord bot lifecycle, slash command registration, and event handler registration.

### DiscordPresenceUseCases
Updates Discord bot presence (status + activity text) via `DiscordClientGateway`.

### NotificationUseCases
Routes domain events to `NotificationGateway` (Discord embeds).

---

## API Endpoints

### REST API (`/api/**`) — requires ADMIN role

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/game-servers` | List all servers |
| `GET` | `/api/game-servers/{id}` | Get server by ID |
| `POST` | `/api/game-servers` | Register server |
| `POST` | `/api/game-servers/{id}/start` | Start server |
| `POST` | `/api/game-servers/{id}/stop` | Stop server |
| `POST` | `/api/game-servers/{id}/restart` | Restart server |
| `DELETE` | `/api/game-servers/{id}` | Delete server |

### Job Endpoints (`/api/jobs/**`) — requires ADMIN or CRON_JOB role

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/jobs/sync-servers` | Sync all servers from Nitrado → `{succeeded, failed}` |
| `POST` | `/api/jobs/update-discord-presence` | Refresh Discord bot presence (Discord enabled only) |

### Public

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/actuator/health` | Health check |

---

## Security

HTTP Basic Auth with bcrypt passwords. Two built-in users:

| User | Role | Configured via |
|------|------|----------------|
| `admin` | ADMIN | `ADMIN_PASSWORD` env var |
| `game-server-bot-job` | CRON_JOB | `CRON_JOB_PASSWORD` env var |

Passwords are auto-generated and logged on startup when the env var is blank.

---

## Error Handling

All operations use **Arrow-kt `Either`** for typed errors. No exceptions cross layer boundaries.

```
Port failures (NitradoFailure, RepositoryFailure, DiscordFailure, …)
  └─ bound inside useCase { } via .bind()
       └─ wrapped into UseCaseError.Nitrado / UseCaseError.Repository / …
            └─ thrown as FailureException in ControllerHandler
                 └─ mapped to HTTP ProblemDetail by ControllerExceptionHandler
```

**UseCaseRaise** provides typed `.bind()` extensions for each failure category, so bind-ing `Either<NitradoFailure, T>` produces `UseCaseError.Nitrado` automatically.

---

## Cron Job Pattern (K8s-native)

Instead of `@Scheduled`, the application exposes HTTP POST endpoints at `/api/jobs/**`. An external **supercronic** container calls these endpoints on a schedule.

```
compose.yaml
  gameserverbot-cron (supercronic)
    docker/crontab:
      */30 * * * * *  POST /api/jobs/sync-servers          (every 30s)
      */15 * * * * *  POST /api/jobs/update-discord-presence (every 15s)
```

In Kubernetes, these would be `CronJob` resources calling the same endpoints.

---

## Nitrado Integration

All Nitrado calls go through a two-layer adapter chain:

```
Use Cases
  └─ CachingNitradoGateway (Caffeine)
       └─ KtorNitradoGateway
            └─ ResilientHttpClient (Resilience4j)
                 └─ Ktor HttpClient
                      └─ Nitrado REST API
```

**Cache TTLs**: server-status 30 s · services 5 m · game-list 5 m · players 30 s · player-list 1 m

**Resilience**: circuit breaker (opens at 50% failure, window=10) · rate limiter (10 req/s) · retry (3 attempts, 500 ms backoff)

---

## Discord Integration

Controlled by `gameserverbot.discord.enabled`. When disabled, **no-op implementations** are injected (`NoOpDiscordClientGateway`, `NoOpNotificationGateway`) so the application starts cleanly without Discord credentials.

When enabled:
- Kord bot connects to the configured guild
- Slash commands are registered on startup
- Notifications are sent as embeds to the notification channel
- Bot presence is updated every 15 s via `/api/jobs/update-discord-presence`

Discord user permissions are resolved against guild role IDs (`admin-role-id`, `allowed-role-id`) configured in `application.yaml`.

---

## Local Development

```bash
# Start WireMock (Nitrado API stub) and supercronic
./gradlew startDependencies       # docker compose up -d

# Run application against WireMock
./gradlew bootRun --args='--spring.profiles.active=local-wiremock'

# Run without Docker (no cron jobs)
./gradlew bootRun --args='--spring.profiles.active=local'
```

WireMock stubs live in `src/test/resources/wiremock/nitrado/`.

---

## Testing Strategy

| Type | Location | Notes |
|------|----------|-------|
| Unit | `unit/` | MockK, no Spring context |
| Integration | `integration/` (`*IT.kt`) | WireMock, full Spring context |
| Architecture | `architecture/ArchitectureTest.kt` | Konsist rules, run with every build |

Key architecture rules enforced:
- `@RestController` classes must be in `..api..`
- `@Repository` classes must be in `..infrastructure..`
- `api` layer must not import port interfaces directly
- `UseCases` classes must have `@Component`
- Domain ID value objects must have `@JvmInline`
- Layer dependency rules (see table above)
