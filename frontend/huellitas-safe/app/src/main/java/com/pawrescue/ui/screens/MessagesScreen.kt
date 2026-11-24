package com.pawrescue.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawrescue.data.model.AdoptionRequestEntity
import com.pawrescue.data.model.ConversationEntity
import com.pawrescue.data.model.MessageEntity
import com.pawrescue.ui.viewmodel.PetViewModel
import com.pawrescue.utils.LocationHelper

@Composable
fun MessagesScreen(
    viewModel: PetViewModel,
    isGlassTheme: Boolean = true
) {
    val context = LocalContext.current
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val adoptionRequests by viewModel.adoptionRequests.collectAsStateWithLifecycle()
    val selectedConversationId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()

    var subTab by remember { mutableStateOf("chats") }

    LaunchedEffect(Unit) {
        viewModel.loadSocialData()
    }

    val activeConversation = remember(selectedConversationId, conversations) {
        conversations.find { it.id == selectedConversationId }
    }

    if (selectedConversationId != null) {
        ChatDetailView(
            conversation = activeConversation,
            conversationId = selectedConversationId!!,
            messages = activeMessages,
            currentUserId = viewModel.currentUserId,
            isGlassTheme = isGlassTheme,
            onBack = { viewModel.openConversation(null) },
            onSendText = { text ->
                viewModel.sendChatText(selectedConversationId!!, text)
            },
            onShareLocation = {
                LocationHelper(context).getLastLocation(
                    onSuccess = { lat, lon ->
                        viewModel.sendChatLocation(
                            conversationId = selectedConversationId!!,
                            latitude = lat,
                            longitude = lon,
                            label = "Ubicación compartida en vivo"
                        ) { ok ->
                            if (ok) Toast.makeText(context, "Ubicación enviada", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = {
                        Toast.makeText(context, "Activa el GPS para compartir tu ubicación", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comunidad y Mensajes",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                IconButton(onClick = { viewModel.loadSocialData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
                }
            }

            // Sub-tabs Glass: Chats / Adopciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassSegmentButton(
                    label = "Chats (${conversations.size})",
                    selected = subTab == "chats",
                    modifier = Modifier.weight(1f),
                    onClick = { subTab = "chats" }
                )
                GlassSegmentButton(
                    label = "Adopciones (${adoptionRequests.size})",
                    selected = subTab == "adoptions",
                    modifier = Modifier.weight(1f),
                    onClick = { subTab = "adoptions" }
                )
            }

            if (subTab == "chats") {
                if (conversations.isEmpty()) {
                    EmptySocialState(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = "Sin conversaciones activas",
                        subtitle = "Abre la ficha de una mascota perdida o en adopción para enviar un mensaje o reportar un avistamiento."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 84.dp)
                    ) {
                        items(conversations, key = { it.id }) { conv ->
                            ConversationCard(
                                conversation = conv,
                                isGlassTheme = isGlassTheme,
                                onClick = { viewModel.openConversation(conv.id) }
                            )
                        }
                    }
                }
            } else {
                if (adoptionRequests.isEmpty()) {
                    EmptySocialState(
                        icon = Icons.Default.FavoriteBorder,
                        title = "Sin solicitudes de adopción",
                        subtitle = "Cuando solicites o recibas una solicitud de adopción, podrás darle seguimiento aquí."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 84.dp)
                    ) {
                        items(adoptionRequests, key = { it.id }) { req ->
                            AdoptionRequestCard(
                                request = req,
                                isGlassTheme = isGlassTheme,
                                onOpenChat = { req.conversationId?.let { viewModel.openConversation(it) } },
                                onApprove = {
                                    viewModel.updateAdoptionStatus(req.id, "APPROVED") {
                                        Toast.makeText(context, "Solicitud aprobada", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onReject = {
                                    viewModel.updateAdoptionStatus(req.id, "REJECTED") {
                                        Toast.makeText(context, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatDetailView(
    conversation: ConversationEntity?,
    conversationId: String,
    messages: List<MessageEntity>,
    currentUserId: String?,
    isGlassTheme: Boolean,
    onBack: () -> Unit,
    onSendText: (String) -> Unit,
    onShareLocation: () -> Unit
) {
    val context = LocalContext.current
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Header del chat
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isGlassTheme) Color.White.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = conversation?.counterpartName ?: conversation?.petName ?: "Conversación",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = conversationTypeLabel(conversation?.type ?: "PET_SIGHTING"),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lista de mensajes
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUserId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                            else Color.White.copy(alpha = 0.18f)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!isMine && !msg.senderName.isNullOrBlank()) {
                                Text(
                                    text = msg.senderName,
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (msg.type == "LOCATION" && msg.latitude != null && msg.longitude != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        val uri = Uri.parse("geo:${msg.latitude},${msg.longitude}?q=${msg.latitude},${msg.longitude}(Mascota)")
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "No se encontró app de mapas", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = msg.locationLabel ?: "Abrir ubicación en mapa",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = msg.content ?: "",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Barra de envío (Texto + Ubicación GPS)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.16f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 70.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShareLocation) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Compartir GPS", tint = Color(0xFFFFD54F))
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Escribe un mensaje...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onSendText(draft)
                            draft = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: ConversationEntity,
    isGlassTheme: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGlassTheme) Color.White.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (conversation.type) {
                        "ADOPTION_INQUIRY" -> Icons.Default.Favorite
                        "VET_CONSULT" -> Icons.Default.LocalHospital
                        "SHELTER_SUPPORT" -> Icons.Default.Home
                        else -> Icons.Default.LocationOn
                    },
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.petName ?: conversation.counterpartName ?: "Conversación",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = conversation.lastMessagePreview ?: conversationTypeLabel(conversation.type),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun AdoptionRequestCard(
    request: AdoptionRequestEntity,
    isGlassTheme: Boolean,
    onOpenChat: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGlassTheme) Color.White.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = request.petName ?: "Mascota en adopción",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = request.status,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            if (!request.notes.isNullOrBlank()) {
                Text(text = request.notes, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!request.conversationId.isNullOrBlank()) {
                    OutlinedButton(onClick = onOpenChat, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))) {
                        Text("Abrir chat", color = Color.White, fontSize = 12.sp)
                    }
                }
                if (request.status == "PENDING") {
                    Button(onClick = onApprove) { Text("Aprobar", fontSize = 12.sp) }
                    OutlinedButton(onClick = onReject, border = BorderStroke(1.dp, Color(0xFFFB7185))) {
                        Text("Rechazar", color = Color(0xFFFFE4E6), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassSegmentButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (selected) 0.5f else 0.2f))
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EmptySocialState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(56.dp))
        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun conversationTypeLabel(type: String): String = when (type.uppercase()) {
    "PET_SIGHTING" -> "Avistamiento de mascota perdida"
    "ADOPTION_INQUIRY" -> "Proceso de adopción"
    "SHELTER_SUPPORT" -> "Contacto con refugio"
    "VET_CONSULT" -> "Consulta veterinaria"
    else -> "Mensaje directo"
}
