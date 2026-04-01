# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MHH (Message History Hub)** is a financial-grade SWIFT message management platform for banking institutions. It centralizes, parses, and audits SWIFT electronic messages (MT103, MT202, MX/ISO 20022). Core compliance feature: **Maker-Checker dual approval** workflow.

## Build & Run Commands

This is a Maven multi-module project (Java 21, Spring Boot 3.4.1).

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

**Dev login bypass:** With `mhh.auth.dev-mode: true` (default), use `/dev/login` to skip SSO.

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

## Data Flows

```
SWAL Sync:  Oracle SWAL DB (read-only) → SwallowSyncJob → MSG_SWAL_SYNC → MSG_HISTORY
PDF Import: /data/MX|MT/ → PdfImportJob → ParserFactory (strategy) → MSG_HISTORY + /data/ARCHIVE
Download:   User request → MSG_DOWNLOAD (PENDING) → Manager approval → ReservationMergeJob → ZIP → /data/TEMP
```

## Key Architectural Patterns

1. **Strategy Pattern for Parsers** — `ParserFactory` in `mhh-core` selects the correct `PdfParser` implementation based on message type. To add support for a new SWIFT message format, implement `PdfParser` and register it in `ParserFactory`.

2. **Maker-Checker Workflow** — Sensitive operations (branch downloads, parameter changes) go through `MSG_APPROVAL` table. Roles: `BRANCH_MAKER`, `BRANCH_CHECKER`, `PARAM_MAKER`, `PARAM_CHECKER`, scoped per `country_code`.

3. **Dynamic Job Scheduling** — Batch cron expressions are stored in the `JOBS_CONF` database table. The scheduler refreshes every 20 minutes (`mhh.jobs.refresh-rate`). Jobs can be enabled/disabled without restarting the application.

4. **AOP-based Auditing** — User actions are automatically written to `USER_LOGS` via Spring AOP in `mhh-core`. System errors go to `SYS_LOGS`.

## Database

- **Engine:** MS SQL Server 2019+ (`MHH_DB`)
- **Secondary datasource:** Oracle (read-only access to SWAL system)
- **Schema init:** [DB-init/01_schema.sql](DB-init/01_schema.sql)
- **Docker setup:** [DB-init/docker-compose.yml](DB-init/docker-compose.yml)

Key tables: `MSG_HISTORY`, `MSG_DOWNLOAD`, `MSG_APPROVAL`, `USER`, `USER_ROLE`, `JOBS_CONF`, `JOBS_LOGS`, `SYS_LOGS`, `USER_LOGS`.

Log retention: `JOBS_LOGS` 3 months, `SYS_LOGS` / `USER_LOGS` 1 year (enforced by `LogCleanupJob`).

## Configuration Files

- [mhh-ap/src/main/resources/application.yml](mhh-ap/src/main/resources/application.yml) — web app config (port, security, Thymeleaf, i18n)
- [mhh-batch/src/main/resources/application.yml](mhh-batch/src/main/resources/application.yml) — batch config (PDF paths, Oracle datasource, job refresh rate)
- `mhh-ap` uses `spring.jpa.hibernate.ddl-auto: update`; `mhh-batch` uses `none` (schema is read-only from batch's perspective)

## Documentation

Detailed docs are in [docs/](docs/):
- [docs/1_overview.md](docs/1_overview.md) — architecture diagrams and design decisions
- [docs/4_design/README.md](docs/4_design/README.md) — technical specs and build progress
- [docs/6_MTMXtype.md](docs/6_MTMXtype.md) — SWIFT message type specifications
- [docs/7_todo.md](docs/7_todo.md) — ongoing tasks and implementation status
