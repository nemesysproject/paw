package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.PetRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PetViewModel
import com.example.ui.viewmodel.PetViewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar base de datos SQLite (Room) y Capa de Red
        val database = AppDatabase.getDatabase(this)
        val apiService = com.example.data.api.RetrofitClient.getApiService(this)
        val repository = com.example.data.repository.PetRepository(
            database.petRegistrationDao(), 
            database.themeDao(), 
            database.catalogDao(), 
            apiService
        )
        val sessionManager = com.example.data.local.SessionManager(this)
        
        // Instanciar el ViewModel
        val viewModel = ViewModelProvider(
            this,
            com.example.ui.viewmodel.PetViewModelFactory(repository, sessionManager)
        )[com.example.ui.viewmodel.PetViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(themeName = currentTheme) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
