package io.github.janhorak58.placereminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.janhorak58.placereminder.data.AppDatabase
import io.github.janhorak58.placereminder.data.FireRules
import io.github.janhorak58.placereminder.data.GeofenceBudget
import io.github.janhorak58.placereminder.data.Limits
import io.github.janhorak58.placereminder.data.Reminder
import io.github.janhorak58.placereminder.geofence.GeofenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val LIMIT_MESSAGE = "Geofence limit reached. Android allows ${Limits.MAX_GEOFENCES} geofences per app " +
    "(a reminder with a heads-up uses 2). Turn off or delete a reminder first."

class ListViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).reminderDao()

    /** Null until the first database read has completed. */
    val reminders: StateFlow<List<Reminder>?> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val message = MutableStateFlow<String?>(null)

    private suspend fun sync(id: Long) = GeofenceManager.update(getApplication(), listOf(id))

    fun setEnabled(r: Reminder, on: Boolean) {
        viewModelScope.launch {
            val updated = if (on) FireRules.afterReEnabled(r) else r.copy(enabled = false)
            if (!GeofenceBudget.fits(dao.getAll(), updated)) {
                message.value = LIMIT_MESSAGE
                return@launch
            }
            dao.update(updated)
            sync(r.id)
        }
    }

    fun duplicate(r: Reminder) {
        viewModelScope.launch {
            var copy = r.copy(id = 0, name = "${r.name} (copy)", lastFiredAt = null, headsUpLastFiredAt = null)
            if (!GeofenceBudget.fits(dao.getAll(), copy)) {
                copy = copy.copy(enabled = false)
                message.value = LIMIT_MESSAGE
            }
            sync(dao.insert(copy))
        }
    }

    fun delete(r: Reminder) {
        viewModelScope.launch {
            dao.delete(r.id)
            sync(r.id)
        }
    }
}
