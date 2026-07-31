package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

object TableStatus {
    const val LIVRE = "LIVRE"
    const val OCUPADA = "OCUPADA"
    const val AGUARDANDO_PAGAMENTO = "AGUARDANDO_PAGAMENTO"
}

@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey val number: Int, // 1 to 50
    val status: String = TableStatus.LIVRE, // LIVRE, OCUPADA, AGUARDANDO_PAGAMENTO
    val activeWaiterId: Long? = null,
    val openedAt: Long? = null
)
