package com.supportportal.service;

import com.supportportal.dto.*;
import com.supportportal.entity.*;
import com.supportportal.exception.*;
import com.supportportal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintStatusHistoryRepository historyRepository;
    private final ComplaintFeedbackRepository feedbackRepository;
    private final UserService userService;
    private final CategoryService categoryService;
    private final ComplaintAnalysisService analysisService;

    // ── Valid status transitions ───────────────────────────────────────────────
    private static final java.util.Map<ComplaintStatus, Set<ComplaintStatus>> ALLOWED_TRANSITIONS =
            java.util.Map.of(
                    ComplaintStatus.NEW,                  EnumSet.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS),
                    ComplaintStatus.ASSIGNED,             EnumSet.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.WAITING_FOR_CUSTOMER),
                    ComplaintStatus.IN_PROGRESS,          EnumSet.of(ComplaintStatus.WAITING_FOR_CUSTOMER, ComplaintStatus.RESOLVED),
                    ComplaintStatus.WAITING_FOR_CUSTOMER, EnumSet.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED),
                    ComplaintStatus.RESOLVED,             EnumSet.of(ComplaintStatus.CLOSED, ComplaintStatus.IN_PROGRESS),
                    ComplaintStatus.CLOSED,               EnumSet.noneOf(ComplaintStatus.class)
            );

    // ── Create ────────────────────────────────────────────────────────────────

    public ComplaintDto createComplaint(ComplaintRequest request, String username) {
        User customer = userService.getUserByUsername(username);
        ComplaintCategory category = categoryService.getCategory(request.getCategoryId());

        // Run AI/NLP analysis
        AnalysisResult analysis = analysisService.analyze(request.getDescription());

        // Use AI priority if customer did not explicitly set one
        ComplaintPriority priority = (request.getPriority() != null)
                ? request.getPriority()
                : analysis.getPredictedPriority();

        Complaint complaint = Complaint.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(ComplaintStatus.NEW)
                .priority(priority)
                .category(category)
                .customer(customer)
                .additionalInfo(request.getAdditionalInfo())
                .aiPredictedCategory(analysis.getPredictedCategory())
                .aiPredictedPriority(analysis.getPredictedPriority())
                .aiAnalysisExplanation(analysis.getExplanation())
                .aiConfidenceScore(analysis.getConfidenceScore())
                .build();

        complaintRepository.save(complaint);

        // Record initial status history
        recordHistory(complaint, null, ComplaintStatus.NEW, customer, "Complaint created");

        log.info("Complaint #{} created by customer {}", complaint.getId(), username);
        return toDto(complaint);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ComplaintDto findById(Long id) {
        return toDto(getComplaint(id));
    }

    @Transactional(readOnly = true)
    public ComplaintDto findByIdForCustomer(Long id, String username) {
        Complaint c = getComplaint(id);
        if (!c.getCustomer().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You can only view your own complaints");
        }
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public Page<ComplaintDto> findByCustomer(String username, int page, int size) {
        User customer = userService.getUserByUsername(username);
        return complaintRepository.findByCustomer(
                customer, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ComplaintDto> findByAgent(String username, int page, int size) {
        User agent = userService.getUserByUsername(username);
        return complaintRepository.findByAssignedAgent(
                agent, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ComplaintDto> searchComplaints(ComplaintSearchRequest req) {
        LocalDateTime from = req.getFromDate() != null ? req.getFromDate().atStartOfDay() : null;
        LocalDateTime to   = req.getToDate()   != null ? req.getToDate().atTime(23, 59, 59) : null;

        return complaintRepository.searchComplaints(
                null,
                req.getStatus(),
                req.getPriority(),
                req.getCategoryId(),
                req.getAgentId(),
                req.getKeyword(),
                from, to,
                PageRequest.of(req.getPage(), req.getSize(), Sort.by("createdAt").descending())
        ).map(this::toDto);
    }

    // ── Status updates ────────────────────────────────────────────────────────

    public ComplaintDto updateStatus(Long id, UpdateStatusRequest request, String username) {
        Complaint complaint = getComplaint(id);
        User actor = userService.getUserByUsername(username);

        validateTransition(complaint.getStatus(), request.getStatus());

        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setStatus(request.getStatus());

        if (request.getStatus() == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        recordHistory(complaint, oldStatus, request.getStatus(), actor, request.getNotes());
        complaintRepository.save(complaint);

        log.info("Complaint #{} status {} → {} by {}", id, oldStatus, request.getStatus(), username);
        return toDto(complaint);
    }

    public ComplaintDto resolveComplaint(Long id, ResolveComplaintRequest request, String username) {
        Complaint complaint = getComplaint(id);
        User agent = userService.getUserByUsername(username);

        // Ensure the resolving agent is the one assigned
        if (complaint.getAssignedAgent() == null
                || !complaint.getAssignedAgent().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("Only the assigned agent can resolve this complaint");
        }

        validateTransition(complaint.getStatus(), ComplaintStatus.RESOLVED);

        ComplaintStatus old = complaint.getStatus();
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolutionNotes(request.getResolutionNotes());
        complaint.setResolvedAt(LocalDateTime.now());

        recordHistory(complaint, old, ComplaintStatus.RESOLVED, agent, request.getResolutionNotes());
        complaintRepository.save(complaint);

        log.info("Complaint #{} resolved by agent {}", id, username);
        return toDto(complaint);
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    public ComplaintDto assignAgent(Long complaintId, AssignAgentRequest request, String adminUsername) {
        Complaint complaint = getComplaint(complaintId);
        User agent = userService.getUser(request.getAgentId());
        User admin = userService.getUserByUsername(adminUsername);

        if (!agent.hasRole("ROLE_AGENT")) {
            throw new BadRequestException("User " + agent.getUsername() + " is not an agent");
        }
        if (!agent.isEnabled()) {
            throw new BadRequestException("Agent " + agent.getUsername() + " is disabled");
        }

        complaint.setAssignedAgent(agent);

        // Transition to ASSIGNED only if currently NEW
        if (complaint.getStatus() == ComplaintStatus.NEW) {
            complaint.setStatus(ComplaintStatus.ASSIGNED);
            recordHistory(complaint, ComplaintStatus.NEW, ComplaintStatus.ASSIGNED,
                    admin, "Assigned to " + agent.getFullName() + ". " +
                           (request.getNotes() != null ? request.getNotes() : ""));
        }

        complaintRepository.save(complaint);
        log.info("Complaint #{} assigned to agent {}", complaintId, agent.getUsername());
        return toDto(complaint);
    }

    // ── Customer adds info ────────────────────────────────────────────────────

    public ComplaintDto addAdditionalInfo(Long id, String info, String username) {
        Complaint complaint = getComplaint(id);
        if (!complaint.getCustomer().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You can only update your own complaints");
        }
        if (complaint.getStatus() == ComplaintStatus.CLOSED) {
            throw new BadRequestException("Cannot update a closed complaint");
        }
        complaint.setAdditionalInfo(info);
        complaintRepository.save(complaint);
        return toDto(complaint);
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    public ComplaintFeedbackDto submitFeedback(Long complaintId, FeedbackRequest request,
                                               String username) {
        Complaint complaint = getComplaint(complaintId);

        if (!complaint.getCustomer().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You can only rate your own complaints");
        }
        if (complaint.getStatus() != ComplaintStatus.RESOLVED
                && complaint.getStatus() != ComplaintStatus.CLOSED) {
            throw new BadRequestException("Feedback can only be submitted for resolved or closed complaints");
        }
        if (feedbackRepository.existsByComplaint(complaint)) {
            throw new DuplicateResourceException("Feedback already submitted for this complaint");
        }

        ComplaintFeedback feedback = ComplaintFeedback.builder()
                .complaint(complaint)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        feedbackRepository.save(feedback);

        // Auto-close after feedback
        if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            complaint.setStatus(ComplaintStatus.CLOSED);
            User customer = userService.getUserByUsername(username);
            recordHistory(complaint, ComplaintStatus.RESOLVED, ComplaintStatus.CLOSED,
                    customer, "Closed after customer feedback");
            complaintRepository.save(complaint);
        }

        log.info("Feedback submitted for complaint #{} by {}", complaintId, username);
        return toFeedbackDto(feedback);
    }

    // ── Overdue ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ComplaintDto> findOverdueComplaints() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
        return complaintRepository.findOverdueComplaints(cutoff)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateTransition(ComplaintStatus from, ComplaintStatus to) {
        Set<ComplaintStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new InvalidStatusTransitionException(from, to);
        }
    }

    private void recordHistory(Complaint complaint, ComplaintStatus oldStatus,
                                ComplaintStatus newStatus, User changedBy, String notes) {
        ComplaintStatusHistory history = ComplaintStatusHistory.builder()
                .complaint(complaint)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .notes(notes)
                .build();
        historyRepository.save(history);
    }

    public Complaint getComplaint(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", id));
    }

    public ComplaintDto toDto(Complaint c) {
        List<StatusHistoryDto> history = historyRepository
                .findByComplaintOrderByChangedAtAsc(c)
                .stream().map(h -> StatusHistoryDto.builder()
                        .id(h.getId())
                        .oldStatus(h.getOldStatus())
                        .newStatus(h.getNewStatus())
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System")
                        .notes(h.getNotes())
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList());

        ComplaintFeedbackDto feedbackDto = null;
        if (c.getFeedback() != null) {
            feedbackDto = toFeedbackDto(c.getFeedback());
        }

        return ComplaintDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .status(c.getStatus())
                .priority(c.getPriority())
                .categoryId(c.getCategory().getId())
                .categoryName(c.getCategory().getName())
                .customerId(c.getCustomer().getId())
                .customerName(c.getCustomer().getFullName())
                .customerEmail(c.getCustomer().getEmail())
                .assignedAgentId(c.getAssignedAgent() != null ? c.getAssignedAgent().getId() : null)
                .assignedAgentName(c.getAssignedAgent() != null ? c.getAssignedAgent().getFullName() : null)
                .aiPredictedCategory(c.getAiPredictedCategory())
                .aiPredictedPriority(c.getAiPredictedPriority())
                .aiAnalysisExplanation(c.getAiAnalysisExplanation())
                .aiConfidenceScore(c.getAiConfidenceScore())
                .resolutionNotes(c.getResolutionNotes())
                .resolvedAt(c.getResolvedAt())
                .additionalInfo(c.getAdditionalInfo())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .feedback(feedbackDto)
                .statusHistory(history)
                .build();
    }

    private ComplaintFeedbackDto toFeedbackDto(ComplaintFeedback f) {
        return ComplaintFeedbackDto.builder()
                .id(f.getId())
                .rating(f.getRating())
                .comment(f.getComment())
                .submittedAt(f.getSubmittedAt())
                .complaintId(f.getComplaint().getId())
                .build();
    }
}
