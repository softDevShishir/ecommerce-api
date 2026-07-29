# API Guide

Complete endpoint reference with request/response examples. For a workflow-level explanation of each feature, see [FEATURES_GUIDE.md](FEATURES_GUIDE.md). For setup, see [README.md](README.md).

All examples use `http://localhost:8080` — swap in `https://ecommerce-api-9236.onrender.com` to hit the [live demo](README.md#-live-demo).

## Table of Contents

- [Conventions](#conventions)
- [Error Responses](#error-responses)
- [Authentication](#authentication-endpoints)
- [Users](#user-endpoints)
- [Products](#product-endpoints)
- [Product Reviews & Ratings](#product-reviews--ratings-endpoints)
- [Shopping Cart](#shopping-cart-endpoints)
- [Wishlist](#wishlist-endpoints)
- [Orders](#order-endpoints)
- [Postman Collection](#postman-collection)

## Conventions

- All request/response bodies are JSON (`Content-Type: application/json`).
- Authenticated endpoints require `Authorization: Bearer <token>`, obtained from `POST /api/v1/auth/login` or `POST /api/v1/auth/register`.
- Timestamps are ISO-8601 `LocalDateTime` (no timezone offset — the server's local time).
- Null fields are omitted from responses (`@JsonInclude(NON_NULL)` / Jackson `non_null` default).
- There is no pagination on any list endpoint yet (`GET /products`, `GET /users`, `GET /orders`, `GET /reviews/my-reviews` all return full unbounded lists).

## Error Responses

Every error follows the same shape (`GlobalExceptionHandler` / `ExceptionResponse`):

```json
{
  "timestamp": "2026-07-29T10:15:30",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Product not found with id: 999",
  "path": "/api/v1/products/999",
  "details": null
}
```

| HTTP status | `error` | When |
|---|---|---|
| 400 | `BAD_REQUEST` | Business-rule violation (e.g. insufficient stock, product already in wishlist) |
| 400 | `VALIDATION_ERROR` | Request body failed `@Valid` bean validation — `details` lists each field error |
| 401 | `UNAUTHORIZED` | Acting on a resource (e.g. a review) that belongs to another user |
| 401 | *(handled by `JwtAuthenticationEntryPoint`)* | Missing or invalid JWT on a protected endpoint |
| 403 | *(handled by `JwtAccessDeniedHandler`)* | Valid JWT, but role lacks permission (e.g. non-admin hitting `DELETE /products/{id}`) |
| 404 | `NOT_FOUND` | Resource doesn't exist |
| 409 | `CONFLICT` | Duplicate resource (e.g. email already registered, already reviewed this product) |
| 500 | `INTERNAL_SERVER_ERROR` | Unhandled exception — `details` is only the exception's simple class name, never a stack trace or message |

## Authentication Endpoints

### `POST /api/v1/auth/register` — Public

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane@example.com",
    "password": "SecurePass1!",
    "firstName": "Jane",
    "lastName": "Doe"
  }'
```

Validation: `email` (required, valid format), `password` (8–50 chars), `firstName`/`lastName` (required, non-blank).

**201 Created** — returns a `LoginResponse` (token included, no separate login call needed):
```json
{
  "userId": 1,
  "email": "jane@example.com",
  "role": "USER",
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "expiresIn": 86400
}
```

**409 Conflict** if the email is already registered.

### `POST /api/v1/auth/login` — Public

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jane@example.com", "password": "SecurePass1!"}'
```

**200 OK** — same `LoginResponse` shape as register. **401** on bad password or unknown email.

### `GET /api/v1/auth/me` — Authenticated

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <token>"
```

**200 OK**:
```json
{
  "id": 1,
  "email": "jane@example.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "role": "USER",
  "createdAt": "2026-07-20T09:00:00"
}
```

## User Endpoints

### `POST /api/v1/users/register` — Public

Same validation and underlying `UserService.registerUser` call as `/auth/register`, but returns a plain `UserResponse` (no token) — call `/auth/login` afterward to get one. Kept for backward compatibility; prefer `/auth/register` for new integrations since it returns a token in one call.

**201 Created**:
```json
{
  "id": 2,
  "email": "bob@example.com",
  "firstName": "Bob",
  "lastName": "Smith",
  "role": "USER",
  "createdAt": "2026-07-29T10:00:00"
}
```

### `GET /api/v1/users/{id}` — Authenticated

```bash
curl http://localhost:8080/api/v1/users/1 -H "Authorization: Bearer <token>"
```
**200 OK** — `UserResponse` (see above). **404** if the ID doesn't exist.

### `PUT /api/v1/users/{id}` — Authenticated

```bash
curl -X PUT http://localhost:8080/api/v1/users/1 \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"firstName": "Janet", "lastName": "Doe"}'
```
Updates first/last name only. **Note:** there is no ownership check — any authenticated user can update any user ID by number, not just their own profile.

### `GET /api/v1/users` — Authenticated

```bash
curl http://localhost:8080/api/v1/users -H "Authorization: Bearer <token>"
```
Returns every user as a `UserResponse` array. **Not** restricted to `ADMIN` despite the name suggesting an admin-only listing — see [README's Security section](README.md#-security).

## Product Endpoints

### `GET /api/v1/products` — Public

Query params (applied with precedence `name` → `category` → `minPrice`+`maxPrice`; only one filter type applies per call):

```bash
curl "http://localhost:8080/api/v1/products?name=laptop"
curl "http://localhost:8080/api/v1/products?category=Electronics"
curl "http://localhost:8080/api/v1/products?minPrice=10&maxPrice=500"
curl "http://localhost:8080/api/v1/products"   # all products, no filter
```

> **Non-standard behavior:** a filtered search (`name`/`category`/price range) that matches zero products returns **404 `NOT_FOUND`**, not `200` with an empty array. The unfiltered call (no query params) always returns `200`, even if the catalog is empty. This is existing, tested behavior — see [README's Future Enhancements](README.md#-future-enhancements) if you're relying on REST-conventional empty-array semantics instead.

**200 OK**:
```json
[
  {
    "id": 1,
    "name": "Laptop",
    "description": "15-inch, 16GB RAM",
    "price": 999.99,
    "stockQuantity": 50,
    "category": "Electronics",
    "createdAt": "2026-07-01T08:00:00",
    "updatedAt": "2026-07-01T08:00:00",
    "averageRating": 4.5,
    "totalReviews": 12
  }
]
```
`averageRating`/`totalReviews` are computed live from `product_reviews` on every call (rounded to 1 decimal; `0.0`/`0` if there are no reviews yet).

### `GET /api/v1/products/{id}` — Public

Same shape as one array element above. **404** if not found.

### `POST /api/v1/products` — ADMIN only

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <admin-token>" -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Mouse",
    "description": "Ergonomic, 2.4GHz",
    "price": 29.99,
    "stockQuantity": 200,
    "category": "Electronics"
  }'
```
**201 Created**, **400** on invalid price/stock, **403** if the caller isn't `ADMIN`.

### `PUT /api/v1/products/{id}` — ADMIN only

Same body shape as create. **200 OK**, **404** if not found.

### `DELETE /api/v1/products/{id}` — ADMIN only

**204 No Content**, **404** if not found.

## Product Reviews & Ratings Endpoints

### `POST /api/v1/products/{productId}/reviews` — Authenticated

```bash
curl -X POST http://localhost:8080/api/v1/products/1/reviews \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"rating": 5, "title": "Great product!", "content": "Exactly what I needed, works perfectly."}'
```

Validation: `rating` (1–5), `title` (required, ≤200 chars), `content` (required, ≤2000 chars).

**201 Created**:
```json
{
  "id": 1,
  "productId": 1,
  "userId": 3,
  "userName": "Jane Doe",
  "rating": 5,
  "title": "Great product!",
  "content": "Exactly what I needed, works perfectly.",
  "createdAt": "2026-07-29T10:20:00",
  "updatedAt": "2026-07-29T10:20:00"
}
```

**409 Conflict** if this user already reviewed this product (`UNIQUE (product_id, user_id)`). **404** if the product doesn't exist.

### `PUT /api/v1/products/{productId}/reviews/{reviewId}` — Authenticated (owner only)

Same body as create, minus `productId`. **200 OK** on success, **401** if the review belongs to a different user, **404** if the review doesn't exist.

### `DELETE /api/v1/products/{productId}/reviews/{reviewId}` — Authenticated (owner only)

**204 No Content**. Same **401**/**404** rules as update.

### `GET /api/v1/products/{productId}/reviews` — Public

```bash
curl http://localhost:8080/api/v1/products/1/reviews
```
Returns all reviews for the product, newest first, as a `ReviewResponse[]` (shape above). **404** if the product doesn't exist.

### `GET /api/v1/products/{productId}/rating` — Public

```bash
curl http://localhost:8080/api/v1/products/1/rating
```

**200 OK**:
```json
{
  "productId": 1,
  "averageRating": 4.5,
  "totalReviews": 12,
  "ratingDistribution": {"1": 0, "2": 1, "3": 1, "4": 4, "5": 6},
  "reviews": [ /* ReviewResponse[], newest first */ ]
}
```

### `GET /api/v1/reviews/my-reviews` — Authenticated

```bash
curl http://localhost:8080/api/v1/reviews/my-reviews -H "Authorization: Bearer <token>"
```
Returns every review the authenticated user has written, across all products.

## Shopping Cart Endpoints

All cart endpoints operate on the authenticated user's own cart (there's no cart ID in the path — it's resolved from the JWT).

### `GET /api/v1/cart` — Authenticated

```bash
curl http://localhost:8080/api/v1/cart -H "Authorization: Bearer <token>"
```

**200 OK**:
```json
{
  "id": 1,
  "userId": 3,
  "cartItems": [
    {"id": 1, "productId": 1, "productName": "Laptop", "quantity": 2, "price": 999.99, "totalPrice": 1999.98}
  ],
  "totalItems": 2,
  "totalPrice": 1999.98
}
```
`totalItems`/`totalPrice` and each item's `totalPrice` are computed on the fly, not stored columns.

### `POST /api/v1/cart/items` — Authenticated

```bash
curl -X POST http://localhost:8080/api/v1/cart/items \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 2}'
```
Adding a product already in the cart increases its quantity rather than creating a duplicate line item. **400** if `quantity` exceeds available stock.

### `PUT /api/v1/cart/items/{cartItemId}` — Authenticated

```bash
curl -X PUT http://localhost:8080/api/v1/cart/items/1 \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"quantity": 3}'
```

### `DELETE /api/v1/cart/items/{cartItemId}` — Authenticated

**200 OK** with the updated `CartResponse` (not 204 — unlike `DELETE /cart`, which does return 204). **404** if the cart item doesn't exist.

### `DELETE /api/v1/cart` — Authenticated

Clears every item from the cart.

## Wishlist Endpoints

All wishlist endpoints operate on the authenticated user's own wishlist, created lazily on first `POST`.

### `GET /api/v1/users/wishlist` — Authenticated

```bash
curl http://localhost:8080/api/v1/users/wishlist -H "Authorization: Bearer <token>"
```

**200 OK**:
```json
{
  "id": 1,
  "userId": 3,
  "productCount": 2,
  "products": [ /* full ProductResponse[] objects, including averageRating/totalReviews */ ],
  "createdAt": "2026-07-29T09:00:00"
}
```
**404** if the wishlist hasn't been created yet (no products ever added).

### `POST /api/v1/users/wishlist/items` — Authenticated

```bash
curl -X POST http://localhost:8080/api/v1/users/wishlist/items \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"productId": 1}'
```
Creates the wishlist on first use. **200 OK** with the updated `WishlistResponse`. **400** if the product is already in the wishlist. **404** if the product doesn't exist.

### `DELETE /api/v1/users/wishlist/items/{productId}` — Authenticated

**200 OK** with the updated `WishlistResponse`. **404** if the wishlist or the product-in-wishlist doesn't exist.

### `GET /api/v1/users/wishlist/items/{productId}/exists` — Authenticated

```bash
curl http://localhost:8080/api/v1/users/wishlist/items/1/exists -H "Authorization: Bearer <token>"
```
**200 OK**: `{"exists": true}` — useful for toggling a "save" button's state without fetching the whole wishlist.

### `DELETE /api/v1/users/wishlist` — Authenticated

**204 No Content**. Removes all products from the wishlist (the wishlist row itself remains). **404** if it doesn't exist yet.

## Order Endpoints

### `POST /api/v1/orders` — Authenticated

```bash
curl -X POST http://localhost:8080/api/v1/orders -H "Authorization: Bearer <token>"
```
No request body — creates an order from the caller's current cart. Stock validation, stock decrement, total calculation, and clearing the cart all happen in one transaction: a failure partway through (e.g. item 2 is out of stock) rolls back everything, including any stock already decremented for item 1.

**201 Created**:
```json
{
  "id": 10,
  "userId": 3,
  "orderDate": "2026-07-29T10:30:00",
  "totalPrice": 1999.98,
  "status": "PENDING",
  "orderItems": [
    {"id": 1, "productId": 1, "productName": "Laptop", "quantity": 2, "price": 999.99}
  ],
  "createdAt": "2026-07-29T10:30:00"
}
```
**400** if the cart is empty or any item's quantity now exceeds available stock.

### `GET /api/v1/orders/{id}` — Authenticated

Returns the `OrderResponse` above for a specific order. **404** if not found. **Note:** no ownership check — any authenticated user can look up any order ID.

### `GET /api/v1/orders` — Authenticated

Returns every order for the authenticated user (not other users' orders — this one *is* scoped by `userId`). **Non-standard behavior:** if the user has zero orders, this returns **404 `NOT_FOUND`** rather than `200` with an empty array — the same empty-result-as-404 pattern used by the product search filters above.

### `PUT /api/v1/orders/{id}/status` — ADMIN only

```bash
curl -X PUT http://localhost:8080/api/v1/orders/10/status \
  -H "Authorization: Bearer <admin-token>" -H "Content-Type: application/json" \
  -d '{"status": "SHIPPED"}'
```
Valid `status` values: `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`. **403** if the caller isn't `ADMIN`. **404** if the order doesn't exist.

## Postman Collection

`postman/ecommerce-api.postman_collection.json` — import into Postman and set the collection variable **`baseUrl`** (not `base_url`) to your target server.

Collection variables it manages automatically via test scripts on each request (`pm.collectionVariables.set(...)`): `token`, `adminToken`, `userId`, `productId`, `cartItemId`, `orderId` — run **Auth → Login** (or **Admin Login**) once and later requests in the collection pick the token up automatically.

Folders: `Auth`, `Users`, `Products`, `Cart`, `Orders`.

**Known gap:** the collection predates the reviews and wishlist features and has no requests for either — use the `curl` examples above, or add your own requests to the collection, until it's updated.
