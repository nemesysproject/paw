package com.example.data.repository

import com.example.data.local.PetRegistrationDao
import com.example.data.model.PetRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class PetRepository(private val dao: PetRegistrationDao) {

    val allRegistrations: Flow<List<PetRegistration>> = dao.getAllRegistrationsFlow()

    suspend fun insertRegistration(registration: PetRegistration): Long {
        return dao.insertRegistration(registration)
    }

    suspend fun deleteRegistration(id: Int) {
        dao.deleteRegistration(id)
    }

    suspend fun markAsSynced(id: Int) {
        dao.markAsSynced(id)
    }

    /**
     * Sincroniza los registros pendientes con el servidor simulado.
     * Retorna el número de elementos sincronizados con éxito.
     */
    suspend fun syncPendingRegistrations(onProgress: (String) -> Unit): Int {
        val unsynced = dao.getUnsyncedRegistrations()
        if (unsynced.isEmpty()) {
            return 0
        }

        var successCount = 0
        onProgress("Iniciando sincronización de ${unsynced.size} registros...")
        
        for (pet in unsynced) {
            onProgress("Sincronizando: ${pet.name}...")
            // Simulamos delay de red
            delay(1500)
            
            // Actualizar estado local a sincronizado
            dao.markAsSynced(pet.id)
            successCount++
            onProgress("Mascota '${pet.name}' sincronizada exitosamente.")
        }
        
        onProgress("Sincronización finalizada. $successCount registros sincronizados.")
        return successCount
    }
}
