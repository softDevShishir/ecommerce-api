package com.shishir.ecommerce.product.review.controller;

import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.product.review.dto.CreateReviewRequest;
import com.shishir.ecommerce.product.review.dto.ProductRatingResponse;
import com.shishir.ecommerce.product.review.dto.ReviewResponse;
import com.shishir.ecommerce.product.review.dto.UpdateReviewRequest;
import com.shishir.ecommerce.product.review.service.ReviewService;
import com.shishir.ecommerce.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@Tag(name = "Product Reviews", description = "Manage product ratings and reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserProvider currentUserProvider;

    public ReviewController(ReviewService reviewService, CurrentUserProvider currentUserProvider) {
        this.reviewService = reviewService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping(Routes.PRODUCT_REVIEWS)
    @Operation(summary = "Create review", description = "Submit a review for a product (requires authentication, one review per user per product)")
    @ApiResponse(responseCode = "201", description = "Review created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewResponse.class)))
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "User already reviewed this product")
    public ResponseEntity<ReviewResponse> createReview(@PathVariable Long productId,
                                                        @Valid @RequestBody CreateReviewRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        request.setProductId(productId);
        log.info("POST {} productId={} userId={}", Routes.PRODUCT_REVIEWS, productId, userId);
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(Routes.PRODUCT_REVIEW_BY_ID)
    @Operation(summary = "Update review", description = "Update the authenticated user's own review")
    @ApiResponse(responseCode = "200", description = "Review updated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewResponse.class)))
    @ApiResponse(responseCode = "401", description = "Review does not belong to the authenticated user")
    @ApiResponse(responseCode = "404", description = "Review not found")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long productId, @PathVariable Long reviewId,
                                                        @Valid @RequestBody UpdateReviewRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("PUT {} productId={} reviewId={} userId={}", Routes.PRODUCT_REVIEW_BY_ID, productId, reviewId, userId);
        return ResponseEntity.ok(reviewService.updateReview(reviewId, userId, request));
    }

    @DeleteMapping(Routes.PRODUCT_REVIEW_BY_ID)
    @Operation(summary = "Delete review", description = "Delete the authenticated user's own review")
    @ApiResponse(responseCode = "204", description = "Review deleted")
    @ApiResponse(responseCode = "401", description = "Review does not belong to the authenticated user")
    @ApiResponse(responseCode = "404", description = "Review not found")
    public ResponseEntity<Void> deleteReview(@PathVariable Long productId, @PathVariable Long reviewId) {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("DELETE {} productId={} reviewId={} userId={}", Routes.PRODUCT_REVIEW_BY_ID, productId, reviewId, userId);
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(Routes.PRODUCT_REVIEWS)
    @Operation(summary = "Get product reviews", description = "Retrieve all reviews for a product, newest first")
    @ApiResponse(responseCode = "200", description = "Reviews retrieved",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReviewResponse.class))))
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<List<ReviewResponse>> getProductReviews(@PathVariable Long productId) {
        log.info("GET {} productId={}", Routes.PRODUCT_REVIEWS, productId);
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    @GetMapping(Routes.PRODUCT_RATING)
    @Operation(summary = "Get product rating summary", description = "Retrieve average rating, distribution, and reviews for a product")
    @ApiResponse(responseCode = "200", description = "Rating summary retrieved",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductRatingResponse.class)))
    @ApiResponse(responseCode = "404", description = "Product not found")
    public ResponseEntity<ProductRatingResponse> getProductRating(@PathVariable Long productId) {
        log.info("GET {} productId={}", Routes.PRODUCT_RATING, productId);
        return ResponseEntity.ok(reviewService.getProductRating(productId));
    }

    @GetMapping(Routes.MY_REVIEWS)
    @Operation(summary = "Get my reviews", description = "Retrieve all reviews written by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Reviews retrieved",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReviewResponse.class))))
    public ResponseEntity<List<ReviewResponse>> getUserReviews() {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("GET {} userId={}", Routes.MY_REVIEWS, userId);
        return ResponseEntity.ok(reviewService.getUserReviews(userId));
    }
}
