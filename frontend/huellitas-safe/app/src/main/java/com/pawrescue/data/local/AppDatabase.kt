package com.pawrescue.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pawrescue.data.model.*

@Database(
    entities = [
        PetRegistration::class,
        AppTheme::class,
        SpeciesDto::class,
        BreedDto::class,
        GenderDto::class,
        StatusDto::class,
        AdFeedItemDto::class,
        PetSightingEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        AdoptionRequestEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petRegistrationDao(): PetRegistrationDao
    abstract fun themeDao(): ThemeDao
    abstract fun catalogDao(): CatalogDao
    abstract fun adDao(): AdDao
    abstract fun socialDao(): SocialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mascotas_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
