# Error Handling Guide - Ahmes Library

## Overview

This guide describes error handling patterns, exception types, and best practices when using the Ahmes library for SEC document parsing and financial data extraction.

---

## Exception Hierarchy

### Current State (v1.x)

The library currently uses standard Kotlin/Java exceptions:
- `IllegalArgumentException` - Invalid input parameters
- `IllegalStateException` - Invalid parser state
- `NullPointerException` - Null values where not expected
- `NumberFormatException` - Invalid number parsing
- `IOException` - File I/O errors

### Recommended Future State (v2.0+)

We recommend implementing a custom exception hierarchy:

```kotlin
sealed class AhmesException(message: String, cause: Throwable? = null) : Exception(message, cause)

// Parsing errors
class ParseException(message: String, cause: Throwable? = null) : AhmesException(message, cause)
class UnsupportedFormatException(format: String) : AhmesException("Unsupported format: $format")
class XbrlExtractionException(message: String, cause: Throwable? = null) : AhmesException(message, cause)

// Data validation errors
class InvalidFinancialDataException(message: String) : AhmesException(message)
class MissingRequiredFieldException(fieldName: String) : AhmesException("Missing required field: $fieldName")

// AI/Model errors
class ModelLoadException(modelName: String, cause: Throwable?) : AhmesException("Failed to load model: $modelName", cause)
class InferenceException(message: String, cause: Throwable? = null) : AhmesException(message, cause)

// Network/API errors
class SecApiException(message: String, statusCode: Int? = null) : AhmesException("SEC API error [$statusCode]: $message")
```

---

## Common Error Scenarios

### 1. File Format Errors

**Problem:** Attempting to parse an unsupported file format

```kotlin
// ❌ Bad: No error handling
val result = ParserFactory.parse(file, metadata)

// ✅ Good: Handle unsupported formats
try {
    val result = ParserFactory.parse(file, metadata)
    // Process result
} catch (e: IllegalArgumentException) {
    logger.error("Unsupported file format: ${file.extension}", e)
    // Fallback: Try text extraction
    val textContent = file.readText()
    // Continue with text parsing...
}
```

**Best Practice:**
- Always check file extension before parsing
- Provide fallback for unsupported formats
- Log the error with context

### 2. Missing or Invalid Financial Data

**Problem:** Expected financial metrics are not found in the document

```kotlin
// ❌ Bad: Assuming data exists
val revenue = metrics.first { it.category == MetricCategory.REVENUE }

// ✅ Good: Handle missing data gracefully
val revenue = metrics.find { it.category == MetricCategory.REVENUE }
if (revenue == null) {
    logger.warn("Revenue metric not found in document")
    // Use default or alternative approach
}

// ✅ Better: Use safe navigation and defaults
val revenueValue = metrics
    .find { it.category == MetricCategory.REVENUE }
    ?.getRawValueBigDecimal()
    ?: BigDecimal.ZERO
```

**Best Practice:**
- Never assume metrics exist
- Use `find()` instead of `first()`
- Provide sensible defaults
- Log warnings for missing critical data

### 3. Number Parsing Errors

**Problem:** Invalid number formats in SEC documents

```kotlin
// ❌ Bad: Direct conversion without validation
val value = text.toDouble()

// ✅ Good: Safe parsing with error handling
fun parseFinancialValue(text: String): BigDecimal? {
    return try {
        // Remove common formatting
        val cleaned = text.replace(",", "")
            .replace("$", "")
            .replace("(", "-")
            .replace(")", "")
            .trim()

        BigDecimal(cleaned)
    } catch (e: NumberFormatException) {
        logger.warn("Failed to parse financial value: '$text'", e)
        null
    }
}
```

**Best Practice:**
- Clean input before parsing
- Use `BigDecimal` for financial values
- Return null for invalid values
- Log parsing failures with original input

### 4. AI Model Loading Errors

**Problem:** DJL model fails to load (missing dependencies, GPU issues)

```kotlin
// ❌ Bad: Crash on model load failure
val predictor = DjlModelManager.getPredictor(ModelType.SENTIMENT)

// ✅ Good: Graceful degradation
val sentimentResult = try {
    val predictor = DjlModelManager.getPredictor(ModelType.SENTIMENT)
    predictor.predict(text)
} catch (e: Exception) {
    logger.error("AI model unavailable, using fallback", e)
    // Fallback: Simple keyword-based sentiment
    simpleSentimentAnalysis(text)
}

// ✅ Better: Check availability first
fun analyzeSentiment(text: String): SentimentResult {
    return if (DjlModelManager.isAvailable()) {
        try {
            val predictor = DjlModelManager.getPredictor(ModelType.SENTIMENT)
            predictor.predict(text)
        } catch (e: Exception) {
            logger.error("Prediction failed", e)
            fallbackSentiment(text)
        }
    } else {
        logger.info("AI models not available, using fallback")
        fallbackSentiment(text)
    }
}
```

**Best Practice:**
- Check model availability before use
- Implement fallback logic
- Don't crash the application if AI fails
- Provide non-AI alternatives

### 5. Large File Handling

**Problem:** OutOfMemoryError when parsing very large files

