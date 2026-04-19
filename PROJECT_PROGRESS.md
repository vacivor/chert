# Chert Project Progress

## Current Status

The project has moved past the "no real code yet" stage and is now in a "first-pass MVP skeleton is in place, but core protocol and stability are not fully closed" stage.

Current rough progress:

- Engineering structure: 80%
- Server core capability: 55%
- Client and starter capability: 65%
- Production readiness: 25%

## Module Progress

### `chert-server`

Current status: first-pass backbone is in place.

Already present:

- Domain objects, JPA repositories, application services, admin APIs, and open APIs
- Application, environment, config resource, config content, config entry, release, rollback, diff, and release message first-pass implementations
- DTO-based admin interfaces
- Error code structure split into common and domain-specific error codes

Representative files:

- [ConfigReleaseService.java](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/java/io/vacivor/chert/server/application/config/ConfigReleaseService.java)
- [ConfigResourceService.java](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/java/io/vacivor/chert/server/application/config/ConfigResourceService.java)
- [ConfigContentAdminController.java](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/java/io/vacivor/chert/server/interfaces/admin/config/ConfigContentAdminController.java)
- [ConfigReleaseAdminController.java](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/java/io/vacivor/chert/server/interfaces/admin/config/ConfigReleaseAdminController.java)

Gaps:

- Full test run is not green yet
- OpenAPI still has behavioral gaps
- Long-poll protocol is not yet aligned with the agreed release-message cursor model
- Domain model still has a few mismatches with the agreed design

### `chert-client`

Current status: usable first-pass client exists.

Already present:

- Fetch config
- Fetch entries
- Update entries
- Polling-based refresh check
- Long-poll watch loop
- Local cache fallback
- Listener registration by config name

Representative files:

- [ChertClient.java](/Users/valurno/Programming/vacivor/chert/chert-client/src/main/java/io/vacivor/chert/client/ChertClient.java)
- [HttpChertClient.java](/Users/valurno/Programming/vacivor/chert/chert-client/src/main/java/io/vacivor/chert/client/HttpChertClient.java)

Gaps:

- Behavior still depends on a server-side notification contract that is not fully correct yet
- Some example wiring is not fully aligned with the current config object shape

### `chert-spring-boot-starter`

Current status: first-pass Spring integration is in place.

Already present:

- Auto-configuration
- `spring.config.import` support
- `@ChertConfig` listener support
- runtime refresh for `@Value`
- runtime refresh for `@ConfigurationProperties`

Representative files:

- [ChertAutoConfiguration.java](/Users/valurno/Programming/vacivor/chert/chert-spring-boot-starter/src/main/java/io/vacivor/chert/starter/ChertAutoConfiguration.java)
- [ChertConfigDataLoader.java](/Users/valurno/Programming/vacivor/chert/chert-spring-boot-starter/src/main/java/io/vacivor/chert/starter/ChertConfigDataLoader.java)
- [ChertConfigBeanPostProcessor.java](/Users/valurno/Programming/vacivor/chert/chert-spring-boot-starter/src/main/java/io/vacivor/chert/starter/ChertConfigBeanPostProcessor.java)
- [ChertConfigRefresher.java](/Users/valurno/Programming/vacivor/chert/chert-spring-boot-starter/src/main/java/io/vacivor/chert/starter/ChertConfigRefresher.java)

Gaps:

- Refresh semantics still need hardening around deleted keys and failure handling
- Integration confidence is moderate, not yet production-grade

### `example`

Current status: examples exist, but not all paths are fully verified.

Already present:

- Spring Boot starter example
- config probe controller
- direct client example
- Micronaut example

Representative files:

- [application.yaml](/Users/valurno/Programming/vacivor/chert/example/src/main/resources/application.yaml)
- [ConfigProbeController.java](/Users/valurno/Programming/vacivor/chert/example/src/main/java/io/vacivor/chert/example/starter/ConfigProbeController.java)
- [DirectClientExample.java](/Users/valurno/Programming/vacivor/chert/example/src/main/java/io/vacivor/chert/example/client/DirectClientExample.java)

