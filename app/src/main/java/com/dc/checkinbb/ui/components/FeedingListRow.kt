package com.dc.checkinbb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dc.checkinbb.data.local.FeedingRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedingListRow(
    record: FeedingRecord,
    currentTimeMs: Long,
    isLatest: Boolean,
    intervalText: String?,
    modifier: Modifier = Modifier,
    use12HourTime: Boolean = false,
    showDateUnderTime: Boolean = true
) {
    val timeFormatter = SimpleDateFormat(
        if (use12HourTime) "h:mm a" else "HH:mm",
        Locale.getDefault()
    )
    val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    val formattedTime = timeFormatter.format(Date(record.timestamp))
    val formattedDate = dateFormatter.format(Date(record.timestamp))

    val elapsedMs = currentTimeMs - record.timestamp
    val hours = (elapsedMs / 3_600_000).toInt()
    val minutes = ((elapsedMs % 3_600_000) / 60_000).toInt()
    val timeAgo = "${hours}h ${minutes}m"

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(12.dp)

    val rowModifier = if (isLatest) {
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(primary.copy(alpha = 0.08f))
            .border(width = 1.5.dp, color = primary.copy(alpha = 0.30f), shape = shape)
            .padding(vertical = 12.dp, horizontal = 12.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 0.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left: time + badge + date ────────────────────────────────────
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Time text
                Text(
                    text = formattedTime,
                    style = if (isLatest) MaterialTheme.typography.bodyLarge
                            else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isLatest) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // "Última" gradient badge (only for latest)
                if (isLatest) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(listOf(primary, secondary))
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Última",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Notes icon (orange, like iOS note.text)
                if (!record.notes.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Nota",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Date label (only for latest row on main screen; hidden when date is in section header)
            if (isLatest && showDateUnderTime) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // ── Right: timeAgo (latest) or interval (others) ─────────────────
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isLatest) timeAgo else (intervalText ?: "—"),
                style = if (isLatest) MaterialTheme.typography.bodyMedium
                        else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isLatest) FontWeight.Medium else FontWeight.Normal,
                color = if (isLatest) primary else MaterialTheme.colorScheme.onSurface
            )
            
            if (!isLatest && intervalText != null) {
                Text(
                    text = "desde anterior",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else if (isLatest) {
                Text(
                    text = "hace",
                    style = MaterialTheme.typography.labelSmall,
                    color = primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
