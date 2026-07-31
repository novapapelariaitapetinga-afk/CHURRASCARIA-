package com.example.data.sync

import android.util.Log
import com.example.data.dao.OrderItemDao
import com.example.data.dao.TableDao
import com.example.data.model.OrderItemEntity
import com.example.data.model.TableEntity
import com.example.data.model.TableStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CloudSyncManager(
    private val tableDao: TableDao,
    private val orderItemDao: OrderItemDao,
    private val scope: CoroutineScope
) {
    private val TAG = "CloudSyncManager"
    private var firestore: FirebaseFirestore? = null
    private var tablesListener: ListenerRegistration? = null
    private var itemsListener: ListenerRegistration? = null

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
            startRealtimeListeners()
        } catch (t: Throwable) {
            Log.w(TAG, "Firebase Firestore not initialized or unavailable: ${t.message}")
            firestore = null
        }
    }

    private fun startRealtimeListeners() {
        val db = firestore ?: return

        // 1. Listen for Table Status changes in Cloud
        tablesListener = db.collection("churrascaria_tables")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || snapshots.metadata.hasPendingWrites()) {
                    return@addSnapshotListener
                }

                if (!snapshots.isEmpty) {
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshots.documents) {
                            try {
                                val number = doc.getLong("number")?.toInt() ?: continue
                                val statusStr = doc.getString("status") ?: TableStatus.LIVRE
                                val waiterId = doc.getLong("activeWaiterId")
                                val openedAt = doc.getLong("openedAt")

                                tableDao.updateTableStatus(
                                    number = number,
                                    status = statusStr,
                                    waiterId = waiterId,
                                    openedAt = openedAt
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing table doc: ${e.message}")
                            }
                        }
                    }
                }
            }

        // 2. Listen for Active Order Items in Cloud
        itemsListener = db.collection("churrascaria_active_items")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || snapshots.metadata.hasPendingWrites()) {
                    return@addSnapshotListener
                }

                scope.launch(Dispatchers.IO) {
                    val cloudItems = mutableListOf<OrderItemEntity>()
                    for (doc in snapshots.documents) {
                        try {
                            val id = doc.getLong("id") ?: continue
                            val tableNumber = doc.getLong("tableNumber")?.toInt() ?: continue
                            val productId = doc.getLong("productId") ?: continue
                            val productName = doc.getString("productName") ?: ""
                            val unitPrice = doc.getDouble("unitPrice") ?: 0.0
                            val quantity = doc.getLong("quantity")?.toInt() ?: 1
                            val notes = doc.getString("notes") ?: ""
                            val orderedAt = doc.getLong("orderedAt") ?: System.currentTimeMillis()
                            val isPaid = doc.getBoolean("isPaid") ?: false

                            cloudItems.add(
                                OrderItemEntity(
                                    id = id,
                                    tableNumber = tableNumber,
                                    productId = productId,
                                    productName = productName,
                                    unitPrice = unitPrice,
                                    quantity = quantity,
                                    notes = notes,
                                    orderedAt = orderedAt,
                                    isPaid = isPaid
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing item doc: ${e.message}")
                        }
                    }

                    // Sync cloud items to local Room DB
                    for (item in cloudItems) {
                        orderItemDao.insertOrderItem(item)
                    }
                }
            }
    }

    // --- Cloud Writers called from Repository ---

    fun syncTableToCloud(table: TableEntity) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "number" to table.number,
                    "status" to table.status,
                    "activeWaiterId" to table.activeWaiterId,
                    "openedAt" to table.openedAt,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("churrascaria_tables")
                    .document("mesa_${table.number}")
                    .set(data)
            } catch (e: Exception) {
                Log.e(TAG, "Failed syncTableToCloud: ${e.message}")
            }
        }
    }

    fun syncItemToCloud(item: OrderItemEntity) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "id" to item.id,
                    "tableNumber" to item.tableNumber,
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "unitPrice" to item.unitPrice,
                    "quantity" to item.quantity,
                    "notes" to item.notes,
                    "orderedAt" to item.orderedAt,
                    "isPaid" to item.isPaid,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("churrascaria_active_items")
                    .document("item_${item.id}")
                    .set(data)
            } catch (e: Exception) {
                Log.e(TAG, "Failed syncItemToCloud: ${e.message}")
            }
        }
    }

    fun removeItemFromCloud(item: OrderItemEntity) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                db.collection("churrascaria_active_items")
                    .document("item_${item.id}")
                    .delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed removeItemFromCloud: ${e.message}")
            }
        }
    }

    fun clearTableItemsInCloud(tableNumber: Int) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val query = db.collection("churrascaria_active_items")
                    .whereEqualTo("tableNumber", tableNumber)
                    .get()
                    .await()

                for (doc in query.documents) {
                    doc.reference.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed clearTableItemsInCloud: ${e.message}")
            }
        }
    }

    fun stop() {
        tablesListener?.remove()
        itemsListener?.remove()
    }
}
