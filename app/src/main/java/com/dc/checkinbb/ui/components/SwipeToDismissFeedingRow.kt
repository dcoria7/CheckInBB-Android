package com.dc.checkinbb.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dc.checkinbb.data.local.FeedingRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissFeedingRow(
    record: FeedingRecord,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    var deleted by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                deleted = true
                onDelete()
                true
            } else false
        }
    )

    if (deleted) return

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isSwiping = dismissState.dismissDirection != null
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = if (isSwiping) Color(0xFFE53935) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwiping) {
                    Text(
                        text = "🗑 Eliminar",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                content()
            }
        }
    )
}
