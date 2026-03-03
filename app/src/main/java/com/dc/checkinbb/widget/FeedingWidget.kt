package com.dc.checkinbb.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.dc.checkinbb.MainActivity
import com.dc.checkinbb.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class FeedingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = withContext(Dispatchers.IO) {
            loadWidgetData(context)
        }
        provideContent {
            WidgetContent(data = widgetData)
        }
    }

    private suspend fun loadWidgetData(context: Context): WidgetData {
        return try {
            val dao = AppDatabase.getInstance(context).feedingDao()
            val baby = dao.getBaby()
            val lastFeeding = baby?.let { dao.getLatestFeedingOnce(it.id) }
            WidgetData(
                babyName = baby?.name ?: "Mi Bebé",
                lastFeedingTimestamp = lastFeeding?.timestamp,
                intervalHours = baby?.feedingIntervalHours ?: 3.0
            )
        } catch (e: Exception) {
            WidgetData()
        }
    }
}

data class WidgetData(
    val babyName: String = "Mi Bebé",
    val lastFeedingTimestamp: Long? = null,
    val intervalHours: Double = 3.0
)

@Composable
private fun WidgetContent(data: WidgetData) {
    val now = System.currentTimeMillis()
    val timeSinceMs = data.lastFeedingTimestamp?.let { now - it } ?: -1L

    val timeSinceText = if (timeSinceMs >= 0) {
        val hours = TimeUnit.MILLISECONDS.toHours(timeSinceMs).toInt()
        val minutes = (TimeUnit.MILLISECONDS.toMinutes(timeSinceMs) % 60).toInt()
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    } else "—"

    val lastFeedingTimeText = data.lastFeedingTimestamp?.let {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))
    } ?: "—"

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF0D1B3E))
            .padding(12.dp)
            .clickable(onClick = actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🍼 ${data.babyName}",
            style = TextStyle(
                color = ColorProvider(Color(0x8CFFFFFF)),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = timeSinceText,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Text(
            text = "desde la última toma",
            style = TextStyle(
                color = ColorProvider(Color(0x8CFFFFFF)),
                fontSize = 10.sp
            )
        )

        Spacer(modifier = GlanceModifier.height(2.dp))

        Text(
            text = lastFeedingTimeText,
            style = TextStyle(
                color = ColorProvider(Color(0x66FFFFFF)),
                fontSize = 10.sp
            )
        )
    }
}
