package io.github.janhorak58.placereminder.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import io.github.janhorak58.placereminder.notify.Notifier

data class PermStatus(
    val fineLocation: Boolean,
    val backgroundLocation: Boolean,
    val notifications: Boolean,
    val fullScreen: Boolean,
) {
    val allGranted get() = fineLocation && backgroundLocation && notifications && fullScreen

    fun missing(): List<String> = buildList {
        if (!fineLocation) add("location")
        if (!backgroundLocation) add("background location")
        if (!notifications) add("notifications")
        if (!fullScreen) add("full-screen alarms")
    }

    companion object {
        fun read(context: Context): PermStatus {
            fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
            val fullScreen = if (Build.VERSION.SDK_INT >= 34) {
                context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
            } else {
                true
            }
            return PermStatus(
                fineLocation = granted(Manifest.permission.ACCESS_FINE_LOCATION),
                backgroundLocation = granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                notifications = if (Build.VERSION.SDK_INT >= 33) granted(Manifest.permission.POST_NOTIFICATIONS) else true,
                fullScreen = fullScreen,
            )
        }
    }
}

object SettingsIntents {
    private fun pkg(context: Context) = Uri.fromParts("package", context.packageName, null)

    fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg(context))

    fun fullScreen(context: Context) =
        if (Build.VERSION.SDK_INT >= 34) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg(context))
        else appDetails(context)

    fun battery() = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun alarmChannel(context: Context) =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, Notifier.CHANNEL_ALARM)

    fun dndAccess() = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun locationSettings() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
}
