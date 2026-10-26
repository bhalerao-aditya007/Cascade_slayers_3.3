package com.cascade.attentiontwin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cascade.attentiontwin.ui.MainViewModel
import com.cascade.attentiontwin.ui.components.GlassCard
import com.cascade.attentiontwin.ui.components.ReceiptCard
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.ElectricViolet
import com.cascade.attentiontwin.ui.theme.NeonBlue
import com.cascade.attentiontwin.ui.theme.Surface1
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo

@Composable
fun ReceiptsScreen(
    viewModel: MainViewModel,
    initialFilter: String? = null
) {
    val receipts by viewModel.receiptsFlow.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState()

    val filterOptions = listOf("All", "Held", "Shown", "Protected", "Failed")
    var selectedFilter by remember(initialFilter) {
        mutableStateOf(
            when (initialFilter) {
                "SHOW" -> "Shown"
                "HOLD" -> "Held"
                else -> initialFilter ?: "All"
            }
        )
    }

    val filteredList = receipts.filter { receipt ->
        val simAllowed = settings.includeDemo || receipt.source != "sim"
        val filterMatches = when (selectedFilter) {
            "Held" -> receipt.action == "HOLD"
            "Shown" -> receipt.action == "SHOW" && receipt.gate != "PROTECTED"
            "Protected" -> receipt.gate == "PROTECTED"
            "Failed" -> receipt.enforcement == "failed"
            else -> true
        }
        simAllowed && filterMatches
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Receipts",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextHi,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Every decision is verified and audited. Tap any row to inspect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
        }

        // Filter chips row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) ElectricViolet.copy(alpha = 0.2f) else Surface2)
                            .border(1.dp, if (isSelected) ElectricViolet else CardBorder, RoundedCornerShape(14.dp))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ElectricViolet else TextHi
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = TextLo,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No receipts matching '$selectedFilter'",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextHi
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (!settings.includeDemo) "Enable 'Include demo data' in Settings to view simulated items." else "Notifications will record receipts here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLo
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { receipt ->
                ReceiptCard(receipt = receipt)
            }
        }
    }
}
