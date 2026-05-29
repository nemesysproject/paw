package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PetRegistration
import com.example.ui.components.AddPetDialog
import com.example.ui.components.PetDetailDialog
import com.example.ui.screens.*
import com.example.ui.viewmodel.PetViewModel
import com.example.utils.BiometricHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PetViewModel) {
    val context = LocalContext.current
    val registrations by viewModel.registrations.collectAsStateWithLifecycle()
    val remotePets by viewModel.remotePets.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    val unifiedPets = remember(registrations, remotePets) {
        // Mapping remote pets to PetRegistration structure for UI consistency
        val mappedRemote = remotePets.map { remote ->
            PetRegistration(
                id = 0,
                remoteId = remote.id,
                name = remote.name ?: "Mascota sin Nombre",
                description = remote.description ?: "Sin descripción",
                photosJson = remote.media.joinToString(",") { it.url },
                videoPath = remote.media.find { it.type == "SightingVideo" }?.url,
                latitude = remote.lastLatitude ?: 0.0,
                longitude = remote.lastLongitude ?: 0.0,
                locationName = "Reporte Remoto",
                timestamp = System.currentTimeMillis(),
                isSynced = true
            )
        }
        val remoteIds = mappedRemote.mapNotNull { it.remoteId }.toSet()
        val uniqueLocal = registrations.filter { it.remoteId == null || it.remoteId !in remoteIds }
        uniqueLocal + mappedRemote
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var petToEdit by remember { mutableStateOf<PetRegistration?>(null) }
    var selectedPetForDetail by remember { mutableStateOf<PetRegistration?>(null) }
    var currentTab by remember { mutableStateOf("registro") }

    // Mock notifications for UI demonstration
    var notificationList by remember { mutableStateOf(listOf(
        AppNotification("1", "¡Sincronización!", "Tus reportes se sincronizaron con éxito.", "Hace 5 min", false, "sync"),
        AppNotification("2", "Actualización v2.1", "Mejoras en el rendimiento de mapas.", "Ayer", true, "system")
    )) }
    var nearbyAlertsEnabled by remember { mutableStateOf(true) }

    val triggerBiometricAuth = {
        val activity = context as? FragmentActivity
        if (activity != null) {
            BiometricHelper.showPrompt(
                activity = activity,
                onSuccess = { viewModel.setAuthenticated(true) },
                onError = { error ->
                    Toast.makeText(context, "Acceso denegado: $error", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            viewModel.setAuthenticated(true)
        }
    }

    LaunchedEffect(isLoggedIn, isAuthenticated) {
        if (isLoggedIn && !isAuthenticated) triggerBiometricAuth()
    }

    if (!isLoggedIn) {
        LoginRegisterRedesignScreen(viewModel)
    } else if (!isAuthenticated) {
        BiometricLockScreen(isBiometricAvailable = BiometricHelper.isBiometricAvailable(context), onUnlockRequested = { triggerBiometricAuth() })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Huellitas Safe", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.toggleNetwork(!isOnline) }) {
                                Icon(if (isOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff, null, modifier = Modifier.size(14.dp), tint = if (isOnline) Color(0xFF5F9E81) else Color.Gray)
                                Text(if (isOnline) "Modo Online" else "Modo Offline", fontSize = 10.sp, color = if (isOnline) Color(0xFF5F9E81) else Color.Gray)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setAuthenticated(false) }) { Icon(Icons.Default.Fingerprint, "BIO LOCK") }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = currentTab == "registro", onClick = { currentTab = "registro" }, icon = { Icon(Icons.Default.Pets, null) }, label = { Text("REGISTRO") })
                    NavigationBarItem(selected = currentTab == "notificaciones", onClick = { currentTab = "notificaciones" }, icon = { Icon(Icons.Default.Notifications, null) }, label = { Text("ALERTAS") })
                    NavigationBarItem(selected = currentTab == "perfil", onClick = { currentTab = "perfil" }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("PERFIL") })
                }
            },
            floatingActionButton = {
                if (currentTab == "registro") {
                    ExtendedFloatingActionButton(onClick = { showAddDialog = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Reportar") })
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))))) {
                when (currentTab) {
                    "registro" -> SurfaceLayout(unifiedPets, isSyncing, syncMessage, isOnline, { selectedPetForDetail = it }, { viewModel.deleteRegistration(it.id, it.remoteId) }, { viewModel.triggerSync() })
                    "notificaciones" -> NotificationsLayout(notificationList, { notificationList = notificationList.map { it.copy(isRead = true) } }, { id -> notificationList = notificationList.filter { it.id != id } }, { nearbyAlertsEnabled = !nearbyAlertsEnabled }, nearbyAlertsEnabled, {})
                    "perfil" -> ProfileLayout(unifiedPets, viewModel.currentUserName ?: "", viewModel.currentUserEmail ?: "", {}, {}, { selectedPetForDetail = it }, {}, { viewModel.logout() })
                }
            }
        }
    }

    if (showAddDialog) {
        AddPetDialog(viewModel, petToEdit, { showAddDialog = false; petToEdit = null }, { showAddDialog = false; petToEdit = null })
    }

    selectedPetForDetail?.let { pet ->
        PetDetailDialog(pet, { selectedPetForDetail = null }, { petToEdit = pet; selectedPetForDetail = null; showAddDialog = true }, { viewModel.deleteRegistration(pet.id, pet.remoteId); selectedPetForDetail = null })
    }
}
