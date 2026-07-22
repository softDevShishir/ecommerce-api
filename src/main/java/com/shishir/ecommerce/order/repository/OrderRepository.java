package com.shishir.ecommerce.order.repository;

import com.shishir.ecommerce.order.entity.Order;
import com.shishir.ecommerce.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);
}
