package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.PetRegistration
import com.example.data.repository.PetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PetViewModel(private val repository: PetRepository) : ViewModel() {

    // Registros locales ordenados por fecha
    val registrations: StateFlow<List<PetRegistration>> = repository.allRegistrations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Simulación de red (True = Online, False = Offline)
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Estado del bloqueo biométrico
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Estado de sincronización en progreso
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Mensaje o logs de sincronización
    private val _syncMessage = MutableStateFlow("")
    val syncMessage: StateFlow<String> = _syncMessage.asStateFlow()

    // Registro de mascotas actualmente en progreso de creación
    private val _formPhotos = MutableStateFlow<List<String>>(emptyList())
    val formPhotos: StateFlow<List<String>> = _formPhotos.asStateFlow()

    private val _formVideoPath = MutableStateFlow<String?>(null)
    val formVideoPath: StateFlow<String?> = _formVideoPath.asStateFlow()

    fun setAuthenticated(auth: Boolean) {
        _isAuthenticated.value = auth
    }

    fun toggleNetwork(online: Boolean) {
        _isOnline.value = online
        // Sincronización automática cuando el usuario recupera la red
        if (online) {
            triggerSync()
        }
    }

    fun addPhotoToForm(photoPath: String) {
        val current = _formPhotos.value.toMutableList()
        if (current.size < 10) {
            current.add(photoPath)
            _formPhotos.value = current
        }
    }

    fun removePhotoFromForm(index: Int) {
        val current = _formPhotos.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _formPhotos.value = current
        }
    }

    fun setVideoPathInForm(path: String?) {
        _formVideoPath.value = path
    }

    fun clearForm() {
        _formPhotos.value = emptyList()
        _formVideoPath.value = null
    }

    // Guardar registro en la base de datos SQLite (Room)
    fun savePetRegistration(
        name: String,
        description: String,
        latitude: Double,
        longitude: Double,
        locationName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            // Unir fotos como JSON o string separado por comas
            val photoPathsString = _formPhotos.value.joinToString(",")
            
            val pet = PetRegistration(
                name = name.ifBlank { "Mascota sin Nombre" },
                description = description.ifBlank { "Sin descripción detallada" },
                photosJson = photoPathsString,
                videoPath = _formVideoPath.value,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName.ifBlank { "Ubicación Desconocida" },
                isSynced = false // Inicialmente sin sincronizar
            )
            
            repository.insertRegistration(pet)
            clearForm()
            onSuccess()

            // Si estamos online, sincronizar inmediatamente
            if (_isOnline.value) {
                triggerSync()
            }
        }
    }

    // Sincronizar mascotas pendientes con el servidor simulado
    fun triggerSync() {
        if (_isSyncing.value || !_isOnline.value) return

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.syncPendingRegistrations { message ->
                    _syncMessage.value = message
                }
            } catch (e: Exception) {
                _syncMessage.value = "Error al sincronizar: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteRegistration(id: Int) {
        viewModelScope.launch {
            repository.deleteRegistration(id)
        }
    }
}

class PetViewModelFactory(private val repository: PetRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
