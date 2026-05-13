package com.example.halifaxtransit.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.halifaxtransit.models.BusStop
import kotlinx.coroutines.flow.Flow

@Dao
interface BusStopDao {

    @Query("SELECT * FROM BusStops")
    fun getAll(): Flow<List<BusStop>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<BusStop>)
}
