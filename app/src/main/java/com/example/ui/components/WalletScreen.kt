package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.data.model.WalletTransaction
import com.example.data.model.WalletDebt
import com.example.data.model.WalletBudget
import com.example.data.model.WalletGoal
import com.example.data.model.WalletAccount
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.theme.BurgundyTopBar
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    transactions: List<WalletTransaction>,
    debts: List<WalletDebt> = emptyList(),
    budgets: List<WalletBudget> = emptyList(),
    goals: List<WalletGoal> = emptyList(),
    accounts: List<WalletAccount> = emptyList(),
    selectedCurrency: String = "₱ PHP",
    onAddTransaction: (title: String, category: String, amount: Double, isIncome: Boolean, note: String, timestampMillis: Long, accountName: String) -> Unit,
    onUpdateTransaction: (WalletTransaction) -> Unit,
    onDeleteTransaction: (WalletTransaction) -> Unit,
    onAddDebt: (personName: String, purpose: String, amount: Double, isLent: Boolean) -> Unit,
    onDeleteDebt: (WalletDebt) -> Unit,
    onToggleDebtSettlement: (WalletDebt) -> Unit,
    onAddBudget: (category: String, limitAmount: Double, fromDateMillis: Long, toDateMillis: Long) -> Unit,
    onUpdateBudget: (WalletBudget) -> Unit = {},
    onDeleteBudget: (WalletBudget) -> Unit,
    onAddGoal: (name: String, targetAmount: Double, currentAmount: Double, deadlineMillis: Long) -> Unit,
    onUpdateGoal: (WalletGoal) -> Unit,
    onDeleteGoal: (WalletGoal) -> Unit,
    onAddContributionToGoal: (WalletGoal, Double) -> Unit,
    onAddAccount: (name: String, type: String, initialBalance: Double) -> Unit,
    onDeleteAccount: (WalletAccount) -> Unit
) {
    val context = LocalContext.current
    val walletPrefs = remember { context.getSharedPreferences("wallet_categories_prefs", Context.MODE_PRIVATE) }
    var customCategories by remember {
        val initialSet: Set<String> = walletPrefs.getStringSet("custom_categories", emptySet<String>()) ?: emptySet()
        mutableStateOf<List<String>>(initialSet.toList().sorted())
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<WalletTransaction?>(null) }
    var defaultIsIncome by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedAccountFilter by remember { mutableStateOf("All") }

    val listState = rememberLazyListState()
    val categoryScrollState = rememberLazyListState()

    // Dialog flags for extra features
    var showAddDebtChoiceDialog by remember { mutableStateOf(false) }
    var showDebtInputDialog by remember { mutableStateOf<Boolean?>(null) } // true for lent, false for borrowed, null for closed
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<WalletBudget?>(null) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showGoalContributionDialog by remember { mutableStateOf<WalletGoal?>(null) }
    var editingGoal by remember { mutableStateOf<WalletGoal?>(null) }
    var showAddAccountDialog by remember { mutableStateOf(false) }

    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val balance = accounts.sumOf { account ->
        val accountTransactions = transactions.filter { it.accountName.equals(account.name, ignoreCase = true) }
        account.initialBalance + 
                accountTransactions.filter { it.isIncome }.sumOf { it.amount } - 
                accountTransactions.filter { !it.isIncome }.sumOf { it.amount }
    }

    val currencySymbol = remember(selectedCurrency) {
        when {
            selectedCurrency.contains("PHP") || selectedCurrency.contains("₱") -> "₱"
            selectedCurrency.contains("EUR") || selectedCurrency.contains("€") -> "€"
            selectedCurrency.contains("GBP") || selectedCurrency.contains("£") -> "£"
            selectedCurrency.contains("JPY") || selectedCurrency.contains("¥") -> "¥"
            else -> "$"
        }
    }

    fun formatAmount(amount: Double): String {
        val formatted = String.format(Locale.US, "%,.2f", amount)
        return "$currencySymbol $formatted"
    }

    val categories: List<String> = remember(customCategories) {
        val defaultCats: List<String> = listOf("All", "Food", "Salary", "Freelance", "Bills", "Shopping", "Entertainment", "Transportation", "Health", "Investment", "Gift", "Others")
        val customOnly: List<String> = customCategories.filter { it !in defaultCats }
        defaultCats + customOnly
    }

    fun addCustomCategory(newCategory: String) {
        val trimmed = newCategory.trim()
        if (trimmed.isNotBlank() && !categories.any { it.equals(trimmed, ignoreCase = true) }) {
            val updated = (customCategories + trimmed).distinct().sorted()
            customCategories = updated
            walletPrefs.edit().putStringSet("custom_categories", updated.toSet()).apply()
            selectedCategoryFilter = trimmed
        }
    }

    val filteredTransactions = transactions.filter { transaction ->
        val matchesCategory = selectedCategoryFilter == "All" || transaction.category.equals(selectedCategoryFilter, ignoreCase = true)
        val matchesAccount = selectedAccountFilter == "All" || transaction.accountName.equals(selectedAccountFilter, ignoreCase = true)
        matchesCategory && matchesAccount
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("wallet_screen")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Balance Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BurgundyTopBar),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "MY WALLET BALANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = formatAmount(balance),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Income / Expense Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Income Box
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Income",
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Income",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = formatAmount(totalIncome),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF81C784)
                                    )
                                }
                            }

                            // Expense Box
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE57373).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Expense",
                                        tint = Color(0xFFFF8A80),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Expenses",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = formatAmount(totalExpense),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFF8A80)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Accounts horizontal list
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MY ACCOUNTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                        TextButton(
                            onClick = { showAddAccountDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Account", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All Accounts Card
                        item {
                            val isSelected = selectedAccountFilter == "All"
                            Card(
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(85.dp)
                                    .clickable { selectedAccountFilter = "All" },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "ALL",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column {
                                        Text("Total Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                        Text(
                                            text = formatAmount(balance),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // User Accounts Cards
                        items(accounts) { account ->
                            val isSelected = selectedAccountFilter == account.name
                            val accountTransactions = transactions.filter { it.accountName.equals(account.name, ignoreCase = true) }
                            val accountBalance = account.initialBalance + 
                                    accountTransactions.filter { it.isIncome }.sumOf { it.amount } - 
                                    accountTransactions.filter { !it.isIncome }.sumOf { it.amount }

                            val accountIcon = when (account.type.lowercase()) {
                                "electronic account" -> Icons.Default.Smartphone
                                "bank account" -> Icons.Default.AccountBalance
                                else -> Icons.Default.AttachMoney
                            }

                            Card(
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(85.dp)
                                    .clickable { selectedAccountFilter = account.name },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = accountIcon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        if (account.name != "Cash") {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete account",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { 
                                                        onDeleteAccount(account)
                                                        if (selectedAccountFilter == account.name) {
                                                            selectedAccountFilter = "All"
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = account.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = formatAmount(accountBalance),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions section
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                defaultIsIncome = true
                                showAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Income", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                defaultIsIncome = false
                                showAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tabs Selector (Under Quick Actions)
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = BurgundyTopBar
                            )
                        }
                    ) {
                        val tabs = listOf("Transactions", "Debts", "Budgets", "Goals")
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                selectedContentColor = BurgundyTopBar,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Dynamic Tab Views
            when (selectedTabIndex) {
                0 -> {
                    // TRANSACTIONS TAB
                    item {
                        SpendingPieChart(
                            transactions = transactions,
                            currencySymbol = currencySymbol,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    item {
                        IncomePieChart(
                            transactions = transactions,
                            currencySymbol = currencySymbol,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    item {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Recent Transactions",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    // + button next to the text recent transactions to add a new category
                                    FilledTonalIconButton(
                                        onClick = { showAddCategoryDialog = true },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("add_wallet_category_btn"),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add New Category",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (filteredTransactions.isNotEmpty()) {
                                    Text(
                                        text = "${filteredTransactions.size} items",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Horizontally scrollable category filter chips
                            LazyRow(
                                state = categoryScrollState,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("categories_scroll_row")
                            ) {
                                items(categories) { category ->
                                    val isSelected = selectedCategoryFilter == category
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategoryFilter = category },
                                        label = { Text(category, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }

                                item {
                                    AssistChip(
                                        onClick = { showAddCategoryDialog = true },
                                        label = { Text("+ Category", fontSize = 12.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Category",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (filteredTransactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No transactions found",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredTransactions, key = { it.id }) { item ->
                            TransactionItemRow(
                                transaction = item,
                                currencySymbol = currencySymbol,
                                onDelete = { onDeleteTransaction(item) },
                                onClick = { editingTransaction = item }
                            )
                        }
                    }
                }
                1 -> {
                    // DEBTS TAB
                    val lentDebts = debts.filter { it.isLent }
                    val borrowedDebts = debts.filter { !it.isLent }
                    val totalLent = lentDebts.filter { !it.isSettled }.sumOf { it.amount }
                    val totalBorrowed = borrowedDebts.filter { !it.isSettled }.sumOf { it.amount }

                    item {
                        // Debts overview cards
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("I Lent (To Collect)", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(formatAmount(totalLent), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("I Borrowed (To Pay)", fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(formatAmount(totalBorrowed), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Manage Debts", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { showAddDebtChoiceDialog = true },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Debt", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    if (debts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No debts recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(debts, key = { it.id }) { debt ->
                            DebtRowItem(
                                debt = debt,
                                currencySymbol = currencySymbol,
                                formatAmount = ::formatAmount,
                                onToggleSettled = { onToggleDebtSettlement(debt) },
                                onDelete = { onDeleteDebt(debt) }
                            )
                        }
                    }
                }
                2 -> {
                    // BUDGETS TAB
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category Budgets", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { showAddBudgetDialog = true },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Budget", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    if (budgets.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No budgets set yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(budgets, key = { it.id }) { budget ->
                            val spent = transactions
                                .filter { tx ->
                                    !tx.isIncome &&
                                    tx.category.equals(budget.category, ignoreCase = true) &&
                                    (budget.fromDateMillis <= 0L || tx.timestampMillis >= DateTimeUtils.getStartOfDay(budget.fromDateMillis)) &&
                                    (budget.toDateMillis <= 0L || tx.timestampMillis <= DateTimeUtils.getEndOfDay(budget.toDateMillis))
                                }
                                .sumOf { it.amount }
                            BudgetRowItem(
                                budget = budget,
                                spent = spent,
                                currencySymbol = currencySymbol,
                                formatAmount = ::formatAmount,
                                onEdit = { editingBudget = budget },
                                onDelete = { onDeleteBudget(budget) }
                            )
                        }
                    }
                }
                3 -> {
                    // GOALS TAB
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Savings Goals", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { showAddGoalDialog = true },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Savings Goal", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    if (goals.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No goals set yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(goals, key = { it.id }) { goal ->
                            GoalRowItem(
                                goal = goal,
                                currencySymbol = currencySymbol,
                                formatAmount = ::formatAmount,
                                onAddContribution = { showGoalContributionDialog = goal },
                                onEdit = { editingGoal = goal },
                                onDelete = { onDeleteGoal(goal) }
                            )
                        }
                    }
                }
            }
        }

        // Vertical scrollbar on the right side ONLY for Recent Transactions in Transactions tab
        if (selectedTabIndex == 0 && filteredTransactions.isNotEmpty()) {
            WalletVerticalScrollbar(
                listState = listState,
                firstItemIndex = 7,
                itemCount = filteredTransactions.size,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .padding(top = 8.dp, bottom = 80.dp)
            )
        }

        // Add Category Dialog
        if (showAddCategoryDialog) {
            var newCategoryName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Add Category",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enter a name for the new transaction category to organize your spending and income.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Category Name") },
                            placeholder = { Text("e.g. Groceries, Gym, Coffee") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_category_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                addCustomCategory(newCategoryName)
                                showAddCategoryDialog = false
                            }
                        },
                        enabled = newCategoryName.isNotBlank(),
                        modifier = Modifier.testTag("save_new_category_btn")
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add Transaction Dialog
        if (showAddDialog) {
            AddTransactionDialog(
                initialIsIncome = defaultIsIncome,
                currencySymbol = currencySymbol,
                availableAccounts = accounts,
                customCategories = customCategories,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, category, amount, isIncome, note, timestampMillis, accountName ->
                    onAddTransaction(title, category, amount, isIncome, note, timestampMillis, accountName)
                    showAddDialog = false
                }
            )
        }

        // Edit Transaction Dialog
        if (editingTransaction != null) {
            AddTransactionDialog(
                initialIsIncome = editingTransaction!!.isIncome,
                currencySymbol = currencySymbol,
                editingTransaction = editingTransaction,
                availableAccounts = accounts,
                customCategories = customCategories,
                onDismiss = { editingTransaction = null },
                onConfirm = { title, category, amount, isIncome, note, timestampMillis, accountName ->
                    onUpdateTransaction(
                        editingTransaction!!.copy(
                            title = title,
                            category = category,
                            amount = amount,
                            isIncome = isIncome,
                            note = note,
                            timestampMillis = timestampMillis,
                            accountName = accountName
                        )
                    )
                    editingTransaction = null
                }
            )
        }

        // Debt Choices Dialog (I Lent or I Borrowed)
        if (showAddDebtChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showAddDebtChoiceDialog = false },
                title = { Text("Choose Debt Type", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                showDebtInputDialog = true // true = Lent
                                showAddDebtChoiceDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Icon(imageVector = Icons.Outlined.AttachMoney, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("I Lent (To Someone)")
                        }
                        Button(
                            onClick = {
                                showDebtInputDialog = false // false = Borrowed
                                showAddDebtChoiceDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Icon(imageVector = Icons.Outlined.MoneyOff, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("I Borrowed (From Someone)")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddDebtChoiceDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Input Details Dialog (Debt)
        showDebtInputDialog?.let { isLent ->
            var personName by remember { mutableStateOf("") }
            var purpose by remember { mutableStateOf("") }
            var amountText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showDebtInputDialog = null },
                title = {
                    Text(
                        text = if (isLent) "Add Record: I Lent" else "Add Record: I Borrowed",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = personName,
                            onValueChange = { personName = it },
                            label = { Text("Person's Name") },
                            placeholder = { Text("e.g. John Doe") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = purpose,
                            onValueChange = { purpose = it },
                            label = { Text("Purpose") },
                            placeholder = { Text("e.g. Dinner, Rent") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount ($currencySymbol)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amountVal = amountText.toDoubleOrNull() ?: 0.0
                            if (personName.isNotBlank() && amountVal > 0) {
                                onAddDebt(personName.trim(), purpose.trim(), amountVal, isLent)
                                showDebtInputDialog = null
                            }
                        },
                        enabled = personName.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDebtInputDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Budget Creator & Editor Dialog
        if (showAddBudgetDialog || editingBudget != null) {
            val isEditing = editingBudget != null
            val currentBudget = editingBudget
            var selectedCategory by remember(editingBudget) {
                mutableStateOf(currentBudget?.category ?: "Food")
            }
            var limitText by remember(editingBudget) {
                mutableStateOf(currentBudget?.limitAmount?.let { if (it > 0) it.toString() else "" } ?: "")
            }
            val budgetCategories: List<String> = remember(customCategories) {
                val defaultList: List<String> = listOf("Food", "Bills", "Shopping", "Entertainment", "Transportation", "Health", "Others")
                val extras: List<String> = customCategories.filter { it !in defaultList }
                defaultList + extras
            }

            val defaultStart = remember {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            val defaultEnd = remember {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                cal.timeInMillis
            }

            var hasDateRange by remember(editingBudget) {
                mutableStateOf(currentBudget?.let { it.fromDateMillis > 0L && it.toDateMillis > 0L } ?: true)
            }
            var fromDateMillis by remember(editingBudget) {
                mutableStateOf(
                    if (currentBudget != null && currentBudget.fromDateMillis > 0L) currentBudget.fromDateMillis else defaultStart
                )
            }
            var toDateMillis by remember(editingBudget) {
                mutableStateOf(
                    if (currentBudget != null && currentBudget.toDateMillis > 0L) currentBudget.toDateMillis else defaultEnd
                )
            }

            val context = LocalContext.current
            val isDateOrderValid = !hasDateRange || toDateMillis >= fromDateMillis

            AlertDialog(
                onDismissRequest = {
                    showAddBudgetDialog = false
                    editingBudget = null
                },
                title = {
                    Text(
                        if (isEditing) "Edit Category Budget" else "Create Category Budget",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Choose Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(budgetCategories) { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = limitText,
                            onValueChange = { limitText = it },
                            label = { Text("Budget Limit ($currencySymbol)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Budget Date Range Option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Budget Period (Date Range)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (hasDateRange) "Filter expenses within set dates" else "All-time (no date limit)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = hasDateRange,
                                onCheckedChange = { hasDateRange = it }
                            )
                        }

                        if (hasDateRange) {
                            // Quick Range Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        fromDateMillis = defaultStart
                                        toDateMillis = defaultEnd
                                    },
                                    label = { Text("This Month", fontSize = 10.5.sp) }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        fromDateMillis = cal.timeInMillis
                                        cal.add(Calendar.DAY_OF_MONTH, 30)
                                        toDateMillis = cal.timeInMillis
                                    },
                                    label = { Text("Next 30 Days", fontSize = 10.5.sp) }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                                        cal.add(Calendar.DAY_OF_WEEK, -(dayOfWeek - 1))
                                        fromDateMillis = cal.timeInMillis
                                        cal.add(Calendar.DAY_OF_WEEK, 6)
                                        toDateMillis = cal.timeInMillis
                                    },
                                    label = { Text("This Week", fontSize = 10.5.sp) }
                                )
                            }

                            // Date From and Date To Pickers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // From Date Box
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val cal = Calendar.getInstance().apply { timeInMillis = fromDateMillis }
                                            android.app.DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val newCal = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, year)
                                                        set(Calendar.MONTH, month)
                                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                        set(Calendar.HOUR_OF_DAY, 0)
                                                        set(Calendar.MINUTE, 0)
                                                        set(Calendar.SECOND, 0)
                                                        set(Calendar.MILLISECOND, 0)
                                                    }
                                                    fromDateMillis = newCal.timeInMillis
                                                    if (toDateMillis < fromDateMillis) {
                                                        toDateMillis = DateTimeUtils.getEndOfDay(newCal.timeInMillis)
                                                    }
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                "From",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            DateTimeUtils.formatMediumDate(fromDateMillis),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // To Date Box
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDateOrderValid) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.error
                                    ),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val cal = Calendar.getInstance().apply { timeInMillis = toDateMillis }
                                            android.app.DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val newCal = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, year)
                                                        set(Calendar.MONTH, month)
                                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                        set(Calendar.HOUR_OF_DAY, 23)
                                                        set(Calendar.MINUTE, 59)
                                                        set(Calendar.SECOND, 59)
                                                        set(Calendar.MILLISECOND, 999)
                                                    }
                                                    toDateMillis = newCal.timeInMillis
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Event,
                                                contentDescription = null,
                                                tint = if (isDateOrderValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                "To",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDateOrderValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            DateTimeUtils.formatMediumDate(toDateMillis),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            if (!isDateOrderValid) {
                                Text(
                                    "\"To\" date must be after or on the \"From\" date.",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val limitVal = limitText.toDoubleOrNull() ?: 0.0
                            if (limitVal > 0 && isDateOrderValid) {
                                val finalFrom = if (hasDateRange) fromDateMillis else 0L
                                val finalTo = if (hasDateRange) toDateMillis else 0L
                                if (isEditing && currentBudget != null) {
                                    onUpdateBudget(
                                        currentBudget.copy(
                                            category = selectedCategory,
                                            limitAmount = limitVal,
                                            fromDateMillis = finalFrom,
                                            toDateMillis = finalTo
                                        )
                                    )
                                } else {
                                    onAddBudget(selectedCategory, limitVal, finalFrom, finalTo)
                                }
                                showAddBudgetDialog = false
                                editingBudget = null
                            }
                        },
                        enabled = (limitText.toDoubleOrNull() ?: 0.0) > 0 && isDateOrderValid
                    ) {
                        Text(if (isEditing) "Save Changes" else "Create Budget")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddBudgetDialog = false
                        editingBudget = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Goal Creator Dialog
        if (showAddGoalDialog) {
            var goalName by remember { mutableStateOf("") }
            var targetText by remember { mutableStateOf("") }
            var initialSavingsText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddGoalDialog = false },
                title = { Text("Create Savings Goal", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = goalName,
                            onValueChange = { goalName = it },
                            label = { Text("Goal Name") },
                            placeholder = { Text("e.g. New Laptop, Vacation") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { targetText = it },
                            label = { Text("Target Amount ($currencySymbol)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = initialSavingsText,
                            onValueChange = { initialSavingsText = it },
                            label = { Text("Initial Contribution ($currencySymbol)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val targetVal = targetText.toDoubleOrNull() ?: 0.0
                            val initialVal = initialSavingsText.toDoubleOrNull() ?: 0.0
                            if (goalName.isNotBlank() && targetVal > 0) {
                                onAddGoal(goalName.trim(), targetVal, initialVal, System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)) // Default 30 days
                                showAddGoalDialog = false
                            }
                        },
                        enabled = goalName.isNotBlank() && (targetText.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddGoalDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Goal Contribution Dialog
        showGoalContributionDialog?.let { goal ->
            var contributionText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showGoalContributionDialog = null },
                title = { Text("Contribute to: ${goal.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Current savings: ${formatAmount(goal.currentAmount)} / ${formatAmount(goal.targetAmount)}", fontSize = 12.sp)
                        OutlinedTextField(
                            value = contributionText,
                            onValueChange = { contributionText = it },
                            label = { Text("Contribution Amount ($currencySymbol)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val contribVal = contributionText.toDoubleOrNull() ?: 0.0
                            if (contribVal > 0) {
                                onAddContributionToGoal(goal, contribVal)
                                showGoalContributionDialog = null
                            }
                        },
                        enabled = (contributionText.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoalContributionDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Edit Goal Dialog
        editingGoal?.let { goal ->
            var goalName by remember { mutableStateOf(goal.name) }
            var targetText by remember { mutableStateOf(goal.targetAmount.let { if (it % 1.0 == 0.0) String.format(Locale.US, "%.0f", it) else it.toString() }) }
            var currentSavingsText by remember { mutableStateOf(goal.currentAmount.let { if (it % 1.0 == 0.0) String.format(Locale.US, "%.0f", it) else it.toString() }) }

            AlertDialog(
                onDismissRequest = { editingGoal = null },
                title = { Text("Edit Savings Goal", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = goalName,
                            onValueChange = { goalName = it },
                            label = { Text("Goal Name") },
                            placeholder = { Text("e.g. New Laptop") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { targetText = it },
                            label = { Text("Target Amount ($currencySymbol)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = currentSavingsText,
                            onValueChange = { currentSavingsText = it },
                            label = { Text("Contributed Savings ($currencySymbol)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val targetVal = targetText.toDoubleOrNull() ?: 0.0
                            val currentVal = currentSavingsText.toDoubleOrNull() ?: 0.0
                            if (goalName.isNotBlank() && targetVal > 0) {
                                onUpdateGoal(
                                    goal.copy(
                                        name = goalName.trim(),
                                        targetAmount = targetVal,
                                        currentAmount = currentVal
                                    )
                                )
                                editingGoal = null
                            }
                        },
                        enabled = goalName.isNotBlank() && (targetText.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingGoal = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add Account Dialog
        if (showAddAccountDialog) {
            var accountName by remember { mutableStateOf("") }
            var accountType by remember { mutableStateOf("Cash") } // "Cash", "Electronic account", "Bank account"
            var initialBalanceText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddAccountDialog = false },
                title = { Text("Create New Account", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = accountName,
                            onValueChange = { accountName = it },
                            label = { Text("Account Name") },
                            placeholder = { Text("e.g. My Savings Card") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Account Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val types = listOf("Cash", "Electronic account", "Bank account")
                            types.forEach { t ->
                                val isSelected = accountType == t
                                val label = when (t) {
                                    "Electronic account" -> "E-Wallet"
                                    "Bank account" -> "Bank"
                                    else -> "Cash"
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { accountType = t },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = initialBalanceText,
                            onValueChange = { initialBalanceText = it },
                            label = { Text("Initial Balance ($currencySymbol)") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val balanceVal = initialBalanceText.toDoubleOrNull() ?: 0.0
                            if (accountName.isNotBlank()) {
                                onAddAccount(accountName.trim(), accountType, balanceVal)
                                showAddAccountDialog = false
                            }
                        },
                        enabled = accountName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddAccountDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun WalletVerticalScrollbar(
    listState: LazyListState,
    firstItemIndex: Int,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo

    if (itemCount <= 0 || visibleItems.isEmpty()) return

    val firstVisibleIndex = listState.firstVisibleItemIndex
    val firstVisibleOffset = listState.firstVisibleItemScrollOffset

    val relativePosition = (firstVisibleIndex - firstItemIndex).coerceIn(0, itemCount - 1)
    val firstItemSize = visibleItems.firstOrNull()?.size?.toFloat() ?: 1f
    val currentPosition = (relativePosition.toFloat() + (firstVisibleOffset / firstItemSize.coerceAtLeast(1f)).coerceIn(0f, 1f))
    val maxScrollableIndex = (itemCount - 1).coerceAtLeast(1).toFloat()
    val scrollFraction = (currentPosition / maxScrollableIndex).coerceIn(0f, 1f)

    val visibleItemCount = visibleItems.count { it.index >= firstItemIndex && it.index < firstItemIndex + itemCount }
    val thumbRatio = (visibleItemCount.toFloat() / itemCount.toFloat()).coerceIn(0.15f, 0.85f)

    val isScrolling = listState.isScrollInProgress
    val alpha by animateFloatAsState(
        targetValue = if (isScrolling || isDragging) 1f else 0.55f,
        label = "wallet_scrollbar_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .testTag("wallet_recent_transactions_scrollbar")
            .width(24.dp)
            .pointerInput(itemCount, firstItemIndex) {
                detectTapGestures { offset ->
                    val trackHeight = size.height.toFloat()
                    if (trackHeight > 0) {
                        val targetProgress = (offset.y / trackHeight).coerceIn(0f, 1f)
                        val targetTxIndex = (targetProgress * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
                        val targetListIndex = firstItemIndex + targetTxIndex
                        coroutineScope.launch {
                            listState.animateScrollToItem(targetListIndex)
                        }
                    }
                }
            }
            .pointerInput(itemCount, firstItemIndex) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        change.consume()
                        val trackHeight = size.height.toFloat()
                        if (trackHeight > 0) {
                            val targetProgress = (change.position.y / trackHeight).coerceIn(0f, 1f)
                            val targetTxIndex = (targetProgress * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
                            val targetListIndex = firstItemIndex + targetTxIndex
                            coroutineScope.launch {
                                listState.scrollToItem(targetListIndex)
                            }
                        }
                    }
                )
            }
    ) {
        val totalHeight = maxHeight
        val minThumbHeight = 36.dp
        val computedThumbHeight = (totalHeight * thumbRatio).coerceAtLeast(minThumbHeight)
        val availableTrack = (totalHeight - computedThumbHeight).coerceAtLeast(0.dp)
        val thumbOffset = availableTrack * scrollFraction

        // Track line
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .width(4.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f * alpha))
        )

        // Thumb pill
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 3.dp)
                .offset(y = thumbOffset)
                .width(if (isDragging) 8.dp else 5.dp)
                .height(computedThumbHeight)
                .clip(CircleShape)
                .background(BurgundyTopBar.copy(alpha = alpha))
        )
    }
}

@Composable
private fun TransactionItemRow(
    transaction: WalletTransaction,
    currencySymbol: String,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val amountColor = if (transaction.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val amountSign = if (transaction.isIncome) "+" else "-"
    val formattedAmount = String.format(Locale.US, "%,.2f", transaction.amount)

    val categoryIcon = when (transaction.category.lowercase()) {
        "food" -> Icons.Default.Restaurant
        "salary" -> Icons.Default.AttachMoney
        "freelance" -> Icons.Default.Work
        "bills" -> Icons.Default.Receipt
        "shopping" -> Icons.Default.ShoppingBag
        "entertainment" -> Icons.Default.Movie
        else -> Icons.Default.AccountBalanceWallet
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (transaction.isIncome) Color(0xFF4CAF50).copy(alpha = 0.15f)
                        else Color(0xFFE57373).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = transaction.category,
                    tint = amountColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.category} • ${DateTimeUtils.formatDayMonth(transaction.timestampMillis)} • ${transaction.accountName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountSign$currencySymbol $formattedAmount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete transaction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DebtRowItem(
    debt: WalletDebt,
    currencySymbol: String,
    formatAmount: (Double) -> String,
    onToggleSettled: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = if (debt.isLent) Color(0xFF2E7D32) else Color(0xFFC62828)
    val typeLabel = if (debt.isLent) "Lent" else "Borrowed"
    val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(debt.timestampMillis))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (debt.isLent) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = typeLabel,
                    tint = typeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = debt.personName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(typeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = typeColor) },
                        modifier = Modifier.height(20.dp)
                    )
                }
                if (debt.purpose.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "For: ${debt.purpose}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Recorded: $formattedDate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(debt.amount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (debt.isSettled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else typeColor,
                    style = if (debt.isSettled) {
                        LocalTextStyle.current.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    } else {
                        LocalTextStyle.current
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onToggleSettled,
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (debt.isSettled) MaterialTheme.colorScheme.outline else typeColor
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(if (debt.isSettled) "Settled" else "Settle", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Debt",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BudgetRowItem(
    budget: WalletBudget,
    spent: Double,
    currencySymbol: String,
    formatAmount: (Double) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat() else 0f
    val isOverBudget = spent > budget.limitAmount
    val progressColor = if (isOverBudget) Color(0xFFC62828) else if (progress > 0.85f) Color(0xFFFFB300) else Color(0xFF2E7D32)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.category,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Date range badge if set
                    if (budget.fromDateMillis > 0L && budget.toDateMillis > 0L) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${DateTimeUtils.formatDayMonth(budget.fromDateMillis)} – ${DateTimeUtils.formatDayMonth(budget.toDateMillis)}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "All-time / Ongoing",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${formatAmount(spent)} of ${formatAmount(budget.limitAmount)}",
                        fontSize = 13.sp,
                        color = if (isOverBudget) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isOverBudget) FontWeight.Bold else FontWeight.Medium
                    )
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.15f)
            )

            if (isOverBudget) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠️ Overbudget by ${formatAmount(spent - budget.limitAmount)}!",
                    color = Color(0xFFC62828),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GoalRowItem(
    goal: WalletGoal,
    currencySymbol: String,
    formatAmount: (Double) -> String,
    onAddContribution: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val isCompleted = goal.currentAmount >= goal.targetAmount
    val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(goal.deadlineMillis))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = goal.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCompleted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Completed", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                        }
                    }
                    Text("Target by: $formattedDate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${formatAmount(goal.currentAmount)} / ${formatAmount(goal.targetAmount)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Goal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onAddContribution,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlusOne, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Contribute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionDialog(
    initialIsIncome: Boolean,
    currencySymbol: String,
    editingTransaction: WalletTransaction? = null,
    availableAccounts: List<WalletAccount> = emptyList(),
    customCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: String, amount: Double, isIncome: Boolean, note: String, timestampMillis: Long, accountName: String) -> Unit
) {
    var isIncome by remember { mutableStateOf(editingTransaction?.isIncome ?: initialIsIncome) }
    var title by remember { mutableStateOf(editingTransaction?.title ?: "") }
    var amountText by remember { mutableStateOf(editingTransaction?.amount?.let { if (it % 1.0 == 0.0) String.format(Locale.US, "%.0f", it) else it.toString() } ?: "") }
    var category by remember { mutableStateOf(editingTransaction?.category ?: (if (initialIsIncome) "Salary" else "Food")) }
    var note by remember { mutableStateOf(editingTransaction?.note ?: "") }
    var selectedDateMillis by remember { mutableStateOf(editingTransaction?.timestampMillis ?: System.currentTimeMillis()) }
    var selectedAccount by remember { mutableStateOf(editingTransaction?.accountName ?: (availableAccounts.firstOrNull()?.name ?: "Cash")) }

    val baseCategories = if (isIncome) {
        listOf("Salary", "Freelance", "Investment", "Gift", "Others")
    } else {
        listOf("Food", "Bills", "Shopping", "Entertainment", "Transportation", "Health", "Others")
    }
    val categories = remember(isIncome, customCategories) {
        (baseCategories + customCategories.filter { it !in baseCategories }).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingTransaction != null) {
                    if (isIncome) "Edit Income" else "Edit Expense"
                } else {
                    if (isIncome) "Add Income" else "Add Expense"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isIncome) Color(0xFF2E7D32) else Color.Transparent)
                            .clickable {
                                isIncome = true
                                category = "Salary"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isIncome) Color(0xFFC62828) else Color.Transparent)
                            .clickable {
                                isIncome = false
                                category = "Food"
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Description") },
                    placeholder = { Text("e.g. Lunch with team") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val context = LocalContext.current
                    val calendarState = remember(selectedDateMillis) {
                        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                    }

                    val dateText = remember(selectedDateMillis) {
                        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(selectedDateMillis))
                    }
                    val timeText = remember(selectedDateMillis) {
                        DateTimeUtils.formatTime(selectedDateMillis)
                    }

                    val datePickerDialog = android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            calendarState.set(Calendar.YEAR, year)
                            calendarState.set(Calendar.MONTH, month)
                            calendarState.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            selectedDateMillis = calendarState.timeInMillis
                        },
                        calendarState.get(Calendar.YEAR),
                        calendarState.get(Calendar.MONTH),
                        calendarState.get(Calendar.DAY_OF_MONTH)
                    )

                    val timePickerDialog = android.app.TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            calendarState.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendarState.set(Calendar.MINUTE, minute)
                            selectedDateMillis = calendarState.timeInMillis
                        },
                        calendarState.get(Calendar.HOUR_OF_DAY),
                        calendarState.get(Calendar.MINUTE),
                        false
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = {},
                            label = { Text("Date") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select Date"
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { datePickerDialog.show() }
                                .testTag("transaction_date_picker")
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = timeText,
                            onValueChange = {},
                            label = { Text("Time") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Select Time"
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { timePickerDialog.show() }
                                .testTag("transaction_time_picker")
                        )
                    }
                }

                Text(
                    text = "Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSelected = category == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Source Account",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val accNames = if (availableAccounts.isEmpty()) listOf("Cash") else availableAccounts.map { it.name }
                    items(accNames) { accName ->
                        val isSelected = selectedAccount == accName
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccount = accName },
                            label = { Text(accName, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amountVal > 0) {
                        onConfirm(title.trim(), category, amountVal, isIncome, note.trim(), selectedDateMillis, selectedAccount)
                    }
                },
                enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
