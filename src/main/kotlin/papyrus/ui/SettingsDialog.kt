package papyrus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import papyrus.core.service.AiAnalysisService
import papyrus.util.SettingsManager

/** API 키 설정 다이얼로그 */
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    var apiKey by remember { mutableStateOf(SettingsManager.getApiKey() ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf<TestStatus>(TestStatus.None) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
                modifier = Modifier.width(550.dp).heightIn(max = 600.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colors.surface,
                elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                // 헤더
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = "설정",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "닫기") }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // API 키 섹션
                Text(
                        text = "OpenRouter API 키",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "AI 재무 분석 기능을 사용하려면 OpenRouter API 키가 필요합니다.",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // API 키 입력 필드
                OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            testStatus = TestStatus.None // 입력 시 테스트 상태 초기화
                        },
                        label = { Text("API 키") },
                        placeholder = { Text("sk-or-v1-...") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation =
                                if (showApiKey) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                        if (showApiKey) Icons.Default.Visibility
                                        else Icons.Default.VisibilityOff,
                                        if (showApiKey) "API 키 숨기기" else "API 키 보기"
                                )
                            }
                        },
                        singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 현재 설정 소스
                val apiKeySource = SettingsManager.getApiKeySource()
                if (apiKeySource != "Not Configured") {
                    Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                                text = "현재 소스: $apiKeySource",
                                style = MaterialTheme.typography.caption,
                                color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 테스트 상태 표시
                when (testStatus) {
                    is TestStatus.Success -> {
                        Card(
                                backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = "API 키가 정상적으로 작동합니다!",
                                        style = MaterialTheme.typography.body2,
                                        color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                    is TestStatus.Error -> {
                        Card(
                                backgroundColor = Color(0xFFF44336).copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            tint = Color(0xFFF44336),
                                            modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                            text = "API 키 테스트 실패",
                                            style = MaterialTheme.typography.body2,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFC62828)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = (testStatus as TestStatus.Error).message,
                                        style = MaterialTheme.typography.caption,
                                        color = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                    TestStatus.None -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 액션 버튼들
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 테스트 버튼
                    OutlinedButton(
                            onClick = {
                                if (apiKey.isNotBlank()) {
                                    scope.launch {
                                        isTesting = true
                                        testStatus = TestStatus.None
                                        val result =
                                                withContext(Dispatchers.IO) {
                                                    AiAnalysisService.testApiKey(apiKey)
                                                }
                                        testStatus =
                                                if (result.first) {
                                                    TestStatus.Success
                                                } else {
                                                    TestStatus.Error(result.second ?: "알 수 없는 오류")
                                                }
                                        isTesting = false
                                    }
                                }
                            },
                            enabled = apiKey.isNotBlank() && !isTesting,
                            modifier = Modifier.weight(1f)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isTesting) "테스트 중..." else "테스트")
                    }

                    // 저장 버튼
                    Button(
                            onClick = {
                                if (apiKey.isNotBlank()) {
                                    SettingsManager.setApiKey(apiKey)
                                    onDismiss()
                                }
                            },
                            enabled = apiKey.isNotBlank(),
                            modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("저장")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 삭제 버튼
                if (SettingsManager.hasApiKey()) {
                    TextButton(
                            onClick = {
                                SettingsManager.clearApiKey()
                                apiKey = ""
                                testStatus = TestStatus.None
                            },
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("저장된 API 키 삭제", color = Color(0xFFF44336))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // 도움말 섹션
                Text(
                        text = "API 키 받는 방법",
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HelpStep("1", "https://openrouter.ai 방문")
                    HelpStep("2", "무료 계정 가입 (Google/GitHub 로그인 가능)")
                    HelpStep("3", "Keys 메뉴에서 새 API 키 생성")
                    HelpStep("4", "생성된 키를 위 입력란에 붙여넣기")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                                Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary,
                                modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = "가입 시 무료 크레딧 제공! 무료로 AI 분석을 시작해보세요.",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpStep(number: String, text: String) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colors.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                        text = number,
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.body2, color = Color.Gray)
    }
}

private sealed class TestStatus {
    object None : TestStatus()
    object Success : TestStatus()
    data class Error(val message: String) : TestStatus()
}
