package com.shishir.ecommerce.security;

import com.shishir.ecommerce.exception.UnauthorizedException;
import com.shishir.ecommerce.user.dto.UserLoginRequest;
import com.shishir.ecommerce.user.dto.UserRegisterRequest;
import com.shishir.ecommerce.user.entity.User;
import com.shishir.ecommerce.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserService userService, JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse registerUser(UserRegisterRequest request) {
        User user = userService.registerUser(
                request.getEmail(), request.getPassword(), request.getFirstName(), request.getLastName());
        return generateLoginResponse(user);
    }

    public LoginResponse authenticateUser(UserLoginRequest request) {
        User user = userService.getUserByEmail(request.getEmail());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return generateLoginResponse(user);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(String email) {
        return userService.getUserByEmail(email);
    }

    public String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        return authHeader.substring(BEARER_PREFIX.length());
    }

    private LoginResponse generateLoginResponse(User user) {
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getId().toString(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .expiresIn(jwtTokenProvider.getExpirationTimeFromToken(token))
                .build();
    }
}
