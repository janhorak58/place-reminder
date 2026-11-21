@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.janhorak58.placereminder.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        topBar = {
            TopAppBar(
                title = { Text("Setup") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Place Reminder needs a few permissions so alerts arrive when the app is closed.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Step(
                "1. Location", "Needed to know where you are relative to your places.",
                status.fineLocation, "Allow location",
            ) { fine.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
            Step(
                "2. Allow all the time", "Geofences work in the background. On Android 11 and later choose \"Allow all the time\" in the settings page that opens.",
                status.backgroundLocation, if (status.fineLocation) "Allow all the time" else "Grant location first",
                enabled = status.fineLocation,
            ) { background.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }
            if (Build.VERSION.SDK_INT >= 33) {
                Step("3. Notifications", "Needed to show reminders.", status.notifications, "Allow notifications") {
                    notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (Build.VERSION.SDK_INT >= 34) {
                Step(
                    "4. Full-screen alarms", "Lets Alarm reminders take over the screen, even on the lockscreen.",
                    status.fullScreen, "Open settings",
                ) { context.startActivity(SettingsIntents.fullScreen(context)) }
            }
            Info(
                "Alarm sound and Do Not Disturb",
                "Alarm reminders can ring through Do Not Disturb if you allow it for the Alarm channel.",
            ) {
                OutlinedButton(onClick = { context.startActivity(SettingsIntents.alarmChannel(context)) }) { Text("Alarm channel settings") }
                OutlinedButton(onClick = { context.startActivity(SettingsIntents.dndAccess()) }) { Text("Do Not Disturb access") }
            }
            Info(
                "Battery optimization",
                "Some phones stop background apps aggressively. If reminders arrive late, exclude Place Reminder from battery optimization.",
            ) {
                OutlinedButton(onClick = { context.startActivity(SettingsIntents.battery()) }) { Text("Battery settings") }
            }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(if (status.allGranted) "Done" else "Continue anyway") }
        }
    }
}

@Composable
private fun Step(title: String, body: String, done: Boolean, action: String, enabled: Boolean = true, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    if (done) "Granted" else "Missing",
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (!done) Button(onClick = onClick, enabled = enabled) { Text(action) }
        }
    }
}

@Composable
private fun Info(title: String, body: String, buttons: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            buttons()
        }
    }
}
