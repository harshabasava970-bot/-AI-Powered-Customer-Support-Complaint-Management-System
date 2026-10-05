package com.supportportal.controller.api;

import com.supportportal.dto.*;
import com.supportportal.service.AdminService;
import com.supportportal.service.CategoryService;
import com.supportportal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminApiController {

    private final AdminService adminService;
    private final UserService userService;
    private final CategoryService categoryService;

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getDashboardStats(), "OK"));
    }

    @GetMapping("/agents/stats")
    public ResponseEntity<ApiResponse<List<AgentStatsDto>>> agentStats() {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getAgentStats(), "OK"));
    }

    // ── User management ───────────────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> allUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.findAllUsers(), "OK"));
    }

    @GetMapping("/users/customers")
    public ResponseEntity<ApiResponse<List<UserDto>>> customers() {
        return ResponseEntity.ok(ApiResponse.success(userService.findAllCustomers(), "OK"));
    }

    @GetMapping("/users/agents")
    public ResponseEntity<ApiResponse<List<UserDto>>> agents() {
        return ResponseEntity.ok(ApiResponse.success(userService.findAllAgents(), "OK"));
    }

    @PostMapping("/users/agents")
    public ResponseEntity<ApiResponse<UserDto>> createAgent(
            @Valid @RequestBody RegisterRequest request) {
        UserDto created = userService.registerAgent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Agent created"));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateUser(id, request), "User updated"));
    }

    @PatchMapping("/users/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleUser(@PathVariable Long id) {
        userService.toggleEnabled(id);
        return ResponseEntity.ok(ApiResponse.success("User status toggled"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted"));
    }

    // ── Category management ───────────────────────────────────────────────────
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> categories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findAll(), "OK"));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(
            @Valid @RequestBody CategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(categoryService.create(dto), "Category created"));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @PathVariable Long id, @Valid @RequestBody CategoryDto dto) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.update(id, dto), "Category updated"));
    }

    @PatchMapping("/categories/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleCategory(@PathVariable Long id) {
        categoryService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success("Category toggled"));
    }
}
