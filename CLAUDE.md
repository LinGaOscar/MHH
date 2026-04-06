# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MHH (Message History Hub)** is a financial-grade SWIFT message management platform for banking institutions. It centralizes, parses, and audits SWIFT electronic messages (MT103, MT202, MX/ISO 20022). Core compliance feature: **Maker-Checker dual approval** workflow.

## Build & Run Commands

This is a Maven multi-module project (Java 21, Spring Boot 4.0.5).

```bash
# Full build (skip tests for speed)
mvn clean install -DskipTests

# Build a single module
mvn clean install -pl mhh-ap -DskipTests

# Run tests
mvn test
mvn test -pl mhh-ap          # single module tests

# Run the web application (port 8080)
mvn spring-boot:run -pl mhh-ap

# Run the batch processor (separate terminal)
mvn spring-boot:run -pl mhh-batch

# Start the database (MS SQL Server via Docker)
cd DB-init && docker-compose up -d
```

**Dev login:** With `mhh.auth.dev-mode: true` (default in `mhh-ap/application.yml`), Spring's form login is enabled at `/login`. Default credentials: `admin` / `1234` (InMemoryUserDetailsManager in `SecurityConfig`). Production mode uses HTTP Basic + SSO redirect to `/sso/login`.

## Module Architecture

```
mhh-common  ←── mhh-core  ←── mhh-ap
                           ←── mhh-batch
```

| Module | Role |
|--------|------|
| **mhh-common** | Shared JPA entities, repositories, DTOs, validation |
| **mhh-core** | Parser strategy pattern (PDF→SWIFT message extraction), AOP audit |
| **mhh-ap** | Spring MVC web app: REST APIs, Thymeleaf views, Spring Security |
| **mhh-batch** | Scheduled batch jobs: PDF import, SWAL sync, HR sync, log cleanup |

Both `mhh-ap` and `mhh-batch` use `spring.jpa.hibernate.ddl-auto: none`. Schema is managed exclusively via `DB-init/01_schema.sql`.

## Data Flows

```
SWAL Sync:  Oracle SWAL DB (read-only) → SwalSyncJob (batch 500) → MSG_INCOMING
PDF Import: d:/MHH_FILES/MX|MT/ → PdfImportJob → ParserFactory → MSG_INCOMING or MSG_OUTGOING
            → success: d:/MHH_FILES/ARCHIVE   fail: d:/MHH_FILES/ERROR
Download:   User request → MSG_DOWNLOAD (PENDING) → Manager approval → ReservationMergeJob → ZIP → d:/MHH_FILES/TEMP
```

## Key Architectural Patterns

### 1. Strategy Pattern — PDF Parsers (`mhh-core`)
`ParserFactory` selects the lowest-priority `PdfParser` whose `supports(text)` returns true. To add a new SWIFT format, implement `PdfParser` and register as a Spring `@Component`.

| Parser | Priority | Handles |
|--------|----------|---------|
| `Mt103Parser` | 50 | SWIFT MT103 (Customer Credit Transfer) |
| `MtGenericParser` | 200 | Any MT message (fallback) |
| `MxGenericParser` | 200 | ISO 20022 MX messages (pacs, camt, pain…) |

### 2. Maker-Checker Workflow
Sensitive operations (branch downloads, parameter changes) go through `MSG_APPROVAL`. Roles: `BRANCH_MAKER`, `BRANCH_CHECKER`, `PARAM_MAKER`, `PARAM_CHECKER`, scoped per `country_code`.

### 3. Dynamic Job Scheduling (`mhh-batch`)
Batch cron expressions live in `JOBS_CONF` (entity: `JobConf`). `JobSyncScheduler` refreshes every 20 min (`mhh.jobs.refresh-rate`). A `CommandLineRunner` syncs on startup. Thread pool: 5 threads (`mhh-job-` prefix). Jobs can be toggled without restart.

Batch jobs implement `MhhJob` (not Spring Batch):

| Job bean | Purpose |
|----------|---------|
| `pdfImportJob` | Scan MX/MT folders, parse PDFs, archive |
| `swalSyncJob` | Pull from Oracle SWAL → `MSG_INCOMING` (500/batch) |
| `ReservationMergeJob` | Merge released messages into ZIP (stub) |
| `LogCleanupJob` | Delete `USER_LOGS` older than 1 year |
| `SwallowSyncJob` | SWALLOW message sync (stub) |
| `HrSyncJob` | HR employee status sync (stub) |

### 4. AOP-based Auditing (`mhh-ap`)
- `@LogAction(action="...", description="...")` on controller methods → `UserActionAspect` intercepts, resolves SpEL in `description`, writes to `USER_LOGS` via `UserLogService.record()`.
- `UserLogService.record()` is `@Async` + `Propagation.REQUIRES_NEW` — never blocks the main thread. `@EnableAsync` is on `MhhApApplication`.
- `AuthEventListener` handles `LOGIN`/`LOGOUT` events from Spring Security.
- Batch jobs log to local files via `JobLoggingAspect` (not DB).

