# E-Commerce REST API

A production-ready REST API for e-commerce applications built with Spring Boot 3.x and modern architecture patterns. Features secure user authentication with JWT, complete product catalog management, shopping cart functionality, and order management with role-based access control.

## 🚀 Features

### User Management
- User registration with email validation
- Secure login with JWT token generation
- Role-based access control (ADMIN, USER)
- User profile management
- Password encryption with BCrypt

### Product Catalog
- Complete product CRUD operations
- Search and filtering by name, category, price range
- Stock quantity tracking
- Product categorization
- Pagination support

### Shopping Cart
- Add/remove products from cart
- Update item quantities
- Real-time cart total calculation
- Cart persistence per user
- Clear cart functionality

### Order Management
- One-click checkout from cart
- Order history and tracking
- Order status management (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- Order item details with pricing
- Admin order status updates

### Security
- JWT (JSON Web Token) authentication
- Spring Security integration
- Role-based endpoint protection
- Password encryption (BCrypt)
- CORS enabled for frontend integration
- Stateless API design

### API Documentation
- Swagger/OpenAPI integration
- Interactive API documentation
- Public and Protected endpoint grouping
- Live API testing with Swagger UI

## 📋 Tech Stack

**Backend Framework**
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- Lombok

**Database**
- PostgreSQL 15
- Hibernate ORM
- Database schema via schema.sql

**API & Documentation**
- REST API
- Swagger/OpenAPI (Springdoc)
- JWT for authentication

**DevOps & Deployment**
- Docker & Docker Compose
- Maven build automation
- Multi-profile configuration (dev, prod)

**Testing**
- JUnit 5
- Spring Boot Test
- MockMvc for integration tests
- 22 integration tests

**Architecture**
- Module-wise organization (user, product, order, cart)
- AuditData base class for timestamp tracking
- Service layer for business logic
- Controller layer for REST endpoints
- Global exception handling
- Clean separation of concerns

## 🏗️ Project Structure

```
ecommerce-api/
├── src/main/java/com/shishir/ecommerce/
│   ├── config/              # Configuration classes (Security, Swagger, AuditData)
│   ├── security/             # JWT, authentication, security config
│   ├── exception/             # Custom exceptions and global handler
│   ├── user/                 # User module (entity, service, controller, dto)
│   ├── product/               # Product module
│   ├── order/                 # Order module
│   └── cart/                  # Cart module
├── src/main/resources/
│   ├── application.yml        # Main configuration
│   ├── application-dev.yml    # Development profile
│   ├── application-prod.yml   # Production profile
│   └── database/
│       ├── schema.sql          # Database schema
│       └── data.sql            # Sample test data
├── src/test/java/              # Integration tests
├── Dockerfile                  # Docker image build
├── docker-compose.yml          # Local development setup
├── pom.xml                     # Maven dependencies
├── .env.example                # Environment variables template
└── README.md                   # This file
```

## 📦 Installation & Setup

### Prerequisites
- Java 21
- Maven 3.8+
- PostgreSQL 13+ (or Docker)
- Git
- Docker & Docker Compose (for containerized setup)

### Local Development (with Docker)

**Fastest way to get started:**

1. **Clone repository:**
```bash
git clone https://github.com/softDevShishir/ecommerce-api.git
cd ecommerce-api
```

2. **Set up environment variables:**
```bash
cp .env.example .env
# Edit .env with your values if needed (defaults should work for local dev)
```

3. **Start services with Docker Compose:**
```bash
docker-compose up -d
```

4. **Verify services are running:**
```bash
curl http://localhost:8080/swagger-ui.html
curl http://localhost:8080/v3/api-docs
```

The API will be available at `http://localhost:8080`

### Local Development (without Docker)

**If you prefer running locally:**

1. **Install PostgreSQL:**
   - Create database: `createdb ecommerce_db`
   - Create user: `createuser ecommerce_user`
   - Set password: `ALTER ROLE ecommerce_user WITH PASSWORD 'ecommerce_password';`
   - Grant privileges: `GRANT ALL PRIVILEGES ON DATABASE ecommerce_db TO ecommerce_user;`

2. **Build project:**
```bash
mvn clean install
```

3. **Run application:**
```bash
mvn spring-boot:run
```

4. **Access API:**
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - API Docs: http://localhost:8080/v3/api-docs

## 🔌 API Endpoints

### Authentication
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login and get JWT token
- `GET /api/v1/auth/me` - Get current authenticated user

### Users
- `GET /api/v1/users` - Get all users (ADMIN only)
- `GET /api/v1/users/{id}` - Get user by ID
- `PUT /api/v1/users/{id}` - Update user profile

### Products
- `GET /api/v1/products` - Get all products (with search/filter)
- `GET /api/v1/products/{id}` - Get product by ID
- `POST /api/v1/products` - Create product (ADMIN only)
- `PUT /api/v1/products/{id}` - Update product (ADMIN only)
- `DELETE /api/v1/products/{id}` - Delete product (ADMIN only)

### Orders
- `GET /api/v1/orders` - Get user's orders
- `GET /api/v1/orders/{id}` - Get order by ID
- `POST /api/v1/orders` - Create order from cart
- `PUT /api/v1/orders/{id}/status` - Update order status (ADMIN only)

### Shopping Cart
- `GET /api/v1/cart` - Get user's cart
- `POST /api/v1/cart/items` - Add item to cart
- `PUT /api/v1/cart/items/{cartItemId}` - Update cart item quantity
- `DELETE /api/v1/cart/items/{cartItemId}` - Remove item from cart
- `DELETE /api/v1/cart` - Clear cart

## 🔐 Authentication

The API uses JWT (JSON Web Tokens) for stateless authentication.

### Getting a Token

1. **Register new user:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

2. **Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

Response includes JWT token:
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "user@example.com",
  "role": "USER",
  "expiresIn": 86400
}
```

### Using the Token

Include token in Authorization header:
```bash
curl -H "Authorization: Bearer <your-token>" \
  http://localhost:8080/api/v1/cart
