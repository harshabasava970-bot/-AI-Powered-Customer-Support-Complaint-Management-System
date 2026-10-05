package com.supportportal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class ErrorController {

    @GetMapping("/403")
    public String forbidden(Model model) {
        model.addAttribute("errorCode", "403");
        model.addAttribute("errorMessage", "You do not have permission to access this page.");
        return "error/generic";
    }

    @GetMapping("/404")
    public String notFound(Model model) {
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorMessage", "The page you are looking for does not exist.");
        return "error/generic";
    }
}
