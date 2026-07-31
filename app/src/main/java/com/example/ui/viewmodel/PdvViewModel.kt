package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.OrderHistoryEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.ProductEntity
import com.example.data.model.TableEntity
import com.example.data.model.WaiterEntity
import com.example.data.repository.PdvRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PdvViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PdvRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PdvRepository(
            tableDao = db.tableDao(),
            waiterDao = db.waiterDao(),
            categoryDao = db.categoryDao(),
            productDao = db.productDao(),
            orderItemDao = db.orderItemDao(),
            orderHistoryDao = db.orderHistoryDao()
        )

        viewModelScope.launch {
            repository.seedMockDataIfEmpty()
        }
    }

    val tables: StateFlow<List<TableEntity>> = repository.allTables
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waiters: StateFlow<List<WaiterEntity>> = repository.allWaiters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeItems: StateFlow<List<OrderItemEntity>> = repository.allActiveItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<OrderHistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter states
    private val _tableFilterStatus = MutableStateFlow<String>("TODOS") // TODOS, LIVRE, OCUPADA, AGUARDANDO_PAGAMENTO
    val tableFilterStatus: StateFlow<String> = _tableFilterStatus.asStateFlow()

    private val _tableSearchQuery = MutableStateFlow("")
    val tableSearchQuery: StateFlow<String> = _tableSearchQuery.asStateFlow()

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null) // null = ALL
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    // Sync Toggles State
    private val _isWifiSyncEnabled = MutableStateFlow(true)
    val isWifiSyncEnabled: StateFlow<Boolean> = _isWifiSyncEnabled.asStateFlow()

    private val _isMultiDeviceEnabled = MutableStateFlow(true)
    val isMultiDeviceEnabled: StateFlow<Boolean> = _isMultiDeviceEnabled.asStateFlow()

    fun toggleWifiSync(enabled: Boolean) {
        _isWifiSyncEnabled.value = enabled
    }

    fun toggleMultiDevice(enabled: Boolean) {
        _isMultiDeviceEnabled.value = enabled
    }

    // Active Selected Table Modal state
    private val _selectedTableNumber = MutableStateFlow<Int?>(null)
    val selectedTableNumber: StateFlow<Int?> = _selectedTableNumber.asStateFlow()

    // Checkout modal state
    private val _checkoutTableNumber = MutableStateFlow<Int?>(null)
    val checkoutTableNumber: StateFlow<Int?> = _checkoutTableNumber.asStateFlow()

    // Filtered Tables
    val filteredTables: StateFlow<List<TableEntity>> = combine(
        tables,
        _tableFilterStatus,
        _tableSearchQuery
    ) { tablesList, statusFilter, query ->
        tablesList.filter { table ->
            val matchesStatus = when (statusFilter) {
                "LIVRE" -> table.status == "LIVRE"
                "OCUPADA" -> table.status == "OCUPADA"
                "AGUARDANDO_PAGAMENTO" -> table.status == "AGUARDANDO_PAGAMENTO"
                else -> true
            }
            val matchesQuery = query.isEmpty() || table.number.toString().contains(query)
            matchesStatus && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Products
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        products,
        _selectedCategoryId,
        _productSearchQuery
    ) { productList, catId, query ->
        productList.filter { product ->
            val matchesCategory = (catId == null || product.categoryId == catId)
            val matchesQuery = query.isEmpty() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Setters for filters
    fun setTableFilterStatus(status: String) {
        _tableFilterStatus.value = status
    }

    fun setTableSearchQuery(query: String) {
        _tableSearchQuery.value = query
    }

    fun setProductSearchQuery(query: String) {
        _productSearchQuery.value = query
    }

    fun setSelectedCategoryId(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setSelectedTableNumber(number: Int?) {
        _selectedTableNumber.value = number
    }

    fun setCheckoutTableNumber(number: Int?) {
        _checkoutTableNumber.value = number
    }

    // Actions
    fun openTable(tableNumber: Int, waiterId: Long) {
        viewModelScope.launch {
            repository.openTable(tableNumber, waiterId)
        }
    }

    fun requestBill(tableNumber: Int) {
        viewModelScope.launch {
            repository.requestBill(tableNumber)
        }
    }

    fun addOrderItem(tableNumber: Int, product: ProductEntity, quantity: Int = 1, notes: String = "") {
        viewModelScope.launch {
            repository.addOrderItem(tableNumber, product, quantity, notes)
        }
    }

    fun removeOrderItem(item: OrderItemEntity) {
        viewModelScope.launch {
            repository.removeOrderItem(item)
        }
    }

    fun updateOrderItemQuantity(item: OrderItemEntity, newQuantity: Int) {
        viewModelScope.launch {
            repository.updateOrderItemQuantity(item, newQuantity)
        }
    }

    fun checkoutTable(
        tableNumber: Int,
        waiterId: Long,
        waiterName: String,
        subtotal: Double,
        serviceFeePercentage: Double,
        serviceFeeAmount: Double,
        discountType: String,
        discountValue: Double,
        totalAmount: Double,
        paymentMethod: String,
        itemCount: Int
    ) {
        viewModelScope.launch {
            repository.checkoutTable(
                tableNumber = tableNumber,
                waiterId = waiterId,
                waiterName = waiterName,
                subtotal = subtotal,
                serviceFeePercentage = serviceFeePercentage,
                serviceFeeAmount = serviceFeeAmount,
                discountType = discountType,
                discountValue = discountValue,
                totalAmount = totalAmount,
                paymentMethod = paymentMethod,
                itemCount = itemCount
            )
            _checkoutTableNumber.value = null
            _selectedTableNumber.value = null
        }
    }

    // Waiter actions
    fun saveWaiter(id: Long = 0, name: String, phone: String) {
        viewModelScope.launch {
            if (id == 0L) {
                repository.insertWaiter(WaiterEntity(name = name, phone = phone))
            } else {
                repository.updateWaiter(WaiterEntity(id = id, name = name, phone = phone))
            }
        }
    }

    fun deleteWaiter(waiter: WaiterEntity) {
        viewModelScope.launch {
            repository.deleteWaiter(waiter)
        }
    }

    // Product actions
    fun saveProduct(
        id: Long = 0,
        name: String,
        categoryId: Long,
        categoryName: String,
        price: Double,
        description: String
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                repository.insertProduct(
                    ProductEntity(
                        name = name,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        price = price,
                        description = description
                    )
                )
            } else {
                repository.updateProduct(
                    ProductEntity(
                        id = id,
                        name = name,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        price = price,
                        description = description
                    )
                )
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Category actions
    fun saveCategory(name: String, iconName: String) {
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name = name, iconName = iconName))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // History actions
    fun deleteHistoryItem(historyId: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(historyId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
}
