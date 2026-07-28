package com.shishir.ecommerce.user.wishlist.controller;

import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.security.CurrentUserProvider;
import com.shishir.ecommerce.user.wishlist.dto.WishlistItemRequest;
import com.shishir.ecommerce.user.wishlist.dto.WishlistResponse;
import com.shishir.ecommerce.user.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@Tag(name = "Wishlist", description = "User wishlist management")
public class WishlistController {

    private final WishlistService wishlistService;
    private final CurrentUserProvider currentUserProvider;

    public WishlistController(WishlistService wishlistService, CurrentUserProvider currentUserProvider) {
        this.wishlistService = wishlistService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping(Routes.WISHLIST)
    @Operation(summary = "Get wishlist", description = "Retrieve the authenticated user's wishlist with all products")
    @ApiResponse(responseCode = "200", description = "Wishlist retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WishlistResponse.class)))
    @ApiResponse(responseCode = "404", description = "Wishlist not found")
    public ResponseEntity<WishlistResponse> getWishlist() {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("GET {} userId={}", Routes.WISHLIST, userId);
        return ResponseEntity.ok(wishlistService.getWishlist(userId));
    }

    @PostMapping(Routes.WISHLIST_ITEMS)
    @Operation(summary = "Add product to wishlist", description = "Add a product to the authenticated user's wishlist (creates the wishlist on first use)")
    @ApiResponse(responseCode = "200", description = "Product added",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WishlistResponse.class)))
    @ApiResponse(responseCode = "400", description = "Product already in wishlist")
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<WishlistResponse> addProductToWishlist(@Valid @RequestBody WishlistItemRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("POST {} userId={} productId={}", Routes.WISHLIST_ITEMS, userId, request.getProductId());
        return ResponseEntity.ok(wishlistService.addProductToWishlist(userId, request.getProductId()));
    }

    @DeleteMapping(Routes.WISHLIST_ITEM_BY_ID)
    @Operation(summary = "Remove product from wishlist", description = "Remove a product from the authenticated user's wishlist")
    @ApiResponse(responseCode = "200", description = "Product removed",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = WishlistResponse.class)))
    @ApiResponse(responseCode = "404", description = "Wishlist or product not found")
    public ResponseEntity<WishlistResponse> removeProductFromWishlist(@PathVariable Long productId) {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("DELETE {} userId={} productId={}", Routes.WISHLIST_ITEM_BY_ID, userId, productId);
        return ResponseEntity.ok(wishlistService.removeProductFromWishlist(userId, productId));
    }

    @DeleteMapping(Routes.WISHLIST)
    @Operation(summary = "Clear wishlist", description = "Remove all products from the authenticated user's wishlist")
    @ApiResponse(responseCode = "204", description = "Wishlist cleared")
    @ApiResponse(responseCode = "404", description = "Wishlist not found")
    public ResponseEntity<Void> clearWishlist() {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("DELETE {} userId={}", Routes.WISHLIST, userId);
        wishlistService.clearWishlist(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(Routes.WISHLIST_ITEM_EXISTS)
    @Operation(summary = "Check if product is in wishlist", description = "Check whether a product is in the authenticated user's wishlist")
    @ApiResponse(responseCode = "200", description = "Existence check result",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"exists\": true}")))
    public ResponseEntity<Map<String, Boolean>> isProductInWishlist(@PathVariable Long productId) {
        Long userId = currentUserProvider.getCurrentUserId();
        boolean exists = wishlistService.isProductInWishlist(userId, productId);
        log.info("GET {} userId={} productId={} exists={}", Routes.WISHLIST_ITEM_EXISTS, userId, productId, exists);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}
