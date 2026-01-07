package papyrus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import papyrus.core.model.FilingItem
import papyrus.core.model.TickerEntry
import papyrus.core.resource.AppStrings
import papyrus.ui.*

/**
 * Company Filings Panel Component
 *
 * Displays company information and list of SEC filings with filtering.
 * Includes:
 * - Company info card with bookmark functionality
 * - Report type filter (10-Q, 10-K, etc.)
 * - Scrollable list of filings with analyze/open actions
 *
 * Single Responsibility: Display and manage company filing list
 */
@Composable
fun CompanyFilingsPanel(
        ticker: TickerEntry,
        filings: List<FilingItem>,
        currentAnalyzingFiling: String?,
        isBookmarked: Boolean,
        onBackClick: () -> Unit,
        onBookmarkClick: () -> Unit,
        onAnalyze: (FilingItem, FileFormatType) -> Unit,
        onOpenInBrowser: (FilingItem) -> Unit
) {
        Column(modifier = Modifier.fillMaxSize()) {
                // Company Info Card
                Box(modifier = Modifier.fillMaxWidth().padding(AppDimens.PaddingMedium)) {
                        CompanyInfoCard(
                                ticker = ticker,
                                isBookmarked = isBookmarked,
                                onBackClick = onBackClick,
                                onBookmarkClick = onBookmarkClick
                        )
                }

                Divider(color = AppColors.Divider)

                // Report type filter state (default: 10-Q for quarterly insights)
                var selectedReportTypes by remember { mutableStateOf(setOf("10-Q")) }

                // Filtered filings list
                val filteredFilings =
                        remember(filings, selectedReportTypes) {
                                if (selectedReportTypes.isEmpty()) {
                                        filings
                                } else {
                                        filings.filter { filing ->
                                                selectedReportTypes.any { type ->
                                                        filing.form.contains(
                                                                type,
                                                                ignoreCase = true
                                                        )
                                                }
                                        }
                                }
                        }

                Divider(color = AppColors.Divider)

                // SEC Filings content
                if (filings.isEmpty()) {
                        EmptyState(
                                icon = Icons.Outlined.FolderOff,
                                title = AppStrings.NO_FILINGS_TITLE,
                                description = AppStrings.NO_FILINGS_DESCRIPTION
                        )
                } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                                // Report type filter
                                ReportTypeFilter(
                                        availableTypes =
                                                filings.map { it.form }.distinct().sorted(),
                                        selectedTypes = selectedReportTypes,
                                        onTypesChanged = { selectedReportTypes = it }
                                )

                                Divider(color = AppColors.Divider)

                                // If no filtered results
                                if (filteredFilings.isEmpty()) {
                                        EmptyState(
                                                icon = Icons.Outlined.FilterAlt,
                                                title = AppStrings.FILTER_NO_RESULTS_TITLE,
                                                description = AppStrings.FILTER_NO_RESULTS_DESCRIPTION
                                        )
                                } else {
                                        LazyColumn(
                                                modifier =
                                                        Modifier.fillMaxSize()
                                                                .padding(
                                                                        horizontal =
                                                                                AppDimens
                                                                                        .PaddingSmall
                                                                ),
                                                verticalArrangement =
                                                        Arrangement.spacedBy(AppDimens.PaddingSmall)
                                        ) {
                                                items(filteredFilings) { filing ->
                                                        FilingCard(
                                                                filing = filing,
                                                                onOpenBrowser = {
                                                                        onOpenInBrowser(filing)
                                                                },
                                                                onAnalyze = {
                                                                        filingItem,
                                                                        format ->
                                                                        onAnalyze(
                                                                                filingItem,
                                                                                format
                                                                        )
                                                                },
                                                                isAnalyzing =
                                                                        currentAnalyzingFiling ==
                                                                                filing.accessionNumber
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}
