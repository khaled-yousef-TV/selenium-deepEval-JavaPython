"""
DeepEval-based LLM Response Evaluator

This module provides evaluation metrics for LLM responses:
- Accuracy: How correct is the response?
- Relevancy: Does it answer the question?
- Coherence: Is it well-structured and logical?
- Hallucination: Does it make things up?
- Compliance: Does it meet custom standards?
- Toxicity: Is it safe and appropriate?
"""

import os
from typing import Dict, Any, List, Optional
from deepeval import evaluate
from deepeval.metrics import (
    AnswerRelevancyMetric,
    FaithfulnessMetric,
    HallucinationMetric,
    GEval,
    ToxicityMetric,
)
from deepeval.test_case import LLMTestCase


# Default thresholds
DEFAULT_THRESHOLDS = {
    "accuracy": 0.7,
    "relevancy": 0.7,
    "coherence": 0.7,
    "hallucination": 0.5,  # Lower score = fewer hallucinations
    "faithfulness": 0.7,
    "compliance": 0.8,
    "toxicity": 0.3,  # Lower score = less toxic
}


class LLMEvaluator:
    """Evaluator class that wraps DeepEval metrics"""

    def __init__(self, model: str = "gpt-4o-mini"):
        """
        Initialize the evaluator.
        
        Args:
            model: The LLM to use for evaluation (requires OPENAI_API_KEY env var)
        """
        self.model = model
        self.thresholds = DEFAULT_THRESHOLDS.copy()

    def set_threshold(self, metric_name: str, threshold: float):
        """Update threshold for a specific metric"""
        self.thresholds[metric_name] = threshold

    def evaluate(
        self,
        llm_response: str,
        expected_output: Optional[str] = None,
        context: Optional[str] = None,
        user_query: Optional[str] = None,
        metrics: List[str] = None,
        custom_criteria: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """
        Evaluate an LLM response using specified metrics.
        
        Args:
            llm_response: The actual LLM output to evaluate
            expected_output: What the response should ideally contain
            context: Background information/facts the response should be based on
            user_query: The original question/prompt
            metrics: List of metric names to evaluate
            custom_criteria: Custom compliance rules
            
        Returns:
            Dictionary with evaluation results for each metric
        """
        if metrics is None:
            metrics = ["accuracy", "relevancy", "coherence"]

        # Create the test case
        test_case = LLMTestCase(
            input=user_query or "",
            actual_output=llm_response,
            expected_output=expected_output,
            context=[context] if context else None,
            retrieval_context=[context] if context else None,
        )

        results = {}
        total_score = 0.0
        all_passed = True

        for metric_name in metrics:
            metric_result = self._evaluate_metric(
                metric_name, test_case, custom_criteria
            )
            results[metric_name] = metric_result
            total_score += metric_result["score"]
            if not metric_result["passed"]:
                all_passed = False

        overall_score = total_score / len(metrics) if metrics else 0.0

        return {
            "success": True,
            "passed": all_passed,
            "overall_score": overall_score,
            "metrics": results,
        }

    def _evaluate_metric(
        self,
        metric_name: str,
        test_case: LLMTestCase,
        custom_criteria: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """Evaluate a single metric"""
        threshold = self.thresholds.get(metric_name, 0.7)

        try:
            if metric_name == "accuracy":
                return self._evaluate_accuracy(test_case, threshold)
            elif metric_name == "relevancy":
                return self._evaluate_relevancy(test_case, threshold)
            elif metric_name == "coherence":
                return self._evaluate_coherence(test_case, threshold)
            elif metric_name == "hallucination":
                return self._evaluate_hallucination(test_case, threshold)
            elif metric_name == "faithfulness":
                return self._evaluate_faithfulness(test_case, threshold)
            elif metric_name == "compliance":
                return self._evaluate_compliance(test_case, custom_criteria, threshold)
            elif metric_name == "toxicity":
                return self._evaluate_toxicity(test_case, threshold)
            else:
                return {
                    "name": metric_name,
                    "score": 0.0,
                    "threshold": threshold,
                    "passed": False,
                    "reason": f"Unknown metric: {metric_name}",
                }
        except Exception as e:
            return {
                "name": metric_name,
                "score": 0.0,
                "threshold": threshold,
                "passed": False,
                "reason": f"Evaluation error: {str(e)}",
            }

    def _evaluate_accuracy(self, test_case: LLMTestCase, threshold: float) -> Dict:
        """Evaluate how accurate/correct the response is"""
        metric = GEval(
            name="Accuracy",
            criteria="Determine if the actual output accurately answers the question and matches the expected output in meaning.",
            evaluation_params=[
                "input",
                "actual_output",
                "expected_output",
            ],
            threshold=threshold,
            model=self.model,
        )
        metric.measure(test_case)
        return {
            "name": "accuracy",
            "score": metric.score or 0.0,
            "threshold": threshold,
            "passed": metric.score >= threshold if metric.score else False,
            "reason": metric.reason,
        }

    def _evaluate_relevancy(self, test_case: LLMTestCase, threshold: float) -> Dict:
        """Evaluate answer relevancy to the query"""
        metric = AnswerRelevancyMetric(threshold=threshold, model=self.model)
        metric.measure(test_case)
        return {
            "name": "relevancy",
            "score": metric.score or 0.0,
            "threshold": threshold,
            "passed": metric.score >= threshold if metric.score else False,
            "reason": metric.reason,
        }

    def _evaluate_coherence(self, test_case: LLMTestCase, threshold: float) -> Dict:
        """Evaluate response coherence and structure"""
        metric = GEval(
            name="Coherence",
            criteria="Evaluate if the response is well-structured, logically organized, and easy to understand.",
            evaluation_params=["actual_output"],
            threshold=threshold,
            model=self.model,
        )
        metric.measure(test_case)
        return {
            "name": "coherence",
            "score": metric.score or 0.0,
            "threshold": threshold,
            "passed": metric.score >= threshold if metric.score else False,
            "reason": metric.reason,
        }

    def _evaluate_hallucination(self, test_case: LLMTestCase, threshold: float) -> Dict:
        """Check for hallucinations (made-up information)"""
        if not test_case.context:
            return {
                "name": "hallucination",
                "score": 1.0,
                "threshold": threshold,
                "passed": True,
                "reason": "No context provided to check hallucination against",
            }

        metric = HallucinationMetric(threshold=threshold, model=self.model)
        metric.measure(test_case)
        # Note: For hallucination, lower score = better (less hallucination)
        score = 1.0 - (metric.score or 0.0)  # Invert so higher = better
        return {
            "name": "hallucination",
            "score": score,
            "threshold": threshold,
            "passed": score >= threshold,
            "reason": metric.reason,
        }

    def _evaluate_faithfulness(self, test_case: LLMTestCase, threshold: float) -> Dict:
        """Evaluate faithfulness to the provided context"""
        if not test_case.retrieval_context:
            return {
                "name": "faithfulness",
                "score": 1.0,
                "threshold": threshold,
                "passed": True,
                "reason": "No context provided",
            }

        metric = FaithfulnessMetric(threshold=threshold, model=self.model)
        metric.measure(test_case)
        return {
            "name": "faithfulness",
            "score": metric.score or 0.0,
            "threshold": threshold,
            "passed": metric.score >= threshold if metric.score else False,
            "reason": metric.reason,
        }

    def _evaluate_compliance(
        self,
        test_case: LLMTestCase,
        custom_criteria: Optional[Dict[str, Any]],
        threshold: float,
    ) -> Dict:
        """Evaluate compliance with custom criteria"""
        if not custom_criteria:
            return {
                "name": "compliance",
                "score": 1.0,
                "threshold": threshold,
                "passed": True,
                "reason": "No custom criteria provided",
            }

        score = 1.0
        reasons = []

        response = test_case.actual_output.lower()

        # Check must_contain
        must_contain = custom_criteria.get("must_contain", [])
        for term in must_contain:
            if term.lower() not in response:
                score -= 0.2
                reasons.append(f"Missing required term: '{term}'")

        # Check must_not_contain
        must_not_contain = custom_criteria.get("must_not_contain", [])
        for term in must_not_contain:
            if term.lower() in response:
                score -= 0.3
                reasons.append(f"Contains forbidden term: '{term}'")

        # Check max_length
        max_length = custom_criteria.get("max_length")
        if max_length and len(test_case.actual_output) > max_length:
            score -= 0.1
            reasons.append(f"Exceeds max length of {max_length}")

        score = max(0.0, score)

        return {
            "name": "compliance",
            "score": score,
            "threshold": threshold,
            "passed": score >= threshold,
            "reason": "; ".join(reasons) if reasons else "All compliance checks passed",
        }

    def _evaluate_toxicity(self, test_case: LLMTestCase, threshold: float) -> Dict:
        """Check for toxic or inappropriate content"""
        metric = ToxicityMetric(threshold=threshold, model=self.model)
        metric.measure(test_case)
        # For toxicity, lower score = better
        score = 1.0 - (metric.score or 0.0)
        return {
            "name": "toxicity",
            "score": score,
            "threshold": 1.0 - threshold,  # Invert threshold too
            "passed": (metric.score or 0.0) <= threshold,
            "reason": metric.reason,
        }


# Create a default evaluator instance
evaluator = LLMEvaluator()

