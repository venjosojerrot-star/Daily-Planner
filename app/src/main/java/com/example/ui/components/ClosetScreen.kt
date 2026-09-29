package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.ClosetItem
import com.example.data.model.ClosetOutfit
import com.example.util.DateTimeUtils
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

enum class ClosetTab {
    WARDROBE, OUTFITS, INSIGHTS
}

enum class ClosetSortOrder {
    RECENT, MOST_WORN, LEAST_WORN, NAME_AZ, PRICE_HIGH, PRICE_LOW
}

fun saveClosetImageToInternalStorage(context: Context, uri: Uri, prefix: String = "closet_item"): String? {
    return try {
        val folder = File(context.filesDir, "closet_images")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, "${prefix}_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

fun isValidImagePath(path: String?): Boolean {
    if (path.isNullOrBlank()) return false
    val file = File(path)
    return (file.exists() && file.length() > 0) || path.startsWith("content://") || path.startsWith("http")
}

private val CLOSET_CATEGORIES = listOf(
    "All",
    "Tops",
    "Bottoms",
    "Dresses",
    "Outerwear",
    "Shoes",
    "Bags & Accessories",
    "Activewear",
    "Sleepwear"
)

private val CLOSET_STATUSES = listOf(
    "In Closet",
    "In Laundry",
    "Dry Cleaning",
    "Borrowed",
    "Stored"
)

private val CLOSET_SEASONS = listOf(
    "All Season",
    "Summer",
    "Winter",
    "Spring",
    "Fall"
)

private val CLOSET_OCCASIONS = listOf(
    "Casual",
    "Work / Formal",
    "Party / Evening",
    "Sport / Gym",
    "Lounge"
)

private val CLOSET_COLORS = listOf(
    "Black" to Color(0xFF1E1E1E),
    "White" to Color(0xFFF5F5F5),
    "Gray" to Color(0xFF757575),
    "Navy" to Color(0xFF1A237E),
    "Blue" to Color(0xFF1E88E5),
    "Green" to Color(0xFF2E7D32),
    "Olive" to Color(0xFF558B2F),
    "Red" to Color(0xFFC62828),
    "Pink" to Color(0xFFE91E63),
    "Beige" to Color(0xFFD7CCC8),
    "Brown" to Color(0xFF4E342E),
    "Purple" to Color(0xFF6A1B9A),
    "Yellow" to Color(0xFFFBC02D),
    "Orange" to Color(0xFFEF6C00),
    "Multi" to Color(0xFF00ACC1)
)

fun getCategoryIcon(category: String): ImageVector {
    return when (category) {
        "Tops" -> Icons.Default.Checkroom
        "Bottoms" -> Icons.Default.Straighten
        "Dresses" -> Icons.Default.Female
        "Outerwear" -> Icons.Default.DryCleaning
        "Shoes" -> Icons.Default.DirectionsWalk
        "Bags & Accessories" -> Icons.Default.ShoppingBag
        "Activewear" -> Icons.Default.FitnessCenter
        "Sleepwear" -> Icons.Default.Bedtime
        else -> Icons.Default.Checkroom
    }
}

fun getColorForName(name: String): Color {
    val found = CLOSET_COLORS.find { it.first.equals(name, ignoreCase = true) }
    return found?.second ?: Color.Gray
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen(
    closetItems: List<ClosetItem>,
    closetOutfits: List<ClosetOutfit>,
    selectedCurrency: String = "₱ PHP",
    onAddItem: (name: String, category: String, color: String, season: String, brand: String, size: String, occasion: String, imagePath: String, status: String, price: Double, notes: String) -> Unit,
    onUpdateItem: (ClosetItem) -> Unit,
    onDeleteItem: (ClosetItem) -> Unit,
    onToggleFavorite: (ClosetItem) -> Unit,
    onUpdateStatus: (Long, String) -> Unit,
    onLogWorn: (Long) -> Unit,
    onAddOutfit: (name: String, occasion: String, itemIds: List<Long>, notes: String, imagePath: String) -> Unit,
    onUpdateOutfit: (ClosetOutfit) -> Unit,
    onDeleteOutfit: (ClosetOutfit) -> Unit,
    onToggleOutfitFavorite: (ClosetOutfit) -> Unit,
    onLogOutfitWorn: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(ClosetTab.WARDROBE) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedSeasonFilter by remember { mutableStateOf("All") }
    var onlyFavorites by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(ClosetSortOrder.RECENT) }
    var isGridView by remember { mutableStateOf(true) }

    var isAddItemDialogOpen by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ClosetItem?>(null) }
    var viewingItemDetail by remember { mutableStateOf<ClosetItem?>(null) }

    var isAddOutfitDialogOpen by remember { mutableStateOf(false) }
    var editingOutfit by remember { mutableStateOf<ClosetOutfit?>(null) }

    // Filtered items
    val filteredItems = remember(
        closetItems,
        searchQuery,
        selectedCategoryFilter,
        selectedStatusFilter,
        selectedSeasonFilter,
        onlyFavorites,
        sortOrder
    ) {
        closetItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.brand.contains(searchQuery, ignoreCase = true) ||
                    item.color.contains(searchQuery, ignoreCase = true) ||
                    item.notes.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategoryFilter == "All" || item.category == selectedCategoryFilter
            val matchesStatus = selectedStatusFilter == "All" || item.status == selectedStatusFilter
            val matchesSeason = selectedSeasonFilter == "All" || item.season == selectedSeasonFilter || item.season == "All Season"
            val matchesFavorite = !onlyFavorites || item.isFavorite

            matchesSearch && matchesCategory && matchesStatus && matchesSeason && matchesFavorite
        }.let { list ->
            when (sortOrder) {
                ClosetSortOrder.RECENT -> list.sortedByDescending { it.id }
                ClosetSortOrder.MOST_WORN -> list.sortedByDescending { it.timesWorn }
                ClosetSortOrder.LEAST_WORN -> list.sortedBy { it.timesWorn }
                ClosetSortOrder.NAME_AZ -> list.sortedBy { it.name.lowercase() }
                ClosetSortOrder.PRICE_HIGH -> list.sortedByDescending { it.purchasePrice }
                ClosetSortOrder.PRICE_LOW -> list.sortedBy { it.purchasePrice }
            }
        }
    }

    // Quick Stats Calculation
    val totalItems = closetItems.size
    val inLaundryCount = closetItems.count { it.status == "In Laundry" || it.status == "Dry Cleaning" }
    val totalValue = closetItems.sumOf { it.purchasePrice }
    val currencySymbol = selectedCurrency.split(" ").firstOrNull() ?: "₱"

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Tab Selector
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == ClosetTab.WARDROBE,
                            onClick = { selectedTab = ClosetTab.WARDROBE },
                            text = {
                                Text(
                                    text = "Wardrobe ($totalItems)",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == ClosetTab.WARDROBE) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == ClosetTab.OUTFITS,
                            onClick = { selectedTab = ClosetTab.OUTFITS },
                            text = {
                                Text(
                                    text = "Outfits (${closetOutfits.size})",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == ClosetTab.OUTFITS) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == ClosetTab.INSIGHTS,
                            onClick = { selectedTab = ClosetTab.INSIGHTS },
                            text = {
                                Text(
                                    text = "Insights",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == ClosetTab.INSIGHTS) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == ClosetTab.OUTFITS && closetOutfits.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { isAddOutfitDialogOpen = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Outfit")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                ClosetTab.WARDROBE -> {
                    WardrobeTabContent(
                        items = filteredItems,
                        totalItemsCount = closetItems.size,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategoryFilter,
                        onCategorySelect = { selectedCategoryFilter = it },
                        selectedStatus = selectedStatusFilter,
                        onStatusSelect = { selectedStatusFilter = it },
                        selectedSeason = selectedSeasonFilter,
                        onSeasonSelect = { selectedSeasonFilter = it },
                        onlyFavorites = onlyFavorites,
                        onToggleFavorites = { onlyFavorites = !onlyFavorites },
                        sortOrder = sortOrder,
                        onSortChange = { sortOrder = it },
                        isGridView = isGridView,
                        currencySymbol = currencySymbol,
                        onItemClick = { viewingItemDetail = it },
                        onToggleFavorite = onToggleFavorite,
                        onLogWorn = onLogWorn,
                        onUpdateStatus = onUpdateStatus,
                        onAddNew = {
                            editingItem = null
                            isAddItemDialogOpen = true
                        }
                    )
                }

                ClosetTab.OUTFITS -> {
                    OutfitsTabContent(
                        outfits = closetOutfits,
                        allClosetItems = closetItems,
                        onAddOutfit = { isAddOutfitDialogOpen = true },
                        onEditOutfit = { editingOutfit = it },
                        onDeleteOutfit = onDeleteOutfit,
                        onToggleFavorite = onToggleOutfitFavorite,
                        onLogOutfitWorn = { outfitId ->
                            onLogOutfitWorn(outfitId)
                            // Also log wear for constituent items
                            val outfit = closetOutfits.find { it.id == outfitId }
                            outfit?.itemIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() }?.forEach { itemId ->
                                onLogWorn(itemId)
                            }
                        }
                    )
                }

                ClosetTab.INSIGHTS -> {
                    ClosetInsightsTabContent(
                        items = closetItems,
                        outfits = closetOutfits,
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }

    // Add / Edit Item Dialog
    if (isAddItemDialogOpen || editingItem != null) {
        AddEditClosetItemDialog(
            itemToEdit = editingItem,
            currencySymbol = currencySymbol,
            onDismiss = {
                isAddItemDialogOpen = false
                editingItem = null
            },
            onSave = { name, category, color, season, brand, size, occasion, imagePath, status, price, notes ->
                if (editingItem != null) {
                    onUpdateItem(
                        editingItem!!.copy(
                            name = name,
                            category = category,
                            color = color,
                            season = season,
                            brand = brand,
                            size = size,
                            occasion = occasion,
                            imagePath = imagePath,
                            status = status,
                            purchasePrice = price,
                            notes = notes
                        )
                    )
                } else {
                    onAddItem(name, category, color, season, brand, size, occasion, imagePath, status, price, notes)
                }
                isAddItemDialogOpen = false
                editingItem = null
            }
        )
    }

    // View Item Details Dialog
    viewingItemDetail?.let { currentItem ->
        // Keep updated item from state
        val updatedItem = closetItems.find { it.id == currentItem.id } ?: currentItem
        ClosetItemDetailDialog(
            item = updatedItem,
            currencySymbol = currencySymbol,
            onDismiss = { viewingItemDetail = null },
            onEdit = {
                viewingItemDetail = null
                editingItem = updatedItem
            },
            onDelete = {
                onDeleteItem(updatedItem)
                viewingItemDetail = null
            },
            onToggleFavorite = { onToggleFavorite(updatedItem) },
            onUpdateStatus = { status -> onUpdateStatus(updatedItem.id, status) },
            onLogWorn = { onLogWorn(updatedItem.id) },
            onUpdateImage = { newImagePath ->
                onUpdateItem(updatedItem.copy(imagePath = newImagePath))
            }
        )
    }

    // Add / Edit Outfit Dialog
    if (isAddOutfitDialogOpen || editingOutfit != null) {
        AddEditOutfitDialog(
            outfitToEdit = editingOutfit,
            allClosetItems = closetItems,
            onDismiss = {
                isAddOutfitDialogOpen = false
                editingOutfit = null
            },
            onSave = { name, occasion, selectedItemIds, notes, imagePath ->
                if (editingOutfit != null) {
                    onUpdateOutfit(
                        editingOutfit!!.copy(
                            name = name,
                            occasion = occasion,
                            itemIds = selectedItemIds.joinToString(","),
                            notes = notes,
                            imagePath = imagePath
                        )
                    )
                } else {
                    onAddOutfit(name, occasion, selectedItemIds, notes, imagePath)
                }
                isAddOutfitDialogOpen = false
                editingOutfit = null
            }
        )
    }
}

// ----------------------------------------------------
// WARDROBE TAB CONTENT
// ----------------------------------------------------
@Composable
private fun WardrobeTabContent(
    items: List<ClosetItem>,
    totalItemsCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    selectedStatus: String,
    onStatusSelect: (String) -> Unit,
    selectedSeason: String,
    onSeasonSelect: (String) -> Unit,
    onlyFavorites: Boolean,
    onToggleFavorites: () -> Unit,
    sortOrder: ClosetSortOrder,
    onSortChange: (ClosetSortOrder) -> Unit,
    isGridView: Boolean,
    currencySymbol: String,
    onItemClick: (ClosetItem) -> Unit,
    onToggleFavorite: (ClosetItem) -> Unit,
    onLogWorn: (Long) -> Unit,
    onUpdateStatus: (Long, String) -> Unit,
    onAddNew: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search clothes, brand, color...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("closet_search_field"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                )

                // Favorite Toggle Button
                FilledIconToggleButton(
                    checked = onlyFavorites,
                    onCheckedChange = { onToggleFavorites() },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (onlyFavorites) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorites Only",
                        tint = if (onlyFavorites) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sort Order Menu
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Recently Added") },
                            onClick = { onSortChange(ClosetSortOrder.RECENT); showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.Schedule, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Most Worn") },
                            onClick = { onSortChange(ClosetSortOrder.MOST_WORN); showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.TrendingUp, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Least Worn") },
                            onClick = { onSortChange(ClosetSortOrder.LEAST_WORN); showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.TrendingDown, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Name A-Z") },
                            onClick = { onSortChange(ClosetSortOrder.NAME_AZ); showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.SortByAlpha, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Price (High to Low)") },
                            onClick = { onSortChange(ClosetSortOrder.PRICE_HIGH); showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.ArrowDownward, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Price (Low to High)") },
                            onClick = { onSortChange(ClosetSortOrder.PRICE_LOW); showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.ArrowUpward, null) }
                        )
                    }
                }
                
                // Add Item Button
                IconButton(
                    onClick = onAddNew,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Filter Row (matching sketch: IN CLOSET, IN LAUNDRY, DRY CLEANING...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CLOSET_STATUSES.take(3).forEach { status ->
                    val isSelected = selectedStatus == status
                    Surface(
                        onClick = { 
                            if (isSelected) onStatusSelect("All") else onStatusSelect(status)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text(
                            text = status.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Clothes List / Grid Content
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checkroom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (totalItemsCount == 0) "Your closet is empty" else "No clothes match your filters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (totalItemsCount == 0)
                            "Store your shirts, pants, shoes, and favorite outfits to manage your daily style!"
                        else
                            "Try changing your search term, category or status filters.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAddNew,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Clothing Item")
                    }
                }
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(72.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ClosetItemGridCard(
                            item = item,
                            currencySymbol = currencySymbol,
                            onClick = { onItemClick(item) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onLogWorn = { onLogWorn(item.id) },
                            onUpdateStatus = { newStatus -> onUpdateStatus(item.id, newStatus) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ClosetItemListRow(
                            item = item,
                            currencySymbol = currencySymbol,
                            onClick = { onItemClick(item) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onLogWorn = { onLogWorn(item.id) },
                            onUpdateStatus = { newStatus -> onUpdateStatus(item.id, newStatus) }
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// CLOSET ITEM GRID CARD
// ----------------------------------------------------
@Composable
fun ClosetItemGridCard(
    item: ClosetItem,
    currencySymbol: String,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLogWorn: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .testTag("closet_item_card_${item.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getColorForName(item.color).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (isValidImagePath(item.imagePath)) {
                AsyncImage(
                    model = File(item.imagePath),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Category Icon center
                Icon(
                    imageVector = getCategoryIcon(item.category),
                    contentDescription = item.category,
                    tint = getColorForName(item.color),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// CLOSET ITEM LIST ROW
// ----------------------------------------------------
@Composable
fun ClosetItemListRow(
    item: ClosetItem,
    currencySymbol: String,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLogWorn: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    val statusColor = when (item.status) {
        "In Closet" -> Color(0xFF2E7D32)
        "In Laundry" -> Color(0xFFE65100)
        "Dry Cleaning" -> Color(0xFF6A1B9A)
        "Borrowed" -> Color(0xFF0277BD)
        else -> Color(0xFF757575)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon or Image thumbnail box
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(getColorForName(item.color).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (isValidImagePath(item.imagePath)) {
                    AsyncImage(
                        model = File(item.imagePath),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = getCategoryIcon(item.category),
                        contentDescription = item.category,
                        tint = getColorForName(item.color),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${item.category} • ${item.color}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.brand.isNotBlank()) {
                        Text(
                            text = "• ${item.brand}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.status,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = "Worn ${item.timesWorn}x",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (item.purchasePrice > 0) {
                        Text(
                            text = "$currencySymbol${item.purchasePrice.toInt()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onLogWorn,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "Log Wear",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// OUTFITS TAB CONTENT
// ----------------------------------------------------
@Composable
private fun OutfitsTabContent(
    outfits: List<ClosetOutfit>,
    allClosetItems: List<ClosetItem>,
    onAddOutfit: () -> Unit,
    onEditOutfit: (ClosetOutfit) -> Unit,
    onDeleteOutfit: (ClosetOutfit) -> Unit,
    onToggleFavorite: (ClosetOutfit) -> Unit,
    onLogOutfitWorn: (Long) -> Unit
) {
    if (outfits.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Style,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No outfits created yet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mix and match your closet items into ready-to-wear daily looks (OOTD) for work, casual, or party occasions!",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddOutfit,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create My First Outfit")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(outfits, key = { it.id }) { outfit ->
                val outfitItems = remember(outfit.itemIds, allClosetItems) {
                    val ids = outfit.itemIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                    ids.mapNotNull { id -> allClosetItems.find { it.id == id } }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header: Outfit Title, Occasion, Favorite, Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Style,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(6.dp).size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = outfit.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${outfit.occasion} • Worn ${outfit.timesWorn} times",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleFavorite(outfit) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (outfit.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (outfit.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { onEditOutfit(outfit) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteOutfit(outfit) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (outfit.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = outfit.notes,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Constituent items grid / chips
                        if (outfitItems.isEmpty()) {
                            Text(
                                text = "No items attached to this outfit",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                outfitItems.forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isValidImagePath(item.imagePath)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = File(item.imagePath),
                                                        contentDescription = item.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(getColorForName(item.color))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = getCategoryIcon(item.category),
                                                    contentDescription = item.category,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = item.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Wear Today Button
                        Button(
                            onClick = { onLogOutfitWorn(outfit.id) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Wear Outfit Today (+1 to all pieces)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// CLOSET INSIGHTS & STATS
// ----------------------------------------------------
@Composable
private fun ClosetInsightsTabContent(
    items: List<ClosetItem>,
    outfits: List<ClosetOutfit>,
    currencySymbol: String
) {
    val totalItems = items.size
    val totalWardrobeValue = items.sumOf { it.purchasePrice }
    val totalWears = items.sumOf { it.timesWorn }
    val averageCostPerWear = if (totalWears > 0) totalWardrobeValue / totalWears else 0.0

    val inLaundryCount = items.count { it.status == "In Laundry" }
    val inDryCleanCount = items.count { it.status == "Dry Cleaning" }
    val cleanInClosetCount = items.count { it.status == "In Closet" }

    val categoryCounts = remember(items) {
        CLOSET_CATEGORIES.filter { it != "All" }.map { cat ->
            cat to items.count { it.category == cat }
        }.filter { it.second > 0 }.sortedByDescending { it.second }
    }

    val mostWornItems = remember(items) {
        items.filter { it.timesWorn > 0 }.sortedByDescending { it.timesWorn }.take(5)
    }

    val leastWornItems = remember(items) {
        items.filter { it.timesWorn == 0 }.take(5)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High Level Metrics
        Text(
            text = "Wardrobe Summary",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InsightMetricCard(
                title = "Total Items",
                value = "$totalItems",
                subtitle = "${outfits.size} outfits saved",
                icon = Icons.Default.Checkroom,
                modifier = Modifier.weight(1f)
            )
            InsightMetricCard(
                title = "Estimated Value",
                value = "$currencySymbol${totalWardrobeValue.toInt()}",
                subtitle = "Total wardrobe cost",
                icon = Icons.Default.AccountBalanceWallet,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InsightMetricCard(
                title = "Total Wears",
                value = "$totalWears",
                subtitle = "Logged wears",
                icon = Icons.Default.Repeat,
                modifier = Modifier.weight(1f)
            )
            InsightMetricCard(
                title = "Avg Cost / Wear",
                value = "$currencySymbol${"%.2f".format(averageCostPerWear)}",
                subtitle = "Efficiency index",
                icon = Icons.Default.TrendingDown,
                modifier = Modifier.weight(1f)
            )
        }

        // Laundry & Status breakdown
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Laundry & Cleanliness Status",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatusPill("Clean in Closet", cleanInClosetCount, Color(0xFF2E7D32))
                    StatusPill("In Laundry", inLaundryCount, Color(0xFFE65100))
                    StatusPill("Dry Cleaning", inDryCleanCount, Color(0xFF6A1B9A))
                }
            }
        }

        // Category Breakdown
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Category Distribution",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (categoryCounts.isEmpty()) {
                    Text("No items to analyze", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    categoryCounts.forEach { (cat, count) ->
                        val percentage = if (totalItems > 0) (count.toFloat() / totalItems.toFloat()) else 0f
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = cat, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    text = "$count items (${(percentage * 100).toInt()}%)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { percentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }

        // Most Worn Clothes
        if (mostWornItems.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Top Most Worn Clothes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    mostWornItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    text = item.name,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "${item.timesWorn} times",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Unworn items
        if (leastWornItems.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Unworn Clothes (0 Wears)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Consider wearing or styling these items soon!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    leastWornItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "• ${item.name}", fontSize = 12.sp)
                            Text(text = item.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$count",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InsightMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ----------------------------------------------------
// ADD / EDIT CLOTHING ITEM DIALOG
// ----------------------------------------------------
@Composable
fun AddEditClosetItemDialog(
    itemToEdit: ClosetItem?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: String,
        color: String,
        season: String,
        brand: String,
        size: String,
        occasion: String,
        imagePath: String,
        status: String,
        price: Double,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(itemToEdit?.category ?: "Tops") }
    var selectedColor by remember { mutableStateOf(itemToEdit?.color ?: "Black") }
    var selectedSeason by remember { mutableStateOf(itemToEdit?.season ?: "All Season") }
    var brand by remember { mutableStateOf(itemToEdit?.brand ?: "") }
    var size by remember { mutableStateOf(itemToEdit?.size ?: "") }
    var selectedOccasion by remember { mutableStateOf(itemToEdit?.occasion ?: "Casual") }
    var selectedStatus by remember { mutableStateOf(itemToEdit?.status ?: "In Closet") }
    var imagePath by remember { mutableStateOf(itemToEdit?.imagePath ?: "") }
    var priceInput by remember { mutableStateOf(if (itemToEdit != null && itemToEdit.purchasePrice > 0) itemToEdit.purchasePrice.toString() else "") }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val saved = saveClosetImageToInternalStorage(context, it, "item")
            if (saved != null) {
                imagePath = saved
                Toast.makeText(context, "Photo imported successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not import image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_edit_closet_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Dialog Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (itemToEdit != null) "Edit Clothing Item" else "Add Clothing Item",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Image Import Section
                Text(
                    text = "Item Photo",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (isValidImagePath(imagePath)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(getColorForName(selectedColor).copy(alpha = 0.15f))
                    ) {
                        AsyncImage(
                            model = File(imagePath),
                            contentDescription = "Item Photo Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Action Overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { imagePickerLauncher.launch("image/*") }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Change Photo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { imagePath = "" }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove Photo",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { imagePickerLauncher.launch("image/*") }
                            .testTag("import_closet_image_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Import Photo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Import Item Photo",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Choose a picture from your gallery",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Item Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name * (e.g. White Oxford Shirt)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("closet_item_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selector
                Text(
                    text = "Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CLOSET_CATEGORIES.filter { it != "All" }.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Selector
                Text(
                    text = "Color: $selectedColor",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CLOSET_COLORS.forEach { (colorName, colorVal) ->
                        val isSelected = selectedColor.equals(colorName, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colorVal)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorName },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (colorName == "White" || colorName == "Beige" || colorName == "Yellow") Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Brand & Size in 1 row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand (e.g. Uniqlo)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = size,
                        onValueChange = { size = it },
                        label = { Text("Size (e.g. M / 32)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Season & Occasion
                Text(
                    text = "Season",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CLOSET_SEASONS.forEach { season ->
                        FilterChip(
                            selected = selectedSeason == season,
                            onClick = { selectedSeason = season },
                            label = { Text(season, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Occasion",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CLOSET_OCCASIONS.forEach { occ ->
                        FilterChip(
                            selected = selectedOccasion == occ,
                            onClick = { selectedOccasion = occ },
                            label = { Text(occ, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status Selector
                Text(
                    text = "Status",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CLOSET_STATUSES.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Purchase Price ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Care details") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val price = priceInput.toDoubleOrNull() ?: 0.0
                                onSave(
                                    name.trim(),
                                    selectedCategory,
                                    selectedColor,
                                    selectedSeason,
                                    brand.trim(),
                                    size.trim(),
                                    selectedOccasion,
                                    imagePath,
                                    selectedStatus,
                                    price,
                                    notes.trim()
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_closet_item_button")
                    ) {
                        Text(if (itemToEdit != null) "Update Item" else "Save to Closet")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// CLOSET ITEM DETAIL DIALOG
// ----------------------------------------------------
@Composable
fun ClosetItemDetailDialog(
    item: ClosetItem,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onLogWorn: () -> Unit,
    onUpdateImage: (String) -> Unit = {}
) {
    val costPerWear = if (item.timesWorn > 0 && item.purchasePrice > 0) item.purchasePrice / item.timesWorn else item.purchasePrice
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val saved = saveClosetImageToInternalStorage(context, it, "item")
            if (saved != null) {
                onUpdateImage(saved)
                Toast.makeText(context, "Item photo updated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not import image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 1. Large Preview Box / Image Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .background(getColorForName(item.color).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isValidImagePath(item.imagePath)) {
                        AsyncImage(
                            model = File(item.imagePath),
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = getCategoryIcon(item.category),
                            contentDescription = item.category,
                            tint = getColorForName(item.color),
                            modifier = Modifier.size(88.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Icon Controls & Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left action icons: Camera, Delete Photo, Close
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Camera / Add photo
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { imagePickerLauncher.launch("image/*") }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Upload Photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Delete / remove photo
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    if (isValidImagePath(item.imagePath)) {
                                        onUpdateImage("")
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Photo",
                                    tint = if (isValidImagePath(item.imagePath)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Close (X) button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable(onClick = onDismiss)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Right metadata: • CATEGORY • OCCASION
                    val metaTags = buildList {
                        if (item.category.isNotBlank()) add(item.category.uppercase())
                        if (item.occasion.isNotBlank() && item.occasion != "Casual") add(item.occasion.uppercase())
                        else if (item.brand.isNotBlank()) add(item.brand.uppercase())
                        else if (item.season.isNotBlank() && item.season != "All Season") add(item.season.uppercase())
                        else add("CASUAL")
                    }
                    Text(
                        text = "• " + metaTags.joinToString(" • "),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Item Name & Favorite Star
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name.uppercase(),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Status Buttons Row: IN CLOSET, IN LAUNDRY, DRY CLEANING
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CLOSET_STATUSES.take(3).forEach { st ->
                        val isSelected = item.status == st
                        Surface(
                            onClick = { onUpdateStatus(st) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Text(
                                text = st.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. Bottom Action Row: DELETE & Edit Item
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "DELETE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Item",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeBadge(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ----------------------------------------------------
// ADD / EDIT OUTFIT DIALOG
// ----------------------------------------------------
@Composable
fun AddEditOutfitDialog(
    outfitToEdit: ClosetOutfit?,
    allClosetItems: List<ClosetItem>,
    onDismiss: () -> Unit,
    onSave: (name: String, occasion: String, selectedItemIds: List<Long>, notes: String, imagePath: String) -> Unit
) {
    var name by remember { mutableStateOf(outfitToEdit?.name ?: "") }
    var selectedOccasion by remember { mutableStateOf(outfitToEdit?.occasion ?: "Casual") }
    var notes by remember { mutableStateOf(outfitToEdit?.notes ?: "") }
    var selectedItemIds by remember {
        mutableStateOf(
            outfitToEdit?.itemIds?.split(",")?.mapNotNull { it.trim().toLongOrNull() }?.toSet() ?: emptySet()
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (outfitToEdit != null) "Edit Outfit" else "Create New Outfit",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Outfit Name * (e.g. Monday Casual)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Occasion",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CLOSET_OCCASIONS.forEach { occ ->
                        FilterChip(
                            selected = selectedOccasion == occ,
                            onClick = { selectedOccasion = occ },
                            label = { Text(occ, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Items for this Outfit (${selectedItemIds.size} selected)",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (allClosetItems.isEmpty()) {
                    Text("No items in closet yet. Add some clothes first!", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.error)
                } else {
                    // Grouped by Category
                    val groupedItems = remember(allClosetItems) {
                        allClosetItems.groupBy { it.category }
                    }

                    groupedItems.forEach { (cat, itemsInCat) ->
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                        itemsInCat.forEach { item ->
                            val isChecked = selectedItemIds.contains(item.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedItemIds = if (isChecked) {
                                            selectedItemIds - item.id
                                        } else {
                                            selectedItemIds + item.id
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedItemIds = if (checked) {
                                            selectedItemIds + item.id
                                        } else {
                                            selectedItemIds - item.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (isValidImagePath(item.imagePath)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    ) {
                                        AsyncImage(
                                            model = File(item.imagePath),
                                            contentDescription = item.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(getColorForName(item.color))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = item.name,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Outfit Notes (optional)") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name.trim(), selectedOccasion, selectedItemIds.toList(), notes.trim(), "")
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (outfitToEdit != null) "Update Outfit" else "Create Outfit")
                    }
                }
            }
        }
    }
}
