package com.shishir.ecommerce.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.exception.ExceptionResponse;
import com.shishir.ecommerce.product.dto.ProductCreateRequest;
import com.shishir.ecommerce.product.dto.ProductResponse;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs {@link ProductController} against a real database (see
 * src/test/resources/application.yml). Read endpoints are public per
 * SecurityConfig; POST only requires an authenticated caller (any role) —
 * the admin user below is used for realism, not because the role is enforced.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should list all products")
    void testGetAllProducts() throws Exception {
        productRepository.save(buildProduct("Laptop", "Electronics", "1200.00", 10));
        productRepository.save(buildProduct("Desk", "Furniture", "300.00", 5));
        productRepository.save(buildProduct("Mouse", "Electronics", "25.00", 50));

        MvcResult result = mockMvc.perform(get(Routes.PRODUCTS))
                .andExpect(status().isOk())
                .andReturn();

        List<ProductResponse> products = readList(result);
        assertEquals(3, products.size());
    }

    @Test
    @DisplayName("Should get a product by id")
    void testGetProductById() throws Exception {
        Product saved = productRepository.save(buildProduct("Keyboard", "Electronics", "75.00", 20));

        MvcResult result = mockMvc.perform(get(Routes.PRODUCT_BY_ID, saved.getId()))
                .andExpect(status().isOk())
                .andReturn();

        ProductResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProductResponse.class);

        assertEquals(saved.getId(), response.getId());
        assertEquals("Keyboard", response.getName());
        assertEquals(0, saved.getPrice().compareTo(response.getPrice()));
    }

    @Test
    @DisplayName("Should return 404 for a non-existent product id")
    void testGetProductByIdNotFound() throws Exception {
        mockMvc.perform(get(Routes.PRODUCT_BY_ID, 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should search products by name")
    void testSearchProductByName() throws Exception {
        productRepository.save(buildProduct("Laptop", "Electronics", "1200.00", 10));
        productRepository.save(buildProduct("Desk", "Furniture", "300.00", 5));

        MvcResult result = mockMvc.perform(get(Routes.PRODUCTS).param("name", "Laptop"))
                .andExpect(status().isOk())
                .andReturn();

        List<ProductResponse> products = readList(result);
        assertEquals(1, products.size());
        assertEquals("Laptop", products.get(0).getName());
    }

    @Test
    @DisplayName("Should filter products by category")
    void testFilterProductByCategory() throws Exception {
        productRepository.save(buildProduct("Laptop", "Electronics", "1200.00", 10));
        productRepository.save(buildProduct("Mouse", "Electronics", "25.00", 50));
        productRepository.save(buildProduct("Desk", "Furniture", "300.00", 5));

        MvcResult result = mockMvc.perform(get(Routes.PRODUCTS).param("category", "Electronics"))
                .andExpect(status().isOk())
                .andReturn();

        List<ProductResponse> products = readList(result);
        assertEquals(2, products.size());
        assertTrue(products.stream().allMatch(p -> p.getCategory().equals("Electronics")));
    }

    @Test
    @DisplayName("Should filter products by price range")
    void testFilterProductByPriceRange() throws Exception {
        productRepository.save(buildProduct("Cheap", "Misc", "100.00", 10));
        productRepository.save(buildProduct("Mid", "Misc", "500.00", 10));
        productRepository.save(buildProduct("Expensive", "Misc", "1000.00", 10));

        MvcResult result = mockMvc.perform(get(Routes.PRODUCTS)
                        .param("minPrice", "200")
                        .param("maxPrice", "700"))
                .andExpect(status().isOk())
                .andReturn();

        List<ProductResponse> products = readList(result);
        assertEquals(1, products.size());
        assertEquals("Mid", products.get(0).getName());
    }

    @Test
    @DisplayName("Should create a product for an authenticated caller")
    void testCreateProduct() throws Exception {
        String token = createUserAndToken("admin@example.com", UserRole.ADMIN);

        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Monitor")
                .description("27-inch 4K display")
                .price(new BigDecimal("399.99"))
                .stockQuantity(15)
                .category("Electronics")
                .build();

        MvcResult result = mockMvc.perform(post(Routes.PRODUCTS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), ProductResponse.class);

        assertEquals("Monitor", response.getName());
        assertEquals(0, request.getPrice().compareTo(response.getPrice()));
        assertEquals(1, productRepository.count());
    }

    @Test
    @DisplayName("Should reject product creation with a non-positive price")
    void testCreateProductInvalidPrice() throws Exception {
        String token = createUserAndToken("buyer@example.com", UserRole.USER);

        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Broken")
                .description("Invalid price")
                .price(new BigDecimal("-10.00"))
                .stockQuantity(5)
                .category("Misc")
                .build();

        MvcResult result = mockMvc.perform(post(Routes.PRODUCTS)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ExceptionResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(), ExceptionResponse.class);
        assertEquals("VALIDATION_ERROR", error.getError());
    }

    private Product buildProduct(String name, String category, String price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " description");
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stock);
        product.setCategory(category);
        return product;
    }

    private String createUserAndToken(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        User saved = userRepository.save(user);
        return jwtTokenProvider.generateToken(saved.getEmail(), saved.getId().toString(), saved.getRole());
    }

    private List<ProductResponse> readList(MvcResult result) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<List<ProductResponse>>() {
                });
    }
}
