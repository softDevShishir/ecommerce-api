package com.shishir.ecommerce.order.dto;

import lombok.Data;

/**
 * Empty by design — an order is created entirely from the caller's
 * existing cart, so no request body fields are needed.
 */
@Data
public class OrderCreateRequest {
}
