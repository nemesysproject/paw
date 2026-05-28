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

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar base de datos SQLite (Room)
        val database = AppDatabase.getDatabase(this)
        val repository = PetRepository(database.petRegistrationDao())
        
        // Instanciar el ViewModel
        val viewModel = ViewModelProvider(
            this,
            PetViewModelFactory(repository)
        )[PetViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
