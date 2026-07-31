package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.OrderHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderHistoryDao {
    @Query("SELECT * FROM order_history ORDER BY closedAt DESC")
    fun getAllHistory(): Flow<List<OrderHistoryEntity>>

    @Query("SELECT * FROM order_history WHERE waiterId = :waiterId ORDER BY closedAt DESC")
    fun getHistoryByWaiter(waiterId: Long): Flow<List<OrderHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: OrderHistoryEntity): Long

    @Query("DELETE FROM order_history WHERE id = :historyId")
    suspend fun deleteHistoryById(historyId: Long)

    @Query("DELETE FROM order_history")
    suspend fun clearAllHistory()
}
