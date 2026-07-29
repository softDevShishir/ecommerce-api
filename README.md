# E-Commerce REST API

A production-style REST API for e-commerce applications built with Spring Boot 3.3 and Java 21. Features JWT authentication, a full product catalog with reviews/ratings, shopping cart, order management, and a user wishlist — all with role-based access control.

## 🌐 Live Demo

- **Base URL:** https://ecommerce-api-9236.onrender.com
- **Swagger UI:** https://ecommerce-api-9236.onrender.com/swagger-ui.html
- **OpenAPI spec:** https://ecommerce-api-9236.onrender.com/v3/api-docs

> Hosted on Render's free tier — the instance spins down after inactivity, so the first request after idle time can take 30–60 seconds to wake up.

### Try It with Postman

1. Install [Postman](https://www.postman.com/downloads/)
2. Import `postman/ecommerce-api.postman_collection.json` (Postman → Import)
3. In the collection's **Variables** tab, set `baseUrl` to `https://ecommerce-api-9236.onrender.com`
4. Run **Auth → Register** then **Auth → Login** to get a token — the collection is set up to capture it automatically for subsequent requests
5. Explore the rest of the collection: products, reviews, cart, wishlist, orders

Full field-by-field usage notes are in [API_GUIDE.md](API_GUIDE.md#postman-collection).

### Try It with cURL

```bash
# Register
curl -X POST https://ecommerce-api-9236.onrender.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"Demo1234!","firstName":"Demo","lastName":"User"}'

# Login
curl -X POST https://ecommerce-api-9236.onrender.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@example.com","password":"Demo1234!"}'

# Browse products (public, no token needed)
curl https://ecommerce-api-9236.onrender.com/api/v1/products
```

## 📋 Table of Contents

- [Features](#-features)
- [Live Demo](#-live-demo)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Quick Start](#-quick-start)
- [Installation](#-installation)
- [API Endpoints](#-api-endpoints)
- [Authentication](#-authentication)
- [Database](#-database)
- [Features Documentation](#-features-documentation)
- [Testing](#-testing)
- [Deployment](#-deployment)
- [Security](#-security)
- [Troubleshooting](#-troubleshooting)
- [Contributing](#-contributing)
- [License](#-license)
- [Author](#-author)

## 🚀 Features

### User Management
- Registration with email/password validation, BCrypt password hashing
- Stateless JWT login (HS512), 24-hour expiration by default
- Role-based access control (`ADMIN`, `USER`)
- Profile retrieval and update

### Product Catalog
- Full CRUD (create/update/delete restricted to `ADMIN`)
- Search by name, filter by category, filter by price range (single-filter precedence: name → category → price range)
- Stock quantity tracking, enforced on cart/order operations

### Product Reviews & Ratings ⭐
- One review per user per product (rating 1–5, title, content)
- Aggregated rating summary per product: average rating, total review count, rating distribution (count per star)
- Product list/detail responses include `averageRating` and `totalReviews`
- Users can view, update, and delete only their own reviews; anyone can read a product's reviews

### Wishlist ❤️
- Per-user wishlist, created automatically on first use
- Add/remove products, clear the whole list, check whether a specific product is already saved
- Returns full product details (including rating info) for each wishlisted product

### Shopping Cart
- Add/update/remove items, clear cart
- Real-time total items and total price calculation
- One cart per user, created lazily

### Order Management
- One-click checkout from the current cart (stock is validated and decremented atomically)
- Order history per user, order status workflow: `PENDING → CONFIRMED → SHIPPED → DELIVERED` (or `CANCELLED`)
- Status transitions restricted to `ADMIN`

### Security
- JWT (HS512) authentication, stateless sessions, CSRF disabled (stateless API)
- Spring Security filter chain with explicit per-route rules (see [Security](#-security))
- BCrypt password hashing
- Configurable CORS (origins/methods/headers/credentials/max-age)

### API Documentation
- Springdoc/Swagger UI with a `Bearer` security scheme pre-wired
- Public vs. secured endpoint groupings in the OpenAPI UI

## 📦 Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security, JWT (jjwt, HS512) |
| Persistence | Spring Data JPA, Hibernate, PostgreSQL 15/16 |
| Boilerplate | Lombok |
| API Docs | Springdoc OpenAPI (Swagger UI) |
| Build | Maven |
| Testing | JUnit 5, Spring Boot Test, MockMvc |
| Containerization | Docker, Docker Compose |
| Deployment | Render.com (see [DEPLOYMENT.md](DEPLOYMENT.md)) |

## 🏗️ Project Structure

Code is organized **feature-first** (not layer-first) — each feature module owns its full vertical slice:

```
ecommerce-api/
├── src/main/java/com/shishir/ecommerce/
│   ├── config/                   # Routes (all path constants), SecurityConfig, SwaggerConfig, AuditData
│   ├── security/                 # JwtTokenProvider, JwtAuthenticationFilter, AuthController/Service,
│   │                              # CurrentUserProvider, UserRole enum, entry-point/access-denied handlers
│   ├── exception/                 # One exception type per HTTP error case + GlobalExceptionHandler
│   ├── user/                      # User accounts (controller/service/repository/entity/dto)
│   │   └── wishlist/              # Wishlist sub-module (its own controller/service/repository/entity/dto)
│   ├── product/                   # Product catalog (controller/service/repository/entity/dto)
│   │   └── review/                # Product reviews & ratings sub-module
│   ├── order/                     # Orders (controller/service/repository/entity/dto)
│   └── cart/                      # Shopping cart (controller/service/repository/entity/dto)
├── src/main/resources/
│   ├── application.yml             # Base config (ddl-auto: validate)
│   ├── application-dev.yml         # Dev profile (ddl-auto: create-drop — throwaway local DB only)
│   ├── application-prod.yml        # Prod profile (env-var driven, ddl-auto: validate)
│   └── database/
│       ├── schema.sql               # Source of truth for schema — must match entities exactly
│       └── data.sql                 # Sample seed data
├── src/test/java/                  # Integration tests (MockMvc, real Postgres)
├── postman/                        # Postman collection
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── .env.example
├── README.md                        # This file
├── DEPLOYMENT.md                    # Deployment guide
├── API_GUIDE.md                     # Full endpoint reference with request/response examples
└── FEATURES_GUIDE.md                # Feature-by-feature workflow documentation
```

`product/review` and `user/wishlist` are sub-modules nested under the feature they extend, rather than top-level packages, since they're additive concerns on top of `product` and `user` respectively.

## ⚡ Quick Start

Fastest path to a running API — three commands, no local Postgres or JDK needed:

```bash
git clone https://github.com/softDevShishir/ecommerce-api.git
cd ecommerce-api
cp .env.example .env
docker-compose up -d
```

Then check it's up:

```bash
curl http://localhost:8080/swagger-ui.html
```

Swagger UI: http://localhost:8080/swagger-ui.html

## 📥 Installation

### Prerequisites
- Docker & Docker Compose (recommended path), **or**
- Java 21, Maven 3.8+, PostgreSQL 13+ (manual path)
- Git

### With Docker (Recommended)

```bash
git clone https://github.com/softDevShishir/ecommerce-api.git
cd ecommerce-api
cp .env.example .env
docker-compose up -d          # builds the API image and starts Postgres + the API
docker-compose logs -f app    # tail API logs
docker-compose down           # stop (add -v to also wipe the Postgres volume)
```

`schema.sql`/`data.sql` are mounted into Postgres's `docker-entrypoint-initdb.d` and only run once, against a fresh volume — if you've already run `docker-compose up` before and need a clean schema, run `docker-compose down -v` first.

### Without Docker

1. **Provision PostgreSQL and apply the schema yourself** (the base and `prod` configs use `ddl-auto: validate`, so nothing is created automatically):
```bash
createdb ecommerce_db
psql ecommerce_db < src/main/resources/database/schema.sql
psql ecommerce_db < src/main/resources/database/data.sql   # optional seed data
```

2. **Build and run:**
```bash
mvn clean install
mvn spring-boot:run
```

   To run against a throwaway local database that rebuilds its schema from the entities on every restart (never point this at data you want to keep), activate the `dev` profile instead:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

3. **Access the API:**
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - OpenAPI spec: http://localhost:8080/v3/api-docs

> Building requires a JDK 21 toolchain registered in `~/.m2/toolchains.xml` — see [CLAUDE.md](CLAUDE.md#commands) if `mvn compile` fails with an annotation-processing error under a newer default JDK.

## 🔌 API Endpoints

Full request/response examples for every endpoint below live in **[API_GUIDE.md](API_GUIDE.md)**.

### Authentication
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register a new user |
| POST | `/api/v1/auth/login` | Public | Login, get a JWT |
| GET | `/api/v1/auth/me` | Authenticated | Get the current authenticated user |

### Users
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/users/register` | Public | Register a new user (duplicate of `/auth/register`, both `permitAll`) |
| GET | `/api/v1/users/{id}` | Authenticated | Get a user by ID |
| PUT | `/api/v1/users/{id}` | Authenticated | Update first/last name |
| GET | `/api/v1/users` | Authenticated | List all users — **any authenticated user**, not admin-restricted |

### Products
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/products` | Public | List/search products (`name`, `category`, `minPrice`+`maxPrice` query params) |
| GET | `/api/v1/products/{id}` | Public | Get product by ID |
| POST | `/api/v1/products` | ADMIN | Create product |
| PUT | `/api/v1/products/{id}` | ADMIN | Update product |
| DELETE | `/api/v1/products/{id}` | ADMIN | Delete product |

### Product Reviews & Ratings
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/products/{productId}/reviews` | Public | List a product's reviews, newest first |
| POST | `/api/v1/products/{productId}/reviews` | Authenticated | Create a review (one per user per product) |
| PUT | `/api/v1/products/{productId}/reviews/{reviewId}` | Authenticated (owner) | Update your own review |
| DELETE | `/api/v1/products/{productId}/reviews/{reviewId}` | Authenticated (owner) | Delete your own review |
| GET | `/api/v1/products/{productId}/rating` | Public | Average rating, distribution, and all reviews |
| GET | `/api/v1/reviews/my-reviews` | Authenticated | List all reviews you've written |

### Shopping Cart
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/cart` | Authenticated | Get your cart |
| POST | `/api/v1/cart/items` | Authenticated | Add an item to the cart |
| PUT | `/api/v1/cart/items/{cartItemId}` | Authenticated | Update item quantity |
| DELETE | `/api/v1/cart/items/{cartItemId}` | Authenticated | Remove an item |
| DELETE | `/api/v1/cart` | Authenticated | Clear the cart |

### Wishlist
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/users/wishlist` | Authenticated | Get your wishlist |
| POST | `/api/v1/users/wishlist/items` | Authenticated | Add a product to the wishlist |
| DELETE | `/api/v1/users/wishlist/items/{productId}` | Authenticated | Remove a product from the wishlist |
| GET | `/api/v1/users/wishlist/items/{productId}/exists` | Authenticated | Check if a product is in the wishlist |
| DELETE | `/api/v1/users/wishlist` | Authenticated | Clear the wishlist |

### Orders
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/orders` | Authenticated | Create an order from the current cart |
| GET | `/api/v1/orders` | Authenticated | List your orders |
| GET | `/api/v1/orders/{id}` | Authenticated | Get an order by ID |
| PUT | `/api/v1/orders/{id}/status` | ADMIN | Update order status |

## 🔐 Authentication

The API uses stateless JWT (HS512) authentication. `JwtAuthenticationFilter` reads the bearer token on every request and populates the security context; it never rejects a request itself, so a missing/invalid token simply leaves the request unauthenticated (and `anyRequest().authenticated()` then rejects it downstream with a 401 via `JwtAuthenticationEntryPoint`).

### Register

```bash
curl -X POST https://ecommerce-api-9236.onrender.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "Demo1234!",
    "firstName": "Demo",
    "lastName": "User"
  }'
```

### Login (get a token)

```bash
curl -X POST https://ecommerce-api-9236.onrender.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@example.com",
    "password": "Demo1234!"
  }'
```

Response:
```json
{
  "userId": 1,
  "email": "demo@example.com",
  "role": "USER",
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "expiresIn": 86400
}
```

### Use the token

```bash
curl -H "Authorization: Bearer <your-token>" \
  https://ecommerce-api-9236.onrender.com/api/v1/cart
```

`role` is folded into the JWT's `role` claim and translated to a `ROLE_<UserRole>` Spring Security authority — this is what backs the `ADMIN`-only endpoints above. Password requirement: 8–50 characters (enforced by `@Size` on `UserRegisterRequest`).

## 💾 Database

**Tables** (`src/main/resources/database/schema.sql` is the source of truth — `ddl-auto: validate` checks entities against it but never creates/alters it):

| Table | Purpose |
|---|---|
| `users` | Accounts, `role` (native `userrole` enum: `ADMIN`/`USER`) |
| `products` | Catalog, `name` is `UNIQUE` |
| `orders` | One row per checkout, `status` (native `orderstatus` enum) |
| `order_items` | Line items per order (price snapshotted at order time) |
| `cart` | One row per user (`user_id UNIQUE`) |
| `cart_items` | Line items per cart |
| `product_reviews` | One row per (product, user) pair — `UNIQUE (product_id, user_id)`, `rating` CHECK 1–5 |
| `wishlist` | One row per user (`user_id UNIQUE`) |
| `wishlist_products` | Join table, composite PK `(wishlist_id, product_id)` |

**Relationships:** `orders`/`cart`/`product_reviews`/`wishlist` all cascade-delete when their owning `user` is deleted; `order_items`/`cart_items`/`wishlist_products` cascade-delete with their parent order/cart/wishlist but `RESTRICT`/cascade differently against `products` — order/cart items use `ON DELETE RESTRICT` on `product_id` (a product with order/cart history can't be deleted outright), while `wishlist_products` and `product_reviews` cascade-delete when the product itself is deleted.

**Enums** are native Postgres types (`CREATE TYPE ... AS ENUM`), not `VARCHAR` + check constraint — see [CLAUDE.md](CLAUDE.md#data-layer) for why the schema's enum type names must match the Java enum's simple name exactly, and why `ddl-auto` must stay on `validate` (never `update`) against any database with existing rows.

## ✨ Features Documentation

Full workflow write-ups for each feature (with request/response flow) live in **[FEATURES_GUIDE.md](FEATURES_GUIDE.md)**. Summary:

### User Authentication
Register → BCrypt-hash the password → login exchanges credentials for a 24h JWT → every subsequent request presents that JWT as a bearer token, verified per-request with no server-side session.

### Product Catalog
Public read access (list/search/get), `ADMIN`-only writes. `GET /api/v1/products` applies at most one filter, in this precedence: `name` → `category` → `minPrice`+`maxPrice` → (no filter = all products). Each product response embeds live `averageRating`/`totalReviews` computed from `product_reviews`.

### Product Reviews & Ratings ⭐ NEW
Any authenticated user can leave exactly one review per product (`UNIQUE (product_id, user_id)` enforced at the DB level, surfaced as `409 CONFLICT` on a duplicate attempt). `GET /products/{id}/rating` returns the average, the total count, and a full 1–5 star distribution — useful for building a rating breakdown UI without pulling every review.

### Wishlist ❤️ NEW
A user's wishlist is created lazily on the first `POST .../wishlist/items` call. Adding an already-wishlisted product returns `400`; removing/clearing are idempotent-friendly (404 only if the wishlist itself doesn't exist yet). The wishlist response embeds full `ProductResponse` objects, so a client can render the saved-items page without a second round trip per product.

### Shopping Cart
One cart per user, created on first use. `open-in-view` is disabled, so cart items are fetched via a dedicated query rather than the entity's lazy `@OneToMany` — see [CLAUDE.md](CLAUDE.md#data-layer) if extending this pattern.

### Order Management
`POST /api/v1/orders` runs stock validation, stock decrement, total calculation, and cart clearing inside one `@Transactional` boundary — a failure partway through (e.g. insufficient stock on the second item) rolls back everything already applied in that request, including stock already decremented for the first item.

## 🧪 Testing

```bash
mvn test                                          # full suite
mvn test -Dtest=ProductControllerIntegrationTest  # single class
mvn test -Dtest=ClassName#methodName              # single method
```

Requires a real Postgres reachable via the settings in `src/test/resources/application.yml` (a standalone config, not merged with `src/main/resources/application.yml`) — see [CLAUDE.md](CLAUDE.md#running-the-integration-tests) for how to stand one up locally, or rely on CI's provisioned instance.

**Current coverage: 29 integration tests**, all passing:

| Test class | Tests | Covers |
|---|---|---|
| `AuthControllerIntegrationTest` | 8 | Register (success + duplicate email), login (success + bad password + unknown user), `/me` (valid/missing/invalid token) |
| `ProductControllerIntegrationTest` | 8 | List, get by ID (found + 404), search by name/category/price range, create (valid + invalid → 400) |
| `CartControllerIntegrationTest` | 6 | Get cart, add item, add with insufficient stock → 400, update quantity, remove item, clear cart |
| `OrderControllerIntegrationTest` | 7 | Order creation, retrieval, status updates, and related failure cases |

> ⚠️ **Gap:** reviews and wishlist have no integration tests yet — both modules were added after the test suite above and aren't covered. Contributions adding `ReviewControllerIntegrationTest` / `WishlistControllerIntegrationTest` would close this.

## 🚀 Deployment

See **[DEPLOYMENT.md](DEPLOYMENT.md)** for the full guide (Render, Railway, AWS, Docker Hub, backups, scaling).

Currently deployed on **Render.com**:
- Live URL: https://ecommerce-api-9236.onrender.com
- Managed PostgreSQL, auto-deploys from the `main` branch
- `SPRING_PROFILES_ACTIVE=prod`, secrets set via Render's environment variable UI

**Note:** CI (`.github/workflows/ci.yml`) invokes `./mvnw`, but no Maven wrapper is committed to this repo — CI will fail until one is added (see [CLAUDE.md](CLAUDE.md#commands)).

## 🔒 Security

`SecurityConfig` enforces these rules explicitly (see `SecurityConfig.filterChain` — this table is the actual source of truth, not the Swagger group labels, which mirror a different grouping):

| Rule | Endpoints |
|---|---|
| Public (`permitAll`) | `POST /auth/login`, `POST /auth/register`, `POST /users/register`, `GET /products`, `GET /products/{id}`, `GET /products/{productId}/reviews`, `GET /products/{productId}/rating`, `/swagger-ui/**`, `/v3/api-docs/**` |
| `ADMIN` only | `POST /products`, `PUT /products/{id}`, `DELETE /products/**` (except review deletion, explicitly carved out first), `PUT /orders/{id}/status` |
| Authenticated (any role) | Everything else, including `GET /users` (list all users) — reachable by any logged-in user, not just `ADMIN`, despite the endpoint name |

Other measures in place:
- BCrypt password hashing (never stored/logged in plaintext)
- Stateless sessions (`SessionCreationPolicy.STATELESS`), CSRF disabled (safe for a token-only API with no cookie auth)
- CORS fully driven by `app.cors.*` properties — not hardcoded
- `/actuator/**` is **not** `permitAll`'d — don't rely on `/actuator/health` for health checks, use `/swagger-ui.html`
- Bean-validation (`@NotBlank`, `@Email`, `@Size`, `@Min`/`@Max`) on every request DTO, surfaced as `400 VALIDATION_ERROR`
- Errors never leak stack traces — the catch-all handler returns only the exception's simple class name as `details`

**Best practices when running this yourself:**
- Never commit `.env` (already gitignored)
- Use a JWT secret ≥256 bits — `openssl rand -base64 32` generates a good one
- Rotate `JWT_SECRET` periodically; rotating invalidates all previously issued tokens immediately
- Keep dependencies updated (`mvn versions:display-dependency-updates`)

## 🆘 Troubleshooting

**API not responding / 502 on Render**
- Free-tier Render instances spin down after inactivity — the first request can take 30–60s while it wakes up.
- Check `docker-compose logs -f app` (local) or the platform's log viewer (deployed).

**`401 Unauthorized` on every request**
- Confirm the header is exactly `Authorization: Bearer <token>` (note the space, no quotes around the token).
- Check the token hasn't expired (`expiresIn` in the login response, default 86400s = 24h).
- `JWT_SECRET` must be identical across all running instances — a secret change invalidates every token issued under the old one.

**`403 Forbidden` on an ADMIN endpoint**
- `POST/PUT /products`, `DELETE /products/{id}`, and `PUT /orders/{id}/status` require `ROLE_ADMIN` — use `{{adminToken}}` (or the seeded `admin@example.com` / `admin123` account) for these. See the [Security](#-security) table above for the full list.

**Database connection failed**
- Verify `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD` (or `SPRING_DATASOURCE_*`, which always wins over profile YAML).
- `docker-compose up` publishes Postgres on the host port from `DB_PORT` in `.env` (default `5434`, chosen to avoid clashing with a native Postgres install on 5432).

**App fails to start with a schema validation error**
- The base and `prod` profiles use `ddl-auto: validate` — apply `src/main/resources/database/schema.sql` to the target database yourself before first boot; it is not applied automatically outside of `docker-compose`'s fresh-volume init.

**`mvn compile` fails with an annotation processing error**
- Lombok 1.18.32 (pulled in transitively) doesn't support annotation processing under newer JDKs. Register a JDK 21 toolchain in `~/.m2/toolchains.xml` — see [CLAUDE.md](CLAUDE.md#commands).

**Swagger UI not loading**
- Clear the browser cache and hard-reload.
- Confirm the exact path: `/swagger-ui.html` (not `/swagger-ui/index.html` directly, though that also works).

**409 on creating a review / 400 on adding to wishlist**
- Both are intentional: one review per user per product (`UNIQUE (product_id, user_id)`), and a product can't be added to the same wishlist twice.

## 🤝 Contributing

This is a portfolio project, open to use as a reference:
1. Fork and clone the repository
2. Follow the [Installation](#-installation) steps
3. Read [CLAUDE.md](CLAUDE.md) for the full architecture/conventions rundown before making changes
4. Review commit history to see the development progression
5. Open an issue or PR with questions or improvements — see the [Testing](#-testing) gap above (reviews/wishlist test coverage) for a good first contribution

## 📄 License

[MIT License](LICENSE) — free to use and modify.

## 👤 Author

**Shishir**
- GitHub: [github.com/softDevShishir](https://github.com/softDevShishir)
- Email: softdevshishir@gmail.com
- Location: Dhaka, Bangladesh
- 3.5 years of backend development experience

Backend Engineer specializing in REST APIs, Microservices, and Security.

## 🎯 Future Enhancements

- [ ] Integration tests for reviews and wishlist
- [ ] Payment gateway integration (Stripe, PayPal)
- [ ] Email notifications for orders
- [ ] Pagination on `GET /products`, `GET /users`, `GET /orders` (all currently unbounded lists)
- [ ] API rate limiting
- [ ] Redis caching layer
- [ ] Real-time notifications (WebSocket)
- [ ] Admin dashboard
- [ ] Maven wrapper committed, so `.github/workflows/ci.yml`'s `./mvnw` actually works

## 📞 Support

- Open a GitHub issue
- Email: softdevshishir@gmail.com
- Interactive docs: `/swagger-ui.html`

---

**Happy coding! 🚀**
