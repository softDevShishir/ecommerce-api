package com.shishir.ecommerce.order.service;

import com.shishir.ecommerce.cart.entity.Cart;
import com.shishir.ecommerce.cart.entity.CartItem;
import com.shishir.ecommerce.cart.repository.CartRepository;
import com.shishir.ecommerce.exception.BadRequestException;
import com.shishir.ecommerce.exception.ResourceNotFoundException;
import com.shishir.ecommerce.order.entity.Order;
import com.shishir.ecommerce.order.entity.OrderItem;
import com.shishir.ecommerce.order.entity.OrderStatus;
import com.shishir.ecommerce.order.repository.OrderRepository;
import com.shishir.ecommerce.product.entity.Product;
import com.shishir.ecommerce.product.repository.ProductRepository;
import com.shishir.ecommerce.user.entity.User;
import com.shishir.ecommerce.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
                         ProductRepository productRepository, CartRepository cartRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
    }

    public Order createOrder(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id " + userId));

        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BadRequestException("Cannot create an order from an empty cart");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            Integer quantity = cartItem.getQuantity();

            if (product.getStockQuantity() < quantity) {
                throw new BadRequestException("Insufficient stock for product id " + product.getId());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);

            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);

            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        // total_price is NOT NULL in schema.sql, so it must be set before the first
        // insert rather than backfilled after — order_items cascade-persist with it.
        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);
        Order savedOrder = orderRepository.save(order);

        cart.getCartItems().clear();
        cartRepository.save(cart);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + userId + " not found"));

        List<Order> orders = orderRepository.findByUserIdWithItems(userId);
        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No orders found for user id " + userId);
        }
        return orders;
    }

    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        if (orders.isEmpty()) {
            throw new ResourceNotFoundException("No orders found with status " + status);
        }
        return orders;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
