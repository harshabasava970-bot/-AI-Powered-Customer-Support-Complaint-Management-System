package com.supportportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaints_customer", columnList = "customer_id"),
        @Index(name = "idx_complaints_agent", columnList = "assigned_agent_id"),
        @Index(name = "idx_complaints_status", columnList = "status"),
        @Index(name = "idx_complaints_priority", columnList = "priority")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotBlank
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private ComplaintPriority priority = ComplaintPriority.MEDIUM;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ComplaintCategory category;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    // AI/NLP analysis fields
    @Column(name = "ai_predicted_category", length = 50)
    private String aiPredictedCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_predicted_priority", length = 20)
    private ComplaintPriority aiPredictedPriority;

    @Column(name = "ai_analysis_explanation", columnDefinition = "TEXT")
    private String aiAnalysisExplanation;

    @Column(name = "ai_confidence_score")
    private Double aiConfidenceScore;

    // Resolution
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComplaintStatusHistory> statusHistory = new ArrayList<>();

    @OneToOne(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private ComplaintFeedback feedback;
}
