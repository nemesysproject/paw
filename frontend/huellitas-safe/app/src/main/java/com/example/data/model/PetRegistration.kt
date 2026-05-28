package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "pet_registrations")
data class PetRegistration(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val photosJson: String, // Comma-separated string or JSON list of Local URIs (max 10)
    val videoPath: String?, // Local video URI/path
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) : Serializable
