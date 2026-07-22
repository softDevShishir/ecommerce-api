package com.shishir.ecommerce.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private Long userId;
    private String email;
    private UserRole role;
    private String token;

    @Builder.Default
    private String type = "Bearer";

    private Long expiresIn;
}
