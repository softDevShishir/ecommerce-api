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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Shopping Cart", description = "Cart management, add/remove items, checkout")
public class CartController {

    private final CartService cartService;
    private final CurrentUserProvider currentUserProvider;

    public CartController(CartService cartService, CurrentUserProvider currentUserProvider) {
        this.cartService = cartService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping(Routes.CART)
    @Operation(summary = "Get shopping cart", description = "Retrieve current user's shopping cart with all items")
    @ApiResponse(responseCode = "200", description = "Cart retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class)))
    @ApiResponse(responseCode = "404", description = "Cart not found")
    public ResponseEntity<CartResponse> getCart() {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("GET {} userId={}", Routes.CART, userId);
        Cart cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(toResponse(cart, cartService.getCartItems(userId)));
    }

    @PostMapping(Routes.CART_ITEMS)
    @Operation(summary = "Add item to cart", description = "Add product to shopping cart with quantity")
    @ApiResponse(responseCode = "200", description = "Item added to cart",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid quantity or insufficient stock")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody CartAddItemRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("POST {} userId={} productId={} quantity={}",
                Routes.CART_ITEMS, userId, request.getProductId(), request.getQuantity());
        Cart cart = cartService.addItemToCart(userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(toResponse(cart, cartService.getCartItems(userId)));
    }

    @PutMapping(Routes.CART_ITEM_BY_ID)
    @Operation(summary = "Update cart item quantity", description = "Update quantity of item in cart")
    @ApiResponse(responseCode = "200", description = "Item quantity updated")
    @ApiResponse(responseCode = "400", description = "Invalid quantity")
    @ApiResponse(responseCode = "404", description = "Cart item not found")
    public ResponseEntity<CartResponse> updateItem(@PathVariable Long cartItemId,
                                                     @Valid @RequestBody CartUpdateItemRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("PUT {} userId={} cartItemId={} quantity={}",
                Routes.CART_ITEM_BY_ID, userId, cartItemId, request.getQuantity());
        Cart cart = cartService.updateCartItemQuantity(userId, cartItemId, request.getQuantity());
        return ResponseEntity.ok(toResponse(cart, cartService.getCartItems(userId)));
    }

    @DeleteMapping(Routes.CART_ITEM_BY_ID)
    @Operation(summary = "Remove item from cart", description = "Remove product from shopping cart")
    @ApiResponse(responseCode = "200", description = "Item removed from cart")
    @ApiResponse(responseCode = "404", description = "Cart item not found")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long cartItemId) {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("DELETE {} userId={} cartItemId={}", Routes.CART_ITEM_BY_ID, userId, cartItemId);
        Cart cart = cartService.removeItemFromCart(userId, cartItemId);
        return ResponseEntity.ok(toResponse(cart, cartService.getCartItems(userId)));
    }

    @DeleteMapping(Routes.CART)
    @Operation(summary = "Clear shopping cart", description = "Remove all items from shopping cart")
    @ApiResponse(responseCode = "204", description = "Cart cleared")
    @ApiResponse(responseCode = "404", description = "Cart not found")
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
