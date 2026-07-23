package com.shishir.ecommerce.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shishir.ecommerce.cart.dto.CartAddItemRequest;
import com.shishir.ecommerce.cart.dto.CartResponse;
import com.shishir.ecommerce.cart.dto.CartUpdateItemRequest;
import com.shishir.ecommerce.cart.entity.Cart;
import com.shishir.ecommerce.cart.repository.CartRepository;
import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.exception.ExceptionResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs {@link CartController} against a real database (see
 * src/test/resources/application.yml). Every cart endpoint resolves the
 * caller from the JWT via {@code CurrentUserProvider}, so each test mints
 * its own token rather than passing a user id.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        cartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        cartRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should get the caller's cart")
    void testGetCartByUserId() throws Exception {
        User user = createUser("cartowner@example.com");
        String token = tokenFor(user);

        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);

        MvcResult result = mockMvc.perform(get(Routes.CART)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        CartResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CartResponse.class);

        assertEquals(user.getId(), response.getUserId());
        assertEquals(0, response.getTotalItems());
        assertTrue(response.getCartItems().isEmpty());
    }

    @Test
    @DisplayName("Should add a product to the cart")
    void testAddItemToCart() throws Exception {
        User user = createUser("adder@example.com");
        String token = tokenFor(user);
        Product product = createProduct("Webcam", 10);

        CartAddItemRequest request = CartAddItemRequest.builder()
                .productId(product.getId())
                .quantity(2)
                .build();

        MvcResult result = mockMvc.perform(post(Routes.CART_ITEMS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        CartResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CartResponse.class);

        assertEquals(1, response.getCartItems().size());
        assertEquals(2, response.getCartItems().get(0).getQuantity());
        assertEquals(product.getId(), response.getCartItems().get(0).getProductId());
    }

    @Test
    @DisplayName("Should reject adding more items than are in stock")
    void testAddItemToCartInsufficientStock() throws Exception {
        User user = createUser("shortstock@example.com");
        String token = tokenFor(user);
        Product product = createProduct("Rare Part", 5);

        CartAddItemRequest request = CartAddItemRequest.builder()
                .productId(product.getId())
                .quantity(10)
                .build();

        MvcResult result = mockMvc.perform(post(Routes.CART_ITEMS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ExceptionResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(), ExceptionResponse.class);
        assertTrue(error.getMessage().contains("Insufficient stock"));
    }

    @Test
    @DisplayName("Should update a cart item's quantity")
    void testUpdateCartItemQuantity() throws Exception {
        User user = createUser("updater@example.com");
        String token = tokenFor(user);
        Product product = createProduct("Headset", 20);

        Long cartItemId = addItem(token, product.getId(), 1);

        CartUpdateItemRequest request = CartUpdateItemRequest.builder().quantity(5).build();

        MvcResult result = mockMvc.perform(put(Routes.CART_ITEM_BY_ID, cartItemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        CartResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CartResponse.class);

        assertEquals(1, response.getCartItems().size());
        assertEquals(5, response.getCartItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("Should remove an item from the cart")
    void testRemoveItemFromCart() throws Exception {
        User user = createUser("remover@example.com");
        String token = tokenFor(user);
        Product product = createProduct("Cable", 20);

        Long cartItemId = addItem(token, product.getId(), 1);

        MvcResult result = mockMvc.perform(delete(Routes.CART_ITEM_BY_ID, cartItemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        CartResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CartResponse.class);

        assertTrue(response.getCartItems().isEmpty());
    }

    @Test
    @DisplayName("Should clear all items from the cart")
    void testClearCart() throws Exception {
        User user = createUser("clearer@example.com");
        String token = tokenFor(user);
        Product first = createProduct("Chair", 10);
        Product second = createProduct("Lamp", 10);

        addItem(token, first.getId(), 1);
        addItem(token, second.getId(), 1);

        mockMvc.perform(delete(Routes.CART)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        MvcResult result = mockMvc.perform(get(Routes.CART)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        CartResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CartResponse.class);
        assertTrue(response.getCartItems().isEmpty());
    }

    private Long addItem(String token, Long productId, int quantity) throws Exception {
        CartAddItemRequest request = CartAddItemRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();

        MvcResult result = mockMvc.perform(post(Routes.CART_ITEMS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        CartResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CartResponse.class);
        return response.getCartItems().get(response.getCartItems().size() - 1).getId();
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

    private Product createProduct(String name, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " description");
        product.setPrice(new BigDecimal("50.00"));
        product.setStockQuantity(stock);
        product.setCategory("Misc");
        return productRepository.save(product);
    }
}
