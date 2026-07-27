package com.shishir.ecommerce.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Single source of truth for API endpoint paths. Each constant is a full,
 * absolute path — controllers apply them directly on every mapping
 * annotation rather than composing them with a class-level
 * {@code @RequestMapping}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Routes {

    public static final String API = "/api";
    public static final String V1 = API + "/v1";

    public static final String USERS = V1 + "/users";
    public static final String USER_REGISTER = USERS + "/register";
    public static final String USER_BY_ID = USERS + "/{id}";

    public static final String PRODUCTS = V1 + "/products";
    public static final String PRODUCT_BY_ID = PRODUCTS + "/{id}";

    public static final String ORDERS = V1 + "/orders";
    public static final String ORDER_BY_ID = ORDERS + "/{id}";
    public static final String ORDER_STATUS = ORDERS + "/{id}/status";

    public static final String CART = V1 + "/cart";
    public static final String CART_ITEMS = CART + "/items";
    public static final String CART_ITEM_BY_ID = CART + "/items/{cartItemId}";

    public static final String AUTH = V1 + "/auth";
    public static final String AUTH_LOGIN = AUTH + "/login";
    public static final String AUTH_REGISTER = AUTH + "/register";
    public static final String AUTH_ME = AUTH + "/me";
}
