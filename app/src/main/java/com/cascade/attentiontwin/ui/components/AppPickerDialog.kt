package com.cascade.attentiontwin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade.attentiontwin.ui.AppInfo
import com.cascade.attentiontwin.ui.AppInfoHelper
import com.cascade.attentiontwin.ui.theme.CardBorder
import com.cascade.attentiontwin.ui.theme.Cyan
import com.cascade.attentiontwin.ui.theme.HoldGreen
import com.cascade.attentiontwin.ui.theme.Surface1
import com.cascade.attentiontwin.ui.theme.Surface2
import com.cascade.attentiontwin.ui.theme.TextHi
import com.cascade.attentiontwin.ui.theme.TextLo

@Composable
fun AppPickerDialog(
    initialSelected: Set<String>,
    title: String = "Apps to Block During Focus",
    subtitle: String = "Notifications from checked apps will be held while Focus is active.",
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val allApps = remember { AppInfoHelper.getInstalledApps(context) }
    var selectedPkgs by remember { mutableStateOf(initialSelected) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Distractions", "Social", "Messaging", "Video", "Shopping", "Other")

    val filteredApps = remember(searchQuery, selectedCategory, allApps) {
        allApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.label.contains(searchQuery, ignoreCase = true) ||
                    app.pkg.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                "All" -> true
                "Distractions" -> app.isDistraction
                "Social" -> app.group == "social"
                "Messaging" -> app.group == "messaging"
                "Video" -> app.group == "video"
                "Shopping" -> app.group == "shopping"
                "Other" -> app.group !in setOf("social", "messaging", "video", "shopping")
                else -> true
            }

            matchesSearch && matchesCategory
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextHi,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLo
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps...", color = TextLo, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = Cyan, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = TextLo, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = Surface2,
                        unfocusedContainerColor = Surface2,
                        focusedTextColor = TextHi,
                        unfocusedTextColor = TextHi
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                // Category chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Cyan.copy(alpha = 0.2f),
                                selectedLabelColor = Cyan,
                                containerColor = Surface2,
                                labelColor = TextLo
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Cyan else CardBorder
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Quick actions & count bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedPkgs.size} apps selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedPkgs.isNotEmpty()) HoldGreen else TextLo,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Distractions",
                            style = MaterialTheme.typography.labelSmall,
                            color = Cyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val distractions = allApps.filter { it.isDistraction }.map { it.pkg }.toSet()
                                    selectedPkgs = selectedPkgs + distractions
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        Text(
                            text = if (selectedPkgs.isEmpty()) "All" else "Clear",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLo,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedPkgs = if (selectedPkgs.isEmpty()) {
                                        allApps.map { it.pkg }.toSet()
                                    } else {
                                        emptySet()
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // App list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.pkg }) { app ->
                        val isChecked = selectedPkgs.contains(app.pkg)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isChecked) Cyan.copy(alpha = 0.08f) else Surface2)
                                .border(
                                    0.75.dp,
                                    if (isChecked) Cyan.copy(alpha = 0.35f) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedPkgs = if (isChecked) selectedPkgs - app.pkg else selectedPkgs + app.pkg
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // App Icon
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon.asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Surface1),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Apps,
                                            contentDescription = null,
                                            tint = Cyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextHi,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = app.group.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextLo,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedPkgs = if (checked) selectedPkgs + app.pkg else selectedPkgs - app.pkg
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Cyan,
                                    checkedTrackColor = Cyan.copy(alpha = 0.35f),
                                    uncheckedThumbColor = TextLo,
                                    uncheckedTrackColor = Surface1
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedPkgs) },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save (${selectedPkgs.size} apps)", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextLo)
            }
        }
    )
}
