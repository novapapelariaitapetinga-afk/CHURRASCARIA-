package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderItemEntity
import com.example.data.model.TableEntity
import com.example.data.model.TableStatus
import com.example.data.model.WaiterEntity
import com.example.ui.theme.TableFreeColor
import com.example.ui.theme.TableFreeDark
import com.example.ui.theme.TableOccupiedColor
import com.example.ui.theme.TableOccupiedDark
import com.example.ui.theme.TableWaitingColor
import com.example.ui.theme.TableWaitingDark
import java.util.Locale

@Composable
fun TablesScreen(
    tables: List<TableEntity>,
    waiters: List<WaiterEntity>,
    activeItems: List<OrderItemEntity>,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onTableClick: (Int) -> Unit
) {
    val darkTheme = isSystemInDarkTheme()

    // Counts
    val totalCount by remember(tables) { derivedStateOf { tables.size } }
    val freeCount by remember(tables) { derivedStateOf { tables.count { it.status == TableStatus.LIVRE } } }
    val occupiedCount by remember(tables) { derivedStateOf { tables.count { it.status == TableStatus.OCUPADA } } }
    val waitingCount by remember(tables) { derivedStateOf { tables.count { it.status == TableStatus.AGUARDANDO_PAGAMENTO } } }

    // Precomputed lookup map for active items per table
    val tableStatsMap by remember(activeItems) {
        derivedStateOf {
            activeItems.groupBy { it.tableNumber }.mapValues { (_, items) ->
                val count = items.sumOf { it.quantity }
                val total = items.sumOf { it.unitPrice * it.quantity }
                Pair(count, total)
            }
        }
    }

    // Precomputed lookup map for waiters
    val waitersMap by remember(waiters) {
        derivedStateOf { waiters.associateBy { it.id } }
    }

    // Filtered tables list
    val filteredTables by remember(tables, filterStatus, searchQuery) {
        derivedStateOf {
            tables.filter { table ->
                val matchesStatus = filterStatus == "TODOS" || table.status == filterStatus
                val matchesQuery = searchQuery.isBlank() ||
                        table.number.toString().contains(searchQuery.trim())
                matchesStatus && matchesQuery
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Search & Quick Status Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_table_input"),
                placeholder = { Text("Buscar Mesa (ex: 5, 12)", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar mesa"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // Filter chips bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filterStatus == "TODOS",
                onClick = { onFilterStatusChange("TODOS") },
                label = { Text("Todas ($totalCount)", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors()
            )

            FilterChip(
                selected = filterStatus == TableStatus.LIVRE,
                onClick = { onFilterStatusChange(TableStatus.LIVRE) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (darkTheme) TableFreeDark else TableFreeColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Livre ($freeCount)", fontSize = 12.sp)
                    }
                }
            )

            FilterChip(
                selected = filterStatus == TableStatus.OCUPADA,
                onClick = { onFilterStatusChange(TableStatus.OCUPADA) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (darkTheme) TableOccupiedDark else TableOccupiedColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ocupada ($occupiedCount)", fontSize = 12.sp)
                    }
                }
            )

            FilterChip(
                selected = filterStatus == TableStatus.AGUARDANDO_PAGAMENTO,
                onClick = { onFilterStatusChange(TableStatus.AGUARDANDO_PAGAMENTO) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (darkTheme) TableWaitingDark else TableWaitingColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Conta ($waitingCount)", fontSize = 12.sp)
                    }
                }
            )
        }

        // 1 to 50 Tables Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 105.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredTables, key = { it.number }) { table ->
                val (itemCount, tableTotal) = tableStatsMap[table.number] ?: Pair(0, 0.0)
                val waiterName = table.activeWaiterId?.let { waitersMap[it]?.name } ?: ""

                TableCard(
                    table = table,
                    waiterName = waiterName,
                    itemCount = itemCount,
                    totalAmount = tableTotal,
                    onClick = { onTableClick(table.number) }
                )
            }
        }
    }
}

@Composable
fun TableCard(
    table: TableEntity,
    waiterName: String,
    itemCount: Int,
    totalAmount: Double,
    onClick: () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()

    val (statusColor, statusBgColor, statusText) = when (table.status) {
        TableStatus.LIVRE -> Triple(
            if (darkTheme) TableFreeDark else TableFreeColor,
            if (darkTheme) TableFreeDark.copy(alpha = 0.15f) else TableFreeColor.copy(alpha = 0.12f),
            "Livre"
        )
        TableStatus.OCUPADA -> Triple(
            if (darkTheme) TableOccupiedDark else TableOccupiedColor,
            if (darkTheme) TableOccupiedDark.copy(alpha = 0.15f) else TableOccupiedColor.copy(alpha = 0.12f),
            "Ocupada"
        )
        TableStatus.AGUARDANDO_PAGAMENTO -> Triple(
            if (darkTheme) TableWaitingDark else TableWaitingColor,
            if (darkTheme) TableWaitingDark.copy(alpha = 0.2f) else TableWaitingColor.copy(alpha = 0.2f),
            "Solicitou Conta"
        )
        else -> Triple(Color.Gray, Color.LightGray, "Desconhecido")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.5.dp,
                color = statusColor,
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("table_card_${table.number}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusBgColor)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusText.uppercase(Locale.getDefault()),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Table Number
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TableRestaurant,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.getDefault(), "%02d", table.number),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Waiter & Total Value or Free
            if (table.status == TableStatus.LIVRE) {
                Text(
                    text = "Disponível",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                if (waiterName.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = waiterName,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = String.format(Locale.getDefault(), "R$ %.2f", totalAmount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )

                if (itemCount > 0) {
                    Text(
                        text = "$itemCount item(ns)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
