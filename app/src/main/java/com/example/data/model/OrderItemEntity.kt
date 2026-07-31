package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableNumber: Int,
    val productId: Long,
    val productName: String,
    val unitPrice: Double,
    val quantity: Int = 1,
    val notes: String = "",
    val orderedAt: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val historyId: Long? = null
)
