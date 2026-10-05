package com.supportportal.controller.api;

import com.supportportal.dto.*;
import com.supportportal.security.CustomUserDetails;
import com.supportportal.service.ComplaintAnalysisService;
import com.supportportal.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintApiController {

    private final ComplaintService complaintService;
    private final ComplaintAnalysisService analysisService;

    // ── POST /api/complaints ─────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ComplaintDto>> create(
            @Valid @RequestBody ComplaintRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ComplaintDto created = complaintService.createComplaint(request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Complaint created successfully"));
    }

    // ── GET /api/complaints  (paginated, with filters) ───────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<ApiResponse<Page<ComplaintDto>>> listAll(
            @ModelAttribute ComplaintSearchRequest search) {

        Page<ComplaintDto> page = complaintService.searchComplaints(search);
        return ResponseEntity.ok(ApiResponse.success(page, "OK"));
    }

    // ── GET /api/complaints/mine ─────────────────────────────────────────────
    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<ComplaintDto>>> myComplaints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Page<ComplaintDto> result = complaintService.findByCustomer(
                principal.getUsername(), page, size);
        return ResponseEntity.ok(ApiResponse.success(result, "OK"));
    }

    // ── GET /api/complaints/assigned ─────────────────────────────────────────
    @GetMapping("/assigned")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<Page<ComplaintDto>>> assignedToMe(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Page<ComplaintDto> result = complaintService.findByAgent(
                principal.getUsername(), page, size);
        return ResponseEntity.ok(ApiResponse.success(result, "OK"));
    }

    // ── GET /api/complaints/{id} ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComplaintDto>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        boolean isCustomer = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        ComplaintDto dto = isCustomer
                ? complaintService.findByIdForCustomer(id, principal.getUsername())
                : complaintService.findById(id);

        return ResponseEntity.ok(ApiResponse.success(dto, "OK"));
    }

    // ── PUT /api/complaints/{id}/status ──────────────────────────────────────
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<ApiResponse<ComplaintDto>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ComplaintDto updated = complaintService.updateStatus(id, request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Status updated"));
    }

    // ── PUT /api/complaints/{id}/resolve ─────────────────────────────────────
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<ApiResponse<ComplaintDto>> resolve(
            @PathVariable Long id,
            @Valid @RequestBody ResolveComplaintRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ComplaintDto updated = complaintService.resolveComplaint(id, request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Complaint resolved"));
    }

    // ── PUT /api/complaints/{id}/assign ──────────────────────────────────────
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComplaintDto>> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignAgentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ComplaintDto updated = complaintService.assignAgent(id, request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Agent assigned"));
    }

    // ── POST /api/complaints/{id}/feedback ───────────────────────────────────
    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ComplaintFeedbackDto>> submitFeedback(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ComplaintFeedbackDto feedback = complaintService.submitFeedback(
                id, request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(feedback, "Feedback submitted"));
    }

    // ── POST /api/complaints/{id}/additional-info ────────────────────────────
    @PostMapping("/{id}/additional-info")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ComplaintDto>> addInfo(
            @PathVariable Long id,
            @RequestParam String info,
            @AuthenticationPrincipal CustomUserDetails principal) {

        ComplaintDto updated = complaintService.addAdditionalInfo(
                id, info, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Information added"));
    }

    // ── POST /api/complaints/analyze ─────────────────────────────────────────
    @PostMapping("/analyze")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AnalysisResult>> analyze(
            @RequestParam String text) {
        AnalysisResult result = analysisService.analyze(text);
        return ResponseEntity.ok(ApiResponse.success(result, "Analysis complete"));
    }
}
