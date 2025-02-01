package com.castle.sefirah.presentation.main

import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.komu.sekia.di.AppCoroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sefirah.data.repository.AppRepository
import sefirah.database.model.toDomain
import sefirah.domain.model.ConnectionState
import sefirah.domain.model.RemoteDevice
import sefirah.domain.repository.NetworkManager
import sefirah.network.NetworkService
import sefirah.network.NetworkService.Companion.Actions
import javax.inject.Inject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val networkManager: NetworkManager,
    private val appScope: AppCoroutineScope,
    private val appRepository: AppRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _deviceDetails = MutableStateFlow<RemoteDevice?>(null)
    val deviceDetails: StateFlow<RemoteDevice?> = _deviceDetails

    init {
        appScope.launch {
            networkManager.connectionState.collectLatest { state ->
                _connectionState.value = state
            }
        }

        appScope.launch {
            appRepository.getLastConnectedDeviceFlow().first()?.let { device->
                Log.d("ConnectionViewModel", "Device found: ${device.deviceName}")
                if (_connectionState.value == ConnectionState.Disconnected) {
                    toggleSync(true)
                }
            }
        }

        appScope.launch {
            appRepository.getLastConnectedDeviceFlow().collectLatest { device ->
                if (device != null) {
                    Log.d("ConnectionViewModel", "Device found: ${device.deviceName}")
                    _deviceDetails.value = device.toDomain()
                } else {
                    _deviceDetails.value = null
                }
            }
        }
    }

    fun toggleSync(syncRequest: Boolean) {
       appScope.launch {
            _deviceDetails.value?.let { device ->
                _isRefreshing.value = true
                when {
                    syncRequest && _connectionState.value == ConnectionState.Disconnected -> {
                        startService(Actions.START, device)
                    }
                    syncRequest && _connectionState.value == ConnectionState.Connected -> {
                        startService(Actions.STOP)
                        delay(200)
                        startService(Actions.START, device)
                    }
                    !syncRequest && _connectionState.value == ConnectionState.Connected -> {
                        startService(Actions.STOP)
                    }

                    else -> {}
                }
            } ?: run {
                _isRefreshing.value = false  // Ensure refreshing stops if no device
            }
        }
    }

    private var connectionStateJob: Job? = null

    private fun startService(action: Actions, device: RemoteDevice? = null) {
        val intent = Intent(getApplication(), NetworkService::class.java).apply {
            this.action = action.name
            device?.let { putExtra(NetworkService.REMOTE_INFO, it) }
        }
        
        try {
            getApplication<Application>().startService(intent)
        } catch (e: Exception) {
            _isRefreshing.value = false
            return
        }
        connectionStateJob = appScope.launch {
            connectionState.collect { state ->
                when (state) {
                    ConnectionState.Connected -> {
                        _isRefreshing.value = false
                        connectionStateJob?.cancel()
                    }
                    ConnectionState.Disconnected -> {
                        // Only stop refreshing if we initiated a STOP
                        if (action == Actions.STOP) {
                            _isRefreshing.value = false
                            connectionStateJob?.cancel()
                        }
                    }
                    ConnectionState.Connecting -> {
                        _isRefreshing.value = true
                    }
                    is ConnectionState.Error -> {
                        _isRefreshing.value = false
                        withContext(Dispatchers.Main.immediate) {
                            Toast.makeText(
                                getApplication(),
                                "Error: ${state.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        connectionStateJob?.cancel()
                    }
                }
            }
        }
    }
}