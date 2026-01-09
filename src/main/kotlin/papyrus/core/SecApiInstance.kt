package papyrus.core

import com.pascal.institute.ahmes.network.SecApiClient
import com.pascal.institute.ahmes.network.SecApiConfig

/**
 * Global SEC API client instance for Papyrus application.
 *
 * This provides a convenient singleton instance configured specifically for Papyrus. Use this
 * instead of creating multiple instances.
 */
val secApiClient =
        SecApiClient(
                SecApiConfig(
                        userAgent = "PapyrusApp/1.0",
                        contactEmail = "admin@example.com", // Replace with your actual email
                        rateLimitDelayMs = 100L
                )
        )
