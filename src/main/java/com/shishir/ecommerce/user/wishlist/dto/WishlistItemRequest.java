package com.shishir.ecommerce.user.wishlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add a product to the wishlist")
public class WishlistItemRequest {

    @NotNull
    @Schema(description = "Product ID to add to the wishlist", example = "1")
    private Long productId;
}
