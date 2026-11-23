package com.pawrescue.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pawrescue.data.model.AdFeedItemDto
import kotlinx.coroutines.flow.Flow

@Dao
interface AdDao {
    @Query("SELECT * FROM ad_campaigns ORDER BY priority DESC")
    fun getAllAdsFlow(): Flow<List<AdFeedItemDto>>

    @Query("SELECT * FROM ad_campaigns WHERE placement = :placement ORDER BY priority DESC")
    fun getAdsByPlacementFlow(placement: String): Flow<List<AdFeedItemDto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAds(ads: List<AdFeedItemDto>)

    @Query("DELETE FROM ad_campaigns")
    suspend fun clearAds()

    @Transaction
    suspend fun replaceAllAds(ads: List<AdFeedItemDto>) {
        clearAds()
        if (ads.isNotEmpty()) {
            insertAds(ads)
        }
    }
}
