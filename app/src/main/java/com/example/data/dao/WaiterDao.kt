package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WaiterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaiterDao {
    @Query("SELECT * FROM waiters ORDER BY name ASC")
    fun getAllWaiters(): Flow<List<WaiterEntity>>

    @Query("SELECT * FROM waiters WHERE id = :id")
    suspend fun getWaiterById(id: Long): WaiterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaiter(waiter: WaiterEntity): Long

    @Update
    suspend fun updateWaiter(waiter: WaiterEntity)

    @Delete
    suspend fun deleteWaiter(waiter: WaiterEntity)
}
