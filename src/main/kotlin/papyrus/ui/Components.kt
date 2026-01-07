package papyrus.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import papyrus.core.model.BookmarkedTicker
import papyrus.core.model.CompanyNews
import papyrus.core.model.FilingItem
import papyrus.core.model.NewsArticle
import papyrus.core.model.TickerEntry

/** Enhanced App Header with gradient background */
@Composable
fun AppHeader(title: String, subtitle: String? = null, onSettingsClick: (() -> Unit)? = null) {
        Surface(
                modifier = Modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevation,
                color = AppColors.Primary
        ) {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(
                                                Brush.horizontalGradient(
                                                        colors =
                                                                listOf(
                                                                        AppColors.Primary,
                                                                        AppColors.PrimaryVariant
                                                                )
                                                )
                                        )
                                        .padding(AppDimens.PaddingMedium)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column {
                                        Text(
                                                text = title,
                                                color = Color.White,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                        if (subtitle != null) {
                                                Text(
                                                        text = subtitle,
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        fontSize = 12.sp
                                                )
                                        }
                                }

                                if (onSettingsClick != null) {
                                        IconButton(onClick = onSettingsClick) {
                                                Icon(
                                                        Icons.Default.Settings,
                                                        contentDescription = "Settings",
                                                        tint = Color.White
                                                )
                                        }
                                }
                        }
                }
        }
}

/** Modern Search Box with animation */
@Composable
fun SearchBox(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String = "Search...",
        isLoading: Boolean = false,
        modifier: Modifier = Modifier
) {
        Card(
                modifier = modifier.fillMaxWidth(),
                elevation = AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Surface
        ) {
                Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = AppColors.OnSurfaceSecondary,
                                modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        TextField(
                                value = value,
                                onValueChange = onValueChange,
                                placeholder = {
                                        Text(
                                                placeholder,
                                                color = AppColors.OnSurfaceSecondary,
                                                style = AppTypography.Body2
                                        )
                                },
                                colors =
                                        TextFieldDefaults.textFieldColors(
                                                backgroundColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                cursorColor = AppColors.Primary
                                        ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                        )

                        AnimatedVisibility(visible = isLoading) {
                                CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = AppColors.Primary
                                )
                        }

                        AnimatedVisibility(visible = value.isNotEmpty() && !isLoading) {
                                IconButton(
                                        onClick = { onValueChange("") },
                                        modifier = Modifier.size(32.dp)
                                ) {
                                        Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = AppColors.OnSurfaceSecondary,
                                                modifier = Modifier.size(18.dp)
                                        )
                                }
                        }
                }
        }
}

/** Enhanced Ticker Card with hover effect */
@Composable
fun TickerCard(ticker: TickerEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()

        Card(
                modifier =
                        modifier.fillMaxWidth()
                                .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = onClick
                                ),
                elevation = if (isHovered) AppDimens.CardElevationHigh else AppDimens.CardElevation,
                shape = AppShapes.Medium,
                backgroundColor = if (isHovered) AppColors.PrimaryLight else AppColors.Surface
        ) {
                Row(
                        modifier = Modifier.padding(AppDimens.PaddingMedium),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        // Ticker Symbol Badge
                        Box(
                                modifier =
                                        Modifier.size(48.dp)
                                                .background(
                                                        AppColors.PrimaryLight,
                                                        shape = AppShapes.Medium
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Text(
                                        text = ticker.ticker.take(3),
                                        style = AppTypography.Subtitle1,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary
                                )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = ticker.ticker,
                                        style = AppTypography.Subtitle1,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary
                                )
                                Text(
                                        text = ticker.title,
                                        style = AppTypography.Body2,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = AppColors.OnSurfaceSecondary
                                )
                        }

                        Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = AppColors.OnSurfaceSecondary
                        )
                }
        }
}

