package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

object PaymentMethod {
    const val CREDIT = "Cartão de Crédito"
    const val DEBIT = "Cartão de Débito"
    const val PIX = "PIX"
    const val CASH = "Dinheiro"
}

@Entity(tableName = "order_history")
data class OrderHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableNumber: Int,
    val waiterId: Long,
    val waiterName: String,
    val subtotal: Double,
    val serviceFeePercentage: Double,
    val serviceFeeAmount: Double,
    val discountType: String, // "NONE", "VALUE", "PERCENT"
    val discountValue: Double,
    val totalAmount: Double,
    val paymentMethod: String,
    val closedAt: Long = System.currentTimeMillis(),
    val itemCount: Int = 0
)
