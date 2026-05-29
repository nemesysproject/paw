package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "pet_registrations")
data class PetRegistration(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val remoteId: String? = null,
    val name: String,
    val description: String,
    val gender: String = "DESCONOCIDO",
    val status: String = "DESCONOCIDO",
    val speciesId: String? = null,
    val breedId: String? = null,
    val photosJson: String,
    val videoPath: String?,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) : Serializable
