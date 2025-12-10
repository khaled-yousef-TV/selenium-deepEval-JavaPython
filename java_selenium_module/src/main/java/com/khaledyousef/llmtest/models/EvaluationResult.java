package com.khaledyousef.llmtest.models;

import java.util.Map;

/**
 * Response object from the DeepEval Python service
 */
public class EvaluationResult {
    private boolean success;
    private boolean passed;
    private double overallScore;
    private Map<String, MetricResult> metrics;
    private String error;

    public EvaluationResult() {}

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }

    public Map<String, MetricResult> getMetrics() { return metrics; }
    public void setMetrics(Map<String, MetricResult> metrics) { this.metrics = metrics; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    /**
     * Check if a specific metric passed
     */
    public boolean metricPassed(String metricName) {
        if (metrics == null || !metrics.containsKey(metricName)) {
            return false;
        }
        return metrics.get(metricName).isPassed();
    }

    /**
     * Get score for a specific metric
     */
    public double getMetricScore(String metricName) {
        if (metrics == null || !metrics.containsKey(metricName)) {
            return 0.0;
        }
        return metrics.get(metricName).getScore();
    }

    /**
     * Individual metric result
     */
    public static class MetricResult {
        private String name;
        private double score;
        private double threshold;
        private boolean passed;
        private String reason;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }

        public double getThreshold() { return threshold; }
        public void setThreshold(double threshold) { this.threshold = threshold; }

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        @Override
        public String toString() {
            return String.format("%s: %.2f (threshold: %.2f) - %s", 
                name, score, threshold, passed ? "PASSED" : "FAILED");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Evaluation Result ===\n");
        sb.append(String.format("Overall: %s (Score: %.2f)\n", passed ? "PASSED" : "FAILED", overallScore));
        if (metrics != null) {
            sb.append("\nMetrics:\n");
            metrics.forEach((key, value) -> sb.append("  - ").append(value).append("\n"));
        }
        return sb.toString();
    }
}

