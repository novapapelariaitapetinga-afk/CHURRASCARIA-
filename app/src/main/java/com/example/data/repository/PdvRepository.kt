package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.dao.OrderHistoryDao
import com.example.data.dao.OrderItemDao
import com.example.data.dao.ProductDao
import com.example.data.dao.TableDao
import com.example.data.dao.WaiterDao
import com.example.data.model.CategoryEntity
import com.example.data.model.OrderHistoryEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductEntity
import com.example.data.model.TableEntity
import com.example.data.model.TableStatus
import com.example.data.model.WaiterEntity
import com.example.data.sync.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class PdvRepository(
    private val tableDao: TableDao,
    private val waiterDao: WaiterDao,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val orderItemDao: OrderItemDao,
    private val orderHistoryDao: OrderHistoryDao,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val cloudSyncManager: CloudSyncManager = CloudSyncManager(tableDao, orderItemDao, coroutineScope)

    val allTables: Flow<List<TableEntity>> = tableDao.getAllTables()
    val allWaiters: Flow<List<WaiterEntity>> = waiterDao.getAllWaiters()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allActiveItems: Flow<List<OrderItemEntity>> = orderItemDao.getAllActiveItems()
    val allHistory: Flow<List<OrderHistoryEntity>> = orderHistoryDao.getAllHistory()

    fun getTableByNumber(number: Int): Flow<TableEntity?> = tableDao.getTableByNumber(number)
    fun getActiveItemsForTable(tableNumber: Int): Flow<List<OrderItemEntity>> = orderItemDao.getActiveItemsForTable(tableNumber)
    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>> = productDao.getProductsByCategory(categoryId)
    fun searchProducts(query: String): Flow<List<ProductEntity>> = productDao.searchProducts(query)
    fun getHistoryByWaiter(waiterId: Long): Flow<List<OrderHistoryEntity>> = orderHistoryDao.getHistoryByWaiter(waiterId)

    suspend fun openTable(tableNumber: Int, waiterId: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        tableDao.updateTableStatus(
            number = tableNumber,
            status = TableStatus.OCUPADA,
            waiterId = waiterId,
            openedAt = now
        )
        cloudSyncManager.syncTableToCloud(
            TableEntity(number = tableNumber, status = TableStatus.OCUPADA, activeWaiterId = waiterId, openedAt = now)
        )
    }

    suspend fun requestBill(tableNumber: Int) = withContext(Dispatchers.IO) {
        val currentTable = tableDao.getTableByNumber(tableNumber).firstOrNull()
        if (currentTable != null) {
            val updated = currentTable.copy(status = TableStatus.AGUARDANDO_PAGAMENTO)
            tableDao.updateTable(updated)
            cloudSyncManager.syncTableToCloud(updated)
        }
    }

    suspend fun addOrderItem(
        tableNumber: Int,
        product: ProductEntity,
        quantity: Int = 1,
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        val item = OrderItemEntity(
            tableNumber = tableNumber,
            productId = product.id,
            productName = product.name,
            unitPrice = product.price,
            quantity = quantity,
            notes = notes,
            orderedAt = System.currentTimeMillis(),
            isPaid = false
        )
        val itemId = orderItemDao.insertOrderItem(item)
        val fullItem = item.copy(id = itemId)
        cloudSyncManager.syncItemToCloud(fullItem)

        // Ensure table status is OCUPADA if items added
        val currentTable = tableDao.getTableByNumber(tableNumber).firstOrNull()
        if (currentTable != null && currentTable.status == TableStatus.LIVRE) {
            val updatedTable = currentTable.copy(status = TableStatus.OCUPADA, openedAt = System.currentTimeMillis())
            tableDao.updateTable(updatedTable)
            cloudSyncManager.syncTableToCloud(updatedTable)
        }
    }

    suspend fun removeOrderItem(item: OrderItemEntity) = withContext(Dispatchers.IO) {
        orderItemDao.deleteOrderItem(item)
        cloudSyncManager.removeItemFromCloud(item)
    }

    suspend fun updateOrderItemQuantity(item: OrderItemEntity, newQuantity: Int) = withContext(Dispatchers.IO) {
        if (newQuantity <= 0) {
            orderItemDao.deleteOrderItem(item)
            cloudSyncManager.removeItemFromCloud(item)
        } else {
            val updatedItem = item.copy(quantity = newQuantity)
            orderItemDao.updateOrderItem(updatedItem)
            cloudSyncManager.syncItemToCloud(updatedItem)
        }
    }

    suspend fun checkoutTable(
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
    ) = withContext(Dispatchers.IO) {
        // 1. Create Order History record
        val history = OrderHistoryEntity(
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
            closedAt = System.currentTimeMillis(),
            itemCount = itemCount
        )
        val historyId = orderHistoryDao.insertHistory(history)

        // 2. Mark active order items as paid attached to historyId
        orderItemDao.markTableItemsPaid(tableNumber, historyId)
        cloudSyncManager.clearTableItemsInCloud(tableNumber)

        // 3. Reset table status back to LIVRE (Verde)
        tableDao.updateTableStatus(
            number = tableNumber,
            status = TableStatus.LIVRE,
            waiterId = null,
            openedAt = null
        )
        cloudSyncManager.syncTableToCloud(
            TableEntity(number = tableNumber, status = TableStatus.LIVRE, activeWaiterId = null, openedAt = null)
        )
    }

    // CRUD Waiters
    suspend fun insertWaiter(waiter: WaiterEntity) = withContext(Dispatchers.IO) {
        waiterDao.insertWaiter(waiter)
    }

    suspend fun updateWaiter(waiter: WaiterEntity) = withContext(Dispatchers.IO) {
        waiterDao.updateWaiter(waiter)
    }

    suspend fun deleteWaiter(waiter: WaiterEntity) = withContext(Dispatchers.IO) {
        waiterDao.deleteWaiter(waiter)
    }

    // CRUD Products
    suspend fun insertProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    // CRUD Categories
    suspend fun insertCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(category)
    }

    // Seed mock data if database is brand new
    suspend fun seedMockDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingTables = tableDao.getAllTables().firstOrNull() ?: emptyList()
        if (existingTables.isNotEmpty()) return@withContext

        // 1. Seed 50 Tables
        val initialTables = (1..50).map { number ->
            TableEntity(number = number, status = TableStatus.LIVRE)
        }
        tableDao.insertTables(initialTables)

        // 2. Seed Waiters
        val carlosId = waiterDao.insertWaiter(WaiterEntity(name = "Carlos Silva", phone = "(11) 98765-4321"))
        val marianaId = waiterDao.insertWaiter(WaiterEntity(name = "Mariana Santos", phone = "(11) 97654-3210"))
        val joaoId = waiterDao.insertWaiter(WaiterEntity(name = "João Oliveira", phone = "(11) 96543-2109"))
        val anaId = waiterDao.insertWaiter(WaiterEntity(name = "Ana Paula Costa", phone = "(11) 95432-1098"))

        // 3. Seed Categories
        val catCarnes = categoryDao.insertCategory(CategoryEntity(name = "Carnes Nobres & Espetos", iconName = "restaurant"))
        val catAcompanhamentos = categoryDao.insertCategory(CategoryEntity(name = "Acompanhamentos", iconName = "tapas"))
        val catBebidas = categoryDao.insertCategory(CategoryEntity(name = "Bebidas & Caipirinhas", iconName = "local_bar"))
        val catSobremesas = categoryDao.insertCategory(CategoryEntity(name = "Sobremesas", iconName = "icecream"))

        // 4. Seed Products
        val p1 = productDao.insertProduct(ProductEntity(name = "Rodízio Completo de Carnes", categoryId = catCarnes, categoryName = "Carnes Nobres & Espetos", price = 89.90, description = "Acesso livre a mais de 15 cortes nobres na mesa, buffet de saladas e pratos quentes"))
        val p2 = productDao.insertProduct(ProductEntity(name = "Picanha Nobre no Espeto (500g)", categoryId = catCarnes, categoryName = "Carnes Nobres & Espetos", price = 119.00, description = "Corte nobre com camada de gordura ideal, assada na brasa e fatiada na mesa"))
        val p3 = productDao.insertProduct(ProductEntity(name = "Cupim Recheado com Queijo (400g)", categoryId = catCarnes, categoryName = "Carnes Nobres & Espetos", price = 78.00, description = "Cupim macio assado em fogo lento com recheio de provolone e chimichurri"))
        val p4 = productDao.insertProduct(ProductEntity(name = "Costela de Chão Gaúcha (600g)", categoryId = catCarnes, categoryName = "Carnes Nobres & Espetos", price = 95.00, description = "Costela janelão assada por 12 horas, desmanchando no osso"))
        val p5 = productDao.insertProduct(ProductEntity(name = "Linguiça Cuiabana com Queijo (300g)", categoryId = catCarnes, categoryName = "Carnes Nobres & Espetos", price = 42.00, description = "Linguiça artesanal com queijo coalho e pimenta biquinho"))

        val p6 = productDao.insertProduct(ProductEntity(name = "Pão de Alho Especial do Bolinha", categoryId = catAcompanhamentos, categoryName = "Acompanhamentos", price = 18.00, description = "Porção com 4 unidades recheadas com requeijão e ervas finas"))
        val p7 = productDao.insertProduct(ProductEntity(name = "Mandioca Frita Crocante com Bacon", categoryId = catAcompanhamentos, categoryName = "Acompanhamentos", price = 32.00, description = "Mandioca amarela macia por dentro e dourada por fora"))
        val p8 = productDao.insertProduct(ProductEntity(name = "Arroz Biro-Biro", categoryId = catAcompanhamentos, categoryName = "Acompanhamentos", price = 24.00, description = "Arroz soltinho com ovos, batata palha, bacon crocante e cheiro-verde"))
        val p9 = productDao.insertProduct(ProductEntity(name = "Farofa Especial com Torresmo", categoryId = catAcompanhamentos, categoryName = "Acompanhamentos", price = 19.50, description = "Farofa de mandioca na manteiga de garrafa com bits de torresmo"))

        val p10 = productDao.insertProduct(ProductEntity(name = "Chopp Amstel 500ml Caneca Zero", categoryId = catBebidas, categoryName = "Bebidas & Caipirinhas", price = 14.90, description = "Chopp estritamente gelado servido em caneca congelada"))
        val p11 = productDao.insertProduct(ProductEntity(name = "Caipirinha Tradicional de Cachaça", categoryId = catBebidas, categoryName = "Bebidas & Caipirinhas", price = 22.00, description = "Limão taiti, açúcar e cachaça envelhecida"))
        val p12 = productDao.insertProduct(ProductEntity(name = "Caipiroska de Frutas Vermelhas", categoryId = catBebidas, categoryName = "Bebidas & Caipirinhas", price = 28.00, description = "Vodka premium, morango, amora e mirtilo frescos"))
        val p13 = productDao.insertProduct(ProductEntity(name = "Guaraná Antarctica 350ml", categoryId = catBebidas, categoryName = "Bebidas & Caipirinhas", price = 7.50, description = "Lata trincando de gelada"))
        val p14 = productDao.insertProduct(ProductEntity(name = "Suco Natural de Abacaxi com Hortelã", categoryId = catBebidas, categoryName = "Bebidas & Caipirinhas", price = 13.00, description = "Suco natural preparado na hora"))

        val p15 = productDao.insertProduct(ProductEntity(name = "Pudim de Leite do Bolinha", categoryId = catSobremesas, categoryName = "Sobremesas", price = 16.00, description = "Super cremoso, sem furinhos, com calda caseira de caramelo"))
        val p16 = productDao.insertProduct(ProductEntity(name = "Abacaxi Assado com Canela & Raspas de Limão", categoryId = catSobremesas, categoryName = "Sobremesas", price = 18.00, description = "Fatia de abacaxi dourada na churrasqueira servida com sorvete de creme"))

        // 5. Pre-occupy sample tables for instant rich testing UI
        // Table 2 -> Ocupada (Carlos)
        tableDao.updateTableStatus(number = 2, status = TableStatus.OCUPADA, waiterId = carlosId, openedAt = System.currentTimeMillis() - 1000 * 60 * 45)
        orderItemDao.insertOrderItem(OrderItemEntity(tableNumber = 2, productId = p2, productName = "Picanha Nobre no Espeto (500g)", unitPrice = 119.00, quantity = 1))
        orderItemDao.insertOrderItem(OrderItemEntity(tableNumber = 2, productId = p10, productName = "Chopp Amstel 500ml Caneca Zero", unitPrice = 14.90, quantity = 3))
        orderItemDao.insertOrderItem(OrderItemEntity(tableNumber = 2, productId = p6, productName = "Pão de Alho Especial do Bolinha", unitPrice = 18.00, quantity = 2))

        // Table 5 -> Ocupada (Mariana)
        tableDao.updateTableStatus(number = 5, status = TableStatus.OCUPADA, waiterId = marianaId, openedAt = System.currentTimeMillis() - 1000 * 60 * 20)
        orderItemDao.insertOrderItem(OrderItemEntity(tableNumber = 5, productId = p1, productName = "Rodízio Completo de Carnes", unitPrice = 89.90, quantity = 2))
        orderItemDao.insertOrderItem(OrderItemEntity(tableNumber = 5, productId = p11, productName = "Caipirinha Tradicional de Cachaça", unitPrice = 22.00, quantity = 2))

        // Table 8 -> Aguardando Pagamento (João)
        tableDao.updateTableStatus(number = 8, status = TableStatus.AGUARDANDO_PAGAMENTO, waiterId = joaoId, openedAt = System.currentTimeMillis() - 1000 * 60 * 60)
        orderItemDao.insertOrderItem(OrderItemEntity(tableNumber = 8, productId = p3, productName = "Cupim Recheado com Queijo (400g)", unitPrice = 78.00, quantity = 1))
        orderItemDao.insertOrderItem(OrderItemEntity(tableNumber = 8, productId = p8, productName = "Arroz Biro-Biro", unitPrice = 24.00, quantity = 1))
        orderItemDao.insertOrderItem(OrderItemEntity(tableNumber = 8, productId = p15, productName = "Pudim de Leite do Bolinha", unitPrice = 16.00, quantity = 1))

        // Table 12 -> Ocupada (Ana Paula)
        tableDao.updateTableStatus(number = 12, status = TableStatus.OCUPADA, waiterId = anaId, openedAt = System.currentTimeMillis() - 1000 * 60 * 15)
        orderItemDao.insertOrderItem(OrderItemEntity(tableNumber = 12, productId = p5, productName = "Linguiça Cuiabana com Queijo (300g)", unitPrice = 42.00, quantity = 1))
        orderItemDao.insertOrderItem(OrderItemEntity(tableNumber = 12, productId = p10, productName = "Chopp Amstel 500ml Caneca Zero", unitPrice = 14.90, quantity = 4))

        // 6. Pre-insert sample order histories for waiter commissions report demo
        orderHistoryDao.insertHistory(
            OrderHistoryEntity(
                tableNumber = 1,
                waiterId = carlosId,
                waiterName = "Carlos Silva",
                subtotal = 110.00,
                serviceFeePercentage = 10.0,
                serviceFeeAmount = 11.00,
                discountType = "NONE",
                discountValue = 0.0,
                totalAmount = 121.00,
                paymentMethod = PaymentMethod.PIX,
                closedAt = System.currentTimeMillis() - 1000 * 60 * 120,
                itemCount = 4
            )
        )
        orderHistoryDao.insertHistory(
            OrderHistoryEntity(
                tableNumber = 3,
                waiterId = marianaId,
                waiterName = "Mariana Santos",
                subtotal = 85.00,
                serviceFeePercentage = 10.0,
                serviceFeeAmount = 8.50,
                discountType = "NONE",
                discountValue = 0.0,
                totalAmount = 93.50,
                paymentMethod = PaymentMethod.CREDIT,
                closedAt = System.currentTimeMillis() - 1000 * 60 * 90,
                itemCount = 3
            )
        )
        orderHistoryDao.insertHistory(
            OrderHistoryEntity(
                tableNumber = 7,
                waiterId = joaoId,
                waiterName = "João Oliveira",
                subtotal = 140.00,
                serviceFeePercentage = 10.0,
                serviceFeeAmount = 14.00,
                discountType = "PERCENT",
                discountValue = 5.0, // 5% discount
                totalAmount = 147.00,
                paymentMethod = PaymentMethod.DEBIT,
                closedAt = System.currentTimeMillis() - 1000 * 60 * 40,
                itemCount = 5
            )
        )
    }

    suspend fun deleteHistoryItem(historyId: Long) = withContext(Dispatchers.IO) {
        orderHistoryDao.deleteHistoryById(historyId)
        orderItemDao.deleteItemsForHistory(historyId)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        orderHistoryDao.clearAllHistory()
        orderItemDao.clearPaidItems()
    }
}
