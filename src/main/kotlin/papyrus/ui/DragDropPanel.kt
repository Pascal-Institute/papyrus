package papyrus.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * Enhanced Drag and Drop Area for file uploads
 */
@Composable
fun DragDropPanel(
    isDragging: Boolean,
    onDragStateChange: (Boolean) -> Unit,
    onFileDropped: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isDragging) AppColors.Primary else AppColors.Divider
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isDragging) AppColors.PrimaryLight else AppColors.Background,
                shape = AppShapes.Large
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = AppShapes.Large
            )
            .padding(AppDimens.PaddingXLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        if (isDragging) AppColors.Primary.copy(alpha = 0.1f) 
                        else AppColors.SurfaceVariant,
                        shape = AppShapes.ExtraLarge
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isDragging) Icons.Default.CloudUpload else Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (isDragging) AppColors.Primary else AppColors.OnSurfaceSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingLarge))
            
            // Title
            Text(
                text = if (isDragging) "Drop Your File Here" else "Drag & Drop SEC Report",
                style = AppTypography.Headline2,
                color = if (isDragging) AppColors.Primary else AppColors.OnSurface,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
            
            // Description
            Text(
                text = "Upload financial documents for instant analysis",
                style = AppTypography.Body1,
                color = AppColors.OnSurfaceSecondary
            )
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingLarge))
            
            // Supported formats
            SupportedFormatsRow()
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingLarge))
            
            // Browse button
            Button(
                onClick = {
                    val fileChooser = javax.swing.JFileChooser()
                    fileChooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                        "SEC Reports (*.pdf, *.html, *.htm, *.txt)",
                        "pdf", "html", "htm", "txt"
                    )
                    val result = fileChooser.showOpenDialog(null)
                    if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                        onFileDropped(fileChooser.selectedFile)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppColors.Primary,
                    contentColor = Color.White
                ),
                shape = AppShapes.Medium,
                modifier = Modifier
                    .height(AppDimens.ButtonHeightLarge)
                    .widthIn(min = 200.dp)
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Browse Files",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
            
            // Divider with text
            Row(
                modifier = Modifier.widthIn(max = 300.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = AppColors.Divider
                )
                Text(
                    text = "  or search for companies above  ",
                    style = AppTypography.Caption,
                    color = AppColors.OnSurfaceSecondary
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = AppColors.Divider
                )
            }
        }
    }
    
    // Enable drag and drop
    DisposableEffect(Unit) {
        val dropTarget = object : java.awt.dnd.DropTarget() {
            override fun drop(dtde: java.awt.dnd.DropTargetDropEvent) {
                try {
                    dtde.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                    val droppedFiles = dtde.transferable.getTransferData(
                        java.awt.datatransfer.DataFlavor.javaFileListFlavor
                    ) as List<*>
                    
                    droppedFiles.firstOrNull()?.let { file ->
                        if (file is File) {
                            onFileDropped(file)
                        }
                    }
                    dtde.dropComplete(true)
                    onDragStateChange(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                    dtde.dropComplete(false)
                }
            }
            
            override fun dragEnter(dtde: java.awt.dnd.DropTargetDragEvent) {
                onDragStateChange(true)
            }
            
            override fun dragExit(dte: java.awt.dnd.DropTargetEvent) {
                onDragStateChange(false)
            }
        }
        
        onDispose {
            // Cleanup if needed
        }
    }
}

@Composable
private fun SupportedFormatsRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FormatBadge("PDF", Icons.Outlined.PictureAsPdf, AppColors.Error)
        FormatBadge("HTML", Icons.Outlined.Code, AppColors.Info)
        FormatBadge("TXT", Icons.Outlined.Description, AppColors.Secondary)
    }
}

@Composable
private fun FormatBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), shape = AppShapes.Small)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
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
 * Welcome Panel - shown when app first loads
 */
@Composable
fun WelcomePanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppDimens.PaddingXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Insights,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = AppColors.Primary
        )
        
        Spacer(modifier = Modifier.height(AppDimens.PaddingLarge))
        
        Text(
            text = "Welcome to Papyrus",
            style = AppTypography.Headline1,
            color = AppColors.OnSurface,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
        
        Text(
            text = "Your SEC Financial Document Analyzer",
            style = AppTypography.Subtitle1,
            color = AppColors.OnSurfaceSecondary
        )
        
        Spacer(modifier = Modifier.height(AppDimens.PaddingXLarge))
        
        // Feature cards
        Row(
            modifier = Modifier.widthIn(max = 800.dp),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingMedium)
        ) {
            FeatureCard(
                icon = Icons.Outlined.Search,
                title = "Search Companies",
                description = "Search by ticker or company name to access SEC filings",
                modifier = Modifier.weight(1f)
            )
            
            FeatureCard(
                icon = Icons.Outlined.Analytics,
                title = "Quick Analyze",
                description = "Instantly analyze 10-K, 10-Q, and other SEC filings",
                modifier = Modifier.weight(1f)
            )
            
            FeatureCard(
                icon = Icons.Outlined.UploadFile,
                title = "Upload Files",
                description = "Drag & drop local PDF, HTML, or TXT documents",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = AppDimens.CardElevation,
        shape = AppShapes.Medium,
        backgroundColor = AppColors.Surface
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.PaddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(AppColors.PrimaryLight, shape = AppShapes.Medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))
            
            Text(
                text = title,
                style = AppTypography.Subtitle1,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))
            
            Text(
                text = description,
                style = AppTypography.Body2,
                color = AppColors.OnSurfaceSecondary,
                modifier = Modifier.padding(horizontal = AppDimens.PaddingSmall)
            )
        }
    }
}
