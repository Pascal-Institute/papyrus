package papyrus.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import papyrus.core.state.AnalysisState
import papyrus.core.state.AppState
import papyrus.ui.*

/**
 * Right Panel Component
 *
 * Displays analysis results and drag-drop interface.
 * Shows different views based on analysis state:
 * - Idle: Drag-drop panel for file upload
 * - Loading: Loading indicator with message
 * - AnalyzeResult: Document analysis view
 * - FinancialAnalysisResult: Financial analysis panel
 * - Error: Error message with retry option
 *
 * Single Responsibility: Display analysis results and file drop zone
 */
@Composable
fun RightPanel(
        appState: AppState,
        onFileDropped: (java.io.File) -> Unit,
        onDragStateChange: (Boolean) -> Unit,
        onCloseAnalysis: () -> Unit,
        onOpenInBrowser: (String) -> Unit
) {
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(AppColors.Background)
                                .padding(AppDimens.PaddingLarge)
        ) {
                AnimatedContent(
                        targetState = appState.analysisState,
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) { state ->
                        when (state) {
                                is AnalysisState.Idle -> {
                                        DragDropPanel(
                                                isDragging = appState.isDragging,
                                                onDragStateChange = onDragStateChange,
                                                onFileDropped = onFileDropped
                                        )
                                }
                                is AnalysisState.Loading -> {
                                        AnalysisLoadingView(message = state.message)
                                }
                                is AnalysisState.AnalyzeResult -> {
                                        AnalyzeResultView(
                                                documentTitle = state.documentTitle,
                                                documentUrl = state.documentUrl,
                                                analysisContent = state.content,
                                                analysisSummary = state.summary,
                                                onClose = onCloseAnalysis,
                                                onOpenInBrowser =
                                                        state.documentUrl?.let { url ->
                                                                { onOpenInBrowser(url) }
                                                        }
                                        )
                                }
                                is AnalysisState.FinancialAnalysisResult -> {
                                        FinancialAnalysisPanel(
                                                analysis = state.analysis,
                                                onClose = onCloseAnalysis
                                        )
                                }
                                is AnalysisState.Error -> {
                                        AnalysisErrorView(
                                                message = state.message,
                                                onRetry = state.retryAction,
                                                onClose = onCloseAnalysis
                                        )
                                }
                        }
                }
        }
}
