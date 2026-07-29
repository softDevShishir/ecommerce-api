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

The `@SpringBootApplication` entry point is `EcommerceApiApplication` (root `com.shishir.ecommerce` package) — `@SpringBootTest` needs it to find a configuration class, so don't remove/relocate it without checking test bootstrapping still works.

The `pom.xml` pins compilation to a JDK 21 toolchain via `maven-toolchains-plugin`, resolved from `~/.m2/toolchains.xml` (`<jdk><version>21</version>` → a local JDK 21 install) — Lombok 1.18.32, pulled in transitively via the Spring Boot 3.3 parent, doesn't support annotation processing under newer JDKs (e.g. JDK 25), so `mvn` running under a newer default `JAVA_HOME` would otherwise fail to compile. This means `mvn compile`/`test`/etc. need a machine-local `toolchains.xml` entry for JDK 21 to work — CI doesn't have one configured, so if the `./mvnw` gap above ever gets fixed, CI will also need a `toolchains.xml` (or the plugin removed in favor of a JDK-21-pinned runner).

### Local database

```bash
docker-compose up -d           # Postgres + API, schema.sql/data.sql auto-loaded into a fresh volume only
docker-compose down
```

`docker-compose.yml` mounts `schema.sql`/`data.sql` into Postgres's `docker-entrypoint-initdb.d` as `01-schema.sql`/`02-data.sql` — the numeric prefixes force alphabetical run order so `data.sql` inserts after the tables exist. This only runs once, against a fresh `postgres_data` volume.

The base (no-profile) config and `application-prod.yml` both use `ddl-auto: validate` (Hibernate checks the schema against the entities but never creates/alters it) — apply `src/main/resources/database/schema.sql` yourself first against any persistent database before running with no profile or with `prod` active. `docker-compose.yml`'s `app` service does this automatically: its `postgres` service mounts `schema.sql`/`data.sql` into `docker-entrypoint-initdb.d`, applied once against a fresh volume before the app ever starts. Only `application-dev.yml` uses `ddl-auto: create-drop` (wipes and rebuilds the schema from the entities on every startup) — safe specifically because nothing persists across restarts on that profile, so use it for throwaway local iteration, never against a database you want to keep data in. `dev` and `prod` are standalone `application-{profile}.yml` files (Spring's per-profile file convention), not sections inside `application.yml`.

**Do not switch the base config or `docker-compose.yml`'s `SPRING_JPA_HIBERNATE_DDL_AUTO` back to `update`.** Hibernate's `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` (see Data layer below) manages its native-enum types by unconditionally emitting `DROP TYPE ... CASCADE` + `CREATE TYPE` on *every* startup, update mode or not, not only when something actually changed — decompiling `PostgreSQLEnumJdbcType` confirms this is unconditional, not a naming-mismatch bug. Against a table that already has rows, the cascade drops the dependent column and the follow-up `ALTER TABLE ADD COLUMN ... NOT NULL` then fails (no default for existing rows), corrupting the table. `validate` mode never emits DDL, so it can't hit this; `create-drop` is fine because it never encounters existing rows in the first place.

