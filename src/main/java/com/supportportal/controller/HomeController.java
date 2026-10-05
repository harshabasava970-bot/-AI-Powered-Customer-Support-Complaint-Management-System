package com.supportportal.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            // Redirect already-logged-in users to their dashboard
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isAgent = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"));
            if (isAdmin)  return "redirect:/admin/dashboard";
            if (isAgent)  return "redirect:/agent/dashboard";
            return "redirect:/customer/dashboard";
        }
        return "home";
    }
}
