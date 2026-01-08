package com.pascal.institute.ahmes.ai

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for DJL Model Manager
 *
 * Note: DJL native library tests are skipped as they require
 * proper PyTorch native binaries which may not be available
 * in all environments.
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
        assertTrue(types.any { it.name == "TEXT_CLASSIFICATION" })
    }

    @Test
    fun `ModelType should have descriptions`() {
        assertEquals("Sentiment Analysis", DjlModelManager.ModelType.SENTIMENT.description)
        assertEquals("Named Entity Recognition", DjlModelManager.ModelType.NER.description)
        assertEquals("Question Answering", DjlModelManager.ModelType.QUESTION_ANSWERING.description)
    }

    @Test
    fun `ModelType enum values count`() {
        val types = DjlModelManager.ModelType.values()
        assertTrue(types.size >= 4)
    }

    @Test
    fun `ModelType TEXT_CLASSIFICATION should have description`() {
        assertEquals("Zero-shot Classification", DjlModelManager.ModelType.TEXT_CLASSIFICATION.description)
    }
}
