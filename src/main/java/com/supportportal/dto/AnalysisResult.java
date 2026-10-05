package com.supportportal.dto;

import com.supportportal.entity.ComplaintPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Result produced by the AI/NLP ComplaintAnalysisService.
 * Contains the predicted category, priority, matched keywords, and a human-readable explanation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {

    /** Best-matching category name */
    private String predictedCategory;

    /** Scores per category (for explainability) */
    private Map<String, Integer> categoryScores;

    /** Confidence in the category prediction (0.0 – 1.0) */
    private double confidenceScore;

    /** Predicted priority level */
    private ComplaintPriority predictedPriority;

    /** Keywords that drove the priority decision */
    private List<String> priorityKeywordsMatched;

    /** Keywords that drove the category decision */
    private List<String> categoryKeywordsMatched;

    /** Human-readable explanation stored on the complaint */
    private String explanation;
}
