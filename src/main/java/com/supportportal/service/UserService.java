package com.supportportal.service;

import com.supportportal.dto.RegisterRequest;
import com.supportportal.dto.UpdateUserRequest;
import com.supportportal.dto.UserDto;
import com.supportportal.entity.Role;
import com.supportportal.entity.User;
import com.supportportal.exception.BadRequestException;
import com.supportportal.exception.DuplicateResourceException;
import com.supportportal.exception.ResourceNotFoundException;
import com.supportportal.repository.RoleRepository;
import com.supportportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Registration ──────────────────────────────────────────────────────────

    public UserDto registerCustomer(RegisterRequest request) {
        validateRegistration(request);
        Role role = findRole(Role.RoleName.ROLE_CUSTOMER);
        User user = buildUser(request, Set.of(role));
        userRepository.save(user);
        log.info("Customer registered: {}", user.getUsername());
        return toDto(user);
    }

    public UserDto registerAgent(RegisterRequest request) {
        validateRegistration(request);
        Role role = findRole(Role.RoleName.ROLE_AGENT);
        User user = buildUser(request, Set.of(role));
        userRepository.save(user);
        log.info("Agent registered: {}", user.getUsername());
        return toDto(user);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return toDto(getUser(id));
    }

    @Transactional(readOnly = true)
    public UserDto findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAllCustomers() {
        return userRepository.findAllByRole(Role.RoleName.ROLE_CUSTOMER)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAllAgents() {
        return userRepository.findAllByRole(Role.RoleName.ROLE_AGENT)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> findActiveAgents() {
        return userRepository.findActiveByRole(Role.RoleName.ROLE_AGENT)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Updates ───────────────────────────────────────────────────────────────

    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = getUser(id);
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        return toDto(userRepository.save(user));
    }

    public void toggleEnabled(Long id) {
        User user = getUser(id);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        log.info("User {} enabled={}", user.getUsername(), user.isEnabled());
    }

    public void deleteUser(Long id) {
        User user = getUser(id);
        userRepository.delete(user);
        log.info("User deleted: id={}", id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private void validateRegistration(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + req.getUsername());
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + req.getEmail());
        }
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
    }

    private Role findRole(Role.RoleName name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role not configured: " + name));
    }

    private User buildUser(RegisterRequest req, Set<Role> roles) {
        return User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phone(req.getPhone())
                .enabled(true)
                .roles(roles)
                .build();
    }

    public UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
