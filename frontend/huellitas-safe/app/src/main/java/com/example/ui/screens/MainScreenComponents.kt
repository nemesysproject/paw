package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PetRegistration

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
    var filterAnimalType by remember { mutableStateOf("Todos") } 
    var filterStatus by remember { mutableStateOf("Todos") }
    var filterDateRange by remember { mutableStateOf("Todos") }
    var showFiltersPanel by remember { mutableStateOf(false) }

    val filteredRegistrations = remember(registrations, searchQuery, filterAnimalType, filterStatus, filterDateRange) {
        registrations.filter { pet ->
            val matchesSearch = pet.name.contains(searchQuery, ignoreCase = true) ||
                    pet.description.contains(searchQuery, ignoreCase = true) ||
                    pet.locationName.contains(searchQuery, ignoreCase = true)
            // Filtering logic
            val matchesType = if (filterAnimalType == "Todos") true else {
                if (filterAnimalType == "Perro") {
                    pet.description.contains("perro", ignoreCase = true) || 
                    pet.name.contains("perro", ignoreCase = true) || 
                    pet.description.contains("can", ignoreCase = true)
                } else if (filterAnimalType == "Gato") {
                    pet.description.contains("gato", ignoreCase = true) || 
                    pet.name.contains("gato", ignoreCase = true) || 
                    pet.description.contains("felino", ignoreCase = true)
                } else true
            }

            val matchesStatus = when (filterStatus) {
                "Sincronizados" -> pet.isSynced
                "Pendientes" -> !pet.isSynced
                else -> true
            }

            val matchesDate = when (filterDateRange) {
                "Últimas 24h" -> System.currentTimeMillis() - pet.timestamp < 24 * 60 * 60 * 1000
                "Última semana" -> System.currentTimeMillis() - pet.timestamp < 7 * 24 * 60 * 60 * 1000
                else -> true
            }

            matchesSearch && matchesType && matchesStatus && matchesDate
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Sync Status Card
        if (syncMessage.isNotEmpty() || isSyncing) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    else Icon(Icons.Default.CloudSync, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = syncMessage.ifEmpty { "Sincronizador activo" }, style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp))
                }
            }
        }

        // Search Bar
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar...", fontSize = 13.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp), tint = Color.White) },
                shape = RoundedCornerShape(100.dp), modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            IconButton(onClick = { showFiltersPanel = !showFiltersPanel }, modifier = Modifier.size(48.dp).clip(CircleShape).background(if (showFiltersPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))) {
                Icon(Icons.Default.Tune, null, tint = Color.White)
            }
        }

        // List
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)), 
            shape = RoundedCornerShape(28.dp), 
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), 
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            if (filteredRegistrations.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FilterListOff, null, modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.3f))
                    Text("Sin resultados", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(filteredRegistrations, key = { pet ->
                        // Generate a truly unique key using local ID, remote ID or fallback
                        val uniqueKey = when {
                            !pet.remoteId.isNullOrBlank() -> "remote_${pet.remoteId}"
                            pet.id != 0 -> "local_${pet.id}"
                            else -> "temp_${pet.hashCode()}"
                        }
                        uniqueKey
                    }) { pet ->
                        PetListItem(pet = pet, onClicked = { onPetClicked(pet) }, onDelete = { onDeleteRequested(pet) })
                    }
                }
            }
        }
    }
}

@Composable
fun PetListItem(pet: PetRegistration, onClicked: () -> Unit, onDelete: () -> Unit) {
    val photoUrls = remember(pet.photosJson) { if (pet.photosJson.isBlank()) emptyList() else pet.photosJson.split(",") }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClicked() }.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
    ) {
        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(76.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.1f))) {
                if (photoUrls.isNotEmpty()) AsyncImage(model = photoUrls.first(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Default.Pets, null, modifier = Modifier.align(Alignment.Center), tint = Color.White.copy(alpha = 0.2f))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = pet.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text(text = pet.description, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(text = pet.locationName, fontSize = 10.sp, color = Color.Gray)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(15.dp), tint = Color.Red.copy(alpha = 0.4f)) }
                }
            }
        }
    }
}
