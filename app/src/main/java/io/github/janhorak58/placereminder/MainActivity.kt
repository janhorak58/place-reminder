package io.github.janhorak58.placereminder

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.janhorak58.placereminder.geofence.GeofenceManager
import io.github.janhorak58.placereminder.geofence.GeofenceState
import io.github.janhorak58.placereminder.ui.EditorScreen
import io.github.janhorak58.placereminder.ui.EditorViewModel
import io.github.janhorak58.placereminder.ui.ListScreen
import io.github.janhorak58.placereminder.ui.ListViewModel
import io.github.janhorak58.placereminder.ui.OnboardingScreen
import io.github.janhorak58.placereminder.ui.PermStatus
import io.github.janhorak58.placereminder.ui.PlaceReminderTheme
import kotlinx.coroutines.launch

private enum class Screen { LIST, EDITOR, SETUP }

class MainActivity : ComponentActivity() {
    /** Bumped on resume so permission status is re-read after returning from settings. */
    private var resumeTick by mutableIntStateOf(0)
    private var openReminderId by mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Every screen puts a navy band under the status bar, so keep its icons light.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        if (savedInstanceState == null) {
            openReminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
            // App start: re-register all geofences.
            lifecycleScope.launch { GeofenceManager.sync(applicationContext) }
        }
        setContent {
            PlaceReminderTheme { App(resumeTick, openReminderId) { openReminderId = -1L } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openReminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
        // Permissions or location may have just been granted: retry if the last registration failed.
        if (GeofenceManager.state.value != GeofenceState.OK) lifecycleScope.launch { GeofenceManager.sync(applicationContext) }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}

@Composable
private fun App(resumeTick: Int, openReminderId: Long, onOpenHandled: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val status = remember(resumeTick, tick) { PermStatus.read(context) }

    var screen by rememberSaveable { mutableStateOf(if (status.fineLocation) Screen.LIST else Screen.SETUP) }
    var editId by rememberSaveable { mutableLongStateOf(0L) }
    // Bumped on every open so the editor gets a fresh ViewModel. Keying by editId alone reused
    // the previous form, e.g. every "new" reminder shares id 0.
    var editSession by rememberSaveable { mutableIntStateOf(0) }
    fun openEditor(id: Long) {
        editId = id
        editSession++
        screen = Screen.EDITOR
    }

    if (openReminderId >= 0) {
        openEditor(openReminderId)
        onOpenHandled()
    }

    when (screen) {
        Screen.LIST -> {
            val vm: ListViewModel = viewModel()
            ListScreen(
                vm, status,
                onAdd = { openEditor(0L) },
                onEdit = { openEditor(it) },
                onSetup = { screen = Screen.SETUP },
            )
        }
        Screen.EDITOR -> {
            val app = context.applicationContext as Application
            val vm: EditorViewModel = viewModel(key = "editor-$editSession", factory = viewModelFactory {
                initializer { EditorViewModel(app, editId) }
            })
            EditorScreen(vm) { screen = Screen.LIST }
        }
        Screen.SETUP -> OnboardingScreen(status, onRefresh = { tick++ }, onDone = { screen = Screen.LIST })
    }
}
