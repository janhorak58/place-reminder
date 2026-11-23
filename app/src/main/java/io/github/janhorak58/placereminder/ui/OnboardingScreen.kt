package io.github.janhorak58.placereminder.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val Indigo = Color(0xFF5856D6)

@Composable
fun OnboardingScreen(status: PermStatus, onRefresh: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val c = ios
    BackHandler(onBack = onDone)
    val fine = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onRefresh() }
    val background = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefresh() }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefresh() }

    Column(Modifier.fillMaxSize().background(c.background)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp)) {
                Text("Set up", style = MaterialTheme.typography.headlineLarge, color = c.label)
                Text(
                    "Reminders only fire when the app can see where you are and tell you about it.",
                    style = MaterialTheme.typography.bodyLarge, color = c.secondary, modifier = Modifier.padding(top = 4.dp),
                )
            }
            IosSection(footer = "On the page that opens for all-the-time location, pick Allow all the time.") {
                PermissionRow(
                    c.blue, GlyphKind.PIN, "Location", "Lets the app know when you reach a place.",
                    status.fineLocation, "Allow",
                ) { fine.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
                IosDivider(58.dp)
                PermissionRow(
                    Indigo, GlyphKind.RADIUS, "Location all the time", "Keeps reminders working while the app is closed.",
                    status.backgroundLocation, "Allow", enabled = status.fineLocation,
                ) { background.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
                if (Build.VERSION.SDK_INT >= 33) {
                    IosDivider(58.dp)
                    PermissionRow(c.red, GlyphKind.NORMAL, "Notifications", "Lets reminders show up on your phone.", status.notifications, "Allow") {
                        notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (Build.VERSION.SDK_INT >= 34) {
                    IosDivider(58.dp)
                    PermissionRow(
                        c.tint, GlyphKind.ALARM, "Full-screen alarms", "Lets Alarm reminders take over the screen.",
                        status.fullScreen, "Open",
                    ) { context.startActivity(SettingsIntents.fullScreen(context)) }
                }
            }
            IosSection(
                header = "Optional",
                footer = "Let Alarm reminders ring through Do Not Disturb. If reminders arrive late, stop your phone from limiting Place Reminder in the background.",
            ) {
                LinkRow("Alarm channel settings") { context.startActivity(SettingsIntents.alarmChannel(context)) }
                IosDivider()
                LinkRow("Do Not Disturb access") { context.startActivity(SettingsIntents.dndAccess()) }
                IosDivider()
                LinkRow("Battery settings") { context.startActivity(SettingsIntents.battery()) }
            }
            Box(Modifier.size(8.dp))
        }
        Box(Modifier.fillMaxWidth().background(c.background).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            IosFilledButton(if (status.allGranted) "Done" else "Continue anyway", onDone)
        }
    }
}

@Composable
private fun PermissionRow(
    tile: Color,
    glyph: GlyphKind,
    title: String,
    reason: String,
    done: Boolean,
    action: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = ios
    IosRow(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
        IconTile(tile) { Glyph(glyph, Color.White, 20.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.label)
            Text(reason, style = MaterialTheme.typography.bodySmall, color = c.secondary)
        }
        if (done) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(c.green).semantics { contentDescription = "Granted" },
                    contentAlignment = Alignment.Center,
                ) { Glyph(GlyphKind.CHECK, Color.White, 14.dp, 2.dp) }
            }
        } else {
            IosCapsuleButton(action, onClick, enabled = enabled)
        }
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    val c = ios
    IosRow(onClick = onClick) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = c.label, modifier = Modifier.weight(1f))
        Glyph(GlyphKind.CHEVRON, c.secondary.copy(alpha = 0.6f), 16.dp, 2.dp)
    }
}
