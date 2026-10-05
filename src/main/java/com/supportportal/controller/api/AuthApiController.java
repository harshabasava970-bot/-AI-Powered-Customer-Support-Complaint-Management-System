package com.supportportal.controller.api;

import com.supportportal.dto.ApiResponse;
import com.supportportal.dto.RegisterRequest;
import com.supportportal.dto.UserDto;
import com.supportportal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final UserService userService;

    /**
     * POST /api/auth/register
     * Public endpoint — registers a new customer account.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserDto created = userService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Registration successful"));
    }
}
