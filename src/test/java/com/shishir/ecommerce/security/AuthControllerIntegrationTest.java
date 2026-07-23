package com.shishir.ecommerce.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.exception.ExceptionResponse;
import com.shishir.ecommerce.user.dto.UserLoginRequest;
import com.shishir.ecommerce.user.dto.UserRegisterRequest;
import com.shishir.ecommerce.user.dto.UserResponse;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs {@link AuthController} against a real database (see
 * src/test/resources/application.yml — a separate "ecommerce_test" schema,
 * rebuilt per test run with ddl-auto=create-drop, so these never touch dev data).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    private static final String RAW_PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register a new user and return a JWT")
    void testRegisterNewUser() throws Exception {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("newuser@example.com")
                .password(RAW_PASSWORD)
                .firstName("New")
                .lastName("User")
                .build();

        MvcResult result = mockMvc.perform(post(Routes.AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponse.class);

        assertNotNull(response.getToken());
        assertFalse(response.getToken().isEmpty());
        assertEquals("Bearer", response.getType());
        assertNotNull(response.getUserId());
        assertEquals(request.getEmail(), response.getEmail());
        assertEquals(UserRole.USER, response.getRole());
        // Computed from real elapsed time between token mint and this assertion,
        // so allow a small tolerance instead of asserting the exact 86400 ceiling.
        assertTrue(response.getExpiresIn() > 86390);
        assertTrue(userRepository.existsByEmail(request.getEmail()));
    }

    @Test
    @DisplayName("Should reject registration when the email is already taken")
    void testRegisterDuplicateEmail() throws Exception {
        userRepository.save(buildUser("test@example.com"));

        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("test@example.com")
                .password(RAW_PASSWORD)
                .firstName("Another")
                .lastName("Person")
                .build();

        MvcResult result = mockMvc.perform(post(Routes.AUTH_REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        ExceptionResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(), ExceptionResponse.class);
        assertTrue(error.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Should log in with correct credentials")
    void testLoginSuccessful() throws Exception {
        User user = userRepository.save(buildUser("login@example.com"));

        UserLoginRequest request = UserLoginRequest.builder()
                .email(user.getEmail())
                .password(RAW_PASSWORD)
                .build();

        MvcResult result = mockMvc.perform(post(Routes.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponse.class);

        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals(user.getId(), response.getUserId());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(UserRole.USER, response.getRole());
    }

    @Test
    @DisplayName("Should reject login with the wrong password")
    void testLoginInvalidPassword() throws Exception {
        User user = userRepository.save(buildUser("wrongpass@example.com"));

        UserLoginRequest request = UserLoginRequest.builder()
                .email(user.getEmail())
                .password("not-the-right-password")
                .build();

        mockMvc.perform(post(Routes.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject login for an email that isn't registered")
    void testLoginUserNotFound() throws Exception {
        UserLoginRequest request = UserLoginRequest.builder()
                .email("nobody@example.com")
                .password(RAW_PASSWORD)
                .build();

        mockMvc.perform(post(Routes.AUTH_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return the caller's profile for a valid bearer token")
    void testGetCurrentUser() throws Exception {
        User user = userRepository.save(buildUser("me@example.com"));
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getId().toString(), user.getRole());

        MvcResult result = mockMvc.perform(get(Routes.AUTH_ME)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), UserResponse.class);

        assertEquals(user.getId(), response.getId());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getFirstName(), response.getFirstName());
        assertEquals(user.getLastName(), response.getLastName());
        assertEquals(user.getRole(), response.getRole());
    }

    @Test
    @DisplayName("Should reject /me when no Authorization header is sent")
    void testGetCurrentUserNoToken() throws Exception {
        mockMvc.perform(get(Routes.AUTH_ME))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject /me when the bearer token is malformed")
    void testGetCurrentUserInvalidToken() throws Exception {
        mockMvc.perform(get(Routes.AUTH_ME)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    private User buildUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(RAW_PASSWORD));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.USER);
        return user;
    }
}
