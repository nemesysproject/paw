package com.pawrescue.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pawrescue.data.model.PetRegistration

@Composable
fun HomeScreen(
    pets: List<PetRegistration>,
    notifications: List<AppNotification>,
    onMarkAllRead: () -> Unit,
    onDeleteNotification: (String) -> Unit,
    onToggleNearbyAlerts: () -> Unit,
    nearbyAlertsEnabled: Boolean,
    isGlassTheme: Boolean = false
) {
    val totalPets = pets.size
    val petsAtRisk = pets.count { it.status.equals("IN_DANGER", ignoreCase = true) || it.status.contains("riesgo", ignoreCase = true) }
    val petsInAdoption = pets.count { it.status.equals("ADOPTION", ignoreCase = true) || it.status.contains("adopción", ignoreCase = true) }
    val lostPets = pets.count { it.status.equals("LOST", ignoreCase = true) || it.status.contains("perdido", ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Resumen de la Red",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        // Dashboard Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main balance-like card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGlassTheme) Color.White.copy(alpha = 0.2f) 
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Mascotas Registradas",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = totalPets.toString(),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Ver Detalles",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "En Riesgo",
                    count = petsAtRisk.toString(),
                    icon = Icons.Default.Warning,
                    gradient = Brush.verticalGradient(listOf(Color(0xFFEC6A7C), Color(0xFFD32F2F))),
                    modifier = Modifier.weight(1f),
                    isGlassTheme = isGlassTheme
                )
                StatCard(
                    title = "En Adopción",
                    count = petsInAdoption.toString(),
                    icon = Icons.Default.Favorite,
                    gradient = Brush.verticalGradient(listOf(Color(0xFF81C784), Color(0xFF2E7D32))),
                    modifier = Modifier.weight(1f),
                    isGlassTheme = isGlassTheme
                )
                StatCard(
                    title = "Perdidas",
                    count = lostPets.toString(),
                    icon = Icons.Default.Search,
                    gradient = Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF1565C0))),
                    modifier = Modifier.weight(1f),
                    isGlassTheme = isGlassTheme
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reuse Notifications Layout logic but embedded here
        NotificationsLayout(
            notifications = notifications,
            onMarkAllRead = onMarkAllRead,
            onDeleteNotification = onDeleteNotification,
            onToggleNearbyAlerts = onToggleNearbyAlerts,
            nearbyAlertsEnabled = nearbyAlertsEnabled,
            onSimulateNotification = {} // No simulation here
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    isGlassTheme: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGlassTheme) Color.White.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(gradient)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
