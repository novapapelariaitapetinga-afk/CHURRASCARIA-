package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Phonelink
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.OrderHistoryEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.TableEntity
import com.example.data.model.TableStatus
import com.example.ui.components.PdvScreen
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.TableAvailable
import com.example.ui.theme.TableOccupied
import com.example.ui.theme.TableWaiting
import java.text.NumberFormat
import java.util.Locale

data class QuickFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val badge: String? = null,
    val targetScreen: PdvScreen
)

@Composable
fun HomeScreen(
    tables: List<TableEntity>,
    activeItems: List<OrderItemEntity>,
    historyList: List<OrderHistoryEntity>,
    isWifiSyncEnabled: Boolean = true,
    isMultiDeviceEnabled: Boolean = true,
    onToggleWifiSync: (Boolean) -> Unit = {},
    onToggleMultiDevice: (Boolean) -> Unit = {},
    onNavigateToScreen: (PdvScreen) -> Unit,
    onOpenTableDirectly: (Int) -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    val activeTablesCount by remember(tables) { derivedStateOf { tables.count { it.status == TableStatus.OCUPADA } } }
    val waitingBillCount by remember(tables) { derivedStateOf { tables.count { it.status == TableStatus.AGUARDANDO_PAGAMENTO } } }
    val freeTablesCount by remember(tables) { derivedStateOf { tables.count { it.status == TableStatus.LIVRE } } }
    val todayTotalRevenue by remember(historyList) { derivedStateOf { historyList.sumOf { it.totalAmount } } }
    val activeConsumptionTotal by remember(activeItems) { derivedStateOf { activeItems.sumOf { it.unitPrice * it.quantity } } }

    val quickFeatures = remember(activeTablesCount, freeTablesCount, activeItems, todayTotalRevenue) {
        listOf(
            QuickFeature(
                title = "Mesas (1 a 50)",
                description = "$activeTablesCount Ocupadas • $freeTablesCount Livres",
                icon = Icons.Default.GridOn,
                color = EmberOrange,
                badge = if (activeTablesCount > 0) "$activeTablesCount ativas" else null,
                targetScreen = PdvScreen.MESAS
            ),
            QuickFeature(
                title = "Comandas & Consumo",
                description = "Controle de consumo em tempo real",
                icon = Icons.Default.ReceiptLong,
                color = Color(0xFFE65100),
                badge = if (activeItems.isNotEmpty()) "${activeItems.size} itens" else null,
                targetScreen = PdvScreen.MESAS
            ),
            QuickFeature(
                title = "Cardápio PDV",
                description = "Carnes, Espetos, Bebidas e Acompanhamentos",
                icon = Icons.Default.RestaurantMenu,
                color = Color(0xFF2E7D32),
                targetScreen = PdvScreen.CARDAPIO
            ),
            QuickFeature(
                title = "Gestão de Garçons",
                description = "Comissões e atendimento individual",
                icon = Icons.Default.Badge,
                color = Color(0xFF1565C0),
                targetScreen = PdvScreen.GARCONS
            ),
            QuickFeature(
                title = "Visão Geral",
                description = "Relatório financeiro e vendas do dia",
                icon = Icons.Default.Analytics,
                color = Color(0xFF6A1B9A),
                badge = currencyFormatter.format(todayTotalRevenue),
                targetScreen = PdvScreen.HISTORICO
            ),
            QuickFeature(
                title = "Estoque / Entradas",
                description = "Controle rápido do catálogo de produtos",
                icon = Icons.Default.Inventory,
                color = Color(0xFF00838F),
                targetScreen = PdvScreen.CARDAPIO
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Hero Branding Header (Compact 135.dp for Mobile)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(135.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_bolinha_poster_1784907431433),
                contentDescription = "Churrascaria do Bolinha",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.30f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Brand Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_bolinha_caricature_1784907417662),
                    contentDescription = "Caricatura do Bolinha",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, EmberOrange, RoundedCornerShape(12.dp))
                        .shadow(4.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Churrascaria do Bolinha",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Sistema PDV • Controle de Consumo",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Interactive Wi-Fi & Multi-Device Controls (100% visible on mobile with On/Off switches)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wi-Fi Sync Toggle Pill
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleWifiSync(!isWifiSyncEnabled) },
                    color = if (isWifiSyncEnabled) Color(0xFF1B5E20).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (isWifiSyncEnabled) Color(0xFF2E7D32) else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Sincronia Wi-Fi",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWifiSyncEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (isWifiSyncEnabled) "Ativo" else "Desativado",
                                    fontSize = 9.sp,
                                    color = if (isWifiSyncEnabled) Color(0xFF2E7D32) else Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Switch(
                            checked = isWifiSyncEnabled,
                            onCheckedChange = { onToggleWifiSync(it) },
                            modifier = Modifier.scale(0.7f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2E7D32),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Multi-Device Toggle Pill
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleMultiDevice(!isMultiDeviceEnabled) },
                    color = if (isMultiDeviceEnabled) EmberOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phonelink,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (isMultiDeviceEnabled) EmberOrange else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Multi-Disp.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMultiDeviceEnabled) EmberOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (isMultiDeviceEnabled) "Ativo" else "Desativado",
                                    fontSize = 9.sp,
                                    color = if (isMultiDeviceEnabled) EmberOrange else Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Switch(
                            checked = isMultiDeviceEnabled,
                            onCheckedChange = { onToggleMultiDevice(it) },
                            modifier = Modifier.scale(0.7f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmberOrange,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }

        // Live Real-Time Consumption Summary Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary Card 1: Active Consumption
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(86.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = OrangePrimary.copy(alpha = 0.12f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Consumo Aberto nas Mesas",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currencyFormatter.format(activeConsumptionTotal),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OrangePrimary
                    )
                    Text(
                        text = "$activeTablesCount mesas consumindo",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Summary Card 2: Today Revenue
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(86.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2E7D32).copy(alpha = 0.12f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Faturamento Fechado Hoje",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currencyFormatter.format(todayTotalRevenue),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "${historyList.size} comandas pagas",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Section Title: Painel de Controle Rápido
        PaddingValues(horizontal = 16.dp).let {
            Text(
                text = "Painel de Controle Rápido",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Grid of Features (3x2 or 2x3 responsive layout)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val chunkedFeatures = quickFeatures.chunked(2)
            for (pair in chunkedFeatures) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (feature in pair) {
                        QuickFeatureCard(
                            feature = feature,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToScreen(feature.targetScreen) }
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Table Quick Access Status Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status Atual das Mesas (1 a 50)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OrangePrimary,
                        modifier = Modifier.clickable { onNavigateToScreen(PdvScreen.MESAS) }
                    ) {
                        Text(
                            text = "Abrir Mapa",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TableStatusIndicator(
                        label = "Livres",
                        count = freeTablesCount,
                        color = TableAvailable
                    )
                    TableStatusIndicator(
                        label = "Ocupadas",
                        count = activeTablesCount,
                        color = TableOccupied
                    )
                    TableStatusIndicator(
                        label = "Aguardando Conta",
                        count = waitingBillCount,
                        color = TableWaiting
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-Time Mobile Device Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EmberOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Acesso Online em Tempo Real",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Qualquer celular ou tablet conectado à rede da churrascaria atualiza os lançamentos das mesas instantaneamente.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickFeatureCard(
    feature: QuickFeature,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(125.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(feature.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = feature.icon,
                            contentDescription = feature.title,
                            tint = feature.color,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    feature.badge?.let { badgeText ->
                        Surface(
                            shape = CircleShape,
                            color = feature.color,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = feature.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = feature.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TableStatusIndicator(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: ",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$count",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
