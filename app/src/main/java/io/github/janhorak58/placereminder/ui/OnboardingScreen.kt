@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.janhorak58.placereminder.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(status: PermStatus, onRefresh: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = onDone)
    val fine = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onRefresh() }
    val background = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefresh() }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefresh() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shadowElevation = 8.dp) {
                Button(
                    onClick = onDone,
                    modifier = Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand.Amber, contentColor = Brand.Ink),
                ) { Text(if (status.allGranted) "Done" else "Continue anyway", style = MaterialTheme.typography.labelLarge) }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            InkHeader {
                Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Brand.Paper)
                    }
                    Text("Set up", style = MaterialTheme.typography.headlineSmall, color = Brand.Paper, modifier = Modifier.padding(start = 4.dp))
                }
                Text(
                    "Reminders only fire when the app can see where you are and tell you about it.",
                    style = MaterialTheme.typography.bodyLarge, color = Brand.Paper.copy(alpha = 0.85f),
                    modifier = Modifier.padding(start = 20.dp, end = 72.dp, top = 6.dp, bottom = 22.dp),
                )
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                var n = 0
                Step(
                    ++n, "Location", "Lets the app know when you reach a place.",
                    status.fineLocation, "Allow location",
                ) { fine.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
                Step(
                    ++n, "Location all the time", "Keeps reminders working while the app is closed. Pick Allow all the time on the page that opens.",
                    status.backgroundLocation, if (status.fineLocation) "Allow all the time" else "Allow location first",
                    enabled = status.fineLocation,
                ) { background.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
                if (Build.VERSION.SDK_INT >= 33) {
                    Step(++n, "Notifications", "Lets reminders show up on your phone.", status.notifications, "Allow notifications") {
                        notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (Build.VERSION.SDK_INT >= 34) {
                    Step(
                        ++n, "Full-screen alarms", "Lets Alarm reminders take over the screen, even when locked.",
                        status.fullScreen, "Open settings",
                    ) { context.startActivity(SettingsIntents.fullScreen(context)) }
                }
                Text(
                    "Optional", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
                Info(
                    "Alarm sound and Do Not Disturb",
                    "Let Alarm reminders ring through Do Not Disturb by allowing the Alarm channel.",
                ) {
                    OutlinedButton(onClick = { context.startActivity(SettingsIntents.alarmChannel(context)) }) { Text("Alarm channel settings") }
                    OutlinedButton(onClick = { context.startActivity(SettingsIntents.dndAccess()) }) { Text("Do Not Disturb access") }
                }
                Info(
                    "Battery optimization",
                    "If reminders arrive late, stop your phone from limiting Place Reminder in the background.",
                ) {
                    OutlinedButton(onClick = { context.startActivity(SettingsIntents.battery()) }) { Text("Battery settings") }
                }
            }
        }
    }
}

@Composable
private fun Step(number: Int, title: String, body: String, done: Boolean, action: String, enabled: Boolean = true, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(cs.surfaceContainerLow)
            .border(1.dp, if (done) cs.outlineVariant else cs.outline, MaterialTheme.shapes.large).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val tick = Brand.Ink
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(if (done) Brand.Amber else cs.surface)
                .border(2.dp, if (done) Brand.Amber else cs.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Canvas(Modifier.size(18.dp)) {
                    val w = 2.6f * density
                    drawLine(tick, Offset(size.width * 0.1f, size.height * 0.55f), Offset(size.width * 0.4f, size.height * 0.85f), w, StrokeCap.Round)
                    drawLine(tick, Offset(size.width * 0.4f, size.height * 0.85f), Offset(size.width * 0.92f, size.height * 0.15f), w, StrokeCap.Round)
                }
            } else {
                Text("$number", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    if (done) "Granted" else "Missing",
                    color = if (done) cs.secondary else cs.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            if (!done) {
                Button(
                    onClick = onClick, enabled = enabled, modifier = Modifier.padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.primary, contentColor = cs.onPrimary,
                        disabledContainerColor = cs.surfaceContainerHighest, disabledContentColor = cs.onSurfaceVariant,
                    ),
                ) { Text(action) }
            }
        }
    }
}

@Composable
private fun Info(title: String, body: String, buttons: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(cs.surfaceContainerLow)
            .border(1.dp, cs.outlineVariant, MaterialTheme.shapes.large).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        buttons()
    }
}
