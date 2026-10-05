package com.supportportal.service;

import com.supportportal.dto.AnalysisResult;
import com.supportportal.entity.ComplaintPriority;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-based NLP complaint classifier.
 *
 * Design (interview-ready explanation):
 * ────────────────────────────────────
 * 1. Category classification  – each category owns a weighted keyword dictionary.
 *    The text is lower-cased and every keyword is searched; matched weights are
 *    summed per category.  The category with the highest total score wins.
 *    Confidence = winner_score / sum_of_all_scores (0–1).
 *
 * 2. Priority prediction      – four tiers (CRITICAL → LOW) each have a list of
 *    indicator phrases.  The text is scanned for all matches; the highest-tier
 *    tier that has ≥1 match wins.  Matched phrases are stored for explainability.
 *
 * 3. Explanation              – a plain-English string is built from the matched
 *    keywords so that any stakeholder can understand why a prediction was made.
 *
 * No external libraries, no ML model, fully deterministic, fully testable.
 */
@Service
public class ComplaintAnalysisService {

    // ─────────────────────────────────────────────────────────────────────────
    // Category keyword dictionary  {keyword → weight}
    // ─────────────────────────────────────────────────────────────────────────
    private static final Map<String, Map<String, Integer>> CATEGORY_KEYWORDS;