Gaps:

- Direct client example is likely out of sync with the current `ChertClientConfig` constructor
- Example paths have not all been verified end-to-end

### Additional Observation

The repository currently also includes a `chert-micronaut-starter` module:

- [chert-micronaut-starter/build.gradle.kts](/Users/valurno/Programming/vacivor/chert/chert-micronaut-starter/build.gradle.kts)

This means the project has already expanded beyond the originally planned four-module scope.

## Confirmed Risks and Incomplete Areas

### 1. Build and test environment is not hermetic

The server defaults to a shared PostgreSQL instance and enables schema mutation:

- [application.yaml](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/resources/application.yaml)

Current impact:

- `./gradlew test` is not fully green
- Local startup and test execution are not safely reproducible

### 2. Long-poll notification protocol is not yet the agreed design

Current notification flow is still in-memory watcher based:

- [ConfigNotificationService.java](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/java/io/vacivor/chert/server/interfaces/openapi/ConfigNotificationService.java)
- [ConfigNotificationOpenApiController.java](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/java/io/vacivor/chert/server/interfaces/openapi/ConfigNotificationOpenApiController.java)

What is missing:

- `lastMessageId`
- incremental cursor semantics
- immediate scan against `release_message`
- no-gap notification guarantee

### 3. OpenAPI entries update semantics are unsafe for a config center

Current implementation only upserts submitted keys:

- [ConfigOpenApiController.java](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/java/io/vacivor/chert/server/interfaces/openapi/ConfigOpenApiController.java)

Impact:

- omitted keys remain in storage
- release snapshots can retain stale keys

### 4. Domain model is not fully aligned with the agreed resource/content/release model

Current state:

- `ConfigResource` still holds `format`
- `ConfigContent` does not yet hold `format`

Relevant files:

- [ConfigResource.java](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/java/io/vacivor/chert/server/domain/config/ConfigResource.java)
- [ConfigContent.java](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/java/io/vacivor/chert/server/domain/config/ConfigContent.java)
- [ConfigRelease.java](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/java/io/vacivor/chert/server/domain/config/ConfigRelease.java)

### 5. Module boundary is still partially inverted

The server still directly uses client-side response DTOs:

- [ConfigOpenApiController.java](/Users/valurno/Programming/vacivor/chert/chert-server/src/main/java/io/vacivor/chert/server/interfaces/openapi/ConfigOpenApiController.java)
- [chert-server/build.gradle.kts](/Users/valurno/Programming/vacivor/chert/chert-server/build.gradle.kts)

## Priority Plan

### P0

These are the tasks that should be done first.

1. Make local startup and tests reproducible
2. Make `./gradlew test` green
3. Fix entries update semantics to support full replacement correctly
4. Replace the current in-memory watch behavior with release-message cursor based long-polling
5. Fix the direct client example so it actually matches the current client API

### P1

These tasks should come right after the system is stable enough to run.

1. Align the domain model with the agreed design
2. Move server OpenAPI DTOs away from `chert-client`
3. Add integration tests for release fetch, notification flow, and rollback flow
4. Normalize entity timestamp handling
5. Finish the `name` / `configName` rename consistently across all modules

### P2

These tasks improve governance, safety, and operability.

1. Complete audit and diff workflows
2. Add sensitive-config handling capabilities
3. Improve indexes and query performance
4. Harden starter refresh behavior
5. Fill in README and usage documentation

## Suggested Next Three Tasks

If we only pick three things to do next, the highest-value sequence is:

1. Make tests and local startup reproducible
2. Rebuild notification protocol around `lastMessageId`
3. Fix entries full-replacement semantics

Once these three are done, the project will move from "MVP skeleton exists" to "first usable version with more trustworthy behavior".
