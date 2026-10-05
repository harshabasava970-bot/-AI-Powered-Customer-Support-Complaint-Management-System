package com.supportportal.controller;

import com.supportportal.dto.RegisterRequest;
import com.supportportal.exception.BadRequestException;
import com.supportportal.exception.DuplicateResourceException;
import com.supportportal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            @RequestParam(required = false) String expired,
                            Authentication authentication,
                            Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        if (error   != null) model.addAttribute("loginError",   "Invalid username or password.");
        if (logout  != null) model.addAttribute("logoutMessage","You have been logged out.");
        if (expired != null) model.addAttribute("loginError",   "Your session has expired. Please log in again.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.registerCustomer(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account created! You can now log in.");
            return "redirect:/login";
        } catch (DuplicateResourceException | BadRequestException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }
    }
}