Key env vars (see `application-prod.yml`): `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `CORS_ALLOWED_ORIGINS`. The base (no-profile) `application.yml` hardcodes its datasource URL/credentials instead of reading `DB_HOST` etc. — those vars only take effect through `application-prod.yml`'s placeholders, or via Spring's own env-var property override (`SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`, which beat *any* profile's yml regardless of placeholders — this is what `docker-compose.yml` actually relies on). `.env.example` documents the full local-dev set (`docker-compose.yml` reads a `.env` file automatically for its `${VAR}` substitutions).

`SecurityConfig` does not `permitAll` `/actuator/**`, so health checks use `/swagger-ui.html` (public) instead of `/actuator/health` — both `Dockerfile`'s `HEALTHCHECK` and `docker-compose.yml`'s `app` healthcheck rely on this.

### Running the integration tests

`src/test/resources/application.yml` is a standalone config (it fully replaces `src/main/resources/application.yml` on the test classpath, not merges with it) pointing at a separate `ecommerce_test` database with `ddl-auto: create-drop`, so tests manage their own schema and never touch dev data. You need a real Postgres reachable with those settings — e.g. `docker run -d -e POSTGRES_DB=ecommerce_test -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16-alpine`, or override `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD` env vars to point elsewhere (CI's `ci.yml` already provisions exactly this). `mvn test` picks it up with no extra flags once the database is reachable.

On this machine, port 5432 is already bound by a native `postgresql.service` (unrelated), so the test DB container above was instead run as `docker run -d --name ecommerce_test_pg -e POSTGRES_DB=ecommerce_test -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5433:5432 postgres:16-alpine`, with `DB_PORT=5433` exported when invoking `mvn test`. That container is left running for reuse; `docker stop/rm ecommerce_test_pg` to tear it down.

## Architecture

### Module layout

Code is organized **feature-first**, not layer-first: `user/`, `product/`, `order/`, `cart/` each contain their own `controller/`, `service/`, `repository/`, `entity/`, `dto/`. Cross-cutting concerns live in three shared packages instead:

- `config/` — `Routes` (all endpoint path constants), `SecurityConfig`, `AuditData` (mapped-superclass for `createdAt`/`updatedAt`, extended by every entity).
- `security/` — `JwtTokenProvider` (issues/parses HS512 JWTs, key = SHA-512 of `jwt.secret`), `JwtAuthenticationFilter` (populates `SecurityContextHolder` from the bearer token, never rejects the request itself), `JwtAuthenticationEntryPoint`/`JwtAccessDeniedHandler` (401/403 JSON responses), `CurrentUserProvider` (resolves the authenticated `User` — assumes the security principal name is the user's email), `UserRole` enum (`ADMIN`, `USER`).
- `exception/` — one exception type per HTTP error case (`ResourceNotFoundException`, `DuplicateResourceException`, `BadRequestException`, `UnauthorizedException`, `ValidationException`) plus `GlobalExceptionHandler` (`@RestControllerAdvice`) that maps each to a shared `ExceptionResponse` JSON shape (`timestamp`, `status`, `error`, `message`, `path`, `details`).

### Routing

**Every** `@RequestMapping`/`@GetMapping`/etc. path comes from a constant in `config/Routes.java` — controllers never inline path strings, and there are no class-level `@RequestMapping` prefixes. Add new endpoints there first.

### Security model

`SecurityConfig` is fully stateless (`SessionCreationPolicy.STATELESS`, CSRF disabled). Public endpoints are enumerated explicitly (auth login, user registration, `GET` products); `POST`/`PUT /api/v1/products/**`, `DELETE /api/v1/products/**`, and `PUT /api/v1/orders/{id}/status` require `ROLE_ADMIN`; everything else just requires authentication. `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter` and derives the caller's authority from the `role` claim in the JWT (`ROLE_<UserRole>`). CORS origins/methods/headers/credentials/max-age are all driven by `app.cors.*` properties, not hardcoded.

### Data layer

- Entities use Lombok `@Data` + `@EqualsAndHashCode(callSuper = true)`, extend `AuditData`, and exclude relationship fields from `equals`/`hashCode`/`toString` (`@ToString.Exclude`/`@EqualsAndHashCode.Exclude`) to avoid recursive entity graphs.
- Enums (`role`, order `status`) are persisted as native Postgres enum types (`UserRole`/`OrderStatus`, folded by Postgres to `userrole`/`orderstatus` since they're unquoted in `schema.sql`), mapped via `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` — Hibernate's named-enum JDBC type, which infers the Postgres type name from the Java enum's **raw simple name**, not a snake_case conversion (confirmed by decompiling `PostgreSQLEnumJdbcType`; there's no naming-strategy hook for it). `schema.sql`'s type names must match that exactly, or `ddl-auto: validate` refuses to start — see the `ddl-auto` warning above for why `update` is worse than a refusal here. The schema file is the source of truth for constraints Hibernate's `validate` mode checks against, so schema and entity changes must be kept in sync manually. `products.name` is `UNIQUE` (backs `data.sql`'s `ON CONFLICT (name)` upsert).
- Services are `@Transactional` by default, with `@Transactional(readOnly = true)` on read-only methods; they throw the `exception/` types directly rather than returning `Optional`/error codes.
- Controllers map entities to response DTOs manually (Lombok `@Builder`), no MapStruct/ModelMapper.
- `open-in-view` is disabled, so lazy `@OneToMany` associations (e.g. `Cart.cartItems`) can't be read after their owning `@Transactional` service method returns. `CartController` deliberately avoids touching `cart.getCartItems()` and instead queries items separately via `CartService.getCartItems()` — follow that pattern (a dedicated query) rather than reaching into a lazy collection from a controller, and rather than reassigning a `cascade = ALL, orphanRemoval = true` collection field directly (breaks Hibernate's orphan-removal tracking).

### Adding a new module or endpoint

Follow the existing per-feature package shape (`controller`/`service`/`repository`/`entity`/`dto`), add path constants to `Routes`, wire authorization rules into `SecurityConfig.filterChain`, and add any new SQL constraints to `database/schema.sql` (remember `ddl-auto: validate` under the `prod` profile).

### API documentation (Swagger / OpenAPI)

`config/SwaggerConfig.java` configures Springdoc: an `OpenAPI` bean with title/contact/license/servers plus a `Bearer Authentication` JWT `SecurityScheme`, and two `GroupedOpenApi` beans (`public-apis`, `secured-apis`) that split docs by path prefix — these groupings mirror the endpoint list in the task spec, not the actual per-method rules in `SecurityConfig` (e.g. `POST /api/v1/users/register` is grouped under "secured" even though it's `permitAll`), so don't treat group membership as a security source of truth. `SecurityConfig.filterChain` explicitly `permitAll`s `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` — new cross-cutting public paths need the same treatment. UI at `/swagger-ui.html`, raw spec at `/v3/api-docs`. Controllers use `@Tag`/`@Operation`/`@ApiResponse` (`io.swagger.v3.oas.annotations.*`); response DTOs use `@Schema`. Springdoc v2's `GroupedOpenApi` lives at `org.springdoc.core.models.GroupedOpenApi` (not `org.springdoc.core.GroupedOpenApi`, which is the v1 package and won't compile against the v2 starter this project uses).
