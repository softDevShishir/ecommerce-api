package com.shishir.ecommerce.order.controller;

import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.order.dto.OrderItemResponse;
import com.shishir.ecommerce.order.dto.OrderResponse;
import com.shishir.ecommerce.order.dto.OrderStatusUpdateRequest;
import com.shishir.ecommerce.order.entity.Order;
import com.shishir.ecommerce.order.entity.OrderItem;
import com.shishir.ecommerce.order.service.OrderService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@Tag(name = "Order Management", description = "Order creation, retrieval, and status management")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    public OrderController(OrderService orderService, CurrentUserProvider currentUserProvider) {
        this.orderService = orderService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping(Routes.ORDERS)
    @Operation(summary = "Create order from cart", description = "Create new order from user's shopping cart (requires authentication)")
    @ApiResponse(responseCode = "201", description = "Order created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class)))
    @ApiResponse(responseCode = "400", description = "Cart is empty or invalid")
    @ApiResponse(responseCode = "404", description = "User or cart not found")
    public ResponseEntity<OrderResponse> create() {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("POST {} userId={}", Routes.ORDERS, userId);
        Order order = orderService.createOrder(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(order));
    }

    @GetMapping(Routes.ORDER_BY_ID)
    @Operation(summary = "Get order by ID", description = "Retrieve order details by order ID")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        log.info("GET {} id={}", Routes.ORDER_BY_ID, id);
        return ResponseEntity.ok(toResponse(orderService.getOrderById(id)));
    }

    @GetMapping(Routes.ORDERS)
    @Operation(summary = "Get user's orders", description = "Retrieve all orders for authenticated user")
    @ApiResponse(responseCode = "200", description = "Orders retrieved",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class))))
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("GET {} userId={}", Routes.ORDERS, userId);
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId).stream().map(this::toResponse).toList());
    }

    @PutMapping(Routes.ORDER_STATUS)
    @Operation(summary = "Update order status", description = "Update order status (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Order status updated")
    @ApiResponse(responseCode = "404", description = "Order not found")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody OrderStatusUpdateRequest request) {
        log.info("PUT {} id={} status={}", Routes.ORDER_STATUS, id, request.getStatus());
        Order order = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(toResponse(order));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems() == null
                ? List.of()
                : order.getOrderItems().stream().map(this::toItemResponse).toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .orderDate(order.getOrderDate())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .orderItems(items)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }
}