    static {
        CATEGORY_KEYWORDS = new LinkedHashMap<>();

        CATEGORY_KEYWORDS.put("Billing", Map.ofEntries(
                Map.entry("invoice", 3), Map.entry("bill", 3), Map.entry("charge", 3),
                Map.entry("overcharged", 5), Map.entry("refund", 4), Map.entry("receipt", 2),
                Map.entry("subscription", 2), Map.entry("plan", 1), Map.entry("price", 2),
                Map.entry("fee", 2), Map.entry("discount", 2), Map.entry("billing", 5)
        ));

        CATEGORY_KEYWORDS.put("Payment", Map.ofEntries(
                Map.entry("payment", 5), Map.entry("pay", 3), Map.entry("transaction", 4),
                Map.entry("transfer", 3), Map.entry("deducted", 5), Map.entry("debit", 4),
                Map.entry("credit", 3), Map.entry("wallet", 3), Map.entry("upi", 4),
                Map.entry("bank", 3), Map.entry("failed payment", 6), Map.entry("payment failed", 6),
                Map.entry("money deducted", 7), Map.entry("amount debited", 7)
        ));

        CATEGORY_KEYWORDS.put("Technical Issue", Map.ofEntries(
                Map.entry("bug", 5), Map.entry("crash", 5), Map.entry("error", 3),
                Map.entry("not working", 4), Map.entry("broken", 4), Map.entry("slow", 3),
                Map.entry("lagging", 3), Map.entry("loading", 2), Map.entry("technical", 4),
                Map.entry("glitch", 4), Map.entry("freeze", 4), Map.entry("unresponsive", 4),
                Map.entry("issue", 2), Map.entry("problem", 2), Map.entry("malfunction", 5)
        ));

        CATEGORY_KEYWORDS.put("Account", Map.ofEntries(
                Map.entry("account", 5), Map.entry("profile", 3), Map.entry("username", 3),
                Map.entry("email", 2), Map.entry("update", 2), Map.entry("change", 2),
                Map.entry("delete account", 6), Map.entry("deactivate", 4), Map.entry("suspend", 4),
                Map.entry("verification", 3), Map.entry("kyc", 4), Map.entry("identity", 3)
        ));

        CATEGORY_KEYWORDS.put("Login/Access", Map.ofEntries(
                Map.entry("login", 5), Map.entry("cannot login", 7), Map.entry("sign in", 4),
                Map.entry("password", 4), Map.entry("forgot password", 6), Map.entry("reset password", 6),
                Map.entry("otp", 4), Map.entry("two factor", 4), Map.entry("2fa", 4),
                Map.entry("locked out", 6), Map.entry("access denied", 5), Map.entry("unauthorized", 4),
                Map.entry("session", 3), Map.entry("logout", 2)
        ));

        CATEGORY_KEYWORDS.put("Delivery", Map.ofEntries(
                Map.entry("delivery", 6), Map.entry("delivered", 5), Map.entry("shipping", 5),
                Map.entry("courier", 4), Map.entry("package", 4), Map.entry("parcel", 4),
                Map.entry("tracking", 3), Map.entry("dispatch", 3), Map.entry("delayed", 4),
                Map.entry("not received", 5), Map.entry("missing", 4), Map.entry("lost", 3),
                Map.entry("return", 3), Map.entry("logistics", 4)
        ));

        CATEGORY_KEYWORDS.put("Product", Map.ofEntries(
                Map.entry("product", 5), Map.entry("item", 4), Map.entry("quality", 4),
                Map.entry("defective", 6), Map.entry("damaged", 5), Map.entry("wrong item", 6),
                Map.entry("broken product", 6), Map.entry("warranty", 4), Map.entry("replacement", 4),
                Map.entry("exchange", 3), Map.entry("manufacturing", 3)
        ));

        CATEGORY_KEYWORDS.put("Service", Map.ofEntries(
                Map.entry("service", 4), Map.entry("support", 3), Map.entry("staff", 3),
                Map.entry("rude", 4), Map.entry("unprofessional", 4), Map.entry("behaviour", 3),
                Map.entry("experience", 2), Map.entry("poor service", 5), Map.entry("bad service", 5),
                Map.entry("customer care", 4), Map.entry("representative", 3), Map.entry("agent", 2)
        ));

        CATEGORY_KEYWORDS.put("Other", Map.ofEntries(
                Map.entry("other", 1), Map.entry("general", 1), Map.entry("information", 1),
                Map.entry("query", 1), Map.entry("request", 1), Map.entry("feedback", 1)
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Priority indicator phrases grouped by tier
    // ─────────────────────────────────────────────────────────────────────────
    private static final List<String> CRITICAL_INDICATORS = List.of(
            "fraud", "hacked", "security breach", "money stolen", "stolen",
            "unauthorized transaction", "identity theft", "scam", "account compromised",
            "data breach", "blackmail", "threat", "extortion", "illegal"
    );

    private static final List<String> HIGH_INDICATORS = List.of(
            "urgent", "failed repeatedly", "money deducted", "amount debited",
            "money was deducted", "amount was debited", "deducted from my",
            "cannot access", "locked out", "service unavailable", "complete outage",
            "business loss", "lost data", "corrupted", "emergency", "critical",
            "immediately", "asap", "not working at all", "totally broken",
            "payment failed multiple times", "repeated failure"
    );

    private static final List<String> MEDIUM_INDICATORS = List.of(
            "not working", "error", "problem", "issue", "doesn't work", "does not work",
            "broken", "fail", "failed", "unable to", "can't", "cannot",
            "wrong", "incorrect", "missing", "delayed", "slow", "waiting"
    );

    private static final List<String> LOW_INDICATORS = List.of(
            "question", "information", "request", "query", "curious",
            "wondering", "suggestion", "feedback", "review", "update",
            "inquiry", "clarification", "help", "assistance", "guide"
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Analyse the given complaint text and return a fully populated AnalysisResult.
     */
    public AnalysisResult analyze(String text) {
        if (text == null || text.isBlank()) {
            return buildDefaultResult();
        }

        String lower = text.toLowerCase(Locale.ROOT);

        // 1. Score every category
        Map<String, Integer> scores = scoreCategoriesFor(lower);
        String bestCategory = pickBestCategory(scores);
        List<String> categoryKeywords = matchedKeywordsFor(lower, bestCategory);
        double confidence = computeConfidence(scores, bestCategory);

        // 2. Predict priority
        List<String> criticalMatches = matchAll(lower, CRITICAL_INDICATORS);
        List<String> highMatches     = matchAll(lower, HIGH_INDICATORS);
        List<String> mediumMatches   = matchAll(lower, MEDIUM_INDICATORS);
        List<String> lowMatches      = matchAll(lower, LOW_INDICATORS);

        ComplaintPriority priority;
        List<String> priorityKeywords;

        if (!criticalMatches.isEmpty()) {
            priority = ComplaintPriority.CRITICAL;
            priorityKeywords = criticalMatches;
        } else if (!highMatches.isEmpty()) {
            priority = ComplaintPriority.HIGH;
            priorityKeywords = highMatches;
        } else if (!mediumMatches.isEmpty()) {
            priority = ComplaintPriority.MEDIUM;
            priorityKeywords = mediumMatches;
        } else if (!lowMatches.isEmpty()) {
            priority = ComplaintPriority.LOW;
            priorityKeywords = lowMatches;
        } else {
            priority = ComplaintPriority.MEDIUM;
            priorityKeywords = List.of();
        }

        String explanation = buildExplanation(bestCategory, categoryKeywords,
                                              confidence, priority, priorityKeywords);

        return AnalysisResult.builder()
                .predictedCategory(bestCategory)
                .categoryScores(scores)
                .confidenceScore(confidence)
                .predictedPriority(priority)
                .categoryKeywordsMatched(categoryKeywords)
                .priorityKeywordsMatched(priorityKeywords)
                .explanation(explanation)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Integer> scoreCategoriesFor(String lower) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int total = 0;
            for (Map.Entry<String, Integer> kw : entry.getValue().entrySet()) {
                if (lower.contains(kw.getKey())) {
                    total += kw.getValue();
                }
            }
            scores.put(entry.getKey(), total);
        }
        return scores;
    }

    private String pickBestCategory(Map<String, Integer> scores) {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse("Other");
    }

    private List<String> matchedKeywordsFor(String lower, String category) {
        Map<String, Integer> dict = CATEGORY_KEYWORDS.getOrDefault(category, Map.of());
        return dict.keySet().stream()
                .filter(lower::contains)
                .collect(Collectors.toList());
    }

    private double computeConfidence(Map<String, Integer> scores, String best) {
        int winnerScore = scores.getOrDefault(best, 0);
        int total = scores.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return 0.0;
        // Round to 2 decimal places
        return Math.round((double) winnerScore / total * 100.0) / 100.0;
    }

    private List<String> matchAll(String lower, List<String> indicators) {
        return indicators.stream()
                .filter(lower::contains)
                .collect(Collectors.toList());
    }

    private String buildExplanation(String category, List<String> catKws,
                                    double confidence, ComplaintPriority priority,
                                    List<String> prioKws) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Category predicted as '%s' (confidence %.0f%%).", category, confidence * 100));

        if (!catKws.isEmpty()) {
            sb.append(" Category keywords detected: [")
              .append(String.join(", ", catKws))
              .append("].");
        }

        sb.append(String.format(" Priority set to %s.", priority.name()));

        if (!prioKws.isEmpty()) {
            sb.append(" Priority indicators found: [")
              .append(String.join(", ", prioKws))
              .append("].");
        } else {
            sb.append(" No specific priority indicators; defaulting to MEDIUM.");
        }

        return sb.toString();
    }

    private AnalysisResult buildDefaultResult() {
        return AnalysisResult.builder()
                .predictedCategory("Other")
                .categoryScores(Map.of())
                .confidenceScore(0.0)
                .predictedPriority(ComplaintPriority.MEDIUM)
                .categoryKeywordsMatched(List.of())
                .priorityKeywordsMatched(List.of())
                .explanation("No text provided; defaulting to category 'Other' and priority MEDIUM.")
                .build();
    }
}
