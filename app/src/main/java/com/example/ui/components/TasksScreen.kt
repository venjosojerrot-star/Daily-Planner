package com.example.ui.components
 
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlannerTask
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    tasks: List<PlannerTask>,
    onToggleTask: (PlannerTask) -> Unit,
    onToggleStar: (PlannerTask) -> Unit,
    onEditTask: (PlannerTask) -> Unit,
    onAddNewTask: (String?) -> Unit,
    onDeleteCategory: (String) -> Unit = {},
    onRenameCategory: (oldCategory: String, newCategory: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tasks_lists", Context.MODE_PRIVATE) }

    var customLists by remember {
        mutableStateOf(
            run {
                val saved = prefs.getStringSet("categories", null)
                val initial = if (saved == null) {
                    val existing = tasks.map { it.category }.distinct()
                    val result = existing.filter { it.isNotBlank() }.map { it.trim() }.distinctBy { it.lowercase() }.sorted()
                    prefs.edit().putStringSet("categories", result.toSet()).apply()
                    result
                } else {
                    saved.filter { it.isNotBlank() }.map { it.trim() }.distinctBy { it.lowercase() }.sorted()
                }
                initial
            }
        )
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    if (selectedTabIndex >= customLists.size && customLists.isNotEmpty()) {
        selectedTabIndex = customLists.size - 1
    }

    var showAddListDialog by remember { mutableStateOf(false) }
    var renamingCategory by remember { mutableStateOf<String?>(null) }

    // Removed LaunchedEffect(tasks) that overwrote customLists and prevented list deletion / creation consistency.

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Scrollable Tab Row at the top
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF81C784) // Green indicator like in image
                        )
                    }
                }
            ) {
                customLists.forEachIndexed { index, category ->
                    val listTasks = tasks.filter { it.category == category && !it.isCompleted }
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = category,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                if (listTasks.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${listTasks.size}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                // "+ New List" Tab
                Tab(
                    selected = false,
                    onClick = { showAddListDialog = true },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New list", fontSize = 14.sp)
                        }
                    }
                )
            }

            if (customLists.isNotEmpty()) {
                val currentCategory = customLists[selectedTabIndex]
                val currentTasks = tasks.filter { it.category == currentCategory }

                TaskListContent(
                    category = currentCategory,
                    tasks = currentTasks,
                    onToggleTask = onToggleTask,
                    onToggleStar = onToggleStar,
                    onEditTask = onEditTask,
                    onAddTaskInList = { onAddNewTask(currentCategory) },
                    onRenameList = {
                        renamingCategory = currentCategory
                    },
                    onDeleteList = {
                        val currentCategoryToDelete = currentCategory
                        val currentSaved = prefs.getStringSet("categories", emptySet()) ?: emptySet()
                        val newSaved = currentSaved.filter { !it.equals(currentCategoryToDelete, ignoreCase = true) && it.isNotBlank() }.toSet()
                        prefs.edit().putStringSet("categories", newSaved).apply()

                        customLists = newSaved.map { it.trim() }.distinctBy { it.lowercase() }.sorted()
                        if (selectedTabIndex >= customLists.size && customLists.isNotEmpty()) {
                            selectedTabIndex = (customLists.size - 1).coerceAtLeast(0)
                        }
                        onDeleteCategory(currentCategoryToDelete)
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No lists. Create one to get started.")
                }
            }
        }

        // Floating Action Button at bottom right
        FloatingActionButton(
            onClick = { 
                val currentCategory = if (customLists.isNotEmpty()) customLists[selectedTabIndex] else null
                onAddNewTask(currentCategory) 
            },
            containerColor = Color(0xFF1B5E20), // Dark green background like image
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp)
                .testTag("add_new_task_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(32.dp))
        }
    }

    // Rename List Dialog
    renamingCategory?.let { oldCategoryName ->
        var updatedListName by remember(oldCategoryName) { mutableStateOf(oldCategoryName) }
        AlertDialog(
            onDismissRequest = { renamingCategory = null },
            title = { Text("Rename List", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = updatedListName,
                    onValueChange = { updatedListName = it },
                    label = { Text("List Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_list_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = updatedListName.trim()
                        if (cleaned.isNotBlank()) {
                            if (!cleaned.equals(oldCategoryName, ignoreCase = false)) {
                                val currentSaved = prefs.getStringSet("categories", emptySet()) ?: emptySet()
                                val newSaved = currentSaved
                                    .filter { !it.equals(oldCategoryName, ignoreCase = true) && it.isNotBlank() }
                                    .toMutableSet()
                                newSaved.add(cleaned)
                                prefs.edit().putStringSet("categories", newSaved).apply()

                                onRenameCategory(oldCategoryName, cleaned)
                                customLists = newSaved.map { it.trim() }.distinctBy { it.lowercase() }.sorted()
                                val newIdx = customLists.indexOfFirst { it.equals(cleaned, ignoreCase = true) }
                                selectedTabIndex = if (newIdx >= 0) newIdx else 0
                            }
                            renamingCategory = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_rename_list_btn")
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add List Name dialog
    if (showAddListDialog) {
        var newListName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddListDialog = false },
            title = { Text("Create New List") },
            text = {
                OutlinedTextField(
                    value = newListName,
                    onValueChange = { newListName = it },
                    label = { Text("List Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_list_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newListName.isNotBlank()) {
                            val cleaned = newListName.trim()
                            val currentSaved = prefs.getStringSet("categories", emptySet()) ?: emptySet()
                            val newSaved = (currentSaved + cleaned).filter { it.isNotBlank() }.map { it.trim() }.distinctBy { it.lowercase() }.toSet()
                            prefs.edit().putStringSet("categories", newSaved).apply()

                            customLists = newSaved.map { it.trim() }.distinctBy { it.lowercase() }.sorted()
                            val idx = customLists.indexOfFirst { it.equals(cleaned, ignoreCase = true) }
                            selectedTabIndex = if (idx >= 0) idx else 0
                            showAddListDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_list_btn")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddListDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TaskListContent(
    category: String,
    tasks: List<PlannerTask>,
    onToggleTask: (PlannerTask) -> Unit,
    onToggleStar: (PlannerTask) -> Unit,
    onEditTask: (PlannerTask) -> Unit,
    onAddTaskInList: () -> Unit,
    onRenameList: () -> Unit = {},
    onDeleteList: () -> Unit
) {
    val incompleteTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }
    var isCompletedExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    IconButton(onClick = { /* Sort */ }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("list_options_menu_btn")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename List") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename List"
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onRenameList()
                                },
                                modifier = Modifier.testTag("menu_rename_list")
                            )
                            DropdownMenuItem(
                                text = { Text("Delete List") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete List",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDeleteList()
                                },
                                modifier = Modifier.testTag("menu_delete_list")
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Incomplete Tasks
                items(incompleteTasks) { task ->
                    TaskItem(
                        task = task,
                        onToggle = { onToggleTask(task) },
                        onToggleStar = { onToggleStar(task) },
                        onClick = { onEditTask(task) }
                    )
                }

                if (completedTasks.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCompletedExpanded = !isCompletedExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Completed (${completedTasks.size})",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (isCompletedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    if (isCompletedExpanded) {
                        items(completedTasks) { task ->
                            TaskItem(
                                task = task,
                                onToggle = { onToggleTask(task) },
                                onToggleStar = { onToggleStar(task) },
                                onClick = { onEditTask(task) }
                            )
                        }
                    }
                }

                if (tasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tasks yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: PlannerTask,
    onToggle: () -> Unit,
    onToggleStar: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Radio-style Checkbox
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = "Toggle Complete",
                tint = if (task.isCompleted) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontSize = 16.sp,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
            if (task.description.isNotEmpty() && task.isCompleted) {
                Text(
                    text = task.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = onToggleStar,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (task.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = "Star",
                tint = if (task.isStarred) Color(0xFFFFD600) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
