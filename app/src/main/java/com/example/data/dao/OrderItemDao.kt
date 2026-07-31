package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderItemDao {
    @Query("SELECT * FROM order_items WHERE tableNumber = :tableNumber AND isPaid = 0 ORDER BY orderedAt ASC")
    fun getActiveItemsForTable(tableNumber: Int): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE isPaid = 0")
    fun getAllActiveItems(): Flow<List<OrderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity): Long

    @Update
    suspend fun updateOrderItem(item: OrderItemEntity)

    @Delete
    suspend fun deleteOrderItem(item: OrderItemEntity)

    @Query("DELETE FROM order_items WHERE tableNumber = :tableNumber AND isPaid = 0")
    suspend fun clearActiveItemsForTable(tableNumber: Int)

    @Query("UPDATE order_items SET isPaid = 1, historyId = :historyId WHERE tableNumber = :tableNumber AND isPaid = 0")
    suspend fun markTableItemsPaid(tableNumber: Int, historyId: Long)

    @Query("DELETE FROM order_items WHERE historyId = :historyId")
    suspend fun deleteItemsForHistory(historyId: Long)

    @Query("DELETE FROM order_items WHERE isPaid = 1")
    suspend fun clearPaidItems()
}
