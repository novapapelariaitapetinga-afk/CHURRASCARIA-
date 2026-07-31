package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.OrderHistoryEntity
import com.example.data.model.WaiterEntity
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.TableFreeColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WaitersScreen(
    waiters: List<WaiterEntity>,
    historyList: List<OrderHistoryEntity>,
    onSaveWaiter: (id: Long, name: String, phone: String) -> Unit,
    onDeleteWaiter: (WaiterEntity) -> Unit
) {
    var selectedWaiterId by remember { mutableStateOf<Long?>(null) } // null = Todos
    var showAddWaiterDialog by remember { mutableStateOf(false) }
    var waiterToEdit by remember { mutableStateOf<WaiterEntity?>(null) }

    val filteredHistory = if (selectedWaiterId == null) {
        historyList
    } else {
        historyList.filter { it.waiterId == selectedWaiterId }
    }

    val totalVendido = filteredHistory.sumOf { it.subtotal }
    val totalComissao = filteredHistory.sumOf { it.serviceFeeAmount }
    val totalMesasAtendidas = filteredHistory.size

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gestão de Garçons",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Desempenho e comissões.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        waiterToEdit = null
                        showAddWaiterDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("btn_top_add_waiter")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Novo Garçom", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Waiter Selector Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedWaiterId == null,
                        onClick = { selectedWaiterId = null },
                        label = { Text("Todos os Garçons") }
                    )
                }
                items(waiters, key = { it.id }) { waiter ->
                    FilterChip(
                        selected = selectedWaiterId == waiter.id,
                        onClick = { selectedWaiterId = waiter.id },
                        label = { Text(waiter.name) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Performance Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Vendido Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Total Vendido", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "R$ %.2f", totalVendido),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = OrangePrimary
                        )
                    }
                }

                // Comissão Acumulada Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = TableFreeColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Comissão (10%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "R$ %.2f", totalComissao),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TableFreeColor
                        )
                    }
                }

                // Mesas Atendidas Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Atendimentos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalMesasAtendidas mesas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Lista de Garçons Cadastrados",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // List of Waiters with CRUD & stats
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(waiters, key = { it.id }) { waiter ->
                    val waiterOrders = historyList.filter { it.waiterId == waiter.id }
                    val waiterSales = waiterOrders.sumOf { it.subtotal }
                    val waiterCommission = waiterOrders.sumOf { it.serviceFeeAmount }

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("waiter_card_${waiter.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(OrangePrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = OrangePrimary)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(text = waiter.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (waiter.phone.isNotEmpty()) {
                                        Text(text = waiter.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.getDefault(), "Vendas: R$ %.2f • Comissão: R$ %.2f", waiterSales, waiterCommission),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TableFreeColor
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = {
                                    waiterToEdit = waiter
                                    showAddWaiterDialog = true
                                }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar Garçom")
                                }

                                IconButton(onClick = { onDeleteWaiter(waiter) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir Garçom", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddWaiterDialog) {
        AddEditWaiterDialog(
            waiterToEdit = waiterToEdit,
            onDismiss = { showAddWaiterDialog = false },
            onSave = { id, name, phone ->
                onSaveWaiter(id, name, phone)
                showAddWaiterDialog = false
            }
        )
    }
}

@Composable
fun AddEditWaiterDialog(
    waiterToEdit: WaiterEntity?,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(waiterToEdit?.name ?: "") }
    var phone by remember { mutableStateOf(waiterToEdit?.phone ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .testTag("add_edit_waiter_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (waiterToEdit == null) "Cadastrar Novo Garçom" else "Editar Garçom",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Garçom *") },
                    modifier = Modifier.fillMaxWidth().testTag("waiter_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / Celular") },
                    modifier = Modifier.fillMaxWidth().testTag("waiter_phone_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(waiterToEdit?.id ?: 0L, name, phone)
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_waiter"),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Text("Salvar Garçom", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
