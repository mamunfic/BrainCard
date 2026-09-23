package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrainCardAmberLight
import com.example.ui.theme.BrainCardSuccessLight

@Composable
fun SummaryCards(
    dueCount: Int,
    tomorrowCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
    onExportCsvClick: (() -> Unit)? = null
) {
    val reviewedCount = (totalCount - dueCount).coerceAtLeast(0)
    val progressFraction = if (totalCount > 0) reviewedCount.toFloat() / totalCount.toFloat() else 1f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "progress_anim")
    val percentInt = (progressFraction * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("summary_section")
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "STUDY OVERVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (totalCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (dueCount == 0) BrainCardSuccessLight.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primaryContainer
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (dueCount == 0) "100% Retained" else "$percentInt% Up to Date",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dueCount == 0) BrainCardSuccessLight else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (onExportCsvClick != null) {
                FilledTonalButton(
                    onClick = onExportCsvClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("summary_export_csv_btn"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export CSV",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Export CSV",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Animated Retention Health Bar (only if there are cards)
        if (totalCount > 0) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (dueCount > 0) "$dueCount cards require review today" else "All reviews completed for today!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (dueCount > 0) MaterialTheme.colorScheme.onSurface else BrainCardSuccessLight
                    )

                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (dueCount == 0) BrainCardSuccessLight
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                    }
                }
            }
        }

        // 3-Pack Summary Grid with Icons & Tactile Accents
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryCardItem(
                label = "Due now",
                value = dueCount.toString(),
                icon = if (dueCount > 0) Icons.Default.NotificationsActive else Icons.Default.CheckCircle,
                isHighlight = dueCount > 0,
                highlightColor = if (dueCount > 0) MaterialTheme.colorScheme.primary else BrainCardSuccessLight,
                modifier = Modifier
                    .weight(1f)
                    .testTag("summary_due_now")
            )
            SummaryCardItem(
                label = "Tomorrow",
                value = tomorrowCount.toString(),
                icon = Icons.Default.QueryBuilder,
                isHighlight = false,
                highlightColor = BrainCardAmberLight,
                modifier = Modifier
                    .weight(1f)
                    .testTag("summary_tomorrow")
            )
            SummaryCardItem(
                label = "Total cards",
                value = totalCount.toString(),
                icon = Icons.Default.Style,
                isHighlight = false,
                highlightColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("summary_total_cards")
            )
        }
    }
}

@Composable
private fun SummaryCardItem(
    label: String,
    value: String,
    icon: ImageVector,
    isHighlight: Boolean,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isHighlight) highlightColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isHighlight) 1.5.dp else 1.dp,
            color = if (isHighlight) highlightColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.5.sp,
                        letterSpacing = 0.6.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (isHighlight) highlightColor.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isHighlight) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                ),
                color = if (isHighlight) highlightColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
