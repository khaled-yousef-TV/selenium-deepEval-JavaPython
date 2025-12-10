package com.khaledyousef.llmtest.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.khaledyousef.llmtest.models.EvaluationRequest;
import com.khaledyousef.llmtest.models.EvaluationResult;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Client for communicating with the DeepEval Python FastAPI service.
 * This is the bridge between Java Selenium tests and Python LLM evaluation.
 */
public class DeepEvalClient {
    private static final Logger logger = LoggerFactory.getLogger(DeepEvalClient.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final Gson gson;

    // Default evaluation thresholds
    private double accuracyThreshold = 0.7;
    private double relevancyThreshold = 0.7;
    private double coherenceThreshold = 0.7;
    private double complianceThreshold = 0.8;

    public DeepEvalClient() {
        this("http://localhost:8000");
    }

    public DeepEvalClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Evaluate an LLM response with default metrics
     */
    public EvaluationResult evaluate(String llmResponse, String expectedOutput) {
        return evaluate(llmResponse, expectedOutput, null, null);
    }

    /**
     * Evaluate an LLM response with context
     */
    public EvaluationResult evaluate(String llmResponse, String expectedOutput, String context, String userQuery) {
        EvaluationRequest request = EvaluationRequest.builder()
                .llmResponse(llmResponse)
                .expectedOutput(expectedOutput)
                .context(context)
                .userQuery(userQuery)
                .metrics(Arrays.asList("accuracy", "relevancy", "coherence", "hallucination"))
                .build();

        return sendEvaluationRequest(request);
    }

    /**
     * Evaluate with custom compliance criteria
     * This is where you define YOUR standards for the AI responses
     */
    public EvaluationResult evaluateWithCompliance(
            String llmResponse,
            String expectedOutput,
            String context,
            List<String> mustContain,
            List<String> mustNotContain,
            int maxLength,
            String requiredTone
    ) {
        Map<String, Object> customCriteria = new HashMap<>();
        customCriteria.put("must_contain", mustContain);
        customCriteria.put("must_not_contain", mustNotContain);
        customCriteria.put("max_length", maxLength);
        customCriteria.put("required_tone", requiredTone);

        EvaluationRequest request = EvaluationRequest.builder()
                .llmResponse(llmResponse)
                .expectedOutput(expectedOutput)
                .context(context)
                .metrics(Arrays.asList("accuracy", "relevancy", "compliance", "toxicity"))
                .customCriteria(customCriteria)
                .build();

        return sendEvaluationRequest(request);
    }

    /**
     * Quick check if response meets minimum quality bar
     */
    public boolean meetsQualityBar(String llmResponse, String expectedOutput, double minScore) {
        EvaluationResult result = evaluate(llmResponse, expectedOutput);
        return result.isSuccess() && result.getOverallScore() >= minScore;
    }

    /**
     * Check for hallucinations in the response
     */
    public EvaluationResult checkHallucination(String llmResponse, String context) {
        EvaluationRequest request = EvaluationRequest.builder()
                .llmResponse(llmResponse)
                .context(context)
                .metrics(Arrays.asList("hallucination", "faithfulness"))
                .build();

        return sendEvaluationRequest(request);
    }

    /**
     * Send evaluation request to Python service
     */
    private EvaluationResult sendEvaluationRequest(EvaluationRequest request) {
        String json = gson.toJson(request);
        logger.info("Sending evaluation request to {}", baseUrl);
        logger.debug("Request payload: {}", json);

        RequestBody body = RequestBody.create(json, JSON);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + "/evaluate")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                EvaluationResult errorResult = new EvaluationResult();
                errorResult.setSuccess(false);
                errorResult.setError("HTTP Error: " + response.code() + " - " + response.message());
                return errorResult;
            }

            String responseBody = response.body().string();
            logger.debug("Response: {}", responseBody);

            return gson.fromJson(responseBody, EvaluationResult.class);

        } catch (IOException e) {
            logger.error("Failed to communicate with DeepEval service", e);
            EvaluationResult errorResult = new EvaluationResult();
            errorResult.setSuccess(false);
            errorResult.setError("Connection error: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Health check for the Python service
     */
    public boolean isServiceHealthy() {
        Request request = new Request.Builder()
                .url(baseUrl + "/health")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            logger.warn("Health check failed: {}", e.getMessage());
            return false;
        }
    }

    // Threshold setters for customization
    public DeepEvalClient setAccuracyThreshold(double threshold) {
        this.accuracyThreshold = threshold;
        return this;
    }

    public DeepEvalClient setRelevancyThreshold(double threshold) {
        this.relevancyThreshold = threshold;
        return this;
    }

    public DeepEvalClient setCoherenceThreshold(double threshold) {
        this.coherenceThreshold = threshold;
        return this;
    }

    public DeepEvalClient setComplianceThreshold(double threshold) {
        this.complianceThreshold = threshold;
        return this;
    }
}

