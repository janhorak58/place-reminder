package io.github.janhorak58.placereminder.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.janhorak58.placereminder.geofence.TAG

/** Full-screen alarm shown over the lockscreen. Loops sound and vibration until dismissed. */
class AlarmActivity : ComponentActivity() {
    private var player: MediaPlayer? = null
    private var title by mutableStateOf("")
    private var text by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        readIntent(intent)
        setContent {
            PlaceReminderTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.errorContainer) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.LocationOn, contentDescription = null,
                            modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 24.dp),
                        )
                        if (text.isNotBlank()) {
                            Text(
                                text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        Button(onClick = { finishAndRemoveTask() }, modifier = Modifier.fillMaxWidth().padding(top = 48.dp).height(64.dp)) {
                            Text("Dismiss", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
        startAlarm()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
        startAlarm()
    }

    private fun readIntent(i: Intent) {
        title = i.getStringExtra(EXTRA_TITLE).orEmpty()
        text = i.getStringExtra(EXTRA_TEXT).orEmpty()
        // The activity now carries the sound, so remove the insistent notification.
        val id = i.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (id >= 0) getSystemService(NotificationManager::class.java).cancel(id)
    }

    private fun startAlarm() {
        stopAlarm()
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@AlarmActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "alarm sound failed: $e")
        }
        vibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0))
    }

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private fun stopAlarm() {
        player?.runCatching { stop(); release() }
        player = null
        vibrator().cancel()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
