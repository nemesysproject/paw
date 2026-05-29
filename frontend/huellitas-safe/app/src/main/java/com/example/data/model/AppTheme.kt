package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_themes")
data class AppTheme(
    @PrimaryKey val id: Int = 0, // We only store one active theme
    val themeName: String = "Premium Dark" // "Warm", "Premium Dark", "Midnight"
)
