package com.supportportal.service;

import com.supportportal.dto.*;
import com.supportportal.entity.*;
import com.supportportal.exception.*;
import com.supportportal.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock ComplaintRepository complaintRepository;
    @Mock ComplaintStatusHistoryRepository historyRepository;
    @Mock ComplaintFeedbackRepository feedbackRepository;
    @Mock UserService userService;
    @Mock CategoryService categoryService;
    @Mock ComplaintAnalysisService analysisService;

    @InjectMocks ComplaintService complaintService;

    private User customer;
    private User agent;
    private ComplaintCategory category;
    private Complaint complaint;
    private AnalysisResult mockAnalysis;

    @BeforeEach
    void setUp() {
        Role customerRole = new Role(1L, Role.RoleName.ROLE_CUSTOMER);
        Role agentRole    = new Role(2L, Role.RoleName.ROLE_AGENT);

        customer = User.builder().id(1L).username("alice").firstName("Alice")
                .lastName("Smith").email("alice@test.com")
                .roles(java.util.Set.of(customerRole)).enabled(true).build();

        agent = User.builder().id(2L).username("bob").firstName("Bob")
                .lastName("Jones").email("bob@test.com")
                .roles(java.util.Set.of(agentRole)).enabled(true).build();

        category = ComplaintCategory.builder().id(1L).name("Payment")
                .description("Payment issues").active(true).build();

        complaint = Complaint.builder()
                .id(1L).title("Payment Failed").description("Payment failed multiple times.")
                .status(ComplaintStatus.NEW).priority(ComplaintPriority.HIGH)
                .category(category).customer(customer).build();

        mockAnalysis = AnalysisResult.builder()
                .predictedCategory("Payment").predictedPriority(ComplaintPriority.HIGH)
                .confidenceScore(0.75).explanation("Test explanation")
                .categoryKeywordsMatched(List.of("payment")).priorityKeywordsMatched(List.of("money deducted"))
                .build();
    }

    // ── Create complaint ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Create complaint — success path")
    void createComplaint_success() {
        ComplaintRequest request = ComplaintRequest.builder()
                .title("Payment Failed").description("My payment failed multiple times.")
                .categoryId(1L).build();

        when(userService.getUserByUsername("alice")).thenReturn(customer);
        when(categoryService.getCategory(1L)).thenReturn(category);
        when(analysisService.analyze(any())).thenReturn(mockAnalysis);
        when(complaintRepository.save(any())).thenAnswer(inv -> {
            Complaint c = inv.getArgument(0);
            c = Complaint.builder().id(1L).title(c.getTitle()).description(c.getDescription())
                    .status(c.getStatus()).priority(c.getPriority()).category(category)
                    .customer(customer).aiPredictedCategory("Payment")
                    .aiPredictedPriority(ComplaintPriority.HIGH)
                    .aiAnalysisExplanation("Test explanation").aiConfidenceScore(0.75).build();
            return c;
        });
        when(historyRepository.save(any())).thenReturn(new ComplaintStatusHistory());
        when(historyRepository.findByComplaintOrderByChangedAtAsc(any())).thenReturn(List.of());

        ComplaintDto result = complaintService.createComplaint(request, "alice");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Payment Failed");
        verify(complaintRepository).save(any(Complaint.class));
        verify(analysisService).analyze("My payment failed multiple times.");
    }

    @Test
    @DisplayName("Create complaint — uses AI priority when none specified")
    void createComplaint_usesAiPriority() {
        ComplaintRequest request = ComplaintRequest.builder()
                .title("Test").description("Some description here.").categoryId(1L)
                .priority(null).build(); // no priority set

        when(userService.getUserByUsername("alice")).thenReturn(customer);
        when(categoryService.getCategory(1L)).thenReturn(category);
        when(analysisService.analyze(any())).thenReturn(mockAnalysis);
        when(complaintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenReturn(new ComplaintStatusHistory());
        when(historyRepository.findByComplaintOrderByChangedAtAsc(any())).thenReturn(List.of());

        complaintService.createComplaint(request, "alice");

        verify(analysisService).analyze(any());
    }

    // ── Status transitions ────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid status transition NEW → ASSIGNED succeeds")
    void validStatusTransition_newToAssigned() {
        complaint.setStatus(ComplaintStatus.NEW);
        UpdateStatusRequest req = new UpdateStatusRequest(ComplaintStatus.ASSIGNED, "Assigned");

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(userService.getUserByUsername("admin")).thenReturn(customer);
        when(complaintRepository.save(any())).thenReturn(complaint);
        when(historyRepository.save(any())).thenReturn(new ComplaintStatusHistory());
        when(historyRepository.findByComplaintOrderByChangedAtAsc(any())).thenReturn(List.of());

        ComplaintDto result = complaintService.updateStatus(1L, req, "admin");

        assertThat(result).isNotNull();
        verify(complaintRepository).save(any());
    }

    @Test
    @DisplayName("Invalid status transition CLOSED → any throws exception")
    void invalidStatusTransition_closedToNew_throws() {
        complaint.setStatus(ComplaintStatus.CLOSED);
        UpdateStatusRequest req = new UpdateStatusRequest(ComplaintStatus.NEW, "Reopen");

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(userService.getUserByUsername("admin")).thenReturn(customer);

        assertThatThrownBy(() -> complaintService.updateStatus(1L, req, "admin"))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    @DisplayName("Invalid transition NEW → RESOLVED throws")
    void invalidTransition_newToResolved_throws() {
        complaint.setStatus(ComplaintStatus.NEW);
        UpdateStatusRequest req = new UpdateStatusRequest(ComplaintStatus.RESOLVED, "Skip");

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(userService.getUserByUsername("admin")).thenReturn(customer);

        assertThatThrownBy(() -> complaintService.updateStatus(1L, req, "admin"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ── Agent assignment ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Assign valid agent — succeeds")
    void assignAgent_success() {
        AssignAgentRequest req = new AssignAgentRequest(2L, "Please handle");

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(userService.getUser(2L)).thenReturn(agent);
        when(userService.getUserByUsername("admin")).thenReturn(customer);
        when(complaintRepository.save(any())).thenReturn(complaint);
        when(historyRepository.save(any())).thenReturn(new ComplaintStatusHistory());
        when(historyRepository.findByComplaintOrderByChangedAtAsc(any())).thenReturn(List.of());

        ComplaintDto result = complaintService.assignAgent(1L, req, "admin");

        assertThat(result).isNotNull();
        verify(complaintRepository).save(any());
    }

    @Test
    @DisplayName("Assign non-agent user throws BadRequestException")
    void assignNonAgent_throws() {
        AssignAgentRequest req = new AssignAgentRequest(1L, null); // customer id

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(userService.getUser(1L)).thenReturn(customer); // customer, not agent

        assertThatThrownBy(() -> complaintService.assignAgent(1L, req, "admin"))
                .isInstanceOf(BadRequestException.class);
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Submit feedback on RESOLVED complaint succeeds")
    void submitFeedback_resolved_success() {
        complaint.setStatus(ComplaintStatus.RESOLVED);
        FeedbackRequest req = new FeedbackRequest(5, "Excellent service!");
        ComplaintFeedback savedFeedback = ComplaintFeedback.builder()
                .id(1L).complaint(complaint).rating(5).comment("Excellent service!").build();

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(userService.getUserByUsername("alice")).thenReturn(customer);
        when(feedbackRepository.existsByComplaint(complaint)).thenReturn(false);
        when(feedbackRepository.save(any())).thenReturn(savedFeedback);
        when(complaintRepository.save(any())).thenReturn(complaint);
        when(historyRepository.save(any())).thenReturn(new ComplaintStatusHistory());

        ComplaintFeedbackDto result = complaintService.submitFeedback(1L, req, "alice");

        assertThat(result).isNotNull();
        assertThat(result.getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("Duplicate feedback throws DuplicateResourceException")
    void duplicateFeedback_throws() {
        complaint.setStatus(ComplaintStatus.RESOLVED);
        FeedbackRequest req = new FeedbackRequest(4, "Good");

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(feedbackRepository.existsByComplaint(any())).thenReturn(true);

        assertThatThrownBy(() -> complaintService.submitFeedback(1L, req, "alice"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Feedback on NEW complaint throws BadRequestException")
    void feedbackOnNewComplaint_throws() {
        complaint.setStatus(ComplaintStatus.NEW);
        FeedbackRequest req = new FeedbackRequest(3, "Ok");

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.submitFeedback(1L, req, "alice"))
                .isInstanceOf(BadRequestException.class);
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Customer viewing another customer's complaint throws UnauthorizedAccessException")
    void customerViewOtherComplaint_throws() {
        User other = User.builder().id(99L).username("other").build();
        complaint.setCustomer(other);

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.findByIdForCustomer(1L, "alice"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    // ── Not found ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById with unknown id throws ResourceNotFoundException")
    void findById_unknown_throws() {
        when(complaintRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complaintService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
