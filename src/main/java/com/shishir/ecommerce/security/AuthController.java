package com.shishir.ecommerce.security;

import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.user.dto.UserLoginRequest;
import com.shishir.ecommerce.user.dto.UserRegisterRequest;
import com.shishir.ecommerce.user.dto.UserResponse;
import com.shishir.ecommerce.user.entity.User;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping(Routes.AUTH_REGISTER)
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("POST {} email={}", Routes.AUTH_REGISTER, request.getEmail());
        LoginResponse response = authService.registerUser(request);
        log.info("User registered successfully: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(Routes.AUTH_LOGIN)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        log.info("POST {} email={}", Routes.AUTH_LOGIN, request.getEmail());
        LoginResponse response = authService.authenticateUser(request);
        log.info("User logged in successfully: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping(Routes.AUTH_ME)
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authService.extractTokenFromHeader(authHeader);
        String email = jwtTokenProvider.getEmailFromToken(token);
        log.info("GET {} email={}", Routes.AUTH_ME, email);
        User user = authService.getCurrentUser(email);
        return ResponseEntity.ok(toResponse(user));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
