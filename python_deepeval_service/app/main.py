"""
FastAPI service for LLM Response Evaluation using DeepEval.

This service acts as a bridge between Java Selenium tests and Python's DeepEval library.
"""

from pathlib import Path
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
import logging

from .models import EvaluationRequest, EvaluationResult, MetricResult, HealthResponse
from .evaluator import evaluator

# Get the static files directory
STATIC_DIR = Path(__file__).parent.parent / "static"

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Create FastAPI app
app = FastAPI(
    title="LLM Evaluation Service",
    description="Evaluate LLM responses for quality, accuracy, and compliance using DeepEval",
    version="1.0.0",
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
async def root():
    """Serve the testing UI"""
    return FileResponse(STATIC_DIR / "index.html")


@app.get("/status", response_model=HealthResponse)
async def status():
    """Status endpoint with service info"""
    return HealthResponse(status="running", version="1.0.0")


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint for Java client"""
    return HealthResponse(status="healthy", version="1.0.0")


@app.post("/evaluate", response_model=EvaluationResult)
async def evaluate_llm_response(request: EvaluationRequest):
    """
    Evaluate an LLM response using DeepEval metrics.
    
    This endpoint receives LLM responses from Java Selenium tests and returns
    evaluation scores for quality, accuracy, hallucination, and compliance.
    
    **Supported Metrics:**
    - `accuracy`: How correct is the response?
    - `relevancy`: Does it answer the question?
    - `coherence`: Is it well-structured?
    - `hallucination`: Does it make things up? (requires context)
    - `faithfulness`: Is it faithful to provided context?
    - `compliance`: Does it meet custom standards?
    - `toxicity`: Is it safe and appropriate?
    
    **Example Request:**
    ```json
    {
        "llm_response": "The capital of France is Paris.",
        "expected_output": "Paris",
        "user_query": "What is the capital of France?",
        "metrics": ["accuracy", "relevancy"]
    }
    ```
    """
    logger.info(f"Received evaluation request for metrics: {request.metrics}")
    
    try:
        # Run evaluation
        result = evaluator.evaluate(
            llm_response=request.llm_response,
            expected_output=request.expected_output,
            context=request.context,
            user_query=request.user_query,
            metrics=request.metrics,
            custom_criteria=request.custom_criteria,
        )
        
        # Convert to response model
        metrics_results = {}
        for name, data in result["metrics"].items():
            metrics_results[name] = MetricResult(
                name=data["name"],
                score=data["score"],
                threshold=data["threshold"],
                passed=data["passed"],
                reason=data.get("reason"),
            )
        
        response = EvaluationResult(
            success=result["success"],
            passed=result["passed"],
            overall_score=result["overall_score"],
            metrics=metrics_results,
        )
        
        logger.info(f"Evaluation complete. Overall score: {response.overall_score:.2f}, Passed: {response.passed}")
        return response
        
    except Exception as e:
        logger.error(f"Evaluation failed: {str(e)}")
        return EvaluationResult(
            success=False,
            passed=False,
            overall_score=0.0,
            metrics={},
            error=str(e),
        )


@app.post("/evaluate/quick")
async def quick_evaluate(llm_response: str, expected_output: str, min_score: float = 0.7):
    """
    Quick evaluation endpoint for simple pass/fail checks.
    
    Returns just a boolean indicating if the response meets the minimum score threshold.
    """
    try:
        result = evaluator.evaluate(
            llm_response=llm_response,
            expected_output=expected_output,
            metrics=["accuracy", "relevancy"],
        )
        passed = result["overall_score"] >= min_score
        return {
            "passed": passed,
            "score": result["overall_score"],
        }
    except Exception as e:
        logger.error(f"Quick evaluation failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/evaluate/hallucination")
async def check_hallucination(llm_response: str, context: str):
    """
    Specifically check for hallucinations in an LLM response.
    
    Compares the response against the provided context to detect made-up information.
    """
    try:
        result = evaluator.evaluate(
            llm_response=llm_response,
            context=context,
            metrics=["hallucination", "faithfulness"],
        )
        return {
            "has_hallucination": not result["metrics"]["hallucination"]["passed"],
            "hallucination_score": result["metrics"]["hallucination"]["score"],
            "faithfulness_score": result["metrics"]["faithfulness"]["score"],
            "details": result["metrics"],
        }
    except Exception as e:
        logger.error(f"Hallucination check failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

