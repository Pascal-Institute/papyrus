package papyrus.util.data

import java.io.File
import java.util.Properties

/** Application settings manager for saving and loading to/from local file */
object SettingsManager {
    private val settingsFile = File(System.getProperty("user.home"), ".papyrus/settings.properties")
    private val properties = Properties()

    init {
        loadSettings()
    }

    /** Load saved settings from file */
    private fun loadSettings() {
        try {
            if (settingsFile.exists()) {
                settingsFile.inputStream().use { input -> properties.load(input) }
            }
        } catch (e: Exception) {
            println("Failed to load settings: ${e.message}")
        }
    }

    /** Save current settings to file */
    private fun saveSettings() {
        try {
            settingsFile.parentFile?.mkdirs()
            settingsFile.outputStream().use { output ->
                properties.store(output, "Papyrus Application Settings")
            }
        } catch (e: Exception) {
            println("Failed to save settings: ${e.message}")
        }
    }

    /**
     * Get OpenRouter API key - Priority: environment variable -> system property -> saved settings
     */
    fun getApiKey(): String? {
        // 1. Check environment variable
        System.getenv("OPENROUTER_API_KEY")?.let {
            return it
        }

        // 2. Check system property
        System.getProperty("openrouter.api.key")?.let {
            return it
        }

        // 3. Check saved settings
        return properties.getProperty("openrouter.api.key")
    }

    /** Save OpenRouter API key */
    fun setApiKey(apiKey: String) {
        properties.setProperty("openrouter.api.key", apiKey)
        saveSettings()
    }

    /** Delete OpenRouter API key */
    fun clearApiKey() {
        properties.remove("openrouter.api.key")
        saveSettings()
    }

    /** Check if API key is configured */
    fun hasApiKey(): Boolean = getApiKey()?.isNotBlank() == true

    /** Show source of saved settings (for debugging) */
    fun getApiKeySource(): String {
        return when {
            System.getenv("OPENROUTER_API_KEY") != null -> "Environment Variable"
            System.getProperty("openrouter.api.key") != null -> "System Property"
            properties.getProperty("openrouter.api.key") != null -> "Saved Settings"
            else -> "Not Configured"
        }
    }
}
