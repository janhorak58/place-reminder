package io.github.janhorak58.placereminder.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.janhorak58.placereminder.MainActivity
import io.github.janhorak58.placereminder.R
import io.github.janhorak58.placereminder.data.Level
import io.github.janhorak58.placereminder.data.Reminder
import io.github.janhorak58.placereminder.geofence.TAG
import io.github.janhorak58.placereminder.ui.AlarmActivity
import java.util.Locale

object Notifier {
    const val CHANNEL_SILENT = "silent"
    const val CHANNEL_NORMAL = "normal"
    const val CHANNEL_ALARM = "alarm"

    private val VIBRATION = longArrayOf(0, 400, 200, 400)

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val silent = NotificationChannel(CHANNEL_SILENT, "Silent reminders", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Reminders without sound or pop-up"
            setSound(null, null)
            enableVibration(false)
        }
        val normal = NotificationChannel(CHANNEL_NORMAL, "Normal reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Reminders with sound and vibration"
            enableVibration(true)
            vibrationPattern = VIBRATION
        }
        val alarm = NotificationChannel(CHANNEL_ALARM, "Alarm reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Loud full-screen alarms"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = VIBRATION
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true) // Only honored if the user grants Do Not Disturb access.
        }
        nm.createNotificationChannels(listOf(silent, normal, alarm))
    }

    fun headsUpTitle(r: Reminder): String = "Approaching ${r.name} (about ${formatKm(r.headsUpMeters)})"

    fun formatKm(meters: Int): String {
        val km = meters / 1000.0
        val text = if (km % 1.0 == 0.0) km.toInt().toString() else String.format(Locale.US, "%.1f", km)
        return "$text km"
    }

    fun notificationId(reminderId: Long, headsUp: Boolean): Int = (reminderId * 2 + if (headsUp) 1 else 0).toInt()

    fun notifyReminder(context: Context, r: Reminder, headsUp: Boolean) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "notification permission missing, cannot show reminder ${r.id}")
            return
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        val id = notificationId(r.id, headsUp)
        val title = if (headsUp) headsUpTitle(r) else r.name
        val text = r.message
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_REMINDER_ID, r.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val channel = when (r.level) {
            Level.SILENT -> CHANNEL_SILENT
            Level.NORMAL -> CHANNEL_NORMAL
            Level.ALARM -> CHANNEL_ALARM
        }
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
        when (r.level) {
            Level.SILENT -> builder.setPriority(NotificationCompat.PRIORITY_LOW)
            Level.NORMAL -> builder.setPriority(NotificationCompat.PRIORITY_HIGH)
            Level.ALARM -> {
                val alarmIntent = PendingIntent.getActivity(
                    context, id,
                    Intent(context, AlarmActivity::class.java)
                        .putExtra(AlarmActivity.EXTRA_TITLE, title)
                        .putExtra(AlarmActivity.EXTRA_TEXT, text)
                        .putExtra(AlarmActivity.EXTRA_NOTIFICATION_ID, id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder.setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(alarmIntent)
                    .setFullScreenIntent(alarmIntent, true)
            }
        }
        nm.notify(id, builder.build())
    }
}
