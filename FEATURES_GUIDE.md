# Features Guide

Workflow-level documentation for each feature — how the pieces fit together, not just the endpoint list (that's [API_GUIDE.md](API_GUIDE.md)). Architecture conventions referenced below are documented in full in [CLAUDE.md](CLAUDE.md).

## Table of Contents

- [User Authentication](#user-authentication)
- [Product Catalog](#product-catalog)
- [Product Reviews & Ratings](#product-reviews--ratings)
- [Wishlist](#wishlist)
- [Shopping Cart](#shopping-cart)
- [Order Management](#order-management)

## User Authentication

**Module:** `user/`, `security/`

```
Client                          API                              DB
  │  POST /auth/register          │                                │
  ├───────────────────────────────►│  hash password (BCrypt)        │
  │                                ├────────────────────────────────►│ INSERT users
  │                                │  issue JWT (HS512, 24h)         │
  │  ◄─── 201 { token, ... } ──────┤                                │
  │                                │                                │
  │  GET /cart                     │                                │
  │  Authorization: Bearer <jwt>   │                                │
  ├───────────────────────────────►│ JwtAuthenticationFilter:        │
  │                                │  verify signature + expiry,     │
  │                                │  set SecurityContext            │
  │                                ├────────────────────────────────►│ SELECT ...
  │  ◄─── 200 { ... } ─────────────┤                                │
```

- Passwords are hashed with BCrypt before storage — never compared or logged in plaintext.
- The JWT carries `sub` (email), `role`, issued-at, and expiry claims, signed HS512 with a key derived from `jwt.secret` (SHA-512).
- `JwtAuthenticationFilter` runs before Spring Security's own `UsernamePasswordAuthenticationFilter`. It **never rejects a request itself** — a missing/invalid token just means no authentication gets set, and `SecurityConfig`'s `anyRequest().authenticated()` (or a specific rule) then rejects it downstream via `JwtAuthenticationEntryPoint` (401) or `JwtAccessDeniedHandler` (403).
- `CurrentUserProvider` resolves "who is making this request" for every authenticated endpoint by treating the JWT's subject as the user's **email** and looking the user up by it — every controller that needs the current user (cart, orders, reviews, wishlist) goes through this rather than trusting a client-supplied user ID.
- There are two registration endpoints — `POST /auth/register` (returns a `LoginResponse` with a token, so no separate login call is needed) and `POST /users/register` (returns a plain `UserResponse`, no token — a login call is required afterward). Both call the same `UserService.registerUser` and are both public; this is intentional, documented duplication, not a bug.
- Tokens are stateless — there's no server-side revocation list. The only way to invalidate all outstanding tokens is to rotate `JWT_SECRET` (which also logs out every currently-logged-in user).

## Product Catalog

**Module:** `product/`

- Reads (`GET /products`, `GET /products/{id}`) are public; writes (`POST`/`PUT`/`DELETE`) require `ROLE_ADMIN`.
- `GET /products` applies **at most one** filter, in this precedence: `name` (substring search) → `category` (exact match) → `minPrice`+`maxPrice` range → no filter (all products). Combining `name` and `category` in the same request silently ignores `category` — this is a deliberate simplicity trade-off in `ProductController.search`, not a bug, but worth knowing if you expect combinable filters.
- Every `ProductResponse` embeds `averageRating`/`totalReviews`, computed live from `product_reviews` on every request (via `ProductService.getRatingStats`) — there's no denormalized/cached rating column on `products` itself, so this is always fresh but does mean an extra aggregate query per product returned.
- Deleting a product is blocked at the database level (`ON DELETE RESTRICT`) if it has existing order or cart line items — attempting it surfaces as a database constraint violation, not a friendly `BadRequestException`, since the check isn't done in application code.
- **Non-standard REST behavior:** the three filtered-search paths (`name`/`category`/price-range) throw `404 NOT_FOUND` when nothing matches, rather than returning `200` with an empty list. This is existing, tested behavior (`ProductControllerIntegrationTest`) — see [API_GUIDE.md](API_GUIDE.md#product-endpoints) for the exact cases this applies to.

## Product Reviews & Ratings

**Module:** `product/review/` (nested under `product/` since it's an extension of the product feature, not a standalone one)

```
POST /products/{id}/reviews
        │
        ▼
  user exists? ──no──► 404
        │yes
        ▼
  product exists? ──no──► 404
        │yes
        ▼
  user already reviewed
  this product? ──yes──► 409 CONFLICT
        │no
        ▼
  INSERT product_reviews
  (UNIQUE(product_id, user_id) is the DB-level backstop
   for the same check, in case of a race)
        │
        ▼
     201 Created
```

- **One review per user per product**, enforced both in application code (`ProductReviewRepository.findByProductIdAndUserId` check before insert) and at the database level (`UNIQUE (product_id, user_id)` in `schema.sql`) — the DB constraint is the actual source of truth under concurrent requests; the application check is a friendlier `409` for the common case.
- Only the review's author can update or delete it (`ReviewService.verifyOwnership`, throwing `UnauthorizedException` → `401` if the caller's user ID doesn't match the review's) — there's no admin override to remove another user's review.
- `GET /products/{id}/rating` computes the average and a full 1–5 star distribution by loading all of the product's reviews into memory and aggregating in Java (`ReviewService.getProductRating`), not via a SQL `GROUP BY` — fine at small review counts, worth revisiting if a single product accumulates a very large number of reviews.
- `ReviewResponse.userName` falls back to the reviewer's email if both first and last name are blank, so the field is never empty in practice.
- **Known gap:** no integration tests exist for this module yet (`ReviewControllerIntegrationTest` doesn't exist) — see [README's Testing section](README.md#-testing).

## Wishlist

**Module:** `user/wishlist/` (nested under `user/`, same rationale as reviews under products)

```
POST /users/wishlist/items { productId }
        │
        ▼
  wishlist exists for this user? ──no──► create one (WishlistService.getOrCreateWishlist)
        │yes
        ▼
  product exists? ──no──► 404
        │yes
        ▼
  product already in
  wishlist.products? ──yes──► 400 BAD_REQUEST
        │no
        ▼
  add to wishlist.products (many-to-many via wishlist_products),
  save wishlist
        │
        ▼
     200 OK { WishlistResponse }
```

- The wishlist row is created lazily — there's no `POST /users/wishlist` to explicitly create one; the first `POST .../wishlist/items` call does it implicitly.
- `GET`/`DELETE`/`DELETE .../items/{productId}` on a wishlist that was never created (no product ever added) return `404` — only `POST .../items` and the `/exists` check tolerate a not-yet-created wishlist (the latter via `existsByUserIdAndProductsId`, which is simply `false` if there's no wishlist row).
- `WishlistResponse.products` embeds full `ProductResponse` objects (including live `averageRating`/`totalReviews`), so a client can render a "saved items" page in one request without N+1 product lookups.
- Duplicate-add returns `400 BAD_REQUEST` (checked in application code against `wishlist.getProducts()`, an in-memory scan of the loaded collection) — there's no unique-constraint backstop at the DB level beyond the join table's composite primary key `(wishlist_id, product_id)`, which would surface as a lower-level constraint violation if the application check were ever bypassed.
- **Known gap:** no integration tests exist for this module yet, and the committed Postman collection predates it — no wishlist requests are in `postman/ecommerce-api.postman_collection.json`.

## Shopping Cart

**Module:** `cart/`

- One cart per user (`cart.user_id UNIQUE`), created on first `POST /cart/items` — mirrors the wishlist's lazy-creation pattern.
- Adding a product already in the cart **merges** into the existing line item (increments quantity) rather than creating a duplicate row for the same product.
- Stock is checked against `products.stock_quantity` at add/update time, but **not re-checked continuously** — it's only enforced again, atomically, at order creation (see [Order Management](#order-management) below). Two users can each add the last unit of a low-stock product to their carts; the race is resolved at checkout, not at add-to-cart time.
- `open-in-view` is disabled project-wide, so `Cart.cartItems` (a lazy `@OneToMany`) can't be read after the owning `@Transactional` service method returns. `CartController` deliberately fetches items via a dedicated `CartService.getCartItems(userId)` query in every response-building path, rather than touching `cart.getCartItems()` directly — follow this pattern if extending the cart, per [CLAUDE.md](CLAUDE.md#data-layer).
- Response status codes are inconsistent by design across cart mutations: `POST`/`PUT`/`DELETE .../items/{id}` all return `200` with the updated cart, while `DELETE /cart` (clear) returns `204` with no body — see [API_GUIDE.md](API_GUIDE.md#shopping-cart-endpoints) for the exact per-endpoint codes.

## Order Management

**Module:** `order/`

```
POST /orders   (no request body — reads the caller's current cart)
        │
        ▼
  user exists? ──no──► 404
        │yes
        ▼
  cart exists and non-empty? ──no──► 404 / 400
        │yes
        │
        ▼            ┌─────────────────────────────────────┐
  for each cart item: │ stock >= quantity? ──no──► 400,      │
        │             │ entire transaction rolls back        │
        │             │ (including stock already decremented│
        │             │  earlier in this same loop)          │
        │             └─────────────────────────────────────┘
        ▼
  build OrderItem (price snapshotted from product.price
                    at this moment — later price changes
                    don't retroactively affect past orders)
  decrement product.stockQuantity
        │
        ▼
  INSERT orders + order_items (order.totalPrice pre-computed
                                 before the insert, since the
                                 column is NOT NULL)
        │
        ▼
  clear the cart (cart.cartItems.clear(), not a reassignment —
                   required for Hibernate's orphan-removal to
                   actually issue the deletes)
        │
        ▼
     201 Created { OrderResponse }
```

- The entire flow — stock validation, stock decrement per item, order total calculation, order/order-item insert, and cart clearing — runs inside **one `@Transactional` method** (`OrderService.createOrder`). A failure partway through (e.g. the second cart item is out of stock) rolls back everything already applied in that call, including stock decremented for the first item — there's no partial order or partial stock deduction left behind.
- Order status follows `PENDING → CONFIRMED → SHIPPED → DELIVERED`, or `CANCELLED` from any state — the enum (`OrderStatus`) doesn't itself enforce valid transitions; `PUT /orders/{id}/status` accepts any value in the enum regardless of the order's current status (e.g. `DELIVERED → PENDING` is not blocked at the service layer).
- Within `order/`, only `PUT /orders/{id}/status` is `ROLE_ADMIN`-restricted (product create/update/delete, in the `product/` module, are separately `ADMIN`-restricted too — see [Product Catalog](#product-catalog)). Creating an order and reading a specific order by ID (`GET /orders/{id}`) require only authentication — **not** ownership of that order, so any authenticated user can look up any order by guessing/incrementing its ID. `GET /orders` (list) *is* scoped to the caller's own orders.
- **Non-standard REST behavior:** `GET /orders` throws `404 NOT_FOUND` if the authenticated user has zero orders, rather than `200` with an empty array — the same empty-result-as-404 pattern documented for product search in [Product Catalog](#product-catalog) above.