```

## 📊 Database Schema

**Tables:**
- `users` - User accounts with roles (ADMIN, USER)
- `products` - Product catalog
- `orders` - Order records
- `order_items` - Items in each order
- `cart` - Shopping cart per user
- `cart_items` - Items in cart

**Key Features:**
- Automatic timestamps (created_at, updated_at)
- Foreign key relationships with cascade delete
- Indexes on frequently searched columns for performance
- Native Postgres enum types for roles and order status

## 🧪 Testing

### Run all tests:
```bash
mvn test
```

### Run specific test class:
```bash
mvn test -Dtest=AuthControllerIntegrationTest
```

### Test coverage:
- 22 integration tests
- Tests for authentication, products, and cart
- Both success and failure scenarios
- Proper status codes and error handling

## 🚀 Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for the full deployment guide.

### Deploy to Render/Railway (Free tier)

1. **Push to GitHub:**
```bash
git push origin main
```

2. **Connect repository to deployment platform:**
   - Go to Render.com or Railway.app
   - Connect your GitHub repository
   - Set environment variables (DB credentials, JWT_SECRET, etc.)
   - Deploy

3. **Environment variables for production:**
   - `DB_HOST` - PostgreSQL host
   - `DB_PORT` - PostgreSQL port
   - `DB_NAME` - Database name
   - `DB_USERNAME` - Database user
   - `DB_PASSWORD` - Database password
   - `JWT_SECRET` - Secret key for JWT (min 256 bits)
   - `JWT_EXPIRATION` - Token expiration time (default: 86400000 ms)
   - `SPRING_PROFILES_ACTIVE` - Set to `prod`

### Build Docker image:
```bash
docker build -t ecommerce-api:latest .
```

### Push to Docker Hub:
```bash
docker tag ecommerce-api:latest <your-username>/ecommerce-api:latest
docker push <your-username>/ecommerce-api:latest
```

## 📝 API Documentation

### Swagger UI
Access interactive API documentation at:
- Local: http://localhost:8080/swagger-ui.html
- Try out endpoints directly from the UI
- See request/response examples
- Download OpenAPI spec

### OpenAPI Spec
Full OpenAPI specification available at:
- http://localhost:8080/v3/api-docs

## 🏆 Key Features

- **Stateless JWT Authentication** - Secure, scalable token-based auth
- **Role-Based Access Control** - Different permissions for ADMIN and USER
- **Module-Wise Architecture** - Clean separation of concerns
- **AuditData Base Class** - Automatic timestamp tracking
- **Global Exception Handling** - Consistent error responses
- **Comprehensive Testing** - 22 integration tests
- **Swagger Documentation** - Interactive API docs
- **Docker Ready** - Containerized for easy deployment
- **Multi-Profile Config** - Dev, test, and production configurations
- **Production Ready** - Security best practices, logging, error handling

## 🔒 Security Considerations

- Passwords encrypted with BCrypt
- JWT tokens with 24-hour expiration
- CORS configured for frontend
- SQL injection prevention via JPA
- CSRF protection (disabled for stateless API)
- Role-based endpoint protection
- Environment variables for secrets (no hardcoding)
- Validation on all inputs

## 🤝 Contributing

This is a portfolio project. Feel free to:
- Fork and use as reference
- Study the architecture and patterns
- Modify for your own projects
- Ask questions about the implementation

## 📄 License

MIT License - See LICENSE file for details

## 👤 Author

**Shishir**
- GitHub: [github.com/softDevShishir](https://github.com/softDevShishir)
- Email: softdevshishir@gmail.com
- Location: Dhaka, Bangladesh

Backend Engineer specializing in REST APIs, Microservices, and Security.
3.5 years of production experience.

## 🎯 Future Enhancements

- [ ] Payment gateway integration (Stripe, PayPal)
- [ ] Email notifications for orders
- [ ] Advanced reporting and analytics
- [ ] API rate limiting
- [ ] Redis caching layer
- [ ] Microservices architecture (split modules)
- [ ] GraphQL API support
- [ ] Real-time notifications (WebSocket)
- [ ] Admin dashboard
- [ ] Mobile app API optimization

## 📞 Support

For issues, questions, or suggestions:
- Open GitHub issues
- Email: softdevshishir@gmail.com
- Check API documentation at `/swagger-ui.html`

---

**Happy coding! 🚀**
