package com.shishir.ecommerce.product.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a product review")
public class CreateReviewRequest {

    @NotNull
    @Schema(description = "Product ID being reviewed", example = "1")
    private Long productId;

    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "Rating from 1 to 5", example = "5")
    private Integer rating;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Review title", example = "Great product!")
    private String title;

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "Review content", example = "Exactly what I needed, works perfectly.")
    private String content;
}
