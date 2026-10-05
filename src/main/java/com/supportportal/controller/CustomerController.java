package com.supportportal.controller;

import com.supportportal.dto.*;
import com.supportportal.exception.BadRequestException;
import com.supportportal.exception.DuplicateResourceException;
import com.supportportal.security.CustomUserDetails;
import com.supportportal.service.CategoryService;
import com.supportportal.service.ComplaintAnalysisService;
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

import java.util.List;

@Controller
@RequestMapping("/customer")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CustomerController {

    private final ComplaintService complaintService;
    private final CategoryService categoryService;
    private final ComplaintAnalysisService analysisService;

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal,
                            Model model) {
        Page<ComplaintDto> complaints = complaintService.findByCustomer(
                principal.getUsername(), 0, 5);
        model.addAttribute("complaints", complaints.getContent());
        model.addAttribute("totalComplaints", complaints.getTotalElements());

        long open = complaints.getContent().stream()
                .filter(c -> c.getStatus().name().equals("NEW")
                          || c.getStatus().name().equals("ASSIGNED")
                          || c.getStatus().name().equals("IN_PROGRESS"))
                .count();
        model.addAttribute("openComplaints", open);
        model.addAttribute("user", principal.getUser());
        return "customer/dashboard";
    }

    // ── My Complaints ─────────────────────────────────────────────────────────
    @GetMapping("/complaints")
    public String myComplaints(@AuthenticationPrincipal CustomUserDetails principal,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Model model) {
        Page<ComplaintDto> complaints = complaintService.findByCustomer(
                principal.getUsername(), page, size);
        model.addAttribute("complaintsPage", complaints);
        model.addAttribute("currentPage", page);
        return "customer/complaints";
    }

    // ── Complaint Detail ──────────────────────────────────────────────────────
    @GetMapping("/complaints/{id}")
    public String complaintDetail(@PathVariable Long id,
                                  @AuthenticationPrincipal CustomUserDetails principal,
                                  Model model) {
        ComplaintDto complaint = complaintService.findByIdForCustomer(id, principal.getUsername());
        model.addAttribute("complaint", complaint);
        model.addAttribute("feedbackRequest", new FeedbackRequest());
        return "customer/complaint-detail";
    }

    // ── Create Complaint Form ─────────────────────────────────────────────────
    @GetMapping("/complaints/new")
    public String newComplaintForm(Model model) {
        model.addAttribute("complaintRequest", new ComplaintRequest());
        model.addAttribute("categories", categoryService.findActive());
        return "customer/create-complaint";
    }

    // ── AI Preview (AJAX) ─────────────────────────────────────────────────────
    @PostMapping("/complaints/analyze")
    @ResponseBody
    public AnalysisResult analyzeText(@RequestParam String text) {
        return analysisService.analyze(text);
    }

    // ── Submit Complaint ──────────────────────────────────────────────────────
    @PostMapping("/complaints")
    public String submitComplaint(
            @Valid @ModelAttribute("complaintRequest") ComplaintRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails principal,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findActive());
            return "customer/create-complaint";
        }
        try {
            ComplaintDto created = complaintService.createComplaint(request, principal.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Complaint #" + created.getId() + " submitted successfully.");
            return "redirect:/customer/complaints/" + created.getId();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("categories", categoryService.findActive());
            return "customer/create-complaint";
        }
    }

    // ── Add Additional Info ───────────────────────────────────────────────────
    @PostMapping("/complaints/{id}/additional-info")
    public String addInfo(@PathVariable Long id,
                          @RequestParam String additionalInfo,
                          @AuthenticationPrincipal CustomUserDetails principal,
                          RedirectAttributes redirectAttributes) {
        try {
            complaintService.addAdditionalInfo(id, additionalInfo, principal.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Information added.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/complaints/" + id;
    }

    // ── Submit Feedback ───────────────────────────────────────────────────────
    @PostMapping("/complaints/{id}/feedback")
    public String submitFeedback(@PathVariable Long id,
                                 @Valid @ModelAttribute("feedbackRequest") FeedbackRequest request,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal CustomUserDetails principal,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("complaint", complaintService.findByIdForCustomer(id, principal.getUsername()));
            return "customer/complaint-detail";
        }
        try {
            complaintService.submitFeedback(id, request, principal.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Thank you for your feedback!");
        } catch (DuplicateResourceException | BadRequestException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/customer/complaints/" + id;
    }
}
