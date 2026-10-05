package com.supportportal.controller;

import com.supportportal.dto.*;
import com.supportportal.security.CustomUserDetails;
import com.supportportal.service.AdminService;
import com.supportportal.service.CategoryService;
import com.supportportal.service.ComplaintService;
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

@Controller
@RequestMapping("/agent")
@PreAuthorize("hasRole('AGENT')")
@RequiredArgsConstructor
public class AgentController {

    private final ComplaintService complaintService;
    private final AdminService adminService;
    private final CategoryService categoryService;

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal,
                            Model model) {
        Page<ComplaintDto> assigned = complaintService.findByAgent(
                principal.getUsername(), 0, 5);
        model.addAttribute("recentComplaints", assigned.getContent());
        model.addAttribute("totalAssigned", assigned.getTotalElements());

        // Per-agent stats
        adminService.getAgentStats().stream()
                .filter(s -> s.getAgentId().equals(principal.getId()))
                .findFirst()
                .ifPresent(s -> model.addAttribute("agentStats", s));

        model.addAttribute("user", principal.getUser());
        return "agent/dashboard";
    }

    // ── Assigned Complaints ───────────────────────────────────────────────────
    @GetMapping("/complaints")
    public String assignedComplaints(@AuthenticationPrincipal CustomUserDetails principal,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     Model model) {
        Page<ComplaintDto> complaintsPage = complaintService.findByAgent(
                principal.getUsername(), page, size);
        model.addAttribute("complaintsPage", complaintsPage);
        model.addAttribute("currentPage", page);
        return "agent/complaints";
    }

    // ── Complaint Detail ──────────────────────────────────────────────────────
    @GetMapping("/complaints/{id}")
    public String complaintDetail(@PathVariable Long id, Model model) {
        ComplaintDto complaint = complaintService.findById(id);
        model.addAttribute("complaint", complaint);
        model.addAttribute("updateStatusRequest", new UpdateStatusRequest());
        model.addAttribute("resolveRequest", new ResolveComplaintRequest());
        return "agent/complaint-detail";
    }

    // ── Update Status ─────────────────────────────────────────────────────────
    @PostMapping("/complaints/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @Valid @ModelAttribute("updateStatusRequest") UpdateStatusRequest request,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("complaint", complaintService.findById(id));
            model.addAttribute("resolveRequest", new ResolveComplaintRequest());
            return "agent/complaint-detail";
        }
        try {
            complaintService.updateStatus(id, request, principal.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Status updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/agent/complaints/" + id;
    }

    // ── Resolve Complaint ─────────────────────────────────────────────────────
    @PostMapping("/complaints/{id}/resolve")
    public String resolve(@PathVariable Long id,
                          @Valid @ModelAttribute("resolveRequest") ResolveComplaintRequest request,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("complaint", complaintService.findById(id));
            model.addAttribute("updateStatusRequest", new UpdateStatusRequest());
            return "agent/complaint-detail";
        }
        try {
            complaintService.resolveComplaint(id, request, principal.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Complaint resolved successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/agent/complaints/" + id;
    }
}
