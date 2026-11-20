package com.mindguard.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mindguard.MainActivity
import com.mindguard.shared.usecases.isPausedAt
import com.mindguard.storage.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FocusTileService : TileService(), KoinComponent {

    private val settingsDataStore: SettingsDataStore by inject()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var listenJob: Job? = null

    override fun onStartListening() {
        listenJob?.cancel()
        listenJob = serviceScope.launch {
            settingsDataStore.pauseUntilFlow.collect { pauseUntil ->
                withContext(Dispatchers.Main) { updateTileState(pauseUntil) }
            }
        }
    }

    override fun onStopListening() {
        listenJob?.cancel()
        listenJob = null
    }

    // Resuming is instant; switching off opens the turn-off guard (countdown + real-world quest).
    override fun onClick() {
        serviceScope.launch {
            val pauseUntil = settingsDataStore.pauseUntilFlow.first()
            if (isPausedAt(pauseUntil, System.currentTimeMillis())) {
                settingsDataStore.resumeFromPause()
            } else {
                withContext(Dispatchers.Main) { openTurnOffGuard() }
            }
        }
    }

    private fun openTurnOffGuard() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_OPEN_TURN_OFF_GUARD, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateTileState(pauseUntilMs: Long) {
        val tile = qsTile ?: return
        val isPaused = isPausedAt(pauseUntilMs, System.currentTimeMillis())
        tile.state = if (isPaused) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.label = if (isPaused) "Focus Paused" else "Focus Mode"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isPaused) "Tap to resume" else "Tap to take a break"
        }
        tile.updateTile()
    }
}
