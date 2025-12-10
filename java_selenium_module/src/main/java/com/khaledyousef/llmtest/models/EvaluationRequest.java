package com.khaledyousef.llmtest.models;

import java.util.List;
import java.util.Map;

/**
 * Request object to send to the DeepEval Python service
 */
public class EvaluationRequest {
    private String llmResponse;
    private String expectedOutput;
    private String context;
    private String userQuery;
    private List<String> metrics;
    private Map<String, Object> customCriteria;

    public EvaluationRequest() {}

    public static Builder builder() {
        return new Builder();
    }

    // Getters and Setters
    public String getLlmResponse() { return llmResponse; }
    public void setLlmResponse(String llmResponse) { this.llmResponse = llmResponse; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getUserQuery() { return userQuery; }
    public void setUserQuery(String userQuery) { this.userQuery = userQuery; }

    public List<String> getMetrics() { return metrics; }
    public void setMetrics(List<String> metrics) { this.metrics = metrics; }

    public Map<String, Object> getCustomCriteria() { return customCriteria; }
    public void setCustomCriteria(Map<String, Object> customCriteria) { this.customCriteria = customCriteria; }

    public static class Builder {
        private final EvaluationRequest request = new EvaluationRequest();

        public Builder llmResponse(String response) {
            request.setLlmResponse(response);
            return this;
        }

        public Builder expectedOutput(String expected) {
            request.setExpectedOutput(expected);
            return this;
        }

        public Builder context(String context) {
            request.setContext(context);
            return this;
        }

        public Builder userQuery(String query) {
            request.setUserQuery(query);
            return this;
        }

        public Builder metrics(List<String> metrics) {
            request.setMetrics(metrics);
            return this;
        }

        public Builder customCriteria(Map<String, Object> criteria) {
            request.setCustomCriteria(criteria);
            return this;
        }

        public EvaluationRequest build() {
            return request;
        }
    }
}

