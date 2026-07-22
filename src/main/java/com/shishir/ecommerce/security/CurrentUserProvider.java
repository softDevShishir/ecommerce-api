package com.shishir.ecommerce.security;

import com.shishir.ecommerce.user.entity.User;
import com.shishir.ecommerce.user.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated caller from Spring Security's context, assuming
 * the principal name is the user's email. That assumption only holds once a
 * JWT filter populates the context that way — no SecurityConfig/filter
 * exists in this project yet, so this reflects whatever principal Spring
 * Security's default auto-configuration puts in place until then.
 */
@Component
public class CurrentUserProvider {

    private final UserService userService;

    public CurrentUserProvider(UserService userService) {
        this.userService = userService;
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByEmail(email);
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
