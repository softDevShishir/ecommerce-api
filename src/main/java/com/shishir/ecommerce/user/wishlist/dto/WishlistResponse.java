package com.shishir.ecommerce.user.wishlist.dto;

import com.shishir.ecommerce.product.dto.ProductResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User wishlist response")
public class WishlistResponse {

    @Schema(description = "Wishlist ID", example = "1")
    private Long id;

    @Schema(description = "Owning user's ID", example = "1")
    private Long userId;

    @Schema(description = "Number of products in the wishlist", example = "3")
    private Integer productCount;

    @Schema(description = "Products in the wishlist")
    private List<ProductResponse> products;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