/** Company Info Header Card with Bookmark */
@Composable
fun CompanyInfoCard(
        ticker: TickerEntry,
        onBackClick: () -> Unit,
        isBookmarked: Boolean = false,
        onBookmarkClick: (() -> Unit)? = null,
        modifier: Modifier = Modifier
) {
        Card(
                modifier = modifier.fillMaxWidth(),
                elevation = 0.dp,
                shape = AppShapes.Large,
                backgroundColor = AppColors.PrimaryLight
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = ticker.title,
                                                style = AppTypography.Headline3,
                                                color = AppColors.OnSurface,
                                                fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                InfoChip(
                                                        icon = Icons.Outlined.Tag,
                                                        label = ticker.ticker,
                                                        color = AppColors.Primary
                                                )
                                                InfoChip(
                                                        icon = Icons.Outlined.Numbers,
                                                        label = "CIK: ${ticker.cik}",
                                                        color = AppColors.Secondary
                                                )
                                        }
                                }

                                // 북마크 버튼
                                if (onBookmarkClick != null) {
                                        IconButton(
                                                onClick = onBookmarkClick,
                                                modifier = Modifier.size(40.dp)
                                        ) {
                                                Icon(
                                                        if (isBookmarked) Icons.Filled.Bookmark
                                                        else Icons.Outlined.BookmarkBorder,
                                                        contentDescription =
                                                                if (isBookmarked)
                                                                        "Remove from bookmarks"
                                                                else "Add to bookmarks",
                                                        tint =
                                                                if (isBookmarked) AppColors.Warning
                                                                else AppColors.OnSurfaceSecondary,
                                                        modifier = Modifier.size(28.dp)
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                                onClick = onBackClick,
                                colors =
                                        ButtonDefaults.outlinedButtonColors(
                                                contentColor = AppColors.Primary
                                        ),
                                border =
                                        ButtonDefaults.outlinedBorder.copy(
                                                brush =
                                                        Brush.horizontalGradient(
                                                                listOf(
                                                                        AppColors.Primary,
                                                                        AppColors.Primary
                                                                )
                                                        )
                                        ),
                                shape = AppShapes.Small
                        ) {
                                Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back to Search")
                        }
                }
        }
}

/** Small info chip component */
@Composable
fun InfoChip(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier) {
        Row(
                modifier =
                        modifier.background(color.copy(alpha = 0.1f), shape = AppShapes.Pill)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                        text = label,
                        style = AppTypography.Caption,
                        color = color,
                        fontWeight = FontWeight.Medium
                )
        }
}

/** Enhanced Filing Row Card */
@Composable
fun FilingCard(
        filing: FilingItem,
        cik: String,
        onOpenBrowser: (String) -> Unit,
        onQuickAnalyze: (FilingItem, FileFormatType) -> Unit,
        isAnalyzing: Boolean = false,
        modifier: Modifier = Modifier
) {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()

        // 파일 형식 선택 상태 (기본값: PDF)
        var selectedFileFormat by remember { mutableStateOf(FileFormatType.PDF) }

        Card(
                modifier = modifier.fillMaxWidth(),
                elevation = if (isHovered) AppDimens.CardElevationHigh else 1.dp,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Surface
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        // Header Row
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Form Type Badge
                                FormTypeBadge(formType = filing.form)

                                // Date
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                Icons.Outlined.CalendarToday,
                                                contentDescription = null,
                                                tint = AppColors.OnSurfaceSecondary,
                                                modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                                text = filing.filingDate,
                                                style = AppTypography.Caption,
                                                color = AppColors.OnSurfaceSecondary
                                        )
                                }
                        }

                        if (filing.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = filing.description,
                                        style = AppTypography.Body2,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = AppColors.OnSurfaceSecondary
                                )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // File Format Selector Row
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = "보고서 형식:",
                                        style = AppTypography.Caption,
                                        color = AppColors.OnSurfaceSecondary,
                                        fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FileFormatSelector(
                                        selectedFormat = selectedFileFormat,
                                        onFormatSelected = { selectedFileFormat = it }
                                )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                // Quick Analyze Button (Primary Action)
                                Button(
                                        onClick = { onQuickAnalyze(filing, selectedFileFormat) },
                                        enabled = !isAnalyzing,
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        backgroundColor = selectedFileFormat.color,
                                                        contentColor = Color.White,
                                                        disabledBackgroundColor =
                                                                selectedFileFormat.color.copy(
                                                                        alpha = 0.5f
                                                                )
                                                ),
                                        shape = AppShapes.Small,
                                        modifier =
                                                Modifier.weight(1f).height(AppDimens.ButtonHeight)
                                ) {
                                        if (isAnalyzing) {
                                                CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp,
                                                        color = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                        } else {
                                                Icon(
                                                        selectedFileFormat.icon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                                if (isAnalyzing) "Analyzing..."
                                                else "Analyze ${selectedFileFormat.displayName}"
                                        )
                                }

                                // Open in Browser Button (Secondary Action)
                                OutlinedButton(
                                        onClick = { onOpenBrowser(filing.accessionNumber) },
                                        colors =
                                                ButtonDefaults.outlinedButtonColors(
                                                        contentColor = AppColors.OnSurfaceSecondary
                                                ),
                                        shape = AppShapes.Small,
                                        modifier = Modifier.height(AppDimens.ButtonHeight)
                                ) {
                                        Icon(
                                                Icons.Default.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                        )
                                }
                        }
                }
        }
}

