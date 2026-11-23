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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            PlaceReminderTheme(darkTheme = true) { AlarmScreen(title, text) { finishAndRemoveTask() } }
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
    val c = ios
    val animate = rememberAnimationsEnabled()
    val progress = if (animate) {
        val t = rememberInfiniteTransition(label = "pulse")
        t.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
            label = "ring",
        ).value
    } else {
        0.2f
    }
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1C1C1E), Color.Black)))
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Place reminder", style = MaterialTheme.typography.titleSmall, color = c.secondary, modifier = Modifier.padding(top = 16.dp))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height * 0.55f
                drawCircle(
                    Brush.radialGradient(listOf(c.tint.copy(alpha = 0.22f), Color.Transparent), center = Offset(cx, cy), radius = size.width * 0.8f),
                    radius = size.width * 0.8f, center = Offset(cx, cy),
                )
                val maxW = size.width * 1.1f
                val minW = 60.dp.toPx()
                for (i in 0 until 3) {
                    val p = (progress + i / 3f) % 1f
                    val w = minW + (maxW - minW) * p
                    val h = w * 0.42f
                    drawOval(
                        Color.White.copy(alpha = (1f - p) * 0.28f),
                        Offset(cx - w / 2, cy - h / 2), Size(w, h), style = Stroke(1.5.dp.toPx()),
                    )
                }
                val rw = size.width * 0.3f
                val rh = rw * 0.36f
                drawOval(c.tint.copy(alpha = 0.18f), Offset(cx - rw / 2, cy - rh / 2), Size(rw, rh))
                drawOval(c.tint, Offset(cx - rw / 2, cy - rh / 2), Size(rw, rh), style = Stroke(1.5.dp.toPx()))
                drawPin(Offset(cx, cy), 140.dp.toPx(), c.tint)
            }
        }
        Text(
            title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, color = Color.White,
        )
        if (text.isNotBlank()) {
            Text(
                text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), textAlign = TextAlign.Center,
                color = c.secondary, modifier = Modifier.padding(top = 8.dp),
            )
        }
        IosFilledButton("Dismiss", onDismiss, Modifier.padding(top = 40.dp), radius = 30.dp)
    }
}
