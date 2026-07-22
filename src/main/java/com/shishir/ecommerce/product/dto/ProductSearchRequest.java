package com.shishir.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * All fields are optional filters; a null field means "don't filter on this."
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {

    private String name;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
