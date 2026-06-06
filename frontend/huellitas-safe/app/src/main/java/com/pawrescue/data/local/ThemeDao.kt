package com.pawrescue.data.local

import androidx.room.*
import com.pawrescue.data.model.AppTheme
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {
    @Query("SELECT * FROM app_themes WHERE id = 0")
    fun getSelectedTheme(): Flow<AppTheme?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setTheme(theme: AppTheme)
}
