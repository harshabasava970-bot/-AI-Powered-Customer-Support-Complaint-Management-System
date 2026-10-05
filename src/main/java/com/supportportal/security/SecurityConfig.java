package com.supportportal.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())

            // CSRF enabled for all Thymeleaf form submissions
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**") // REST API calls may use stateless CSRF tokens
            )

            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers("/", "/home", "/login", "/register",
                                 "/css/**", "/js/**", "/images/**",
                                 "/error", "/error/**").permitAll()
                // REST API auth endpoints
                .requestMatchers("/api/auth/**").permitAll()
                // Admin area
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                // Agent area
                .requestMatchers("/agent/**", "/api/agent/**").hasRole("AGENT")
                // Customer area
                .requestMatchers("/customer/**").hasRole("CUSTOMER")
                // Shared API — authenticated users only
                .requestMatchers("/api/complaints/**").authenticated()
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler())
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired")
            )

            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            );

        return http.build();
    }

    /**
     * Redirect to role-specific dashboard after successful login.
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin   = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isAgent   = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"));

            if (isAdmin) {
                response.sendRedirect("/admin/dashboard");
            } else if (isAgent) {
                response.sendRedirect("/agent/dashboard");
            } else {
                response.sendRedirect("/customer/dashboard");
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) ->
                response.sendRedirect("/login?error");
    }
}
