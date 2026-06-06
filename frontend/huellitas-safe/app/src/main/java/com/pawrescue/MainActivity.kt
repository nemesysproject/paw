package com.pawrescue

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.pawrescue.data.local.AppDatabase
import com.pawrescue.ui.screens.MainScreen
import com.pawrescue.ui.theme.MyApplicationTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar base de datos SQLite (Room) y Capa de Red
        val database = AppDatabase.getDatabase(this)
        val apiService = com.pawrescue.data.api.RetrofitClient.getApiService(this)
        val repository = com.pawrescue.data.repository.PetRepository(
            database.petRegistrationDao(), 
            database.themeDao(), 
            database.catalogDao(), 
            apiService
        )
        val sessionManager = com.pawrescue.data.local.SessionManager(this)
        
        // Instanciar el ViewModel
        val viewModel = ViewModelProvider(
            this,
            com.pawrescue.ui.viewmodel.PetViewModelFactory(repository, sessionManager)
        )[com.pawrescue.ui.viewmodel.PetViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
            MyApplicationTheme(themeName = currentTheme) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