## Database

- **Engine:** MS SQL Server 2019+ (`MHH_DB`), `localhost:1433`
- **Secondary datasource:** Oracle read-only SWAL, `localhost:1521/XEPDB1` (`@Qualifier("swalJdbcTemplate")` in `ExternalDbConfig`)
- **Schema init:** [DB-init/01_schema.sql](DB-init/01_schema.sql)
- **Docker setup:** [DB-init/docker-compose.yml](DB-init/docker-compose.yml)

Entity → table mapping:

| Entity | Table | Notes |
|--------|-------|-------|
| `MsgIncoming` | `MSG_INCOMING` | Search/filter fields only |
| `MsgIncomingTx` | `MSG_INCOMING_TX` | Raw MT/MX content (`@Lob`) |
| `MsgOutgoing` | `MSG_OUTGOING` | Search/filter fields only |
| `MsgOutgoingTx` | `MSG_OUTGOING_TX` | Raw MT/MX content (`@Lob`) |
| `JobConf` | `JOBS_CONF` | |
| `JobLog` | `JOBS_LOGS` | |
| `SysLog` | `SYS_LOGS` | |
| `UserLog` | `USER_LOGS` | |
| `SwiftMessageBase` | (abstract `@MappedSuperclass`) | Shared search fields |
| `SwiftMessageTxBase` | (abstract `@MappedSuperclass`) | Shared TX content fields |

**Search/TX split:** `MSG_INCOMING` and `MSG_OUTGOING` hold indexed search fields. Raw message content (`MT_CONTENT`, `MX_CONTENT` as `NVARCHAR(MAX)`) lives in the separate `_TX` tables, loaded only on detail view. `mtContent`/`mxContent` on `SwiftMessageBase` are `@Transient` — jobs write them to the TX table explicitly.

`MSG_DOWNLOAD` and `MSG_APPROVAL` are in the schema SQL but not yet mapped as entities.

**Logging strategy:** Only `USER_LOGS` in DB (1-year retention via `LogCleanupJob`). Batch/system logs go to `d:/MHH_FILES/LOGS/` via Logback — `batch.log` (90-day rotation), `sys.log` (1-year rotation).

## mhh-ap Web Layer

**Page routes** (served by `RouterController` / `RootController`, templates in `resources/templates/`):

| Path | Template |
|------|----------|
| `/` | → redirect `/login` (dev) or `/sso/login` (prod) |
| `/dashboard` | `dashboard.html` |
| `/incoming` | `incoming.html` |
| `/outgoing` | `outgoing.html` |
| `/tasks/history` | `tasks/history.html` — user activity trail |

**REST endpoints:**

| Path | Controller | Notes |
|------|------------|-------|
| `GET /api/auth/status` | `UserLoginController` | Auth status + devMode flag |
| `GET /api/user-trail` | `UserTrailController` | Paginated `USER_LOGS` with filters (userId, action, status, dateFrom, dateTo) |
| `GET /api/incoming` | `MsgIncomingController` | Paginated MSG_INCOMING with 16 filter params (dateFrom/To, msgType, sender, receiver, amountFrom/To, reference, tag20/21, osnFrom/To, unitCode, amlFlag, amlStatus, flowStatus) |
| `GET /api/incoming/{messageId}/content` | `MsgIncomingController` | Fetch raw MT/MX content from MSG_INCOMING_TX |
| `GET /api/outgoing` | `MsgOutgoingController` | Same as incoming; uses isnFrom/isnTo instead of osn |
| `GET /api/outgoing/{messageId}/content` | `MsgOutgoingController` | Fetch raw MT/MX content from MSG_OUTGOING_TX |

**Frontend stack:** Bootstrap 5.3.2, Bootstrap Icons 1.11.2, Vue 3, Axios (all via CDN in `layout.html`). Custom styles in `static/css/main.css` — white/cyan/light-blue theme using CSS variables.

## Configuration Files

- [mhh-ap/src/main/resources/application.yml](mhh-ap/src/main/resources/application.yml) — port 8080, SQL Server, Thymeleaf, devtools, dev-mode flag
- [mhh-batch/src/main/resources/application.yml](mhh-batch/src/main/resources/application.yml) — file paths (`d:/MHH_FILES/`), Oracle datasource, job refresh rate

## Documentation

- [docs/1_overview.md](docs/1_overview.md) — architecture diagrams and design decisions
- [docs/4_design/README.md](docs/4_design/README.md) — technical specs and build progress
- [docs/7_msg/README.md](docs/7_msg/README.md) — SWIFT message reference (index)
  - [1_message_types.md](docs/7_msg/1_message_types.md) — all MT/MX types and key parse fields
  - [2_mtmx_mapping.md](docs/7_msg/2_mtmx_mapping.md) — MT ↔ MX mapping for incoming and outgoing
  - [3_swal_mhh_mapping.md](docs/7_msg/3_swal_mhh_mapping.md) — SWAL → MHH field mapping with search fields
