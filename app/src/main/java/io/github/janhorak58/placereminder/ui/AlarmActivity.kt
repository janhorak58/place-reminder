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
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.janhorak58.placereminder.geofence.TAG
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Full-screen alarm shown over the lockscreen. Loops sound and vibration until dismissed. */
class AlarmActivity : ComponentActivity() {
    private var player: MediaPlayer? = null
    private var title by mutableStateOf("")
    private var text by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        readIntent(intent)
        setContent {
            PlaceReminderTheme { AlarmScreen(title, text) { finishAndRemoveTask() } }
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

@Composable
private fun AlarmScreen(title: String, text: String, onDismiss: () -> Unit) {
    val animate = rememberAnimationsEnabled()
    val progress = if (animate) {
        val t = rememberInfiniteTransition(label = "pulse")
        t.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
            label = "ring",
        ).value
    } else {
        0.15f
    }
    Column(
        Modifier.fillMaxSize().background(Brand.Ink).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.56f
                val maxW = size.width * 1.15f
                val minW = 56.dp.toPx()
                for (i in 0 until 3) {
                    val p = (progress + i / 3f) % 1f
                    val w = minW + (maxW - minW) * p
                    val h = w * 0.44f
                    drawOval(
                        Brand.Ring.copy(alpha = (1f - p) * 0.9f),
                        Offset(cx - w / 2, cy - h / 2), Size(w, h), style = Stroke(2.5.dp.toPx()),
                    )
                }
                val rw = size.width * 0.34f
                val rh = rw * 0.36f
                drawOval(Brand.Amber.copy(alpha = 0.16f), Offset(cx - rw / 2, cy - rh / 2), Size(rw, rh))
                drawOval(Brand.Amber, Offset(cx - rw / 2, cy - rh / 2), Size(rw, rh), style = Stroke(3.dp.toPx()))
                val ph = 150.dp.toPx()
                drawPin(Offset(cx, cy), ph, amberBrush(cy - ph, cy))
            }
        }
        Text("Place reminder", style = MaterialTheme.typography.labelLarge, color = Brand.Ring)
        Text(
            title, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center,
            color = Brand.Paper, modifier = Modifier.padding(top = 6.dp),
        )
        if (text.isNotBlank()) {
            Text(
                text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                color = Brand.Paper.copy(alpha = 0.85f), modifier = Modifier.padding(top = 10.dp),
            )
        }
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(top = 36.dp).height(68.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand.Amber, contentColor = Brand.Ink),
        ) { Text("Dismiss", style = MaterialTheme.typography.titleLarge) }
    }
}
