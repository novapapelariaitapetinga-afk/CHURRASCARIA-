package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.OrderItemEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.TableEntity
import com.example.data.model.WaiterEntity
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.TableFreeColor
import java.util.Locale

@Composable
fun CheckoutDialog(
    table: TableEntity,
    waiters: List<WaiterEntity>,
    activeItems: List<OrderItemEntity>,
    onDismiss: () -> Unit,
    onConfirmCheckout: (
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
    ) -> Unit
) {
    val waiter = waiters.find { it.id == table.activeWaiterId } ?: waiters.firstOrNull()
    val subtotal = activeItems.sumOf { it.unitPrice * it.quantity }
    val totalItemsCount = activeItems.sumOf { it.quantity }

    var includeServiceFee by remember { mutableStateOf(true) }
    var serviceFeeRate by remember { mutableDoubleStateOf(10.0) } // 10%
    var discountType by remember { mutableStateOf("NONE") } // "NONE", "VALUE", "PERCENT"
    var discountInput by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CREDIT) }

    // Calculations
    val serviceFeeAmount = if (includeServiceFee) (subtotal * (serviceFeeRate / 100.0)) else 0.0
    val rawDiscount = discountInput.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
    val discountAmount = when (discountType) {
        "VALUE" -> rawDiscount.coerceAtMost(subtotal + serviceFeeAmount)
        "PERCENT" -> (subtotal * (rawDiscount.coerceAtMost(100.0) / 100.0))
        else -> 0.0
    }

    val totalAmount = (subtotal + serviceFeeAmount - discountAmount).coerceAtLeast(0.0)

    var cashGivenInput by remember { mutableStateOf("") }
    val cashGiven = cashGivenInput.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
    val changeAmount = if (selectedPaymentMethod == PaymentMethod.CASH && cashGiven > totalAmount) cashGiven - totalAmount else 0.0


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("checkout_dialog_${table.number}"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TableFreeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = TableFreeColor
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Fechamento de Conta",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "Mesa %02d • Garçom: %s", table.number, waiter?.name ?: "N/A"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal dos Itens ($totalItemsCount items):", fontSize = 13.sp)
                                Text(
                                    text = String.format(Locale.getDefault(), "R$ %.2f", subtotal),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Taxa de Serviço Opcional
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = includeServiceFee,
                                        onCheckedChange = { includeServiceFee = it },
                                        colors = CheckboxDefaults.colors(checkedColor = OrangePrimary)
                                    )
                                    Text("Taxa de Serviço (10% Garçom):", fontSize = 13.sp)
                                }
                                Text(
                                    text = String.format(Locale.getDefault(), "R$ %.2f", serviceFeeAmount),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (includeServiceFee) OrangePrimary else Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Calculated Discount Display Row
                            if (discountAmount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Desconto Aplicado:", fontSize = 13.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = String.format(Locale.getDefault(), "- R$ %.2f", discountAmount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Desconto Section
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Desconto Especial:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = discountType == "NONE",
                                            onClick = { discountType = "NONE"; discountInput = "" }
                                        )
                                        Text("Nenhum", fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = discountType == "VALUE",
                                            onClick = { discountType = "VALUE" }
                                        )
                                        Text("R$", fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = discountType == "PERCENT",
                                            onClick = { discountType = "PERCENT" }
                                        )
                                        Text("%", fontSize = 11.sp)
                                    }

                                    if (discountType != "NONE") {
                                        OutlinedTextField(
                                            value = discountInput,
                                            onValueChange = { discountInput = it },
                                            placeholder = { Text(if (discountType == "VALUE") "R$ 0,00" else "10%") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Total Final Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TableFreeColor)
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL A PAGAR:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "R$ %.2f", totalAmount),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Formas de Pagamento
                    Text(
                        text = "Forma de Pagamento:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val paymentMethods = listOf(
                        PaymentMethod.CREDIT to Icons.Default.CreditCard,
                        PaymentMethod.DEBIT to Icons.Default.CreditCard,
                        PaymentMethod.PIX to Icons.Default.Pix,
                        PaymentMethod.CASH to Icons.Default.Money
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paymentMethods.forEach { (method, icon) ->
                            val isSelected = selectedPaymentMethod == method
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPaymentMethod = method }
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) OrangePrimary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) OrangePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = method,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedPaymentMethod = method },
                                            colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                                        )
                                    }

                                    // Payment specific helper details
                                    if (isSelected && method == PaymentMethod.PIX) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Chave PIX (CNPJ): 12.345.678/0001-90\nChurrascaria do Bolinha LTDA",
                                            fontSize = 11.sp,
                                            color = OrangePrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else if (isSelected && method == PaymentMethod.CASH) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = cashGivenInput,
                                                onValueChange = { cashGivenInput = it },
                                                label = { Text("Valor Recebido R$") },
                                                placeholder = { Text("ex: 100.00") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(50.dp),
                                                singleLine = true
                                            )

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Troco a devolver:", fontSize = 11.sp)
                                                Text(
                                                    text = String.format(Locale.getDefault(), "R$ %.2f", changeAmount),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (changeAmount > 0) TableFreeColor else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pinned Confirm button at bottom
                Button(
                    onClick = {
                        onConfirmCheckout(
                            table.number,
                            waiter?.id ?: 0L,
                            waiter?.name ?: "Garçom Geral",
                            subtotal,
                            if (includeServiceFee) serviceFeeRate else 0.0,
                            serviceFeeAmount,
                            discountType,
                            discountAmount,
                            totalAmount,
                            selectedPaymentMethod,
                            totalItemsCount
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_confirm_payment_and_close_table"),
                    colors = ButtonDefaults.buttonColors(containerColor = TableFreeColor)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Confirmar Pagamento e Liberar Mesa",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
