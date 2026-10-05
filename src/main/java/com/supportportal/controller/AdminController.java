package com.supportportal.controller;

import com.supportportal.dto.*;
import com.supportportal.entity.ComplaintPriority;
import com.supportportal.entity.ComplaintStatus;
import com.supportportal.exception.BadRequestException;
import com.supportportal.exception.DuplicateResourceException;
import com.supportportal.security.CustomUserDetails;
import com.supportportal.service.AdminService;
import com.supportportal.service.CategoryService;
import com.supportportal.service.ComplaintService;
import com.supportportal.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ComplaintService complaintService;
    private final UserService userService;
    private final CategoryService categoryService;

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardStatsDto stats = adminService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("agentStats", adminService.getAgentStats());
        model.addAttribute("overdueComplaints", complaintService.findOverdueComplaints());
        return "admin/dashboard";
    }

    // ── All Complaints ────────────────────────────────────────────────────────
    @GetMapping("/complaints")
    public String complaints(@ModelAttribute ComplaintSearchRequest search, Model model) {
        Page<ComplaintDto> page = complaintService.searchComplaints(search);
        model.addAttribute("complaintsPage", page);
        model.addAttribute("search", search);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("agents", userService.findAllAgents());
        model.addAttribute("statuses", ComplaintStatus.values());
        model.addAttribute("priorities", ComplaintPriority.values());
        return "admin/complaints";
    }

    // ── Complaint Detail ──────────────────────────────────────────────────────
    @GetMapping("/complaints/{id}")
    public String complaintDetail(@PathVariable Long id, Model model) {
        model.addAttribute("complaint", complaintService.findById(id));
        model.addAttribute("agents", userService.findActiveAgents());
        model.addAttribute("assignRequest", new AssignAgentRequest());
        model.addAttribute("statusRequest", new UpdateStatusRequest());
        model.addAttribute("statuses", ComplaintStatus.values());
        return "admin/complaint-detail";
    }

    // ── Assign Agent ──────────────────────────────────────────────────────────
    @PostMapping("/complaints/{id}/assign")
    public String assignAgent(@PathVariable Long id,
                              @Valid @ModelAttribute("assignRequest") AssignAgentRequest request,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("complaint", complaintService.findById(id));
            model.addAttribute("agents", userService.findActiveAgents());
            model.addAttribute("statusRequest", new UpdateStatusRequest());
            return "admin/complaint-detail";
        }
        try {
            complaintService.assignAgent(id, request, principal.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Agent assigned successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/complaints/" + id;
    }

    // ── Update Status (Admin override) ────────────────────────────────────────
    @PostMapping("/complaints/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @Valid @ModelAttribute("statusRequest") UpdateStatusRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("complaint", complaintService.findById(id));
            model.addAttribute("agents", userService.findActiveAgents());
            model.addAttribute("assignRequest", new AssignAgentRequest());
            return "admin/complaint-detail";
        }
        try {
            complaintService.updateStatus(id, request, principal.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Status updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/complaints/" + id;
    }

    // ── Users ─────────────────────────────────────────────────────────────────
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.toggleEnabled(id);
            ra.addFlashAttribute("successMessage", "User status updated.");
        } catch (Exception ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ── Agents ────────────────────────────────────────────────────────────────
    @GetMapping("/agents")
    public String agents(Model model) {
        model.addAttribute("agents", userService.findAllAgents());
        model.addAttribute("agentStats", adminService.getAgentStats());
        model.addAttribute("registerRequest", new RegisterRequest());
        return "admin/agents";
    }

    @PostMapping("/agents")
    public String createAgent(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("agents", userService.findAllAgents());
            model.addAttribute("agentStats", adminService.getAgentStats());
            return "admin/agents";
        }
        try {
            userService.registerAgent(request);
            redirectAttributes.addFlashAttribute("successMessage", "Agent created successfully.");
        } catch (DuplicateResourceException | BadRequestException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/agents";
    }

    // ── Categories ────────────────────────────────────────────────────────────
    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("categoryDto", new CategoryDto());
        return "admin/categories";
    }

    @PostMapping("/categories")
    public String createCategory(@Valid @ModelAttribute("categoryDto") CategoryDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "admin/categories";
        }
        try {
            categoryService.create(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Category created.");
        } catch (DuplicateResourceException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/toggle")
    public String toggleCategory(@PathVariable Long id, RedirectAttributes ra) {
        categoryService.toggleActive(id);
        ra.addFlashAttribute("successMessage", "Category status updated.");
        return "redirect:/admin/categories";
    }

    // ── Analytics ─────────────────────────────────────────────────────────────
    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("stats", adminService.getDashboardStats());
        model.addAttribute("agentStats", adminService.getAgentStats());
        return "admin/analytics";
    }
}
