from pydantic import BaseModel
from typing import List, Dict, Any, Optional


class EvaluationRequest(BaseModel):
    """Request model for LLM evaluation"""
    llm_response: str
    expected_output: Optional[str] = None
    context: Optional[str] = None
    user_query: Optional[str] = None
    metrics: List[str] = ["accuracy", "relevancy", "coherence"]
    custom_criteria: Optional[Dict[str, Any]] = None


class MetricResult(BaseModel):
    """Individual metric evaluation result"""
    name: str
    score: float
    threshold: float
    passed: bool
    reason: Optional[str] = None


class EvaluationResult(BaseModel):
    """Complete evaluation result"""
    success: bool
    passed: bool
    overall_score: float
    metrics: Dict[str, MetricResult]
    error: Optional[str] = None


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    version: str

