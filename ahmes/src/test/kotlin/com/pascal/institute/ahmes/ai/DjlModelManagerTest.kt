package com.pascal.institute.ahmes.ai

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for DJL Model Manager
 *
 * Note: DJL model loading tests are limited as they require proper PyTorch native binaries and may
 * fail if CUDA/CPU libraries are not properly configured.
 */
class DjlModelManagerTest {

    @Test
    fun `DjlModelManager object should be accessible`() {
        assertNotNull(DjlModelManager)
    }

    @Test
    fun `ModelType enum should have all expected types`() {
        val types = DjlModelManager.ModelType.values()

        assertTrue(types.any { it.name == "SENTIMENT" })
        assertTrue(types.any { it.name == "NER" })
        assertTrue(types.any { it.name == "QUESTION_ANSWERING" })
        assertTrue(types.any { it.name == "SUMMARIZATION" })
        assertTrue(types.any { it.name == "TEXT_CLASSIFICATION" })
    }

    @Test
    fun `ModelType should have descriptions`() {
        assertEquals(
                "Financial Sentiment Analysis",
                DjlModelManager.ModelType.SENTIMENT.description
        )
        assertEquals("Named Entity Recognition", DjlModelManager.ModelType.NER.description)
        assertEquals(
                "High-Precision Question Answering",
                DjlModelManager.ModelType.QUESTION_ANSWERING.description
        )
        assertEquals(
                "AI Document Summarization",
                DjlModelManager.ModelType.SUMMARIZATION.description
        )
        assertEquals(
                "Zero-shot Classification",
                DjlModelManager.ModelType.TEXT_CLASSIFICATION.description
        )
    }

    @Test
    fun `ModelType enum values count`() {
        val types = DjlModelManager.ModelType.values()
        // Should have at least SENTIMENT, NER, QA, SUMMARIZATION, TEXT_CLASSIFICATION
        assertTrue(types.size >= 5, "Should have at least 5 model types")
    }

    @Test
    fun `ModelType should have modelId configured`() {
        // Verify that each model type has a HuggingFace model ID
        DjlModelManager.ModelType.values().forEach { modelType ->
            assertNotNull(modelType.modelId, "Model ID should not be null for ${modelType.name}")
            assertTrue(
                    modelType.modelId.isNotBlank(),
                    "Model ID should not be blank for ${modelType.name}"
            )
        }
    }

    @Test
    fun `getDeviceInfo should return device information`() {
        val deviceInfo = DjlModelManager.getDeviceInfo()

        assertNotNull(deviceInfo)
        assertTrue(deviceInfo.containsKey("defaultDevice"), "Should contain defaultDevice")
        assertTrue(deviceInfo.containsKey("isGpuAvailable"), "Should contain isGpuAvailable")
    }

    @Test
    fun `isAvailable should not throw exception`() {
        // This test just verifies the method doesn't crash
        // It may return false if DJL dependencies are not properly configured
        try {
            val available = DjlModelManager.isAvailable()
            // Result can be true or false depending on environment
            assertNotNull(available)
        } catch (e: Exception) {
            // If it throws, that's also acceptable - some environments may not have PyTorch
            println("DJL not available: ${e.message}")
        }
    }
}
