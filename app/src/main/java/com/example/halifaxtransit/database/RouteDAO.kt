package com.example.halifaxtransit.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.example.halifaxtransit.models.Route

@Dao
interface RoutesDao {

    @Query("SELECT * FROM Routes")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<Route>>

    @Update
    suspend fun updateRoute(route: Route)

    @Query("UPDATE Routes SET Highlights = :highlight WHERE route_id = :id")
    suspend fun setHighlight(id: String, highlight: Boolean)

    @Query("UPDATE Routes SET favourite = :fav WHERE route_id = :id")
    suspend fun setFavourite(id: String, fav: Boolean)
}
