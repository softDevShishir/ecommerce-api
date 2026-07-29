# Deployment Guide

## Overview

This guide covers deploying the E-Commerce REST API to production environments.

## Currently Deployed

The API is live on Render.com:
- **Base URL:** https://ecommerce-api-9236.onrender.com
- **Swagger UI:** https://ecommerce-api-9236.onrender.com/swagger-ui.html
- **OpenAPI spec:** https://ecommerce-api-9236.onrender.com/v3/api-docs

Deployed with `SPRING_PROFILES_ACTIVE=prod`, a managed Render PostgreSQL instance, and auto-deploy from the `main` branch. Free-tier instances spin down after inactivity — the first request after idle time can take 30–60 seconds.

**CI mismatch:** `.github/workflows/ci.yml` invokes `./mvnw`, but this repo has no committed Maven wrapper (`mvnw`/`.mvn`) — CI will fail as-is until one is added (`mvn -N wrapper:wrapper`) or the workflow is changed to call `mvn` directly. If the wrapper is added, CI also needs a `~/.m2/toolchains.xml` with a JDK 21 entry (or a JDK-21-pinned runner), since `pom.xml` pins compilation to a JDK 21 toolchain that Lombok 1.18.32 requires — CI doesn't currently have one configured.

## Prerequisites

- Docker & Docker Compose
- A cloud platform (Render, Railway, AWS, Heroku, etc.)
- PostgreSQL database (managed or self-hosted)
- Domain name (optional)
- SSL certificate (for HTTPS)

## Local Development

### With Docker Compose

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Clean up volumes
docker-compose down -v
```

### Without Docker

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# With specific profile
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

## Production Deployment

### Option 1: Render.com (Recommended for Beginners)

1. Push to GitHub
2. Sign up at Render.com
3. Create new Web Service
4. Connect GitHub repository
5. Set environment variables:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `JWT_SECRET` (generate a secure key)
   - `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD` (from managed PostgreSQL)
6. Deploy

### Option 2: Railway

1. Push to GitHub
2. Sign up at Railway.app
3. Create new project
4. Add PostgreSQL plugin
5. Add Web service from GitHub
6. Set environment variables
7. Deploy

### Option 3: Docker Hub + Cloud

```bash
# Build and tag image
docker build -t username/ecommerce-api:latest .

# Push to Docker Hub
docker push username/ecommerce-api:latest

# Deploy from image using docker-compose in cloud
```

### Option 4: AWS EC2

1. Launch EC2 instance
2. Install Docker and Docker Compose
3. Clone repository
4. Create `.env` with production values
5. Run `docker-compose up -d`
6. Configure security groups
7. Set up RDS for PostgreSQL
8. Update database credentials

## Environment Variables

These are the variables the application actually reads (verified against `application.yml`/`application-prod.yml`/`SecurityConfig`) — see `.env.example` for the canonical local-dev list.

### Required (on the `prod` profile)

- `DB_HOST` - PostgreSQL hostname
- `DB_PORT` - PostgreSQL port (default: 5432)
- `DB_NAME` - Database name
- `DB_USERNAME` - Database user
- `DB_PASSWORD` - Database password
- `JWT_SECRET` - Secure random key (min 256 bits)

The base (no-profile) config hardcodes its datasource URL/credentials instead — only `application-prod.yml`'s placeholders read `DB_*`, or Spring's own env-var override (`SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`, which beats *any* profile's YAML regardless of placeholders — this is what `docker-compose.yml` relies on).

### Optional

- `SPRING_PROFILES_ACTIVE` - Profile (dev, prod) - default: dev (Spring Boot's own convention, not a custom property)
- `SERVER_PORT` - Server port - default: 8080
- `JWT_EXPIRATION` - Token expiration in ms - default: 86400000
- `CORS_ALLOWED_ORIGINS` - Comma-separated allowed origins, read by `SecurityConfig` via `app.cors.allowed-origins` - default: `http://localhost:3000,http://localhost:4200`

Nothing else in `src/main/resources/*.yml` reads from the environment — `application-prod.yml`'s `logging.file.name` (`logs/ecommerce-api.log`) and Hikari pool sizes (max 20, min idle 5) are fixed in the file itself, not env-configurable.

### Generate a Secure JWT Secret

```bash
# Linux/Mac
openssl rand -base64 32

# Result: use this as JWT_SECRET
```

## Database Setup

### PostgreSQL Cloud Providers

Recommended:
- Render (includes managed PostgreSQL)
- Railway (includes PostgreSQL plugin)
- AWS RDS
- Azure Database for PostgreSQL
- Google Cloud SQL

### Schema

