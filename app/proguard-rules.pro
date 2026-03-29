# CheckInBB — R8 / ProGuard (release)

# Stack traces legibles en Play Console / informes de fallos
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# kotlinx.serialization (DTOs P2P)
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keep class com.dc.checkinbb.sync.**$$serializer { *; }
-keep class com.dc.checkinbb.sync.FeedingRecordDTO { *; }

# Glance (widget): el sistema instancia el Receiver por reflexión
-keep class com.dc.checkinbb.widget.FeedingWidget { *; }
-keep class com.dc.checkinbb.widget.FeedingWidgetReceiver { *; }

# Alarmas / notificaciones: PendingIntent apunta al BroadcastReceiver
-keep class com.dc.checkinbb.workers.FeedingAlarmReceiver { *; }
