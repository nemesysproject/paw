package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM species")
    fun getAllSpeciesFlow(): Flow<List<SpeciesDto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecies(species: List<SpeciesDto>)

    @Query("SELECT * FROM breeds")
    fun getAllBreedsFlow(): Flow<List<BreedDto>>

    @Query("SELECT * FROM breeds WHERE speciesId = :speciesId")
    fun getBreedsBySpeciesFlow(speciesId: String): Flow<List<BreedDto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreeds(breeds: List<BreedDto>)

    @Query("SELECT * FROM genders")
    fun getAllGendersFlow(): Flow<List<GenderDto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenders(genders: List<GenderDto>)

    @Query("SELECT * FROM statuses")
    fun getAllStatusesFlow(): Flow<List<StatusDto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatuses(statuses: List<StatusDto>)
}
