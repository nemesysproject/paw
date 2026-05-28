package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.PetRegistration
import com.example.ui.viewmodel.PetViewModel
import com.example.ui.theme.WarmDialogBackground
import com.example.utils.BiometricHelper
import com.example.utils.LocationHelper
import com.example.utils.MediaUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PetViewModel) {
    val context = LocalContext.current
    val registrations by viewModel.registrations.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPetForDetail by remember { mutableStateOf<PetRegistration?>(null) }
    
    // Control de pestañas de navegación (Figma/Tailwind Professional Polish)
    var currentTab by remember { mutableStateOf("registro") }

    // Notificaciones sutiles push locales
    var notificationList by remember {
        mutableStateOf(
            listOf(
                AppNotification(
                    id = "1",
                    title = "¡Sincronización Completada!",
                    description = "Tus reportes locales de SQLite se sincronizaron con éxito en la red de Huellitas Safe.",
                    date = "Hace 5 min",
                    isRead = false,
                    type = "sync"
                ),
                AppNotification(
                    id = "2",
                    title = "Actualización de Huellitas Safe v2.1",
                    description = "Los mapas y la indexación local de mascotas de la calle se han acelerado significativamente.",
                    date = "Ayer",
                    isRead = true,
                    type = "system"
                ),
                AppNotification(
                    id = "3",
                    title = "Alerta Comunitaria de Rescate",
                    description = "Se reportó un canino extraviado herido a 350m de tu perímetro registrado (Av. Siempre Viva).",
                    date = "Hace 2 días",
                    isRead = true,
                    type = "neighborhood"
                )
            )
        )
    }

    var nearbyAlertsEnabled by remember { mutableStateOf(true) }

    val activeNotificationCount = remember(notificationList) {
        notificationList.count { !it.isRead }
    }

    // Perfil de usuario personalizable
    var profileName by remember { mutableStateOf("Mario Zúñiga Trejo") }
    var profileEmail by remember { mutableStateOf("mario.zuniga.trejo@gmail.com") }

    // Disparador de Notificaciones de Sincronización Automática al pasar a Online
    var hasNotifiedSync by remember { mutableStateOf(false) }
    LaunchedEffect(isOnline, isSyncing) {
        if (isOnline && !isSyncing && !hasNotifiedSync) {
            val newNotif = AppNotification(
                id = java.util.UUID.randomUUID().toString(),
                title = "Sincronización Automática",
                description = "Se sincronizaron con éxito tus reportes pendientes con los servidores comunitarios en la nube.",
                date = "Ahora",
                isRead = false,
                type = "sync"
            )
            notificationList = listOf(newNotif) + notificationList.filter { it.type != "sync_now" }
            hasNotifiedSync = true
        } else if (!isOnline) {
            hasNotifiedSync = false
        }
    }

    // Función para invocar el prompt biométrico
    val triggerBiometricAuth = {
        val activity = context as? FragmentActivity
        if (activity != null) {
            BiometricHelper.showPrompt(
                activity = activity,
                onSuccess = {
                    viewModel.setAuthenticated(true)
                    Toast.makeText(context, "¡Acceso autorizado!", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    Toast.makeText(context, "Acceso denegado: $error", Toast.LENGTH_LONG).show()
                }
            )
        } else {
            // Fallback por si no es FragmentActivity (autentica de todos modos)
            viewModel.setAuthenticated(true)
        }
    }

    // Al arrancar la app, lanzamos la autenticación biométrica de forma automática
    LaunchedEffect(Unit) {
        if (!isAuthenticated) {
            triggerBiometricAuth()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = "Mascotas",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Huellitas Safe",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 18.sp
                                    )
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { viewModel.toggleNetwork(!isOnline) }
                                ) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                        contentDescription = "Estado online",
                                        tint = if (isOnline) Color(0xFF5F9E81) else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isOnline) "Modo Online" else "Modo Offline",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isOnline) Color(0xFF5F9E81) else MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isAuthenticated) {
                                        viewModel.setAuthenticated(false)
                                    } else {
                                        triggerBiometricAuth()
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Autenticación Biométrica",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = if (isAuthenticated) "BIO ON" else "BIO OFF",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp
                                    )
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                HorizontalDivider(color = Color(0xFFF3E9E5), thickness = 1.dp)
            }
        },
        bottomBar = {
            if (isAuthenticated) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(72.dp)
                ) {
                    NavigationBarItem(
                        selected = currentTab == "registro",
                        onClick = { currentTab = "registro" },
                        icon = { Icon(Icons.Default.Pets, contentDescription = "REGISTRO") },
                        label = { Text("REGISTRO", fontWeight = FontWeight.SemiBold, fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                            indicatorColor = Color(0xFFFCEEE9)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "notificaciones",
                        onClick = { currentTab = "notificaciones" },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (activeNotificationCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.White
                                        ) {
                                            Text(activeNotificationCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "ALERTAS")
                            }
                        },
                        label = { Text("ALERTAS", fontWeight = FontWeight.SemiBold, fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                            indicatorColor = Color(0xFFFCEEE9)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "perfil",
                        onClick = { currentTab = "perfil" },
                        icon = { Icon(Icons.Default.Person, contentDescription = "PERFIL") },
                        label = { Text("PERFIL", fontWeight = FontWeight.SemiBold, fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                            indicatorColor = Color(0xFFFCEEE9)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (isAuthenticated && currentTab == "registro") {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, "Registrar Rescate") },
                    text = { Text("Registrar Rescate", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(100.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                        )
                    )
                )
        ) {
            if (!isAuthenticated) {
                // Pantalla de bloqueo biométrico cálida
                BiometricLockScreen(
                    isBiometricAvailable = BiometricHelper.isBiometricAvailable(context),
                    onUnlockRequested = { triggerBiometricAuth() }
                )
            } else {
                when (currentTab) {
                    "registro" -> {
                        SurfaceLayout(
                            registrations = registrations,
                            isSyncing = isSyncing,
                            syncMessage = syncMessage,
                            isOnline = isOnline,
                            onPetClicked = { selectedPetForDetail = it },
                            onDeleteRequested = { viewModel.deleteRegistration(it.id) },
                            onManualSyncRequested = { viewModel.triggerSync() }
                        )
                    }
                    "notificaciones" -> {
                        NotificationsLayout(
                            notifications = notificationList,
                            onMarkAllRead = {
                                notificationList = notificationList.map { it.copy(isRead = true) }
                            },
                            onDeleteNotification = { notificationId ->
                                notificationList = notificationList.filter { it.id != notificationId }
                            },
                            onToggleNearbyAlerts = { nearbyAlertsEnabled = !nearbyAlertsEnabled },
                            nearbyAlertsEnabled = nearbyAlertsEnabled,
                            onSimulateNotification = {
                                val titles = listOf(
                                    "Reporte en tu vecindario",
                                    "Actualización Importante",
                                    "Rescate Exitoso",
                                    "Alerta Veterinaria"
                                )
                                val descriptions = listOf(
                                    "Se reportó un felino cruza persa herido a menos de 600 metros de tu zona configurada.",
                                    "Huellitas Safe ha optimizado la sincronización masiva en segundo plano desde el hilo local.",
                                    "¡Buenas noticias! El gatito de los reportes anteriores fue adoptado permanentemente.",
                                    "Campaña de vacunación y desparasitación gratuita para mascotas comunitarias este fin de semana."
                                )
                                val types = listOf("neighborhood", "system", "sync", "neighborhood")
                                val randomIndex = (0..3).random()
                                val randNotif = AppNotification(
                                    id = java.util.UUID.randomUUID().toString(),
                                    title = titles[randomIndex],
                                    description = descriptions[randomIndex],
                                    date = "Hace un momento",
                                    isRead = false,
                                    type = types[randomIndex]
                                )
                                notificationList = listOf(randNotif) + notificationList
                                Toast.makeText(context, "Sutil alerta push entrante simulada", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    "perfil" -> {
                        ProfileLayout(
                            registrations = registrations,
                            name = profileName,
                            email = profileEmail,
                            onNameChanged = { profileName = it },
                            onEmailChanged = { profileEmail = it },
                            onPetClicked = { selectedPetForDetail = it },
                            onSaveProfile = {
                                Toast.makeText(context, "Perfil guardado correctamente", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para registrar una nueva mascota
    if (showAddDialog) {
        AddPetDialog(
            viewModel = viewModel,
            onDismiss = {
                viewModel.clearForm()
                showAddDialog = false
            },
            onSaveSuccess = {
                Toast.makeText(context, "Mascota registrada localmente en SQLite", Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }

    // Diálogo de detalles de la mascota seleccionada
    selectedPetForDetail?.let { pet ->
        PetDetailDialog(
            pet = pet,
            onDismiss = { selectedPetForDetail = null }
        )
    }
}

@Composable
fun BiometricLockScreen(isBiometricAvailable: Boolean, onUnlockRequested: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Pantalla Bloqueada",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Acceso Protegido",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Esta aplicación requiere autenticación para resguardar la información de las mascotas registradas en la vía pública.",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onUnlockRequested,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isBiometricAvailable) Icons.Default.Fingerprint else Icons.Default.VpnKey,
                contentDescription = "Login"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isBiometricAvailable) "Desbloquear con Biometría" else "Ingresar con PIN / Patrón",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SurfaceLayout(
    registrations: List<PetRegistration>,
    isSyncing: Boolean,
    syncMessage: String,
    isOnline: Boolean,
    onPetClicked: (PetRegistration) -> Unit,
    onDeleteRequested: (PetRegistration) -> Unit,
    onManualSyncRequested: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var filterAnimalType by remember { mutableStateOf("Todos") } // Todos, Perros, Gatos, Otros
    var filterStatus by remember { mutableStateOf("Todos") } // Todos, Sincronizados, Pendientes
    var filterDateRange by remember { mutableStateOf("Todos") } // Todos, Últimas 24h, Última semana
    var showFiltersPanel by remember { mutableStateOf(false) }

    // Filtrar los reportes localmente de forma avanzada
    val filteredRegistrations = remember(registrations, searchQuery, filterAnimalType, filterStatus, filterDateRange) {
        registrations.filter { pet ->
            val matchesSearch = pet.name.contains(searchQuery, ignoreCase = true) ||
                    pet.description.contains(searchQuery, ignoreCase = true) ||
                    pet.locationName.contains(searchQuery, ignoreCase = true)

            val matchesType = if (filterAnimalType == "Todos") true else {
                if (filterAnimalType == "Perro") {
                    pet.description.contains("perro", ignoreCase = true) || 
                    pet.name.contains("perro", ignoreCase = true) || 
                    pet.description.contains("can", ignoreCase = true) ||
                    pet.description.contains("perrito", ignoreCase = true) ||
                    pet.description.contains("pata", ignoreCase = true)
                } else if (filterAnimalType == "Gato") {
                    pet.description.contains("gato", ignoreCase = true) || 
                    pet.name.contains("gato", ignoreCase = true) || 
                    pet.description.contains("miau", ignoreCase = true) || 
                    pet.description.contains("felino", ignoreCase = true) ||
                    pet.description.contains("gatito", ignoreCase = true)
                } else {
                    // Otro
                    !pet.description.contains("perro", ignoreCase = true) && 
                    !pet.name.contains("perro", ignoreCase = true) && 
                    !pet.description.contains("gato", ignoreCase = true) && 
                    !pet.name.contains("gato", ignoreCase = true) &&
                    !pet.description.contains("can", ignoreCase = true)
                }
            }

            val matchesStatus = when (filterStatus) {
                "Sincronizados" -> pet.isSynced
                "Pendientes" -> !pet.isSynced
                else -> true
            }

            val matchesDate = when (filterDateRange) {
                "Últimas 24h" -> {
                    System.currentTimeMillis() - pet.timestamp < 24 * 60 * 60 * 1000
                }
                "Última semana" -> {
                    System.currentTimeMillis() - pet.timestamp < 7 * 24 * 60 * 60 * 1000
                }
                else -> true
            }

            matchesSearch && matchesType && matchesStatus && matchesDate
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Consola o barra de estado de sincronización SQLite cuando esté disponible
        if (syncMessage.isNotEmpty() || isSyncing) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                ),
                border = BorderStroke(1.dp, Color(0xFFF3E9E5)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sincronización masiva",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = syncMessage.ifEmpty { "Sincronizador local SQLite activo." },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Barra de Búsqueda y Botón de Filtros Integrados (Figma Warm Design)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nombre, herida, ubicación...", fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, "Buscar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Limpiar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFF3E9E5),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.weight(1f)
            )

            // Botón de tuning de Filtros
            IconButton(
                onClick = { showFiltersPanel = !showFiltersPanel },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (showFiltersPanel) MaterialTheme.colorScheme.primary else Color(0xFFFCEEE9))
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filtros Avanzados",
                    tint = if (showFiltersPanel) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Panel de Filtros Avanzados Desplegable
        AnimatedVisibility(
            visible = showFiltersPanel,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF3E9E5)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Filtros Avanzados de Búsqueda",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    )

                    // 1. Tipo de Animal
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tipo de Animal:", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Todos", "Perro", "Gato", "Otro").forEach { type ->
                                FilterChip(
                                    selected = filterAnimalType == type,
                                    onClick = { filterAnimalType = type },
                                    label = { Text(type, fontSize = 10.sp) },
                                    shape = RoundedCornerShape(100.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // 2. Estado de Sincronización
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Estado de Servidor:", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Todos", "Sincronizados", "Pendientes").forEach { status ->
                                FilterChip(
                                    selected = filterStatus == status,
                                    onClick = { filterStatus = status },
                                    label = { Text(status, fontSize = 10.sp) },
                                    shape = RoundedCornerShape(100.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // 3. Rango de Fecha de Registro
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Fecha de Reporte:", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Todos", "Últimas 24h", "Última semana").forEach { date ->
                                FilterChip(
                                    selected = filterDateRange == date,
                                    onClick = { filterDateRange = date },
                                    label = { Text(date, fontSize = 10.sp) },
                                    shape = RoundedCornerShape(100.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Botón para reiniciar filtros
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                filterAnimalType = "Todos"
                                filterStatus = "Todos"
                                filterDateRange = "Todos"
                                searchQuery = ""
                                Toast.makeText(context, "Filtros restablecidos", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Limpiar Filtros", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Título de la lista, con el contador "Pendientes" estilizado exactamente como la badge del template
        val unsyncedCount = filteredRegistrations.count { !it.isSynced }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isNotEmpty() || filterAnimalType != "Todos" || filterStatus != "Todos" || filterDateRange != "Todos") {
                    "Resultados (${filteredRegistrations.size})"
                } else {
                    "Mascotas Registradas"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 14.sp
                )
            )

            if (unsyncedCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFFCEEE9))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$unsyncedCount PENDIENTES DE SINCRO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }

                    if (isOnline) {
                        IconButton(
                            onClick = onManualSyncRequested,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFCEEE9))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sincronizar ahora",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // El contenedor blanco del template: rounded-[28px] p-6 shadow-sm border border-[#f3e9e5]
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFFF3E9E5)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (filteredRegistrations.isEmpty()) {
                    // Empty State cálido y minimalista
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFCEEE9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterListOff,
                                contentDescription = "Sin registros coincidentes",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Sin resultados coincidentes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Prueba con otros términos de búsqueda o cambia la configuración de los filtros de ubicación, animal o estado.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredRegistrations, key = { it.id }) { pet ->
                            PetListItem(
                                pet = pet,
                                onClicked = { onPetClicked(pet) },
                                onDelete = { onDeleteRequested(pet) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Emulador helper modifier
fun Modifier.fillweight(weight: Float): Modifier = this.fillMaxHeight(weight)

@Composable
fun PetListItem(
    pet: PetRegistration,
    onClicked: () -> Unit,
    onDelete: () -> Unit
) {
    val photoUrls = remember(pet.photosJson) {
        if (pet.photosJson.isBlank()) emptyList() else pet.photosJson.split(",")
    }
    
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8F6)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClicked() }
            .border(
                width = 1.dp,
                color = Color(0xFFF3E9E5),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail principal (Foto de la Mascota)
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF3E9E5))
            ) {
                if (photoUrls.isNotEmpty()) {
                    AsyncImage(
                        model = photoUrls.first(),
                        contentDescription = "Mascota thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = "Sin Imagen",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Fila de Nombre + Estado de Sincronización
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pet.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Chip de Sincronización Profesional
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (pet.isSynced) Color(0xFFE2F3EB)
                                else Color(0xFFFCEEE9)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (pet.isSynced) Color(0xFF5F9E81) else MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = if (pet.isSynced) "Sincronizado" else "Pendiente",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pet.isSynced) Color(0xFF5F9E81) else MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = pet.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fila de Localización + Multimedia counts
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Ubicación",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = pet.locationName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Contador fotos
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Fotos",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${photoUrls.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Indicador video
                        if (pet.videoPath != null) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Tiene Video",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPetDialog(
    viewModel: PetViewModel,
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val formPhotos by viewModel.formPhotos.collectAsStateWithLifecycle()
    val formVideoPath by viewModel.formVideoPath.collectAsStateWithLifecycle()

    var petName by remember { mutableStateOf("") }
    var petDescription by remember { mutableStateOf("") }
    
    // Coordenadas iniciales por defecto (un lindo sector central de la ciudad)
    var latitude by remember { mutableDoubleStateOf(-12.046374) }
    var longitude by remember { mutableDoubleStateOf(-77.042793) }
    var locationName by remember { mutableStateOf("Calle de la Esperanza N° 450") }

    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }

    // Launcher para capturar foto real mediante la Cámara nativa
    var photoFileUri by remember { mutableStateOf<Uri?>(null) }
    var rawPhotoFile by remember { mutableStateOf<File?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && rawPhotoFile != null) {
            viewModel.addPhotoToForm(rawPhotoFile!!.absolutePath)
        }
    }

    // Launcher para capturar video real mediante la Cámara nativa
    var videoFileUri by remember { mutableStateOf<Uri?>(null) }
    var rawVideoFile by remember { mutableStateOf<File?>(null) }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && rawVideoFile != null) {
            viewModel.setVideoPathInForm(rawVideoFile!!.absolutePath)
        }
    }

    // Lanzador múltiple para permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        
        if (locationGranted) {
            locationHelper.getLastLocation(
                onSuccess = { lat, lng ->
                    latitude = lat
                    longitude = lng
                    locationName = "Registro GPS: [$lat, $lng]"
                    Toast.makeText(context, "GPS Localizado con éxito", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(context, "No se pudo obtener el GPS exacto, usando simulación", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WarmDialogBackground),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header del Formulario
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Registrar Mascota",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                // Campo: Nombre de mascota
                OutlinedTextField(
                    value = petName,
                    onValueChange = { petName = it },
                    label = { Text("Identificador o Nombre Temporal") },
                    placeholder = { Text("Ej. Perrito Cojo, Firulais, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Campo: Descripción
                OutlinedTextField(
                    value = petDescription,
                    onValueChange = { petDescription = it },
                    label = { Text("Descripción de la Situación") },
                    placeholder = { Text("Ej: Desnutrido, asustadizo, sin collar, tiene herida de pata...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                // CAPTURA DE UBICACIÓN
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.LocationOn, "Localización", tint = MaterialTheme.colorScheme.primary)
                                Text("Ubicación del Reporte", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.GpsFixed, "GPS", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Capturar GPS", fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = locationName,
                            onValueChange = { locationName = it },
                            label = { Text("Descripción de la dirección o intersección") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = String.format(Locale.US, "%.6f", latitude),
                                onValueChange = { latitude = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text("Latitud") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = String.format(Locale.US, "%.6f", longitude),
                                onValueChange = { longitude = it.toDoubleOrNull() ?: 0.0 },
                                label = { Text("Longitud") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                // MULTIMEDIA: FOTOS (MÁXIMO 10)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Fotografías (${formPhotos.size}/10)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Máximo 10 fotos requeridas", fontSize = 11.sp, color = Color.Gray)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Captura real de foto
                                Button(
                                    onClick = {
                                        try {
                                            val file = MediaUtils.createPhotoFile(context)
                                            rawPhotoFile = file
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            photoFileUri = uri
                                            cameraLauncher.launch(uri)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error cámara: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, "Cámara", modifier = Modifier.size(16.dp))
                                }

                                // Simulación de foto (para web/emuladores)
                                Button(
                                    onClick = {
                                        if (formPhotos.size < 10) {
                                            val mockImgUrl = MediaUtils.getMockPetImage(formPhotos.size)
                                            viewModel.addPhotoToForm(mockImgUrl)
                                            Toast.makeText(context, "Foto simulada de mascota agregada", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Límite máximo de 10 fotos alcanzado", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, "Simular", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (formPhotos.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(8.dp))
                                    .background(Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sin fotos. Añade capturas reales o simuladas.",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(formPhotos) { idx, path ->
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    ) {
                                        AsyncImage(
                                            model = path,
                                            contentDescription = "Foto capturada",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { viewModel.removePhotoFromForm(idx) },
                                            modifier = Modifier
                                                .size(20.dp)
                                                .align(Alignment.TopEnd)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Quitar foto",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // MULTIMEDIA: VIDEO
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Registro en Video", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Video opcional de comportamiento o estado", fontSize = 11.sp, color = Color.Gray)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Grabar Video Real
                                Button(
                                    onClick = {
                                        try {
                                            val file = MediaUtils.createVideoFile(context)
                                            rawVideoFile = file
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            videoFileUri = uri
                                            videoLauncher.launch(uri)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error video: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Videocam, "Camcorder", modifier = Modifier.size(16.dp))
                                }

                                // Simular Video
                                Button(
                                    onClick = {
                                        viewModel.setVideoPathInForm("/mock_video.mp4")
                                        Toast.makeText(context, "Video simulado añadido", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.Add, "Simular Video", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (formVideoPath != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Listo",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "¡Video Registrado! (${formVideoPath})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.setVideoPathInForm(null) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Quitar", tint = Color.Gray, modifier = Modifier.size(12.dp))
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(8.dp))
                                    .background(Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Sin video adjunto", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // BOTONES DE ACCIÓN: Guardar en base de datos SQLite
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            viewModel.savePetRegistration(
                                name = petName,
                                description = petDescription,
                                latitude = latitude,
                                longitude = longitude,
                                locationName = locationName,
                                onSuccess = onSaveSuccess
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Icon(Icons.Default.Save, "Guardar")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PetDetailDialog(
    pet: PetRegistration,
    onDismiss: () -> Unit
) {
    val photoUrls = remember(pet.photosJson) {
        if (pet.photosJson.isBlank()) emptyList() else pet.photosJson.split(",")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Detalle del Reporte",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Cerrar")
                    }
                }

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                // Fila de fotos en carousel
                if (photoUrls.isNotEmpty()) {
                    Text("Fotografías Adjuntas (${photoUrls.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(photoUrls) { path ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .size(120.dp)
                            ) {
                                AsyncImage(
                                    model = path,
                                    contentDescription = "Foto Mascota",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoCamera, "Sin fotos", tint = Color.Gray)
                    }
                }

                // Datos de mascota
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = pet.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )

                // Detalles de ubicación
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, "Ubica", tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(pet.locationName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Coordenadas: ${pet.latitude}, ${pet.longitude}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // Detalles adicionales
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dateStr = remember(pet.timestamp) {
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        sdf.format(Date(pet.timestamp))
                    }
                    Column {
                        Text("Fecha de Registro", fontSize = 11.sp, color = Color.Gray)
                        Text(dateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Estado de Servidor", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = if (pet.isSynced) "Sincronizado" else "Pendiente de Sincronizar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pet.isSynced) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (pet.videoPath != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VideoLibrary, "Video", tint = MaterialTheme.colorScheme.secondary)
                            Text("Posee video registrado: ${pet.videoPath}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// DEFINICIÓN DE NOTIFICACIÓN PUSH LOCAL (Huellitas Safe Design Asset)
data class AppNotification(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val isRead: Boolean,
    val type: String // "sync", "system", "neighborhood"
)

// COMPONENTE: BANDEJA DE NOTIFICACIONES PUSH SUTILES
@Composable
fun NotificationsLayout(
    notifications: List<AppNotification>,
    onMarkAllRead: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onToggleNearbyAlerts: () -> Unit,
    nearbyAlertsEnabled: Boolean,
    onSimulateNotification: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado de la Sección de Notificaciones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Buzón de Alertas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = "Notificaciones sutiles push y comunitarias",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }

            if (notifications.any { !it.isRead }) {
                TextButton(onClick = onMarkAllRead) {
                    Text(
                        text = "Marcar leídas",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Panel de Configuración de Alertas Perimetrales (Comunidad)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFF3E9E5)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFCEEE9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Alertas comunidad",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Alertas de Cercanía (500m)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Alertas push no intrusivas sobre nuevos rescates",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Switch(
                    checked = nearbyAlertsEnabled,
                    onCheckedChange = { onToggleNearbyAlerts() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFFFCEEE9)
                    )
                )
            }
        }

        // Listado de Notificaciones Sutiles
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFFF3E9E5)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (notifications.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFCEEE9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = "Sin alertas",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Todo al día",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No tienes notificaciones pendientes de leer en Huellitas Safe.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.tertiary)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notifications, key = { it.id }) { item ->
                            NotificationListItem(
                                notification = item,
                                onDelete = { onDeleteNotification(item.id) }
                            )
                        }
                    }
                }
            }
        }

        // Botón de demostración (Simula Alertas Push)
        Button(
            onClick = onSimulateNotification,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5F9E81)),
            shape = RoundedCornerShape(100.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.CellTower, "Simular", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simular Alerta de Red Comunitario", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ELEMENTO DE LA LISTA DE NOTIFICACIONES
@Composable
fun NotificationListItem(
    notification: AppNotification,
    onDelete: () -> Unit
) {
    val containerBg = if (notification.isRead) Color(0xFFFDF8F6) else Color(0xFFFFF4F1)
    val iconColor = MaterialTheme.colorScheme.primary
    val iconVector = when (notification.type) {
        "sync" -> Icons.Default.CloudSync
        "neighborhood" -> Icons.Default.NotificationImportant
        else -> Icons.Default.Info
    }

    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF3E9E5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = notification.date,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 11.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Borrar alerta",
                    tint = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// COMPONENTE: SECCIÓN DE PERFIL DE USUARIO & HISTORIAL DE REPORTES
@Composable
fun ProfileLayout(
    registrations: List<PetRegistration>,
    name: String,
    email: String,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPetClicked: (PetRegistration) -> Unit,
    onSaveProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tarjeta de Identidad y Editar Información Personal
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFF3E9E5)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    // Iniciales con display tipográfico
                    Text(
                        text = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Text(
                    text = "Editor De Datos Personales",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    label = { Text("Nombre Completo") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFF3E9E5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChanged,
                    label = { Text("Dirección De Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFFF3E9E5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onSaveProfile,
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Check, "Guardar", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Datos De Perfil", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Métricas de Rescates Realizados por el Usuario
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7F5)),
            border = BorderStroke(1.dp, Color(0xFFFADCD3)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = registrations.size.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Total Rescates", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color(0xFFF3E9E5))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val syncedCount = registrations.count { it.isSynced }
                    Text(
                        text = syncedCount.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF5F9E81)
                    )
                    Text("Sincronizados", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(Color(0xFFF3E9E5))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val pendingCount = registrations.count { !it.isSynced }
                    Text(
                        text = pendingCount.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("Pendientes", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sección del Historial de Mascotas Registradas (Con enlaces de redirección directos al detalle)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Historial De Mascotas Registradas (${registrations.size})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (registrations.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFF3E9E5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no has registrado ninguna mascota. ¡Comienza a reportar!",
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    registrations.forEach { pet ->
                        ProfilePetItem(
                            pet = pet,
                            onClick = { onPetClicked(pet) }
                        )
                    }
                }
            }
        }
    }
}

// ELEMENTO DEL PANEL DE HISTORIAL DEL PERFIL
@Composable
fun ProfilePetItem(
    pet: PetRegistration,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF3E9E5)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFCEEE9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = pet.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = pet.locationName,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Chip de estado super sutil
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (pet.isSynced) Color(0xFFE2F3EB) else Color(0xFFFCEEE9))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (pet.isSynced) "Sincro" else "Pendiente",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pet.isSynced) Color(0xFF5F9E81) else MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver detalles",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
