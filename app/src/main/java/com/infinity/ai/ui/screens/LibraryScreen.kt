package com.infinity.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infinity.ai.data.library.EntryType
import com.infinity.ai.data.library.LibraryEntry
import com.infinity.ai.ui.components.GlassCard
import com.infinity.ai.ui.components.GradientBackground
import com.infinity.ai.ui.theme.*
import com.infinity.ai.viewmodel.LibraryViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LibraryScreen(
    isDarkTheme   : Boolean,
    bottomPadding : Dp,
    onOpenEntry   : (Long) -> Unit,
    vm            : LibraryViewModel = viewModel()
) {
    val entries      by vm.entries.collectAsState()
    val selectedType by vm.selectedType.collectAsState()
    val searchQuery  by vm.searchQuery.collectAsState()

    GradientBackground(darkTheme = isDarkTheme, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = bottomPadding)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Library", style = MaterialTheme.typography.headlineMedium,
                        color = if (isDarkTheme) TextPrimary else TextPrimaryLight,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${entries.size}", style = MaterialTheme.typography.labelMedium,
                        color = Blue500, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Blue500.copy(0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(12.dp))

                // Search bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDarkTheme) DarkGlass else LightGlass)
                        .border(0.5.dp,
                            if (isDarkTheme) Color.White.copy(0.08f) else Color.White.copy(0.6f),
                            RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Search, null,
                        tint = if (isDarkTheme) TextSecondary else TextSecondaryLight,
                        modifier = Modifier.size(16.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { vm.setSearch(it) },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDarkTheme) TextPrimary else TextPrimaryLight
                        ),
                        cursorBrush = SolidColor(Blue500),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Search saved content…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDarkTheme) TextSecondary else TextSecondaryLight)
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(Icons.Default.Close, "Clear",
                            tint = if (isDarkTheme) TextSecondary else TextSecondaryLight,
                            modifier = Modifier.size(16.dp).clickable { vm.setSearch("") })
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Category filter chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        label = "All", selected = selectedType == null,
                        isDarkTheme = isDarkTheme, color = Blue500
                    ) { vm.setFilter(null) }
                    EntryType.entries.forEach { type ->
                        FilterChip(
                            label = type.label, selected = selectedType == type,
                            isDarkTheme = isDarkTheme, color = typeColor(type)
                        ) { vm.setFilter(if (selectedType == type) null else type) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Content ───────────────────────────────────────────────────────
            if (entries.isEmpty()) {
                LibraryEmptyState(isDarkTheme, searchQuery.isNotBlank())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        LibraryEntryCard(
                            entry       = entry,
                            isDarkTheme = isDarkTheme,
                            onClick     = { onOpenEntry(entry.id) },
                            onDelete    = { vm.delete(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label      : String,
    selected   : Boolean,
    isDarkTheme: Boolean,
    color      : Color,
    onClick    : () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) color.copy(0.15f) else if (isDarkTheme) DarkGlass else LightGlass)
            .border(0.5.dp,
                if (selected) color.copy(0.4f) else if (isDarkTheme) Color.White.copy(0.08f) else Color.White.copy(0.5f),
                RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = if (selected) color else if (isDarkTheme) TextSecondary else TextSecondaryLight,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun LibraryEntryCard(
    entry      : LibraryEntry,
    isDarkTheme: Boolean,
    onClick    : () -> Unit,
    onDelete   : () -> Unit
) {
    val color = typeColor(entry.type)
    val fmt   = remember { SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    GlassCard(darkTheme = isDarkTheme, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Type badge
            Box(
                modifier = Modifier.size(40.dp).background(color.copy(0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(typeIcon(entry.type), null, tint = color, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleSmall,
                    color = if (isDarkTheme) TextPrimary else TextPrimaryLight,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(entry.content.take(120).replace("\n", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme) TextSecondary else TextSecondaryLight,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(entry.type.label, style = MaterialTheme.typography.labelSmall, color = color)
                    }
                    Text(fmt.format(Date(entry.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkTheme) TextDisabled else TextSecondaryLight)
                }
            }

            // Delete button
            if (!showDeleteConfirm) {
                IconButton(onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, "Delete",
                        tint = if (isDarkTheme) TextDisabled else TextSecondaryLight,
                        modifier = Modifier.size(18.dp))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showDeleteConfirm = false },
                        modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Cancel",
                            tint = if (isDarkTheme) TextSecondary else TextSecondaryLight,
                            modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onDelete(); showDeleteConfirm = false },
                        modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Confirm delete",
                            tint = ErrorRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(isDarkTheme: Boolean, isSearching: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).background(Blue500.copy(0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSearching) Icons.Default.SearchOff else Icons.Default.AutoStories,
                null, tint = Blue500, modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            if (isSearching) "No results found" else "Library is empty",
            style = MaterialTheme.typography.titleMedium,
            color = if (isDarkTheme) TextPrimary else TextPrimaryLight,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isSearching) "Try different keywords"
            else "Generate a PDF summary, OCR scan,\nor quiz — it will be saved here automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkTheme) TextSecondary else TextSecondaryLight,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun typeColor(type: EntryType): Color = when (type) {
    EntryType.PDF_SUMMARY -> Color(0xFF10B981)
    EntryType.OCR         -> Color(0xFF3B82F6)
    EntryType.SCREENSHOT  -> Color(0xFF8B5CF6)
    EntryType.QUIZ        -> Color(0xFF10B981)
    EntryType.NOTE        -> Color(0xFFF59E0B)
}

fun typeIcon(type: EntryType): ImageVector = when (type) {
    EntryType.PDF_SUMMARY -> Icons.Default.PictureAsPdf
    EntryType.OCR         -> Icons.Default.DocumentScanner
    EntryType.SCREENSHOT  -> Icons.Default.ScreenSearchDesktop
    EntryType.QUIZ        -> Icons.Default.Quiz
    EntryType.NOTE        -> Icons.Default.EditNote
}
