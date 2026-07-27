package com.shishir.ecommerce.product.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated rating summary for a product")
public class ProductRatingResponse {

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "Average rating across all reviews, 0-5", example = "4.5")
    private Double averageRating;

    @Schema(description = "Total number of reviews", example = "12")
    private Integer totalReviews;

    @Schema(description = "Count of reviews per star rating, keyed 1-5")
    private Map<Integer, Long> ratingDistribution;

    @Schema(description = "All reviews for the product, newest first")
    private List<ReviewResponse> reviews;
}
