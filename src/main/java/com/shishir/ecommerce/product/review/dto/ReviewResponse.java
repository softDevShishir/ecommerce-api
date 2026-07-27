package com.shishir.ecommerce.product.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product review response")
public class ReviewResponse {

    @Schema(description = "Review ID", example = "1")
    private Long id;

    @Schema(description = "Product ID", example = "1")
    private Long productId;

    @Schema(description = "ID of the user who wrote the review", example = "1")
    private Long userId;

    @Schema(description = "Display name of the reviewer", example = "John Doe")
    private String userName;

    @Schema(description = "Rating from 1 to 5", example = "5")
    private Integer rating;

    @Schema(description = "Review title", example = "Great product!")
    private String title;

    @Schema(description = "Review content", example = "Exactly what I needed, works perfectly.")
    private String content;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Last update time")
    private LocalDateTime updatedAt;
}