// File Format Types
enum class FileFormatType(
        val displayName: String,
        val extension: String,
        val icon: ImageVector,
        val color: Color
) {
        PDF("PDF", "pdf", Icons.Outlined.PictureAsPdf, Color(0xFFE53935)),
        HTML("HTML", "html", Icons.Outlined.Code, Color(0xFF1E88E5)),
        HTM("HTM", "htm", Icons.Outlined.Code, Color(0xFF1E88E5)),
        TXT("TXT", "txt", Icons.Outlined.Description, Color(0xFF43A047))
}

@Composable
private fun FileFormatSelector(
        selectedFormat: FileFormatType,
        onFormatSelected: (FileFormatType) -> Unit
) {
        Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                FileFormatType.values().forEach { format ->
                        val isSelected = format == selectedFormat
                        val backgroundColor =
                                if (isSelected) format.color.copy(alpha = 0.15f)
                                else Color.Transparent
                        val borderColor = if (isSelected) format.color else AppColors.Divider

                        Box(
                                modifier =
                                        Modifier.size(36.dp)
                                                .background(
                                                        backgroundColor,
                                                        shape = AppShapes.Small
                                                )
                                                .border(
                                                        width = 1.dp,
                                                        color = borderColor,
                                                        shape = AppShapes.Small
                                                )
                                                .clickable { onFormatSelected(format) }
                                                .padding(6.dp),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        format.icon,
                                        contentDescription = format.displayName,
                                        tint =
                                                if (isSelected) format.color
                                                else AppColors.OnSurfaceSecondary,
                                        modifier = Modifier.size(20.dp)
                                )
                        }
                }
        }
}

/** Form Type Badge with color coding */
@Composable
fun FormTypeBadge(formType: String) {
        val (backgroundColor, textColor) =
                when {
                        formType.contains("10-K", ignoreCase = true) ->
                                AppColors.SuccessLight to AppColors.Success
                        formType.contains("10-Q", ignoreCase = true) ->
                                AppColors.InfoLight to AppColors.Info
                        formType.contains("8-K", ignoreCase = true) ->
                                AppColors.WarningLight to AppColors.Warning
                        formType.contains("S-1", ignoreCase = true) ->
                                AppColors.SecondaryLight to AppColors.Secondary
                        else -> AppColors.SurfaceVariant to AppColors.OnSurfaceSecondary
                }

        Box(
                modifier =
                        Modifier.background(backgroundColor, shape = AppShapes.Small)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
                Text(
                        text = formType,
                        style = AppTypography.Caption,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                )
        }
}

/** Empty State component */
@Composable
fun EmptyState(
        icon: ImageVector,
        title: String,
        description: String,
        modifier: Modifier = Modifier
) {
        Column(
                modifier = modifier.fillMaxWidth().padding(AppDimens.PaddingXLarge),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconHuge),
                        tint = AppColors.Divider
                )

                Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                Text(
                        text = title,
                        style = AppTypography.Subtitle1,
                        color = AppColors.OnSurfaceSecondary
                )

                Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

                Text(
                        text = description,
                        style = AppTypography.Body2,
                        color = AppColors.OnSurfaceSecondary.copy(alpha = 0.7f)
                )
        }
}

/** Loading Indicator component */
@Composable
fun LoadingIndicator(message: String = "Loading...", modifier: Modifier = Modifier) {
        Column(
                modifier = modifier.fillMaxWidth().padding(AppDimens.PaddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                CircularProgressIndicator(
                        color = AppColors.Primary,
                        modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

                Text(
                        text = message,
                        style = AppTypography.Body2,
                        color = AppColors.OnSurfaceSecondary
                )
        }
}

/** Status indicator component */
@Composable
fun StatusIndicator(status: AnalysisStatus, modifier: Modifier = Modifier) {
        val (color, icon, text) =
                when (status) {
                        AnalysisStatus.IDLE ->
                                Triple(
                                        AppColors.OnSurfaceSecondary,
                                        Icons.Default.HourglassEmpty,
                                        "Ready"
                                )
                        AnalysisStatus.LOADING ->
                                Triple(AppColors.Info, Icons.Default.Sync, "Loading...")
                        AnalysisStatus.ANALYZING ->
                                Triple(AppColors.Warning, Icons.Default.Analytics, "Analyzing...")
                        AnalysisStatus.SUCCESS ->
                                Triple(AppColors.Success, Icons.Default.CheckCircle, "Complete")
                        AnalysisStatus.ERROR ->
                                Triple(AppColors.Error, Icons.Default.Error, "Error")
                }

        Row(
                modifier =
                        modifier.background(color.copy(alpha = 0.1f), shape = AppShapes.Pill)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                if (status == AnalysisStatus.LOADING || status == AnalysisStatus.ANALYZING) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = color
                        )
                } else {
                        Icon(
                                icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(16.dp)
                        )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                        text = text,
                        style = AppTypography.Caption,
                        color = color,
                        fontWeight = FontWeight.Medium
                )
        }
}

