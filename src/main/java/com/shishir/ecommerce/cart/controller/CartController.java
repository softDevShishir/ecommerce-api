package com.shishir.ecommerce.cart.controller;

import com.shishir.ecommerce.cart.dto.CartAddItemRequest;
import com.shishir.ecommerce.cart.dto.CartItemResponse;
import com.shishir.ecommerce.cart.dto.CartResponse;
import com.shishir.ecommerce.cart.dto.CartUpdateItemRequest;
import com.shishir.ecommerce.cart.entity.Cart;
import com.shishir.ecommerce.cart.entity.CartItem;
import com.shishir.ecommerce.cart.service.CartService;
import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
public class CartController {

    private final CartService cartService;
    private final CurrentUserProvider currentUserProvider;

    public CartController(CartService cartService, CurrentUserProvider currentUserProvider) {
        this.cartService = cartService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping(Routes.CART)
    public ResponseEntity<CartResponse> getCart() {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("GET {} userId={}", Routes.CART, userId);
        Cart cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(toResponse(cart, cartService.getCartItems(userId)));
    }

    @PostMapping(Routes.CART_ITEMS)
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody CartAddItemRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("POST {} userId={} productId={} quantity={}",
                Routes.CART_ITEMS, userId, request.getProductId(), request.getQuantity());
        Cart cart = cartService.addItemToCart(userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(toResponse(cart, cartService.getCartItems(userId)));
    }

    @PutMapping(Routes.CART_ITEM_BY_ID)
    public ResponseEntity<CartResponse> updateItem(@PathVariable Long cartItemId,
                                                     @Valid @RequestBody CartUpdateItemRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("PUT {} userId={} cartItemId={} quantity={}",
                Routes.CART_ITEM_BY_ID, userId, cartItemId, request.getQuantity());
        Cart cart = cartService.updateCartItemQuantity(userId, cartItemId, request.getQuantity());
        return ResponseEntity.ok(toResponse(cart, cartService.getCartItems(userId)));
    }

    @DeleteMapping(Routes.CART_ITEM_BY_ID)
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long cartItemId) {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("DELETE {} userId={} cartItemId={}", Routes.CART_ITEM_BY_ID, userId, cartItemId);
        Cart cart = cartService.removeItemFromCart(userId, cartItemId);
        return ResponseEntity.ok(toResponse(cart, cartService.getCartItems(userId)));
    }

    @DeleteMapping(Routes.CART)
    public ResponseEntity<Void> clear() {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("DELETE {} userId={}", Routes.CART, userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    // cartItems come from a dedicated query rather than cart.getCartItems() — that
    // association is a lazy @OneToMany, and with open-in-view disabled it's no longer
    // safe to touch by the time the response is built here.
    private CartResponse toResponse(Cart cart, List<CartItem> cartItems) {
        List<CartItemResponse> items = cartItems.stream().map(this::toItemResponse).toList();

        int totalItems = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal totalPrice = items.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .cartItems(items)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        BigDecimal price = item.getProduct().getPrice();
        BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .price(price)
                .totalPrice(totalPrice)
                .build();
    }
}
