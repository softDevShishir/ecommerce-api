# E-Commerce REST API

## Description
A production-ready REST API for e-commerce applications built with Spring Boot 3.x. Features secure user authentication, product catalog management, shopping cart functionality, and order management with role-based access control.

## Tech Stack

**Backend:**
- Spring Boot 3.x
- Spring Security with JWT authentication
- Spring Data JPA
- Lombok

**Database:**
- PostgreSQL

**Build & DevOps:**
- Maven
- Docker & Docker Compose
- Java 21

**Other:**
- JWT (JSON Web Tokens) for stateless authentication
- Hibernate ORM

## Features

✅ **User Management**
- User registration & login with JWT authentication
- Role-based access control (ADMIN, USER)
- Secure password handling

✅ **Product Catalog**
- Product CRUD operations
- Search & filtering by category, price range
- Stock quantity tracking
- Pagination support

✅ **Shopping Cart**
- Add/remove items from cart
- Quantity management
- Cart persistence per user

✅ **Order Management**
- Create orders from cart
- Order history & tracking
- Order status management (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- Order item details with pricing

✅ **Security**
- JWT token-based authentication
- Role-based authorization
- Secure API endpoints
- Password encryption

✅ **Architecture**
- Module-wise organization (user, product, order, cart)
- Clean code with Lombok
- AuditData base class for timestamp tracking
- Spring Data JPA repositories

## Project Structure

```
ecommerce-api/
├── src/main/java/com/shishir/ecommerce/
│   ├── config/
│   │   └── AuditData.java (Base class for entities)
│   ├── security/
│   │   ├── UserRole.java
│   │   ├── JwtTokenProvider.java
│   │   └── JwtAuthenticationFilter.java
│   ├── user/ (User management module)
│   ├── product/ (Product catalog module)
│   ├── order/ (Order management module)
│   └── cart/ (Shopping cart module)
├── src/main/resources/
│   ├── application.yml
│   └── database/schema.sql
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## Installation

### Prerequisites
- Java 21 or higher
- Maven 3.8+
- PostgreSQL 13+ (or use Docker)
- Git

### Local Development Setup

1. **Clone repository:**
```bash
git clone https://github.com/softDevShishir/ecommerce-api.git
cd ecommerce-api
```

2. **Configure database:**
   - Create PostgreSQL database: `ecommerce_db`
   - Update `application.yml` with database credentials

3. **Build project:**
```bash
mvn clean install
```

4. **Run application:**
```bash
mvn spring-boot:run
```

Application will start on `http://localhost:8080`

## Docker Setup

### Quick Start with Docker Compose

1. **Build and run services:**
```bash
docker-compose up -d
```

This will:
- Start PostgreSQL on port 5432
- Load database schema automatically
- Start Spring Boot API on port 8080

2. **Access application:**
- API: http://localhost:8080
- Database: localhost:5432

3. **Stop services:**
```bash
docker-compose down
```

### Environment Variables

Database credentials can be customized in `docker-compose.yml`:
- `POSTGRES_DB=ecommerce_db`
- `POSTGRES_USER=ecommerce_user`
- `POSTGRES_PASSWORD=ecommerce_password`

## Database Schema

**Tables:**
- `users` - User accounts with roles
- `products` - Product catalog
- `orders` - Order records
- `order_items` - Items in each order
- `cart` - Shopping cart per user
- `cart_items` - Items in cart

**Relationships:**
- One User → Many Orders
- One User → One Cart
- One Order → Many OrderItems
- One Product → Many OrderItems
- One Cart → Many CartItems

## API Endpoints (Preview)

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login (returns JWT token)

### Products
- `GET /api/products` - Get all products (with filters)
- `GET /api/products/{id}` - Get product by ID
- `POST /api/products` - Create product (ADMIN only)
- `PUT /api/products/{id}` - Update product (ADMIN only)
- `DELETE /api/products/{id}` - Delete product (ADMIN only)

### Orders
- `GET /api/orders` - Get user's orders
- `POST /api/orders` - Create new order
- `GET /api/orders/{id}` - Get order details
- `PUT /api/orders/{id}/status` - Update order status (ADMIN only)

### Cart
- `GET /api/cart` - Get user's cart
- `POST /api/cart/items` - Add item to cart
- `DELETE /api/cart/items/{itemId}` - Remove item from cart

## Authentication

API uses JWT (JSON Web Tokens) for stateless authentication.

**Getting a Token:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

**Using Token:**
```bash
curl -H "Authorization: Bearer <your_jwt_token>" \
  http://localhost:8080/api/products
```

## Testing

Run unit tests:
```bash
mvn test
```

## Deployment

### Build Docker Image
```bash
docker build -t ecommerce-api:latest .
```

### Push to Docker Registry
```bash
docker tag ecommerce-api:latest <your-registry>/ecommerce-api:latest
docker push <your-registry>/ecommerce-api:latest
```

## Architecture Highlights

- **Module-wise Organization** - Each feature (user, product, order, cart) is a separate module
- **AuditData Base Class** - All entities inherit createdAt/updatedAt timestamps
- **Repository Pattern** - Spring Data JPA for data access
- **Security** - JWT authentication with role-based authorization
- **Docker Support** - Production-ready containerization

## Performance Features

- Database indexes on foreign keys and search columns
- Pagination support for large datasets
- Efficient queries with Spring Data JPA
- Caching-ready architecture

## Future Enhancements

- Payment gateway integration
- Email notifications
- Advanced reporting & analytics
- API rate limiting
- Cache layer (Redis)
- Microservices architecture (split modules)

## Contributing

This is a portfolio project. Feel free to fork and use as reference.

## Author

**Shishir**
- GitHub: [github.com/softDevShishir](https://github.com/softDevShishir)
- Email: softdevshishir@gmail.com
- Location: Dhaka, Bangladesh

Backend Engineer specializing in REST APIs, Microservices, and Security.
3.5 years of production experience.

## License

MIT License