enum class AnalysisStatus {
        IDLE,
        LOADING,
        ANALYZING,
        SUCCESS,
        ERROR
}

/** Section Header component */
@Composable
fun SectionHeader(
        title: String,
        icon: ImageVector? = null,
        action: (@Composable () -> Unit)? = null,
        modifier: Modifier = Modifier
) {
        Row(
                modifier = modifier.fillMaxWidth().padding(vertical = AppDimens.PaddingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                                Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = AppColors.Primary,
                                        modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                                text = title,
                                style = AppTypography.Subtitle1,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnSurface
                        )
                }

                action?.invoke()
        }
}

/** Tab component for switching views */
@Composable
fun AnalysisTabRow(
        selectedTab: Int,
        onTabSelected: (Int) -> Unit,
        tabs: List<String>,
        modifier: Modifier = Modifier
) {
        TabRow(
                selectedTabIndex = selectedTab,
                modifier = modifier,
                backgroundColor = AppColors.Surface,
                contentColor = AppColors.Primary
        ) {
                tabs.forEachIndexed { index, title ->
                        Tab(
                                selected = selectedTab == index,
                                onClick = { onTabSelected(index) },
                                text = {
                                        Text(
                                                text = title,
                                                fontWeight =
                                                        if (selectedTab == index) FontWeight.Bold
                                                        else FontWeight.Normal
                                        )
                                }
                        )
                }
        }
}

// ==========================================
// 북마크 관련 컴포넌트
// ==========================================

/** 북마크 섹션 헤더 */
@Composable
fun BookmarkSectionHeader(
        onViewAllClick: () -> Unit,
        bookmarkCount: Int,
        modifier: Modifier = Modifier
) {
        Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                                Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = AppColors.Warning,
                                modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = "즐겨찾기",
                                style = AppTypography.Subtitle1,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnSurface
                        )
                        if (bookmarkCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                        modifier =
                                                Modifier.background(
                                                                AppColors.Warning,
                                                                shape = AppShapes.Pill
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                        Text(
                                                text = bookmarkCount.toString(),
                                                style = AppTypography.Caption,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        }
                }

                if (bookmarkCount > 3) {
                        TextButton(onClick = onViewAllClick) {
                                Text(
                                        text = "전체 보기",
                                        style = AppTypography.Caption,
                                        color = AppColors.Primary
                                )
                        }
                }
        }
}

/** 북마크 티커 카드 (간략 버전) */
@Composable
fun BookmarkedTickerCard(
        ticker: String,
        companyName: String,
        onClick: () -> Unit,
        onRemove: () -> Unit,
        modifier: Modifier = Modifier
) {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()

        Card(
                modifier =
                        modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = onClick
                        ),
                elevation = if (isHovered) AppDimens.CardElevationHigh else 1.dp,
                shape = AppShapes.Medium,
                backgroundColor = if (isHovered) AppColors.WarningLight else AppColors.Surface
        ) {
                Row(
                        modifier =
                                Modifier.padding(
                                        horizontal = AppDimens.PaddingSmall,
                                        vertical = AppDimens.PaddingSmall
                                ),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        // 티커 배지
                        Box(
                                modifier =
                                        Modifier.size(36.dp)
                                                .background(
                                                        AppColors.Warning.copy(alpha = 0.15f),
                                                        shape = AppShapes.Small
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Text(
                                        text = ticker.take(4),
                                        style = AppTypography.Caption,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Warning
                                )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = ticker,
                                        style = AppTypography.Caption,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.OnSurface
                                )
                                Text(
                                        text = companyName,
                                        style = AppTypography.Caption,
                                        color = AppColors.OnSurfaceSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                        }

                        // 호버 시 삭제 버튼 표시
                        AnimatedVisibility(visible = isHovered) {
                                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = AppColors.Error,
                                                modifier = Modifier.size(16.dp)
                                        )
                                }
                        }
                }
        }
}

