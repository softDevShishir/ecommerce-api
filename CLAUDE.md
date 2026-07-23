# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Maintaining this file

Keep this file up to date as the codebase evolves. When you make a significant change to architecture, conventions, commands, or anything else documented here, update CLAUDE.md accordingly — no need to ask first.

## Code comments

Don't add comments that just restate what the code does (e.g. a Javadoc paragraph describing a method whose name and body already make it obvious). Only comment where the *why* isn't visible from the code itself — a non-obvious constraint, a workaround, a decision that would otherwise look arbitrary. When editing code, remove existing comments that don't meet this bar.

## Project

Production-style e-commerce REST API: Spring Boot 3.3 / Java 21, Spring Security with JWT auth, Spring Data JPA over PostgreSQL, Lombok. See README.md for the full feature list and API endpoint overview.

## Commands

There is no committed Maven wrapper (`mvnw`/`.mvn`) — use `mvn` directly. Note CI's workflow (`.github/workflows/ci.yml`) invokes `./mvnw`, which will fail unless a wrapper is added; be aware of this mismatch if touching CI.

```bash
mvn clean install          # full build
mvn spring-boot:run        # run locally (needs Postgres — see below)
mvn test                   # run tests
mvn test -Dtest=ClassName                     # single test class
mvn test -Dtest=ClassName#methodName          # single test method
mvn verify                 # build + tests, what CI runs
```

`src/test/java/.../ecommerce/` currently only contains a `.gitkeep` — no tests exist yet.

### Local database

```bash
docker-compose up -d           # Postgres + API, schema.sql auto-loaded into a fresh volume only
docker-compose down
```

For `mvn spring-boot:run` against a manually-created `ecommerce_db` Postgres instance, apply `src/main/resources/database/schema.sql` yourself first — the `default` and `prod` Spring profiles use `ddl-auto: validate` (Hibernate checks the schema but never creates/alters it). Only the `dev` profile (`application-dev.yml`) uses `ddl-auto: update`.

Key env vars (see `application.yml`): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `CORS_ALLOWED_ORIGINS`.

## Architecture

### Module layout

Code is organized **feature-first**, not layer-first: `user/`, `product/`, `order/`, `cart/` each contain their own `controller/`, `service/`, `repository/`, `entity/`, `dto/`. Cross-cutting concerns live in three shared packages instead:

- `config/` — `Routes` (all endpoint path constants), `SecurityConfig`, `AuditData` (mapped-superclass for `createdAt`/`updatedAt`, extended by every entity).
- `security/` — `JwtTokenProvider` (issues/parses HS512 JWTs, key = SHA-512 of `app.jwt.secret`), `JwtAuthenticationFilter` (populates `SecurityContextHolder` from the bearer token, never rejects the request itself), `JwtAuthenticationEntryPoint`/`JwtAccessDeniedHandler` (401/403 JSON responses), `CurrentUserProvider` (resolves the authenticated `User` — assumes the security principal name is the user's email), `UserRole` enum (`ADMIN`, `USER`).
- `exception/` — one exception type per HTTP error case (`ResourceNotFoundException`, `DuplicateResourceException`, `BadRequestException`, `UnauthorizedException`, `ValidationException`) plus `GlobalExceptionHandler` (`@RestControllerAdvice`) that maps each to a shared `ExceptionResponse` JSON shape (`timestamp`, `status`, `error`, `message`, `path`, `details`).

### Routing

**Every** `@RequestMapping`/`@GetMapping`/etc. path comes from a constant in `config/Routes.java` — controllers never inline path strings, and there are no class-level `@RequestMapping` prefixes. Add new endpoints there first.

### Security model

`SecurityConfig` is fully stateless (`SessionCreationPolicy.STATELESS`, CSRF disabled). Public endpoints are enumerated explicitly (auth login, user registration, `GET` products); `DELETE /api/v1/products/**` and `PUT /api/v1/orders/{id}/status` require `ROLE_ADMIN`; everything else just requires authentication. `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter` and derives the caller's authority from the `role` claim in the JWT (`ROLE_<UserRole>`). CORS origins/methods/headers/credentials/max-age are all driven by `app.cors.*` properties, not hardcoded.

### Data layer

- Entities use Lombok `@Data` + `@EqualsAndHashCode(callSuper = true)`, extend `AuditData`, and exclude relationship fields from `equals`/`hashCode`/`toString` (`@ToString.Exclude`/`@EqualsAndHashCode.Exclude`) to avoid recursive entity graphs.
- Enums (`role`, order `status`) are persisted with `@Enumerated(EnumType.STRING)` and mirrored by a `CHECK` constraint in `schema.sql` — the schema file is the source of truth for constraints Hibernate's `validate` mode checks against, so schema and entity changes must be kept in sync manually.
- Services are `@Transactional` by default, with `@Transactional(readOnly = true)` on read-only methods; they throw the `exception/` types directly rather than returning `Optional`/error codes.
- Controllers map entities to response DTOs manually (Lombok `@Builder`), no MapStruct/ModelMapper.

### Adding a new module or endpoint

Follow the existing per-feature package shape (`controller`/`service`/`repository`/`entity`/`dto`), add path constants to `Routes`, wire authorization rules into `SecurityConfig.filterChain`, and add any new SQL constraints to `database/schema.sql` (remember `ddl-auto: validate` outside `dev`).
