package papyrus.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import papyrus.FilingItem
import papyrus.TickerEntry

/**
 * Enhanced App Header with gradient background
 */
@Composable
fun AppHeader(
    title: String,
    subtitle: String? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        elevation = AppDimens.CardElevation,
        color = AppColors.Primary
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(AppColors.Primary, AppColors.PrimaryVariant)
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

/**
 * Modern Search Box with animation
 */
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
                colors = TextFieldDefaults.textFieldColors(
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

/**
 * Enhanced Ticker Card with hover effect
 */
@Composable
fun TickerCard(
    ticker: TickerEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
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
                modifier = Modifier
                    .size(48.dp)
                    .background(AppColors.PrimaryLight, shape = AppShapes.Medium),
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

/**
 * Company Info Header Card
 */
@Composable
fun CompanyInfoCard(
    ticker: TickerEntry,
    onBackClick: () -> Unit,
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
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onBackClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Primary
                ),
                border = ButtonDefaults.outlinedBorder.copy(
                    brush = Brush.horizontalGradient(listOf(AppColors.Primary, AppColors.Primary))
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

/**
 * Small info chip component
 */
@Composable
fun InfoChip(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), shape = AppShapes.Pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = AppTypography.Caption,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Enhanced Filing Row Card
 */
@Composable
fun FilingCard(
    filing: FilingItem,
    cik: String,
    onOpenBrowser: (String) -> Unit,
    onQuickAnalyze: (String) -> Unit,
    isAnalyzing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
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
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Analyze Button (Primary Action)
                Button(
                    onClick = { onQuickAnalyze(filing.accessionNumber) },
                    enabled = !isAnalyzing,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppColors.Primary,
                        contentColor = Color.White,
                        disabledBackgroundColor = AppColors.Primary.copy(alpha = 0.5f)
                    ),
                    shape = AppShapes.Small,
                    modifier = Modifier.weight(1f).height(AppDimens.ButtonHeight)
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
                            Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(if (isAnalyzing) "Analyzing..." else "Quick Analyze")
                }
                
                // Open in Browser Button (Secondary Action)
                OutlinedButton(
                    onClick = { onOpenBrowser(filing.accessionNumber) },
                    colors = ButtonDefaults.outlinedButtonColors(
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

/**
 * Form Type Badge with color coding
 */
@Composable
fun FormTypeBadge(formType: String) {
    val (backgroundColor, textColor) = when {
        formType.contains("10-K", ignoreCase = true) -> AppColors.SuccessLight to AppColors.Success
        formType.contains("10-Q", ignoreCase = true) -> AppColors.InfoLight to AppColors.Info
        formType.contains("8-K", ignoreCase = true) -> AppColors.WarningLight to AppColors.Warning
        formType.contains("S-1", ignoreCase = true) -> AppColors.SecondaryLight to AppColors.Secondary
        else -> AppColors.SurfaceVariant to AppColors.OnSurfaceSecondary
    }
    
    Box(
        modifier = Modifier
            .background(backgroundColor, shape = AppShapes.Small)
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

/**
 * Empty State component
 */
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

/**
 * Loading Indicator component
 */
@Composable
fun LoadingIndicator(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
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

/**
 * Status indicator component
 */
@Composable
fun StatusIndicator(
    status: AnalysisStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon, text) = when (status) {
        AnalysisStatus.IDLE -> Triple(AppColors.OnSurfaceSecondary, Icons.Default.HourglassEmpty, "Ready")
        AnalysisStatus.LOADING -> Triple(AppColors.Info, Icons.Default.Sync, "Loading...")
        AnalysisStatus.ANALYZING -> Triple(AppColors.Warning, Icons.Default.Analytics, "Analyzing...")
        AnalysisStatus.SUCCESS -> Triple(AppColors.Success, Icons.Default.CheckCircle, "Complete")
        AnalysisStatus.ERROR -> Triple(AppColors.Error, Icons.Default.Error, "Error")
    }
    
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), shape = AppShapes.Pill)
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
    IDLE, LOADING, ANALYZING, SUCCESS, ERROR
}

/**
 * Section Header component
 */
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

/**
 * Tab component for switching views
 */
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
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}
