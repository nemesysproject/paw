package com.pawrescue.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pawrescue.data.model.PetRegistration
import kotlinx.coroutines.flow.Flow

@Dao
interface PetRegistrationDao {
    @Query("SELECT * FROM pet_registrations ORDER BY timestamp DESC")
    fun getAllRegistrationsFlow(): Flow<List<PetRegistration>>

    @Query("SELECT * FROM pet_registrations WHERE isSynced = 0")
    suspend fun getUnsyncedRegistrations(): List<PetRegistration>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: PetRegistration): Long

    @Update
    suspend fun updateRegistration(registration: PetRegistration)

    @Query("UPDATE pet_registrations SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("UPDATE pet_registrations SET remoteId = :remoteId, isSynced = 1 WHERE id = :id")
    suspend fun updateRemoteIdAndSync(id: Int, remoteId: String)

    @Query("SELECT * FROM pet_registrations WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): PetRegistration?

    @Query("DELETE FROM pet_registrations WHERE id = :id")
    suspend fun deleteRegistration(id: Int)

    @Query("DELETE FROM pet_registrations WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: String)
}
