package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.OrderHistoryDao
import com.example.data.dao.OrderItemDao
import com.example.data.dao.ProductDao
import com.example.data.dao.TableDao
import com.example.data.dao.WaiterDao
import com.example.data.model.CategoryEntity
import com.example.data.model.OrderHistoryEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.ProductEntity
import com.example.data.model.TableEntity
import com.example.data.model.WaiterEntity

@Database(
    entities = [
        TableEntity::class,
        WaiterEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        OrderItemEntity::class,
        OrderHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tableDao(): TableDao
    abstract fun waiterDao(): WaiterDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun orderHistoryDao(): OrderHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "consumer_pdv_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
