package com.supportportal.service;

import com.supportportal.dto.AnalysisResult;
import com.supportportal.entity.ComplaintPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the rule-based NLP ComplaintAnalysisService.
 * No Spring context needed — pure Java unit tests.
 */
class ComplaintAnalysisServiceTest {

    private ComplaintAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new ComplaintAnalysisService();
    }

    // ── Category prediction ───────────────────────────────────────────────────

    @Test
    @DisplayName("Payment keywords → Payment category")
    void paymentKeywords_predictPaymentCategory() {
        AnalysisResult result = service.analyze(
                "My payment failed and money was deducted from my account.");
        assertThat(result.getPredictedCategory()).isEqualTo("Payment");
    }

    @Test
    @DisplayName("Login keywords → Login/Access category")
    void loginKeywords_predictLoginCategory() {
        AnalysisResult result = service.analyze(
                "I cannot login to my account. Forgot my password and OTP is not working.");
        assertThat(result.getPredictedCategory()).isEqualTo("Login/Access");
    }

    @Test
    @DisplayName("Technical keywords → Technical Issue category")
    void technicalKeywords_predictTechnicalCategory() {
        AnalysisResult result = service.analyze(
                "The app keeps crashing with an error. It is completely broken and buggy.");
        assertThat(result.getPredictedCategory()).isEqualTo("Technical Issue");
    }

    @Test
    @DisplayName("Delivery keywords → Delivery category")
    void deliveryKeywords_predictDeliveryCategory() {
        AnalysisResult result = service.analyze(
                "My package has not been delivered. The shipment is delayed and tracking shows nothing.");
        assertThat(result.getPredictedCategory()).isEqualTo("Delivery");
    }

    @Test
    @DisplayName("Billing keywords → Billing category")
    void billingKeywords_predictBillingCategory() {
        AnalysisResult result = service.analyze(
                "I was overcharged on my invoice. I need a refund for the incorrect billing.");
        assertThat(result.getPredictedCategory()).isEqualTo("Billing");
    }

    @Test
    @DisplayName("Empty text → Other category, MEDIUM priority")
    void emptyText_returnsDefaults() {
        AnalysisResult result = service.analyze("");
        assertThat(result.getPredictedCategory()).isEqualTo("Other");
        assertThat(result.getPredictedPriority()).isEqualTo(ComplaintPriority.MEDIUM);
    }

    @Test
    @DisplayName("Null text → Other category")
    void nullText_returnsDefaults() {
        AnalysisResult result = service.analyze(null);
        assertThat(result.getPredictedCategory()).isEqualTo("Other");
    }

    // ── Priority prediction ───────────────────────────────────────────────────

    @Test
    @DisplayName("'fraud' keyword → CRITICAL priority")
    void fraudKeyword_predictsCritical() {
        AnalysisResult result = service.analyze(
                "There is fraud on my account. Someone stole my money through an unauthorized transaction.");
        assertThat(result.getPredictedPriority()).isEqualTo(ComplaintPriority.CRITICAL);
    }

    @Test
    @DisplayName("'hacked' keyword → CRITICAL priority")
    void hackedKeyword_predictsCritical() {
        AnalysisResult result = service.analyze("My account has been hacked.");
        assertThat(result.getPredictedPriority()).isEqualTo(ComplaintPriority.CRITICAL);
    }

    @Test
    @DisplayName("'urgent' keyword → HIGH priority")
    void urgentKeyword_predictsHigh() {
        AnalysisResult result = service.analyze(
                "This is urgent! I cannot access my account at all.");
        assertThat(result.getPredictedPriority()).isEqualTo(ComplaintPriority.HIGH);
    }

    @Test
    @DisplayName("'money deducted' phrase → HIGH priority")
    void moneyDeducted_predictsHigh() {
        AnalysisResult result = service.analyze(
                "Money was deducted from my wallet but the order was not placed.");
        assertThat(result.getPredictedPriority()).isEqualTo(ComplaintPriority.HIGH);
    }

    @Test
    @DisplayName("'not working' → MEDIUM priority")
    void notWorking_predictsMedium() {
        AnalysisResult result = service.analyze(
                "The feature is not working properly. There is an error on the screen.");
        assertThat(result.getPredictedPriority()).isEqualTo(ComplaintPriority.MEDIUM);
    }

    @Test
    @DisplayName("'question' / 'information' → LOW priority")
    void infoKeyword_predictsLow() {
        AnalysisResult result = service.analyze(
                "I have a question about my subscription. Just need some information.");
        assertThat(result.getPredictedPriority()).isEqualTo(ComplaintPriority.LOW);
    }

    // ── Confidence and explanation ────────────────────────────────────────────

    @Test
    @DisplayName("Analysis result always has non-null explanation")
    void analysisResult_hasExplanation() {
        AnalysisResult result = service.analyze("My payment failed multiple times.");
        assertThat(result.getExplanation()).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Confidence score between 0 and 1")
    void confidenceScore_validRange() {
        AnalysisResult result = service.analyze("payment failed transaction error");
        assertThat(result.getConfidenceScore()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("Category keywords matched list is populated")
    void categoryKeywords_populated() {
        AnalysisResult result = service.analyze("Payment failed and money was deducted.");
        assertThat(result.getCategoryKeywordsMatched()).isNotEmpty();
    }

    @Test
    @DisplayName("Priority keywords matched list is populated for HIGH")
    void priorityKeywords_populatedForHigh() {
        AnalysisResult result = service.analyze("This is urgent and money deducted!");
        assertThat(result.getPriorityKeywordsMatched()).isNotEmpty();
    }

    @Test
    @DisplayName("CRITICAL overrides HIGH when both present")
    void criticalOverridesHigh() {
        AnalysisResult result = service.analyze(
                "This is urgent. There is fraud on my account and money was stolen.");
        assertThat(result.getPredictedPriority()).isEqualTo(ComplaintPriority.CRITICAL);
    }
}