```kotlin
// ❌ Bad: Load entire file into memory
val content = File("huge-10k.html").readText()
val result = parser.parseHtml(content, metadata)

// ✅ Good: Stream processing for large files
fun parseLargeFile(file: File, metadata: SecReportMetadata): ParseResult {
    return if (file.length() > 10 * 1024 * 1024) { // > 10MB
        // Use streaming or chunked processing
        file.bufferedReader().use { reader ->
            val sections = mutableMapOf<String, String>()
            var currentSection: String? = null
            val buffer = StringBuilder()

            reader.forEachLine { line ->
                // Process line by line
                if (isSectionHeader(line)) {
                    if (currentSection != null) {
                        sections[currentSection] = buffer.toString()
                        buffer.clear()
                    }
                    currentSection = extractSectionName(line)
                } else {
                    buffer.appendLine(line)
                }
            }

            // Create result from sections
            createParseResult(sections, metadata)
        }
    } else {
        // Normal processing for smaller files
        parser.parseHtml(file.readText(), metadata)
    }
}
```

**Best Practice:**
- Check file size before loading
- Use streaming for files > 10MB
- Process line-by-line or in chunks
- Monitor memory usage

---

## Error Recovery Patterns

### Partial Success Pattern

When some data can be extracted despite errors:

```kotlin
data class ParseResult(
    val data: FinancialData,
    val warnings: List<ParseWarning> = emptyList(),
    val errors: List<ParseError> = emptyList()
) {
    val isComplete: Boolean get() = errors.isEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
}

fun parseDocument(content: String): ParseResult {
    val warnings = mutableListOf<ParseWarning>()
    val errors = mutableListOf<ParseError>()

    val metrics = try {
        extractMetrics(content)
    } catch (e: Exception) {
        errors.add(ParseError("Metric extraction failed", e))
        emptyList()
    }

    val riskFactors = try {
        extractRiskFactors(content)
    } catch (e: Exception) {
        warnings.add(ParseWarning("Risk factors not found"))
        emptyList()
    }

    return ParseResult(
        data = FinancialData(metrics, riskFactors),
        warnings = warnings,
        errors = errors
    )
}
```

### Retry Pattern

For transient failures (network, file locks):

```kotlin
fun <T> retryOnFailure(
    maxAttempts: Int = 3,
    delayMs: Long = 1000,
    operation: () -> T
): T {
    var lastException: Exception? = null

    repeat(maxAttempts) { attempt ->
        try {
            return operation()
        } catch (e: IOException) {
            lastException = e
            if (attempt < maxAttempts - 1) {
                Thread.sleep(delayMs * (attempt + 1))
            }
        }
    }

    throw lastException ?: IllegalStateException("Retry failed")
}

// Usage
val content = retryOnFailure(maxAttempts = 3) {
    downloadSecDocument(url)
}
```

---

## Logging Best Practices

### Error Logging

```kotlin
import org.slf4j.LoggerFactory

class Form10KParser {
    private val logger = LoggerFactory.getLogger(Form10KParser::class.java)

    fun parse(content: String): Form10KParseResult {
        logger.info("Starting 10-K parsing, content length: ${content.length}")

        try {
            val sections = extractSections(content)
            logger.debug("Extracted ${sections.size} sections")

            return Form10KParseResult(/* ... */)
        } catch (e: Exception) {
            logger.error("Failed to parse 10-K document", e)
            throw ParseException("10-K parsing failed", e)
        }
    }
}
```

### Log Levels

- **ERROR**: Unrecoverable failures
- **WARN**: Recoverable issues, missing optional data
- **INFO**: Important milestones (parsing started/completed)
- **DEBUG**: Detailed progress, extracted counts
- **TRACE**: Very detailed (line-by-line processing)

---

## Testing Error Scenarios

Always test error conditions:

```kotlin
@Test
fun `parser should handle missing sections gracefully`() {
    val invalidHtml = "<html><body><p>No sections</p></body></html>"
    val metadata = createTestMetadata()

    val result = parser.parseHtml(invalidHtml, metadata)

    // Should not crash - return empty or null sections
    assertNotNull(result)
    assertTrue(result.businessDescription == null || result.businessDescription!!.isEmpty())
}

@Test
fun `parser should handle malformed numbers`() {
    val content = "Revenue: $invalid123.45"

    val metrics = EnhancedFinancialParser.parseFinancialMetrics(content)

    // Should skip invalid values, not crash
    assertTrue(metrics.none { it.name == "Revenue" } || metrics.isEmpty())
}
```

---

## Quick Reference

| Error Type | Handling Strategy | Fail or Recover? |
|------------|-------------------|------------------|
| Invalid file format | Log + Try fallback | Recover |
| Missing required data | Log + Return null | Recover |
| Malformed numbers | Log + Skip value | Recover |
| AI model unavailable | Log + Use fallback | Recover |
| Out of memory | Log + Stream process | Recover |
| Invalid document structure | Log + Partial parse | Recover |
| Network timeout | Log + Retry 3x | Recover or Fail |
| File not found | Log + Fail fast | **Fail** |

---

## Additional Resources

- [Kotlin Exception Handling](https://kotlinlang.org/docs/exceptions.html)
- [SLF4J Logging](http://www.slf4j.org/manual.html)
- [BigDecimal Best Practices](https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html)

---

*Last Updated: 2026-01-11*
