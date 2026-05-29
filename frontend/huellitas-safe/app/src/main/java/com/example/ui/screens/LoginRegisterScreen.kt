package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.PetViewModel

@Composable
fun LoginRegisterRedesignScreen(
    viewModel: PetViewModel,
    onBiometricAuthRequested: () -> Unit
) {
    var isLoginTab by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isAuthProcessing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Check if we have credentials stored to show biometric option instead of form
    val savedEmail = viewModel.currentUserEmail
    val hasCredentials = !savedEmail.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // ... (Background blurred circles remain the same)
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopStart)
                .offset(x = (-150).dp, y = (-100).dp)
                .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), Color.Transparent)), CircleShape)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Logo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = "Huellitas Logo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = if (hasCredentials && isLoginTab) "Hola de nuevo" else if (isLoginTab) "Bienvenido de Nuevo" else "Crear Cuenta",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                )

                Text(
                    text = if (hasCredentials && isLoginTab) "Usa tu huella para acceder rápidamente" else if (isLoginTab) "Inicia sesión para continuar protegiendo mascotas" else "Regístrate en la red comunitaria de Huellitas Safe",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                if (hasCredentials && isLoginTab) {
                    // BIOMETRIC QUICK ACCESS VIEW
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable { onBiometricAuthRequested() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometría",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        Text(
                            text = savedEmail ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )

                        TextButton(onClick = { viewModel.clearStoredCredentials() }) {
                            Text("Usar otra cuenta", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    // STANDARD LOGIN/REGISTER FORM
                    // Tab Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { isLoginTab = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLoginTab) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Acceder", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isLoginTab = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isLoginTab) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Registrarse", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Error Message
                    authError?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Input Fields
                    if (!isLoginTab) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre Completo", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (isLoginTab) {
                                viewModel.login(email, password) {
                                    Toast.makeText(context, "Sesión iniciada con éxito", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel.register(name, email, password) {
                                    Toast.makeText(context, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)), RoundedCornerShape(16.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(if (isLoginTab) "Iniciar Sesión" else "Registrar Cuenta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
