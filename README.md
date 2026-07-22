# E-Commerce API

A production-ready RESTful e-commerce backend built with **Spring Boot 3.x**, **Spring Security**, **Spring Data JPA**, and **PostgreSQL**.

## Tech Stack

| Layer       | Technology                         |
|-------------|-------------------------------------|
| Framework   | Spring Boot 3.3                    |
| Security    | Spring Security + JWT (jjwt 0.12)  |
| Persistence | Spring Data JPA + Hibernate        |
| Database    | PostgreSQL 16                      |
| Build       | Maven (Java 21)                    |
| Container   | Docker + Docker Compose            |

## Project Structure

```
src/main/java/com/shishir/ecommerce/
├── EcommerceApiApplication.java
├── entity/          # JPA entities (User, Product, Order, OrderItem, Cart)
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic
├── controller/      # REST controllers
├── config/          # App-level configuration beans
└── security/        # JWT filter, UserDetailsService, SecurityConfig
```

## Getting Started

### Prerequisites
- Java 21
- Maven 3.9+
- Docker & Docker Compose (or a running PostgreSQL instance)

### Run with Docker Compose

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080/api/v1`.

### Run Locally

1. Start PostgreSQL and create the database:

```bash
psql -U postgres -c "CREATE DATABASE ecommerce_db;"
psql -U postgres -d ecommerce_db -f src/main/resources/database/schema.sql
```

2. Export environment variables (or edit `application.yml`):

```bash
export DB_HOST=localhost
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=your-256-bit-secret
```

3. Start the application:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Environment Variables

| Variable                  | Default                        | Description                     |
|---------------------------|--------------------------------|---------------------------------|
| `DB_HOST`                 | `localhost`                    | PostgreSQL host                 |
| `DB_PORT`                 | `5432`                         | PostgreSQL port                 |
| `DB_NAME`                 | `ecommerce_db`                 | Database name                   |
| `DB_USERNAME`             | `postgres`                     | Database user                   |
| `DB_PASSWORD`             | `postgres`                     | Database password               |
| `JWT_SECRET`              | *(change in prod)*             | HS256 secret (min 256 bits)     |
| `JWT_EXPIRATION_MS`       | `86400000`                     | Access token TTL (ms)           |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000`                  | Refresh token TTL (ms)          |
| `CORS_ALLOWED_ORIGINS`    | `http://localhost:3000`        | Comma-separated allowed origins |

## Running Tests

```bash
./mvnw test
```

## API Endpoints (planned)

| Method | Path                        | Description           | Auth     |
|--------|-----------------------------|-----------------------|----------|
| POST   | `/auth/register`            | Register user         | Public   |
| POST   | `/auth/login`               | Login / get JWT       | Public   |
| GET    | `/products`                 | List products         | Public   |
| GET    | `/products/{id}`            | Get product           | Public   |
| POST   | `/products`                 | Create product        | ADMIN    |
| GET    | `/cart`                     | View cart             | User     |
| POST   | `/cart/items`               | Add item to cart      | User     |
| POST   | `/orders`                   | Place order           | User     |
| GET    | `/orders`                   | List user orders      | User     |
| GET    | `/admin/orders`             | List all orders       | ADMIN    |

## License

MIT
