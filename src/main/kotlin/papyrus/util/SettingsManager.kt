package papyrus.util

import java.io.File
import java.util.Properties

/** Application settings manager for saving and loading to/from local file */
object SettingsManager {
    private val settingsFile = File(System.getProperty("user.home"), ".papyrus/settings.properties")
    private val properties = Properties()

    init {
        loadSettings()
    }

    /** 저장된 설정을 파일에서 로드 */
    private fun loadSettings() {
        try {
            if (settingsFile.exists()) {
                settingsFile.inputStream().use { input -> properties.load(input) }
            }
        } catch (e: Exception) {
            println("Failed to load settings: ${e.message}")
        }
    }

    /** 현재 설정을 파일에 저장 */
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

    /** Get OpenRouter API key - Priority: environment variable -> system property -> saved settings */
    fun getApiKey(): String? {
        // 1. 환경 변수 확인
        System.getenv("OPENROUTER_API_KEY")?.let {
            return it
        }

        // 2. 시스템 프로퍼티 확인
        System.getProperty("openrouter.api.key")?.let {
            return it
        }

        // 3. 저장된 설정 확인
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

    /** API 키가 설정되어 있는지 확인 */
    fun hasApiKey(): Boolean = getApiKey()?.isNotBlank() == true

    /** 저장된 설정의 소스 표시 (디버깅용) */
    fun getApiKeySource(): String {
        return when {
            System.getenv("OPENROUTER_API_KEY") != null -> "Environment Variable"
            System.getProperty("openrouter.api.key") != null -> "System Property"
            properties.getProperty("openrouter.api.key") != null -> "Saved Settings"
            else -> "Not Configured"
        }
    }
}
