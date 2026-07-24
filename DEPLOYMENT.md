# Deployment Guide

## Overview

This guide covers deploying the E-Commerce REST API to production environments.

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

### Required

- `DB_HOST` - PostgreSQL hostname
- `DB_PORT` - PostgreSQL port (default: 5432)
- `DB_NAME` - Database name
- `DB_USERNAME` - Database user
- `DB_PASSWORD` - Database password
- `JWT_SECRET` - Secure random key (min 256 bits)

### Optional

- `SPRING_PROFILES_ACTIVE` - Profile (dev, prod) - default: dev
- `SERVER_PORT` - Server port - default: 8080
- `JWT_EXPIRATION` - Token expiration in ms - default: 86400000

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
- **Default and `dev` profiles** use `ddl-auto: update` — Hibernate creates/alters the schema automatically on startup, including the native Postgres enum types (`user_role`, `order_status`) via `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
- **`prod` profile** uses `ddl-auto: validate` — Hibernate only checks the schema, it never creates or alters it. Apply `src/main/resources/database/schema.sql` yourself against the production database before first deploy.
- `docker-compose.yml` mounts `schema.sql`/`data.sql` into Postgres's `docker-entrypoint-initdb.d`, so a fresh Docker-managed database gets both automatically — but only once, on an empty data volume.

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

Already created on:
- `users.email`
- `products.category`
- `orders.user_id`
- `cart.user_id`

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
