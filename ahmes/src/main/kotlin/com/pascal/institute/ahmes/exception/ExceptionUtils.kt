package com.pascal.institute.ahmes.exception

/**
 * Result wrapper for operations that may fail with partial success.
 *
 * Some parsing operations can succeed partially, extracting some data while failing on other parts.
 * This class allows returning both successful data and errors/warnings.
 *
 * ## Usage Example
 *
 * ```kotlin
 * fun parseDocument(content: String): ParseResult<FinancialData> {
 *     val warnings = mutableListOf<String>()
 *     val errors = mutableListOf<String>()
 *
 *     val metrics = try {
 *         extractMetrics(content)
 *     } catch (e: Exception) {
 *         errors.add("Failed to extract metrics: ${e.message}")
 *         emptyList()
 *     }
 *
 *     val risks = try {
 *         extractRiskFactors(content)
 *     } catch (e: Exception) {
 *         warnings.add("Risk factors not found")
 *         emptyList()
 *     }
 *
 *     return ParseResult(
 *         data = FinancialData(metrics, risks),
 *         warnings = warnings,
 *         errors = errors
 *     )
 * }
 * ```
 *
 * @property data The successfully parsed data
 * @property warnings Non-critical issues encountered during parsing
 * @property errors Critical errors encountered (but parsing continued)
 */
data class ParseResult<T>(
        val data: T,
        val warnings: List<String> = emptyList(),
        val errors: List<String> = emptyList()
) {
    /** Returns true if parsing completed without any errors. */
    val isComplete: Boolean
        get() = errors.isEmpty()

    /** Returns true if there are warnings but no errors. */
    val hasWarnings: Boolean
        get() = warnings.isNotEmpty() && errors.isEmpty()

    /** Returns true if there are any errors. */
    val hasErrors: Boolean
        get() = errors.isNotEmpty()

    /** Overall status of the parsing operation. */
    val status: Status
        get() =
                when {
                    errors.isNotEmpty() -> Status.PARTIAL
                    warnings.isNotEmpty() -> Status.SUCCESS_WITH_WARNINGS
                    else -> Status.SUCCESS
                }

    enum class Status {
        /** Parsing completed successfully with no issues */
        SUCCESS,

        /** Parsing completed but with warning messages */
        SUCCESS_WITH_WARNINGS,

        /** Parsing completed partially with some errors */
        PARTIAL
    }

    /** Adds a warning message and returns a new result. */
    fun addWarning(warning: String): ParseResult<T> {
        return copy(warnings = warnings + warning)
    }

    /** Adds an error message and returns a new result. */
    fun addError(error: String): ParseResult<T> {
        return copy(errors = errors + error)
    }

    /** Returns a summary string of the parse result. */
    fun summary(): String {
        return buildString {
            append("Status: $status")
            if (warnings.isNotEmpty()) {
                append(", Warnings: ${warnings.size}")
            }
            if (errors.isNotEmpty()) {
                append(", Errors: ${errors.size}")
            }
        }
    }
}

/** Utility functions for safe exception handling. */
object ExceptionUtils {

    /**
     * Executes an operation and returns a ParseResult.
     *
     * If the operation succeeds, returns SUCCESS. If it fails, captures the exception as an error.
     *
     * @param operation The operation to execute
     * @param onError Optional callback for error handling
     * @return ParseResult containing data or error
     */
    inline fun <T> tryParse(
            operation: () -> T,
            noinline onError: ((Exception) -> String)? = null
    ): ParseResult<T?> {
        return try {
            val data = operation()
            ParseResult(data = data)
        } catch (e: Exception) {
            val errorMessage = onError?.invoke(e) ?: e.message ?: "Unknown error"
            ParseResult(data = null, errors = listOf(errorMessage))
        }
    }

    /**
     * Executes an operation with a default fallback value on error.
     *
     * @param operation The operation to execute
     * @param fallback The fallback value to use on error
     * @param onError Optional callback when error occurs
     * @return The result of operation or fallback value
     */
    inline fun <T> withFallback(
            operation: () -> T,
            fallback: T,
            crossinline onError: (Exception) -> Unit = {}
    ): T {
        return try {
            operation()
        } catch (e: Exception) {
            onError(e)
            fallback
        }
    }

    /**
     * Retries an operation multiple times with exponential backoff.
     *
     * Useful for network operations that may fail transiently.
     *
     * @param maxAttempts Maximum number of retry attempts
     * @param initialDelayMs Initial delay between retries in milliseconds
     * @param maxDelayMs Maximum delay between retries
     * @param factor Backoff factor (default: 2.0 for exponential)
     * @param retryOn Predicate to determine if exception should trigger retry
     * @param operation The operation to retry
     * @return The successful result
     * @throws AhmesException if all retries fail
     */
    inline fun <T> retry(
            maxAttempts: Int = 3,
            initialDelayMs: Long = 1000,
            maxDelayMs: Long = 10000,
            factor: Double = 2.0,
            crossinline retryOn: (Exception) -> Boolean = { true },
            operation: () -> T
    ): T {
        var lastException: Exception? = null
        var delayMs = initialDelayMs

        repeat(maxAttempts) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e

                if (!retryOn(e) || attempt == maxAttempts - 1) {
                    throw e
                }

                // Wait before retry
                Thread.sleep(delayMs)
                delayMs = (delayMs * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }

        throw lastException ?: IllegalStateException("Retry failed without exception")
    }

    /**
     * Wraps checked exceptions into AhmesException.
     *
     * @param message Custom error message
     * @param operation The operation that may throw
     * @return The successful result
     * @throws AhmesException wrapping any thrown exception
     */
    inline fun <T> wrapExceptions(message: String, operation: () -> T): T {
        return try {
            operation()
        } catch (e: AhmesException) {
            throw e // Don't double-wrap
        } catch (e: Exception) {
            throw ParseException(message, e)
        }
    }

    /**
     * Creates a standardized error message with context.
     *
     * @param context Description of what was being attempted
     * @param details Additional details about the error
     * @param cause The exception that caused the error
     * @return Formatted error message
     */
    fun createErrorMessage(
            context: String,
            details: String? = null,
            cause: Throwable? = null
    ): String {
        return buildString {
            append(context)
            if (details != null) {
                append(": ")
                append(details)
            }
            if (cause != null) {
                append(" (")
                append(cause.javaClass.simpleName)
                if (cause.message != null) {
                    append(": ")
                    append(cause.message)
                }
                append(")")
            }
        }
    }

    /**
     * Logs and rethrows an exception with additional context.
     *
     * @param context Description of what was being attempted
     * @param exception The exception to wrap
     * @return Never returns (always throws)
     * @throws ParseException wrapping the original exception
     */
    fun rethrowWithContext(context: String, exception: Exception): Nothing {
        val message = createErrorMessage(context, cause = exception)
        throw when (exception) {
            is AhmesException -> exception
            else -> ParseException(message, exception)
        }
    }
}

/**
 * Extension function to safely get a value or return a default.
 *
 * Catches all exceptions and returns the default value.
 */
inline fun <T> (() -> T).orDefault(default: T): T {
    return try {
        this()
    } catch (e: Exception) {
        default
    }
}

/**
 * Extension function to safely get a nullable value.
 *
 * Catches all exceptions and returns null.
 */
inline fun <T> (() -> T).orNull(): T? {
    return try {
        this()
    } catch (e: Exception) {
        null
    }
}
