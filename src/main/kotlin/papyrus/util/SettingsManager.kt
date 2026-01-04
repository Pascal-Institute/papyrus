package papyrus.util

import java.io.File
import java.util.Properties

/** 애플리케이션 설정을 로컬 파일에 저장하고 로드하는 관리자 */
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

    /** OpenRouter API 키 가져오기 우선순위: 환경 변수 -> 시스템 프로퍼티 -> 저장된 설정 */
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

    /** OpenRouter API 키 저장 */
    fun setApiKey(apiKey: String) {
        properties.setProperty("openrouter.api.key", apiKey)
        saveSettings()
    }

    /** OpenRouter API 키 삭제 */
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
