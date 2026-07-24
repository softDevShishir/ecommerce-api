package com.shishir.ecommerce.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response with JWT token")
public class LoginResponse {

    @Schema(description = "User ID", example = "1")
    private Long userId;

    @Schema(description = "User email", example = "john@example.com")
    private String email;

    @Schema(description = "User role", example = "USER")
    private UserRole role;

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String token;

    @Builder.Default
    @Schema(description = "Token type", example = "Bearer")
    private String type = "Bearer";

    @Schema(description = "Token expiration time in seconds", example = "86400")
    private Long expiresIn;
}
