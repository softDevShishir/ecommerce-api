# Integration Test Report

**Date:** 2026-07-23
**Command:** `mvn clean test` against a real Postgres 16 instance (Docker, `ecommerce_test` DB, `ddl-auto: create-drop`)
**Result:** 22/22 passed, 0 failures, 0 errors

## Summary

| Test class | Tests | Failures | Errors |
|---|---|---|---|
| `AuthControllerIntegrationTest` | 8 | 0 | 0 |
| `ProductControllerIntegrationTest` | 8 | 0 | 0 |
| `CartControllerIntegrationTest` | 6 | 0 | 0 |
| **Total** | **22** | **0** | **0** |

## Coverage

- **Auth** — register (success + duplicate email), login (success + bad password + unknown user), `/me` (valid token, missing token, invalid token)
- **Product** — list, get by id (found + 404), search by name, filter by category, filter by price range, create (valid + invalid price → 400 `VALIDATION_ERROR`)
- **Cart** — get cart, add item, add item with insufficient stock → 400, update item quantity, remove item, clear cart

## Bug found and fixed

`GET /api/v1/cart` threw `LazyInitializationException`: `CartController` read the lazy `cart.getCartItems()` `@OneToMany` collection after the owning `@Transactional` service method had already returned (`open-in-view: false`). Fixed by having `CartController` fetch items via the existing `CartService.getCartItems(userId)` query instead of touching the entity's mapped collection directly (`src/main/java/com/shishir/ecommerce/cart/controller/CartController.java`).

Re-verified against a fresh, independent Postgres container after the fix — no `LazyInitializationException` or orphan-removal errors in any run.
