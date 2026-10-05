package com.supportportal.service;

import com.supportportal.dto.RegisterRequest;
import com.supportportal.dto.UserDto;
import com.supportportal.entity.Role;
import com.supportportal.entity.User;
import com.supportportal.exception.BadRequestException;
import com.supportportal.exception.DuplicateResourceException;
import com.supportportal.repository.RoleRepository;
import com.supportportal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    private RegisterRequest validRequest;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        validRequest = RegisterRequest.builder()
                .username("john123").email("john@test.com")
                .password("password123").confirmPassword("password123")
                .firstName("John").lastName("Doe").build();

        customerRole = new Role(1L, Role.RoleName.ROLE_CUSTOMER);
    }

    @Test
    @DisplayName("Register customer — success")
    void registerCustomer_success() {
        User savedUser = User.builder().id(1L).username("john123").email("john@test.com")
                .firstName("John").lastName("Doe").enabled(true)
                .roles(Set.of(customerRole)).build();

        when(userRepository.existsByUsername("john123")).thenReturn(false);
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any())).thenReturn(savedUser);

        UserDto result = userService.registerCustomer(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("john123");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register customer — duplicate username throws")
    void registerCustomer_duplicateUsername_throws() {
        when(userRepository.existsByUsername("john123")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerCustomer(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john123");
    }

    @Test
    @DisplayName("Register customer — duplicate email throws")
    void registerCustomer_duplicateEmail_throws() {
        when(userRepository.existsByUsername("john123")).thenReturn(false);
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerCustomer(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john@test.com");
    }

    @Test
    @DisplayName("Register customer — password mismatch throws")
    void registerCustomer_passwordMismatch_throws() {
        validRequest.setConfirmPassword("different_password");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.registerCustomer(validRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Passwords do not match");
    }

    @Test
    @DisplayName("Plain text password is never stored — BCrypt is called")
    void passwordIsHashed_notStoredPlainText() {
        User savedUser = User.builder().id(1L).username("john123").email("john@test.com")
                .firstName("John").lastName("Doe").enabled(true)
                .roles(Set.of(customerRole)).build();

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any())).thenReturn(savedUser);

        userService.registerCustomer(validRequest);

        // Verify encode was called with the raw password, never the hash stored directly
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository).save(argThat(u ->
                !u.getPassword().equals("password123")));
    }

    @Test
    @DisplayName("findById — user not found throws ResourceNotFoundException")
    void findById_notFound_throws() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(com.supportportal.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Toggle enabled flips the enabled state")
    void toggleEnabled_flipsState() {
        User user = User.builder().id(1L).username("alice").email("a@b.com")
                .firstName("Alice").lastName("Smith").enabled(true)
                .roles(Set.of(customerRole)).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.toggleEnabled(1L);

        verify(userRepository).save(argThat(u -> !u.isEnabled()));
    }
}
