# 🧪 Selenium + DeepEval: LLM Response Testing Framework

A framework for evaluating LLM responses in your applications using Java Selenium tests with a Python evaluation service powered by **Google Gemini**.

![Testing Console](https://img.shields.io/badge/UI-Testing%20Console-purple)
![Gemini Powered](https://img.shields.io/badge/AI-Gemini%20Powered-blue)
![Java](https://img.shields.io/badge/Java-Selenium-orange)
![Python](https://img.shields.io/badge/Python-FastAPI-green)

## ✨ Features

- **7 Evaluation Metrics**: Accuracy, Relevancy, Coherence, Hallucination, Faithfulness, Compliance, Toxicity
- **Web Testing Console**: Beautiful dark-themed UI for manual testing
- **Gemini Powered**: Uses Google's Gemini AI for intelligent evaluation
- **Java + Python Bridge**: Seamless integration between Selenium tests and Python evaluation
- **Custom Compliance Rules**: Define must-contain/must-not-contain terms
- **Detailed Feedback**: Get scores and explanations for each metric

## 🏗️ Architecture

```
┌─────────────────────┐     HTTP POST      ┌─────────────────────┐
│   Java Selenium     │ ──────────────────▶│   Python FastAPI    │
│   Test Suite        │                    │   Evaluation Service│
│                     │ ◀────────────────── │                     │
│   • UI Automation   │   JSON Response    │   • Gemini AI       │
│   • LLM Response    │                    │   • Score & Reason  │
│     Capture         │                    │   • Pass/Fail       │
└─────────────────────┘                    └─────────────────────┘
```

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/khaled-yousef-TV/selenium-deepEval-JavaPython.git
cd selenium-deepEval-JavaPython
```

### 2. Setup Python Service

```bash
cd python_deepeval_service

# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install fastapi uvicorn pydantic google-generativeai python-dotenv

# Configure your Gemini API key
echo "GEMINI_API_KEY=your-api-key-here" > .env
```

> 🔑 Get a free Gemini API key at: https://aistudio.google.com/apikey

### 3. Start the Service

```bash
python -m uvicorn app.main:app --reload --port 8000
```

### 4. Open the Testing Console

Navigate to: **http://localhost:8000**

## 📊 Available Metrics

| Metric | Description | Use Case |
|--------|-------------|----------|
| **Accuracy** | How correct is the response? | Fact-checking |
| **Relevancy** | Does it answer the question? | Q&A validation |
| **Coherence** | Is it well-structured? | Quality assurance |
| **Hallucination** | Does it make things up? | Fact verification |
| **Faithfulness** | Is it faithful to context? | RAG evaluation |
| **Compliance** | Does it meet custom rules? | Policy enforcement |
| **Toxicity** | Is it safe & appropriate? | Content moderation |

## 🔌 API Endpoints

### Health Check
```bash
GET /health
```

### Full Evaluation
```bash
POST /evaluate
Content-Type: application/json

{
  "llm_response": "Paris is the capital of France.",
  "expected_output": "Paris",
  "user_query": "What is the capital of France?",
  "context": "France is a country in Europe. Its capital is Paris.",
  "metrics": ["accuracy", "relevancy", "hallucination"],
  "custom_criteria": {
    "must_contain": ["Paris"],
    "must_not_contain": ["guaranteed"]
  }
}
```

### Response
```json
{
  "success": true,
  "passed": true,
  "overall_score": 0.95,
  "metrics": {
    "accuracy": {
      "name": "accuracy",
      "score": 1.0,
      "threshold": 0.7,
      "passed": true,
      "reason": "The response is perfectly accurate..."
    }
  }
}
```

### Quick Evaluation
```bash
POST /evaluate/quick?llm_response=...&expected_output=...&min_score=0.7
```

### Hallucination Check
```bash
POST /evaluate/hallucination
{
  "llm_response": "...",
  "context": "..."
}
```

## ☕ Java Integration

### Add Dependencies (build.gradle)

```gradle
dependencies {
    implementation 'org.seleniumhq.selenium:selenium-java:4.+'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
    testImplementation 'org.testng:testng:7.+'
}
```

### Example Test

```java
public class LLMQualityTest {
    
    private static final String EVAL_URL = "http://127.0.0.1:8000/evaluate";
    
    @Test
    public void testChatbotResponseQuality() throws Exception {
        // 1. Selenium captures LLM response from your app
        String llmResponse = driver.findElement(By.id("chatResponse")).getText();
        
        // 2. Send to evaluation service
        String json = """
            {
                "llm_response": "%s",
                "user_query": "What are your business hours?",
                "expected_output": "9 AM to 5 PM",
                "metrics": ["accuracy", "relevancy"]
            }
            """.formatted(llmResponse);
        
        Request request = new Request.Builder()
            .url(EVAL_URL)
            .post(RequestBody.create(json, MediaType.parse("application/json")))
            .build();
        
        // 3. Assert on evaluation results
        Response response = httpClient.newCall(request).execute();
        EvalResult result = objectMapper.readValue(response.body().string(), EvalResult.class);
        
        assertTrue(result.passed, "LLM response quality check failed");
        assertTrue(result.overall_score > 0.7, "Score below threshold");
    }
}
```

## 📁 Project Structure

```
.
├── python_deepeval_service/
│   ├── app/
│   │   ├── main.py          # FastAPI endpoints
│   │   ├── models.py        # Pydantic models
│   │   └── evaluator.py     # Gemini evaluation logic
│   ├── static/
│   │   └── index.html       # Testing Console UI
│   ├── requirements.txt
│   └── .env                 # API key (git-ignored)
│
├── java_selenium_module/
│   ├── src/test/java/       # Selenium tests
│   └── build.gradle
│
├── .gitignore
└── README.md
```

## 🔒 Security

- API keys are stored in `.env` files (git-ignored)
- Never commit API keys to version control
- The `.gitignore` includes: `.env`, `venv/`, `__pycache__/`

## 🎨 Testing Console Features

The web UI at `http://localhost:8000` provides:

- 🌙 Dark theme with gradient accents
- 📝 Input fields for response, query, expected output, context
- ☑️ Metric selection checkboxes
- 📏 Custom compliance rules
- 📊 Visual score bars with pass/fail indicators
- 💬 Detailed reasoning for each metric

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

---

**Built with ❤️ by [Khaled Yousef](https://khaledyousef.io)**
