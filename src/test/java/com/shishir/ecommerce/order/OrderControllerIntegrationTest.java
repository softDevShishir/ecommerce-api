package com.shishir.ecommerce.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shishir.ecommerce.cart.dto.CartAddItemRequest;
import com.shishir.ecommerce.cart.repository.CartRepository;
import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.exception.ExceptionResponse;
import com.shishir.ecommerce.order.dto.OrderResponse;
import com.shishir.ecommerce.order.repository.OrderItemRepository;
import com.shishir.ecommerce.order.repository.OrderRepository;
import com.shishir.ecommerce.product.entity.Product;
import com.shishir.ecommerce.product.repository.ProductRepository;
import com.shishir.ecommerce.security.JwtTokenProvider;
import com.shishir.ecommerce.security.UserRole;
import com.shishir.ecommerce.user.entity.User;
import com.shishir.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs {@link com.shishir.ecommerce.order.controller.OrderController} against a real
 * database (see src/test/resources/application.yml). Order creation resolves the caller
 * from the JWT via {@code CurrentUserProvider}, so each test mints its own token rather
 * than passing a user id.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create an order from the caller's cart")
    void testCreateOrderFromCart() throws Exception {
        User user = createUser("orderer@example.com");
        String token = tokenFor(user);
        Product product = createProduct("Monitor", new BigDecimal("199.99"), 10);

        addItemToCart(token, product.getId(), 3);

        MvcResult result = mockMvc.perform(post(Routes.ORDERS)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), OrderResponse.class);

        assertEquals(user.getId(), response.getUserId());
        assertEquals(1, response.getOrderItems().size());
        assertEquals(product.getId(), response.getOrderItems().get(0).getProductId());
        assertEquals(3, response.getOrderItems().get(0).getQuantity());
        assertEquals(0, new BigDecimal("599.97").compareTo(response.getTotalPrice()));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(7, updated.getStockQuantity());

        MvcResult cartResult = mockMvc.perform(get(Routes.CART)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(cartResult.getResponse().getContentAsString().contains("\"cartItems\":[]"));
    }

    @Test
    @DisplayName("Should reject creating an order from an empty cart")
    void testCreateOrderFromEmptyCart() throws Exception {
        User user = createUser("emptycart@example.com");
        String token = tokenFor(user);
        Product product = createProduct("Keyboard", new BigDecimal("49.99"), 5);

        // Cart rows only exist once an item is added, so add then clear it to reach a
        // genuinely empty (as opposed to nonexistent) cart.
        addItemToCart(token, product.getId(), 1);
        mockMvc.perform(delete(Routes.CART)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        MvcResult result = mockMvc.perform(post(Routes.ORDERS)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andReturn();

        ExceptionResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(), ExceptionResponse.class);
        assertTrue(error.getMessage().contains("empty cart"));
    }

    @Test
    @DisplayName("Should 404 when creating an order and the user has no cart at all")
    void testCreateOrderWithNoCart() throws Exception {
        User user = createUser("nocart@example.com");
        String token = tokenFor(user);

        mockMvc.perform(post(Routes.ORDERS)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject creating an order without authentication")
    void testCreateOrderRequiresAuthentication() throws Exception {
        mockMvc.perform(post(Routes.ORDERS))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should list the caller's orders with their items")
    void testGetMyOrders() throws Exception {
        User user = createUser("myorders@example.com");
        String token = tokenFor(user);
        Product product = createProduct("Desk Lamp", new BigDecimal("29.99"), 10);

        addItemToCart(token, product.getId(), 2);
        mockMvc.perform(post(Routes.ORDERS).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get(Routes.ORDERS)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        OrderResponse[] responses = objectMapper.readValue(
                result.getResponse().getContentAsString(), OrderResponse[].class);

        assertEquals(1, responses.length);
        assertEquals(1, responses[0].getOrderItems().size());
        assertEquals(product.getId(), responses[0].getOrderItems().get(0).getProductId());
    }

    @Test
    @DisplayName("Should 404 when the caller has no orders")
    void testGetMyOrdersWhenNoneExist() throws Exception {
        User user = createUser("nomyorders@example.com");
        String token = tokenFor(user);

        mockMvc.perform(get(Routes.ORDERS)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get an order by id with its items")
    void testGetOrderById() throws Exception {
        User user = createUser("byid@example.com");
        String token = tokenFor(user);
        Product product = createProduct("Mousepad", new BigDecimal("9.99"), 10);

        addItemToCart(token, product.getId(), 1);
        MvcResult createResult = mockMvc.perform(post(Routes.ORDERS).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        OrderResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), OrderResponse.class);

        MvcResult result = mockMvc.perform(get(Routes.ORDER_BY_ID, created.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        OrderResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), OrderResponse.class);
        assertEquals(created.getId(), response.getId());
        assertEquals(1, response.getOrderItems().size());
        assertEquals(product.getId(), response.getOrderItems().get(0).getProductId());
    }

    private void addItemToCart(String token, Long productId, int quantity) throws Exception {
        CartAddItemRequest request = CartAddItemRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();

        mockMvc.perform(post(Routes.CART_ITEMS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.USER);
        return userRepository.save(user);
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.generateToken(user.getEmail(), user.getId().toString(), user.getRole());
    }

    private Product createProduct(String name, BigDecimal price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " description");
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setCategory("Electronics");
        return productRepository.save(product);
    }
}