/** Bookmark list (horizontal scroll) */
@Composable
fun BookmarkHorizontalList(
        bookmarks: List<BookmarkedTicker>,
        onTickerClick: (Int) -> Unit,
        onRemove: (Int) -> Unit,
        modifier: Modifier = Modifier
) {
        if (bookmarks.isEmpty()) {
                Card(
                        modifier = modifier.fillMaxWidth(),
                        elevation = 0.dp,
                        backgroundColor = AppColors.SurfaceVariant,
                        shape = AppShapes.Medium
                ) {
                        Column(
                                modifier = Modifier.fillMaxWidth().padding(AppDimens.PaddingMedium),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Icon(
                                        Icons.Outlined.BookmarkBorder,
                                        contentDescription = null,
                                        tint = AppColors.OnSurfaceSecondary,
                                        modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = "즐겨찾기가 없습니다",
                                        style = AppTypography.Body2,
                                        color = AppColors.OnSurfaceSecondary
                                )
                                Text(
                                        text = "관심 있는 티커를 북마크하세요",
                                        style = AppTypography.Caption,
                                        color = AppColors.OnSurfaceSecondary.copy(alpha = 0.7f)
                                )
                        }
                }
        } else {
                Row(
                        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall)
                ) {
                        for (bookmark in bookmarks.take(5)) {
                                BookmarkedTickerCard(
                                        ticker = bookmark.ticker,
                                        companyName = bookmark.companyName,
                                        onClick = { onTickerClick(bookmark.cik) },
                                        onRemove = { onRemove(bookmark.cik) },
                                        modifier = Modifier.width(160.dp)
                                )
                        }
                }
        }
}

/** Recent views section */
@Composable
fun RecentlyViewedSection(
        recentTickers: List<TickerEntry>,
        onTickerClick: (TickerEntry) -> Unit,
        modifier: Modifier = Modifier
) {
        if (recentTickers.isEmpty()) return

        Column(modifier = modifier) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Icon(
                                Icons.Outlined.History,
                                contentDescription = null,
                                tint = AppColors.OnSurfaceSecondary,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                                text = "Recent Views",
                                style = AppTypography.Caption,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnSurfaceSecondary
                        )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        for (ticker in recentTickers.take(4)) {
                                TextButton(
                                        onClick = { onTickerClick(ticker) },
                                        modifier =
                                                Modifier.background(
                                                        AppColors.SurfaceVariant,
                                                        shape = AppShapes.Pill
                                                )
                                ) {
                                        Text(
                                                text = ticker.ticker,
                                                style = AppTypography.Caption,
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.Primary
                                        )
                                }
                        }
                }
        }
}

// ===== News Components =====

/** 뉴스 섹션 헤더 */
@Composable
fun NewsSectionHeader(companyName: String, articleCount: Int, modifier: Modifier = Modifier) {
        Row(
                modifier =
                        modifier.fillMaxWidth()
                                .padding(
                                        horizontal = AppDimens.PaddingMedium,
                                        vertical = AppDimens.PaddingSmall
                                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Icon(
                                Icons.Outlined.Newspaper,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(24.dp)
                        )
                        Text(
                                text = "$companyName 관련 뉴스",
                                style = AppTypography.Headline3,
                                color = AppColors.OnSurface,
                                fontWeight = FontWeight.Bold
                        )
                }

                Text(
                        text = "$articleCount articles",
                        style = AppTypography.Caption,
                        color = AppColors.OnSurfaceSecondary
                )
        }
}

