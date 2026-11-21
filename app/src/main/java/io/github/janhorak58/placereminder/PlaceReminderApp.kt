package io.github.janhorak58.placereminder

import android.app.Application
import io.github.janhorak58.placereminder.notify.Notifier
import org.osmdroid.config.Configuration

class PlaceReminderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = filesDir.resolve("osmdroid")
            osmdroidTileCache = cacheDir.resolve("osmdroid/tiles")
        }
    }
}
