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
@Schema(description = "Request to update an existing product review")
public class UpdateReviewRequest {

    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "Rating from 1 to 5", example = "4")
    private Integer rating;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Review title", example = "Updated thoughts")
    private String title;

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "Review content", example = "After more use, here is my updated take.")
    private String content;
}