/** 개별 뉴스 카드 */
@Composable
fun NewsArticleCard(
        article: NewsArticle,
        onOpenInBrowser: (String) -> Unit,
        modifier: Modifier = Modifier
) {
        Card(
                modifier = modifier.fillMaxWidth().clickable { onOpenInBrowser(article.url) },
                elevation = 2.dp,
                shape = AppShapes.Medium,
                backgroundColor = AppColors.Surface
        ) {
                Column(modifier = Modifier.padding(AppDimens.PaddingMedium)) {
                        // 제목
                        Text(
                                text = article.title,
                                style = AppTypography.Headline3,
                                color = AppColors.OnSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 설명
                        if (!article.description.isNullOrEmpty()) {
                                Text(
                                        text = article.description,
                                        style = AppTypography.Body2,
                                        color = AppColors.OnSurfaceSecondary,
                                        maxLines = 3,
                                        overflow =
                                                androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 메타 정보
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        // 출처
                                        Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Icon(
                                                        Icons.Outlined.Source,
                                                        contentDescription = null,
                                                        tint = AppColors.Primary,
                                                        modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                        text = article.source,
                                                        style = AppTypography.Caption,
                                                        color = AppColors.Primary,
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                        }

                                        // 날짜
                                        Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Icon(
                                                        Icons.Outlined.Schedule,
                                                        contentDescription = null,
                                                        tint = AppColors.OnSurfaceSecondary,
                                                        modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                        text = article.publishedAt,
                                                        style = AppTypography.Caption,
                                                        color = AppColors.OnSurfaceSecondary
                                                )
                                        }
                                }

                                // 외부 링크 아이콘
                                Icon(
                                        Icons.Outlined.OpenInNew,
                                        contentDescription = "Open in browser",
                                        tint = AppColors.Primary,
                                        modifier = Modifier.size(18.dp)
                                )
                        }
                }
        }
}

/** 뉴스 리스트 (스크롤 가능) */
@Composable
fun NewsArticleList(
        news: CompanyNews,
        onOpenInBrowser: (String) -> Unit,
        modifier: Modifier = Modifier
) {
        Column(modifier = modifier.fillMaxSize()) {
                NewsSectionHeader(companyName = news.companyName, articleCount = news.articles.size)

                if (news.articles.isEmpty()) {
                        EmptyState(
                                icon = Icons.Outlined.Newspaper,
                                title = "뉴스가 없습니다",
                                description = "현재 이 기업에 대한 최신 뉴스가 없습니다"
                        )
                } else {
                        LazyColumn(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .padding(horizontal = AppDimens.PaddingMedium),
                                verticalArrangement = Arrangement.spacedBy(AppDimens.PaddingMedium)
                        ) {
                                items(news.articles) { article ->
                                        NewsArticleCard(
                                                article = article,
                                                onOpenInBrowser = onOpenInBrowser
                                        )
                                }

                                // 마지막 업데이트 시간
                                item {
                                        Text(
                                                text = "Last updated: ${news.lastUpdated}",
                                                style = AppTypography.Caption,
                                                color = AppColors.OnSurfaceSecondary,
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        vertical =
                                                                                AppDimens
                                                                                        .PaddingMedium
                                                                ),
                                                textAlign =
                                                        androidx.compose.ui.text.style.TextAlign
                                                                .Center
                                        )
                                }
                        }
                }
        }
}
/** \ubcf4\uace0\uc11c \ud0c0\uc785 \ud544\ud130 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ReportTypeFilter(
    availableTypes: List<String>,
    selectedTypes: Set<String>,
    onTypesChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Surface)
            .padding(AppDimens.PaddingMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.FilterAlt,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "\ubcf4\uace0\uc11c \ud0c0\uc785 \ud544\ud130",
                    style = AppTypography.Subtitle1,
                    color = AppColors.OnSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // \uc804\uccb4 \uc120\ud0dd/\ud574\uc81c \ubc84\ud2bc
            if (selectedTypes.isNotEmpty()) {
                TextButton(
                    onClick = { onTypesChanged(emptySet()) }
                ) {
                    Text(
                        text = "\ubaa8\ub450 \ud574\uc81c",
                        style = AppTypography.Caption,
                        color = AppColors.Primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // \ubcf4\uace0\uc11c \ud0c0\uc785 \uce69\ub4e4
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableTypes.forEach { type ->
                val isSelected = selectedTypes.contains(type)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newTypes = if (isSelected) {
                            selectedTypes - type
                        } else {
                            selectedTypes + type
                        }
                        onTypesChanged(newTypes)
                    },
                    content = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = type,
                                style = AppTypography.Caption,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    colors = ChipDefaults.filterChipColors(
                        backgroundColor = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else AppColors.Surface,
                        contentColor = if (isSelected) AppColors.Primary else AppColors.OnSurface,
                        selectedBackgroundColor = AppColors.Primary.copy(alpha = 0.15f),
                        selectedContentColor = AppColors.Primary
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) AppColors.Primary else AppColors.Divider
                    )
                )
            }
        }
        
        // \uc120\ud0dd\ub41c \ud544\ud130 \ud45c\uc2dc
        if (selectedTypes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${selectedTypes.size}\uac1c \ud0c0\uc785 \uc120\ud0dd\ub428",
                style = AppTypography.Caption,
                color = AppColors.OnSurfaceSecondary
            )
        }
    }
}