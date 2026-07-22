package com.shishir.ecommerce.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * totalItems and totalPrice are aggregated across cartItems by the
 * mapping layer, not stored.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long id;
    private Long userId;
    private List<CartItemResponse> cartItems;
    private Integer totalItems;
    private BigDecimal totalPrice;
}
