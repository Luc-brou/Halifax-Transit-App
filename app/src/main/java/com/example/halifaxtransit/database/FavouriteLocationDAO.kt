package com.example.halifaxtransit.database

import androidx.room.*
import com.example.halifaxtransit.models.FavouriteLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteLocationDao {

    @Query("SELECT * FROM FavouriteLocations ORDER BY id DESC")
    fun getAll(): Flow<List<FavouriteLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: FavouriteLocation)

    @Delete
    suspend fun delete(location: FavouriteLocation)
}
