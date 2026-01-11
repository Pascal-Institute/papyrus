package com.pascal.institute.ahmes.exception

/**
 * Base exception for all Ahmes library errors.
 *
 * All exceptions thrown by the Ahmes library inherit from this sealed class, making it easy to
 * catch all library-specific errors in client code.
 *
 * ## Usage Example
 *
 * ```kotlin
 * try {
 *     val result = parser.parseHtml(content, metadata)
 * } catch (e: AhmesException) {
 *     // Handle all Ahmes-specific errors
 *     logger.error("Ahmes parsing failed", e)
 * }
 * ```
 *
 * @property message The error message
 * @property cause The underlying cause of this exception (if any)
 */
sealed class AhmesException(message: String, cause: Throwable? = null) : Exception(message, cause)

// ============================================================================
// Parsing Exceptions
// ============================================================================

/**
 * Thrown when document parsing fails.
 *
 * This is a general parsing error that can occur during:
 * - HTML/PDF/TXT parsing
 * - Section extraction
 * - Structure recognition
 *
 * @property message Describes what failed during parsing
 * @property cause The underlying exception that caused the parsing failure
 */
class ParseException(message: String, cause: Throwable? = null) : AhmesException(message, cause)

/**
 * Thrown when XBRL data extraction fails.
 *
 * Common causes:
 * - Malformed XBRL tags
 * - Missing required XBRL namespaces
 * - Invalid fact values
 *
 * @property message Describes the XBRL extraction failure
 * @property cause The underlying exception
 */
class XbrlExtractionException(message: String, cause: Throwable? = null) :
        AhmesException(message, cause)

/**
 * Thrown when attempting to parse an unsupported file format.
 *
 * Ahmes supports HTML, PDF, and TXT formats. Any other format will throw this exception.
 *
 * ## Example
 *
 * ```kotlin
 * try {
 *     parser.parse(File("document.docx"))
 * } catch (e: UnsupportedFormatException) {
 *     println("Unsupported format: ${e.format}")
 * }
 * ```
 *
 * @property format The unsupported file format (e.g., "docx", "xlsx")
 */
class UnsupportedFormatException(val format: String) :
        AhmesException("Unsupported format: $format. Supported formats: HTML, PDF, TXT")

/**
 * Thrown when the document structure doesn't match the expected format.
 *
 * For example:
 * - Missing required sections in a 10-K
 * - Invalid Part/Item numbering
 * - Malformed table structure
 *
 * @property formType The SEC form type (e.g., "10-K", "10-Q")
 * @property reason Description of why the structure is invalid
 */
class InvalidDocumentStructureException(val formType: String, val reason: String) :
        AhmesException("Invalid $formType structure: $reason")

// ============================================================================
// Data Validation Exceptions
// ============================================================================

/**
 * Thrown when extracted financial data is invalid.
 *
 * This can occur when:
 * - Numerical values cannot be parsed
 * - Units are inconsistent or missing
 * - Data constraints are violated
 *
 * @property message Describes the invalid data
 */
class InvalidFinancialDataException(message: String) : AhmesException(message)

/**
 * Thrown when a required field is missing from the document or metadata.
 *
 * @property fieldName The name of the missing field
 */
class MissingRequiredFieldException(val fieldName: String) :
        AhmesException("Missing required field: $fieldName")

/**
 * Thrown when financial calculations produce invalid results.
 *
 * For example:
 * - Division by zero in ratio calculations
 * - Negative values where only positive expected
 * - Out-of-range percentage values
 *
 * @property calculation The name of the failed calculation
 * @property reason Why the calculation failed
 */
class CalculationException(val calculation: String, val reason: String) :
        AhmesException("Calculation failed for $calculation: $reason")

// ============================================================================
// AI/Model Exceptions
// ============================================================================

/**
 * Thrown when an AI model fails to load.
 *
 * Common causes:
 * - Missing model files
 * - Incompatible model version
 * - Insufficient memory
 * - Missing CUDA/GPU dependencies
 *
 * ## Example
 *
 * ```kotlin
 * try {
 *     val predictor = DjlModelManager.getPredictor(ModelType.SENTIMENT)
 * } catch (e: ModelLoadException) {
 *     logger.warn("AI model unavailable: ${e.modelName}")
 *     // Fall back to non-AI analysis
 * }
 * ```
 *
 * @property modelName The name of the model that failed to load
 * @property cause The underlying exception
 */
class ModelLoadException(val modelName: String, cause: Throwable?) :
        AhmesException("Failed to load model: $modelName", cause)

/**
 * Thrown when AI inference/prediction fails.
 *
 * This can happen during:
 * - Sentiment analysis
 * - Named entity recognition
 * - Question answering
 * - Document classification
 *
 * @property message Describes the inference failure
 * @property cause The underlying exception
 */
class InferenceException(message: String, cause: Throwable? = null) :
        AhmesException(message, cause)

// ============================================================================
// Network/API Exceptions
// ============================================================================

/**
 * Thrown when SEC EDGAR API requests fail.
 *
 * Common HTTP status codes:
 * - 403: Access forbidden (missing/invalid User-Agent)
 * - 404: Document not found
 * - 429: Too many requests (rate limited)
 * - 500: SEC server error
 *
 * ## Example
 *
 * ```kotlin
 * try {
 *     val filing = secApi.fetchFiling(cik, accessionNumber)
 * } catch (e: SecApiException) {
 *     when (e.statusCode) {
 *         429 -> logger.warn("Rate limited, retrying after delay")
 *         404 -> logger.error("Filing not found")
 *         else -> logger.error("SEC API error: ${e.message}")
 *     }
 * }
 * ```
 *
 * @property message Error message from SEC API
 * @property statusCode HTTP status code (if available)
 */
class SecApiException(message: String, val statusCode: Int? = null) :
        AhmesException(
                if (statusCode != null) "SEC API error [$statusCode]: $message"
                else "SEC API error: $message"
        )

// ============================================================================
// Configuration Exceptions
// ============================================================================

/**
 * Thrown when library configuration is invalid.
 *
 * Examples:
 * - Invalid cache size
 * - Invalid thread pool configuration
 * - Missing required configuration properties
 *
 * @property configKey The configuration key that is invalid
 * @property reason Why the configuration is invalid
 */
class ConfigurationException(val configKey: String, val reason: String) :
        AhmesException("Invalid configuration for '$configKey': $reason")

/**
 * Thrown when a required dependency is not available.
 *
 * Examples:
 * - DJL not on classpath
 * - PDFBox missing for PDF parsing
 * - CUDA unavailable for GPU acceleration
 *
 * @property dependency The name of the missing dependency
 * @property suggestion Suggested action to resolve the issue
 */
class DependencyException(val dependency: String, val suggestion: String) :
        AhmesException("Missing dependency: $dependency. $suggestion")