There's no migration tool (Flyway/Liquibase) in this project. Schema management differs by profile:
- **Default profile and `prod`** use `ddl-auto: validate` — Hibernate only checks the schema, it never creates or alters it. Apply `src/main/resources/database/schema.sql` yourself against the database before first deploy (any profile, not just `prod`).
- **`dev` profile** uses `ddl-auto: create-drop` — Hibernate wipes and rebuilds the schema from the entities on every startup. Only safe because nothing persists across restarts on that profile; never point it at a database you want to keep data in.
- `docker-compose.yml` mounts `schema.sql`/`data.sql` into Postgres's `docker-entrypoint-initdb.d`, so a fresh Docker-managed database gets both automatically — but only once, on an empty data volume. Its `app` service also runs `ddl-auto: validate` (overridden via `SPRING_JPA_HIBERNATE_DDL_AUTO`), relying on that automatic schema.sql application.
- **Never run `ddl-auto: update` against a database with existing data.** The native Postgres enum types (`UserRole`/`OrderStatus`, via `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`) are managed by Hibernate with an unconditional `DROP TYPE ... CASCADE` + `CREATE TYPE` on every single startup — not only when something changed. Against a populated table this cascades through the enum-typed column, then fails re-adding it `NOT NULL` (no default for existing rows), corrupting the table. This isn't a hypothetical: it's exactly what `update` mode used to do here before the config was switched to `validate`.

## Monitoring & Logs

### Docker Logs

```bash
docker-compose logs -f app
```

### Application Logs

Logs are written to:
- Console (stdout)
- File: `logs/ecommerce-api.log` (in the `prod` profile only)

### Health Check

`/actuator/health` requires authentication (it isn't `permitAll`'d in `SecurityConfig`), so healthchecks use the public Swagger UI page instead:

```bash
curl http://localhost:8080/swagger-ui.html
```

## Verifying a Deployment

Once deployed, sanity-check it with the Postman collection in `postman/ecommerce-api.postman_collection.json` — import it, set the collection's `base_url` variable to your deployment's URL, and run the auth → products → cart → orders flow. See [README.md's Live Demo section](README.md#-live-demo) for step-by-step Postman import instructions, or use the `curl` examples in [API_GUIDE.md](API_GUIDE.md).

## SSL/HTTPS

### Using Render/Railway

- Automatic HTTPS with custom domain
- No additional configuration needed

### Self-Hosted

- Use Let's Encrypt with Certbot
- Configure Nginx as reverse proxy
- Redirect HTTP → HTTPS

## Performance Optimization

### Database Connection Pooling

Configured in `application-prod.yml`:
- Max pool size: 20
- Min idle: 5

### Caching (Future)

- Implement Redis for product catalog
- Cache frequently accessed data

### Database Indexes

Already created (see `schema.sql`) on:
- `users.email`
- `products.category`
- `orders.user_id`, `orders.status`
- `order_items.order_id`, `order_items.product_id`
- `cart.user_id`
- `cart_items.cart_id`, `cart_items.product_id`
- `product_reviews.product_id`, `product_reviews.user_id`
- `wishlist.user_id`
- `wishlist_products.product_id`

## Backup & Restore

### PostgreSQL Backup

```bash
# Create backup
pg_dump ecommerce_db > backup.sql

# Restore backup
psql ecommerce_db < backup.sql
```

### Docker Volume Backup

```bash
# Backup volume
docker run --rm -v postgres_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/postgres_backup.tar.gz -C /data .

# Restore volume
docker run --rm -v postgres_data:/data -v $(pwd):/backup \
  alpine tar xzf /backup/postgres_backup.tar.gz -C /data
```

## Troubleshooting

### Database Connection Failed

- Check `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`
- Verify database is running
- Check network connectivity

### JWT Token Invalid

- Ensure `JWT_SECRET` is consistent across restarts/instances
- Check token expiration time
- Verify `Authorization` header format (`Bearer <token>`)

### Container Won't Start

- Check logs: `docker-compose logs app`
- Verify all required environment variables are set
- Check port availability

## Scaling

### Horizontal Scaling

- Use a load balancer (AWS ALB, Nginx)
- Run multiple API instances
- Ensure a shared database (PostgreSQL)
- Use connection pooling

### Vertical Scaling

- Increase instance memory
- Optimize database queries
- Add a caching layer (Redis)
- Optimize JVM parameters

## Cost Optimization

**Free Tier Options:**
- Render: 1 free PostgreSQL instance
- Railway: $5/month free credits
- Heroku: Eco dyno ($5/month)

**Paid (Recommended for Production):**
- Render: $7/month web service
- Railway: Pay-as-you-go
- AWS: Various pricing tiers
