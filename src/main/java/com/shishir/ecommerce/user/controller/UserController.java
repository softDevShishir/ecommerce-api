package com.shishir.ecommerce.user.controller;

import com.shishir.ecommerce.config.Routes;
import com.shishir.ecommerce.user.dto.UserRegisterRequest;
import com.shishir.ecommerce.user.dto.UserResponse;
import com.shishir.ecommerce.user.dto.UserUpdateRequest;
import com.shishir.ecommerce.user.entity.User;
import com.shishir.ecommerce.user.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(Routes.USER_REGISTER)
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("POST {} email={}", Routes.USER_REGISTER, request.getEmail());
        User user = userService.registerUser(
                request.getEmail(), request.getPassword(), request.getFirstName(), request.getLastName());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @GetMapping(Routes.USER_BY_ID)
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        log.info("GET {} id={}", Routes.USER_BY_ID, id);
        return ResponseEntity.ok(toResponse(userService.getUserById(id)));
    }

    @PutMapping(Routes.USER_BY_ID)
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        log.info("PUT {} id={}", Routes.USER_BY_ID, id);
        User user = userService.updateUser(id, request.getFirstName(), request.getLastName());
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping(Routes.USERS)
    public ResponseEntity<List<UserResponse>> getAll() {
        log.info("GET {}", Routes.USERS);
        return ResponseEntity.ok(userService.getAll().stream().map(this::toResponse).toList());
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
