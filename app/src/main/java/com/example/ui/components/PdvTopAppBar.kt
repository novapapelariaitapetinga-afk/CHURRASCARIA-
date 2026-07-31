package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmberOrange
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.TableFreeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdvTopAppBar(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    activeTablesCount: Int,
    waitingBillCount: Int
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_bolinha_caricature_1784907417662),
                    contentDescription = "Caricatura Bolinha",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, EmberOrange, RoundedCornerShape(10.dp))
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Churrascaria ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Bolinha",
                            fontWeight = FontWeight.Black,
                            color = OrangePrimary,
                            fontSize = 15.sp
                        )
                    }

                    // Cloud & Wi-Fi Multi-device Live Sync Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(TableFreeColor)
                        )
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Wi-Fi",
                            tint = TableFreeColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Sync Nuvem/Wi-Fi ON",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TableFreeColor
                        )
                    }
                }
            }
        },
        actions = {
            // Active Tables pill badge
            if (activeTablesCount > 0 || waitingBillCount > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmberOrange.copy(alpha = 0.15f),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(EmberOrange)
                        )
                        Text(
                            text = "$activeTablesCount ocup. ${if (waitingBillCount > 0) "• $waitingBillCount c/ conta" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmberOrange
                        )
                    }
                }
            }

            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Alternar Tema Dark/Light"
                )
            }
        }
    )
}

