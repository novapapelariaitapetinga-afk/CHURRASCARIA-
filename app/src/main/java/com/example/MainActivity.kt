package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TableStatus
import com.example.ui.components.PdvBottomNavigation
import com.example.ui.components.PdvScreen
import com.example.ui.components.PdvTopAppBar
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.CheckoutDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrderHistoryScreen
import com.example.ui.screens.ProductSelectorDialog
import com.example.ui.screens.TableDetailDialog
import com.example.ui.screens.TablesScreen
import com.example.ui.screens.WaitersScreen
import com.example.ui.theme.ConsumerPdvTheme
import com.example.ui.viewmodel.PdvViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PdvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            ConsumerPdvTheme(darkTheme = isDarkTheme) {
                ConsumerPdvApp(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

@Composable
fun ConsumerPdvApp(
    viewModel: PdvViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(PdvScreen.INICIO) }

    // State observation
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val filteredTables by viewModel.filteredTables.collectAsStateWithLifecycle()
    val waiters by viewModel.waiters.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val activeItems by viewModel.activeItems.collectAsStateWithLifecycle()
    val historyList by viewModel.history.collectAsStateWithLifecycle()

    val tableFilterStatus by viewModel.tableFilterStatus.collectAsStateWithLifecycle()
    val tableSearchQuery by viewModel.tableSearchQuery.collectAsStateWithLifecycle()
    val productSearchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()

    val selectedTableNumber by viewModel.selectedTableNumber.collectAsStateWithLifecycle()
    val checkoutTableNumber by viewModel.checkoutTableNumber.collectAsStateWithLifecycle()

    val isWifiSyncEnabled by viewModel.isWifiSyncEnabled.collectAsStateWithLifecycle()
    val isMultiDeviceEnabled by viewModel.isMultiDeviceEnabled.collectAsStateWithLifecycle()

    var showProductSelectorForTable by remember { mutableStateOf<Int?>(null) }

    val activeTablesCount = tables.count { it.status == TableStatus.OCUPADA }
    val waitingBillCount = tables.count { it.status == TableStatus.AGUARDANDO_PAGAMENTO }

    Scaffold(
        topBar = {
            PdvTopAppBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                activeTablesCount = activeTablesCount,
                waitingBillCount = waitingBillCount
            )
        },
        bottomBar = {
            PdvBottomNavigation(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                PdvScreen.INICIO -> {
                    HomeScreen(
                        tables = tables,
                        activeItems = activeItems,
                        historyList = historyList,
                        isWifiSyncEnabled = isWifiSyncEnabled,
                        isMultiDeviceEnabled = isMultiDeviceEnabled,
                        onToggleWifiSync = { viewModel.toggleWifiSync(it) },
                        onToggleMultiDevice = { viewModel.toggleMultiDevice(it) },
                        onNavigateToScreen = { currentScreen = it },
                        onOpenTableDirectly = { tableNum ->
                            currentScreen = PdvScreen.MESAS
                            viewModel.setSelectedTableNumber(tableNum)
                        }
                    )
                }

                PdvScreen.MESAS -> {
                    TablesScreen(
                        tables = filteredTables,
                        waiters = waiters,
                        activeItems = activeItems,
                        filterStatus = tableFilterStatus,
                        onFilterStatusChange = { viewModel.setTableFilterStatus(it) },
                        searchQuery = tableSearchQuery,
                        onSearchQueryChange = { viewModel.setTableSearchQuery(it) },
                        onTableClick = { viewModel.setSelectedTableNumber(it) }
                    )
                }

                PdvScreen.CARDAPIO -> {
                    CatalogScreen(
                        products = filteredProducts,
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onSelectCategory = { viewModel.setSelectedCategoryId(it) },
                        searchQuery = productSearchQuery,
                        onSearchChange = { viewModel.setProductSearchQuery(it) },
                        onSaveProduct = { id, name, categoryId, categoryName, price, description ->
                            viewModel.saveProduct(id, name, categoryId, categoryName, price, description)
                        },
                        onDeleteProduct = { viewModel.deleteProduct(it) }
                    )
                }

                PdvScreen.GARCONS -> {
                    WaitersScreen(
                        waiters = waiters,
                        historyList = historyList,
                        onSaveWaiter = { id, name, phone -> viewModel.saveWaiter(id, name, phone) },
                        onDeleteWaiter = { viewModel.deleteWaiter(it) }
                    )
                }

                PdvScreen.HISTORICO -> {
                    OrderHistoryScreen(
                        historyList = historyList,
                        onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
                        onClearAllHistory = { viewModel.clearAllHistory() }
                    )
                }
            }

            // Dialogs
            // 1. Table Detail Dialog
            selectedTableNumber?.let { tableNum ->
                val tableObj = tables.find { it.number == tableNum }
                val tableActiveItems = activeItems.filter { it.tableNumber == tableNum }

                if (tableObj != null) {
                    TableDetailDialog(
                        table = tableObj,
                        waiters = waiters,
                        activeItems = tableActiveItems,
                        onDismiss = { viewModel.setSelectedTableNumber(null) },
                        onOpenTable = { num, waiterId -> viewModel.openTable(num, waiterId) },
                        onRequestBill = { num -> viewModel.requestBill(num) },
                        onAddItemsClick = { showProductSelectorForTable = tableNum },
                        onUpdateQuantity = { item, newQty -> viewModel.updateOrderItemQuantity(item, newQty) },
                        onRemoveItem = { item -> viewModel.removeOrderItem(item) },
                        onProceedCheckout = { num -> viewModel.setCheckoutTableNumber(num) }
                    )
                }
            }

            // 2. Product Selector for active table
            showProductSelectorForTable?.let { tableNum ->
                ProductSelectorDialog(
                    tableNumber = tableNum,
                    products = filteredProducts,
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onSelectCategory = { viewModel.setSelectedCategoryId(it) },
                    searchQuery = productSearchQuery,
                    onSearchChange = { viewModel.setProductSearchQuery(it) },
                    onAddProductToTable = { product, qty, notes ->
                        viewModel.addOrderItem(tableNum, product, qty, notes)
                    },
                    onDismiss = { showProductSelectorForTable = null }
                )
            }

            // 3. Checkout Dialog
            checkoutTableNumber?.let { tableNum ->
                val tableObj = tables.find { it.number == tableNum }
                val tableActiveItems = activeItems.filter { it.tableNumber == tableNum }

                if (tableObj != null) {
                    CheckoutDialog(
                        table = tableObj,
                        waiters = waiters,
                        activeItems = tableActiveItems,
                        onDismiss = { viewModel.setCheckoutTableNumber(null) },
                        onConfirmCheckout = { number, waiterId, waiterName, subtotal, serviceFeePct, serviceFeeAmt, discType, discVal, total, payMethod, itemCount ->
                            viewModel.checkoutTable(
                                tableNumber = number,
                                waiterId = waiterId,
                                waiterName = waiterName,
                                subtotal = subtotal,
                                serviceFeePercentage = serviceFeePct,
                                serviceFeeAmount = serviceFeeAmt,
                                discountType = discType,
                                discountValue = discVal,
                                totalAmount = total,
                                paymentMethod = payMethod,
                                itemCount = itemCount
                            )
                        }
                    )
                }
            }
        }
    }
}
