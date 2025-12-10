package com.khaledyousef.llmtest;

import com.khaledyousef.llmtest.client.DeepEvalClient;
import com.khaledyousef.llmtest.models.EvaluationResult;
import com.khaledyousef.llmtest.utils.LLMResponseCapture;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Example test demonstrating LLM response evaluation with Selenium + DeepEval.
 * 
 * These tests show how to:
 * 1. Capture LLM responses from a web UI
 * 2. Evaluate them for quality, accuracy, and compliance
 * 3. Assert based on evaluation results
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LLMAppTests {
    private static final Logger logger = LoggerFactory.getLogger(LLMAppTests.class);

    private WebDriver driver;
    private DeepEvalClient evalClient;
    private LLMResponseCapture responseCapture;

    @BeforeAll
    void setUp() {
        // Setup WebDriver
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Run headless for CI/CD
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        driver = new ChromeDriver(options);

        // Setup DeepEval client
        evalClient = new DeepEvalClient("http://localhost:8000");
        
        // Setup response capture utility
        responseCapture = new LLMResponseCapture(driver);

        // Verify DeepEval service is running
        assertThat(evalClient.isServiceHealthy())
            .as("DeepEval Python service should be running")
            .isTrue();
    }

    @AfterAll
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    @DisplayName("Test: LLM response meets quality threshold")
    void testLLMResponseQuality() {
        // Example: Testing a customer support chatbot
        String userQuery = "How do I reset my password?";
        String expectedOutput = "To reset your password, go to Settings > Security > Reset Password";
        
        // In a real test, you would:
        // 1. Navigate to your app
        // 2. Send the query via UI
        // 3. Capture the response
        
        // For demo, we simulate the LLM response
        String llmResponse = "To reset your password, navigate to Settings, then click on Security, " +
                           "and select the Reset Password option. You'll receive an email with further instructions.";

        // Evaluate the response
        EvaluationResult result = evalClient.evaluate(llmResponse, expectedOutput, null, userQuery);

        // Log the results
        logger.info("\n{}", result);

        // Assertions
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOverallScore()).isGreaterThan(0.7);
        assertThat(result.metricPassed("relevancy")).isTrue();
    }

    @Test
    @DisplayName("Test: LLM response has no hallucinations")
    void testNoHallucinations() {
        String context = "Our company offers three products: Basic Plan ($10/mo), Pro Plan ($25/mo), Enterprise Plan (custom pricing).";
        String llmResponse = "We offer three plans: Basic at $10 per month, Pro at $25 per month, and Enterprise with custom pricing based on your needs.";

        EvaluationResult result = evalClient.checkHallucination(llmResponse, context);

        logger.info("\n{}", result);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.metricPassed("hallucination")).isTrue();
    }

    @Test
    @DisplayName("Test: LLM response with hallucination should fail")
    void testHallucinationDetection() {
        String context = "Our company offers three products: Basic Plan ($10/mo), Pro Plan ($25/mo), Enterprise Plan (custom pricing).";
        String llmResponse = "We offer four plans including a Premium Plan at $50/mo and a free trial for 30 days."; // This is hallucinated!

        EvaluationResult result = evalClient.checkHallucination(llmResponse, context);

        logger.info("\n{}", result);

        // The hallucination check should fail because the response contains made-up information
        assertThat(result.getMetricScore("hallucination")).isLessThan(0.5);
    }

    @Test
    @DisplayName("Test: LLM response meets custom compliance standards")
    void testComplianceWithCustomStandards() {
        String llmResponse = "Thank you for contacting support. To resolve your issue, please follow these steps: " +
                           "1. Check your internet connection. 2. Clear your browser cache. 3. Try again. " +
                           "If the issue persists, our team is here to help.";
        String expectedOutput = "Provide troubleshooting steps for connectivity issues";
        
        // Define YOUR compliance standards
        List<String> mustContain = Arrays.asList("support", "steps");
        List<String> mustNotContain = Arrays.asList("stupid", "idiot", "competitor-name");
        int maxLength = 500;
        String requiredTone = "professional";

        EvaluationResult result = evalClient.evaluateWithCompliance(
            llmResponse,
            expectedOutput,
            null,
            mustContain,
            mustNotContain,
            maxLength,
            requiredTone
        );

        logger.info("\n{}", result);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.metricPassed("compliance")).isTrue();
        assertThat(result.metricPassed("toxicity")).isTrue();
    }

    @Test
    @DisplayName("Test: Quick quality bar check")
    void testQuickQualityBar() {
        String llmResponse = "Your order #12345 has been shipped and will arrive in 3-5 business days.";
        String expectedOutput = "Order status update with tracking information";

        boolean meetsBar = evalClient.meetsQualityBar(llmResponse, expectedOutput, 0.7);

        assertThat(meetsBar)
            .as("Response should meet minimum quality bar of 0.7")
            .isTrue();
    }

    @Test
    @DisplayName("Integration Test: Full UI flow with LLM evaluation")
    @Disabled("Enable when testing against a real application")
    void testFullUIFlowWithEvaluation() {
        // Navigate to your chat application
        driver.get("https://your-app.com/chat");

        // Use the response capture utility
        String llmResponse = responseCapture.sendPromptAndCapture(
            "#chat-input",           // Input field selector
            "#send-button",          // Submit button selector
            ".assistant-message:last-child",  // Response selector
            "What are your business hours?"   // The prompt
        );

        // Define expected behavior
        String expectedOutput = "Business hours information";
        String context = "Our business hours are Monday-Friday 9AM-5PM EST";

        // Evaluate
        EvaluationResult result = evalClient.evaluate(llmResponse, expectedOutput, context, "What are your business hours?");

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOverallScore()).isGreaterThan(0.8);
        assertThat(result.metricPassed("accuracy")).isTrue();
        assertThat(result.metricPassed("relevancy")).isTrue();
    }
}

