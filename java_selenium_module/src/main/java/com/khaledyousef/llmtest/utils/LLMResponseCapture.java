package com.khaledyousef.llmtest.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Function;

/**
 * Utility class for capturing LLM responses from web applications.
 * Handles the async nature of LLM responses with smart waiting.
 */
public class LLMResponseCapture {
    private static final Logger logger = LoggerFactory.getLogger(LLMResponseCapture.class);

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Duration defaultTimeout;

    public LLMResponseCapture(WebDriver driver) {
        this(driver, Duration.ofSeconds(30));
    }

    public LLMResponseCapture(WebDriver driver, Duration timeout) {
        this.driver = driver;
        this.defaultTimeout = timeout;
        this.wait = new WebDriverWait(driver, timeout);
    }

    /**
     * Send a prompt to the LLM via a chat input and capture the response.
     * 
     * @param inputSelector CSS selector for the input field
     * @param submitSelector CSS selector for the submit button
     * @param responseSelector CSS selector for the response container
     * @param prompt The prompt to send to the LLM
     * @return The captured LLM response text
     */
    public String sendPromptAndCapture(
            String inputSelector,
            String submitSelector,
            String responseSelector,
            String prompt
    ) {
        logger.info("Sending prompt: {}", prompt);

        // Find and clear input
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(inputSelector)));
        input.clear();
        input.sendKeys(prompt);

        // Click submit
        WebElement submitBtn = driver.findElement(By.cssSelector(submitSelector));
        submitBtn.click();

        // Wait for response to appear and stabilize
        return waitForStableResponse(responseSelector);
    }

    /**
     * Capture the latest LLM response from a chat interface.
     * Useful for chatbots where responses are in a list.
     * 
     * @param responseContainerSelector Selector for the response messages container
     * @param lastMessageSelector Selector for individual messages (captures the last one)
     * @return The latest response text
     */
    public String captureLatestResponse(String responseContainerSelector, String lastMessageSelector) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(responseContainerSelector)));
        
        // Wait a bit for streaming to complete
        waitForResponseToStabilize(lastMessageSelector);
        
        java.util.List<WebElement> messages = driver.findElements(By.cssSelector(lastMessageSelector));
        if (messages.isEmpty()) {
            throw new NoSuchElementException("No response messages found");
        }
        
        return messages.get(messages.size() - 1).getText();
    }

    /**
     * Wait for a response element to stop changing (streaming complete).
     * LLM responses often stream in character by character.
     */
    public String waitForStableResponse(String responseSelector) {
        logger.debug("Waiting for stable response at: {}", responseSelector);

        // First wait for element to exist
        WebElement responseElement = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.cssSelector(responseSelector))
        );

        // Wait for content to appear
        wait.until(driver -> {
            String text = responseElement.getText();
            return text != null && !text.trim().isEmpty();
        });

        // Wait for response to stabilize (stop streaming)
        return waitForTextToStabilize(responseElement, Duration.ofSeconds(2));
    }

    /**
     * Wait for element text to stop changing
     */
    private String waitForTextToStabilize(WebElement element, Duration stableDuration) {
        String previousText = "";
        long stableStartTime = 0;
        long stableMillis = stableDuration.toMillis();

        long startTime = System.currentTimeMillis();
        long maxWaitMillis = defaultTimeout.toMillis();

        while (System.currentTimeMillis() - startTime < maxWaitMillis) {
            String currentText = element.getText();

            if (currentText.equals(previousText)) {
                if (stableStartTime == 0) {
                    stableStartTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - stableStartTime >= stableMillis) {
                    logger.debug("Response stabilized after {}ms", System.currentTimeMillis() - startTime);
                    return currentText;
                }
            } else {
                previousText = currentText;
                stableStartTime = 0;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for stable response", e);
            }
        }

        logger.warn("Response did not fully stabilize, returning current text");
        return element.getText();
    }

    /**
     * Wait for response in elements that use typing indicators
     */
    public String waitForResponseWithTypingIndicator(
            String responseSelector,
            String typingIndicatorSelector
    ) {
        // Wait for typing indicator to disappear
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(typingIndicatorSelector)));
        
        // Then capture the response
        return waitForStableResponse(responseSelector);
    }

    /**
     * Capture response with custom wait condition
     */
    public String captureWithCustomWait(String responseSelector, Function<WebDriver, Boolean> customCondition) {
        wait.until(customCondition);
        WebElement responseElement = driver.findElement(By.cssSelector(responseSelector));
        return responseElement.getText();
    }

    private void waitForResponseToStabilize(String selector) {
        try {
            Thread.sleep(1000); // Initial wait for streaming to start
            
            String previousText = "";
            int stableCount = 0;
            
            while (stableCount < 3) { // Text must be stable for 3 checks
                java.util.List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                String currentText = elements.isEmpty() ? "" : elements.get(elements.size() - 1).getText();
                
                if (currentText.equals(previousText) && !currentText.isEmpty()) {
                    stableCount++;
                } else {
                    stableCount = 0;
                    previousText = currentText;
                }
                
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

