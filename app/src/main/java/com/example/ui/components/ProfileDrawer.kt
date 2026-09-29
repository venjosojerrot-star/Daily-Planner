package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.BurgundyTopBar
import com.example.ui.viewmodel.PlannerViewMode
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProfileDrawerContent(
    currentViewMode: PlannerViewMode,
    userName: String = "James Torrejos",
    userEmail: String = "torrejosjames17@gmail.com",
    onViewModeSelect: (PlannerViewMode) -> Unit,
    onOpenSettings: (String) -> Unit,
    onOpenQuoteOfTheDay: () -> Unit = {},
    onOpenDailyVerse: () -> Unit = {},
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current
    val profilePrefs = remember { context.getSharedPreferences("user_profile", Context.MODE_PRIVATE) }

    var showProfileEditDialog by remember { mutableStateOf(false) }
    var currentName by remember {
        mutableStateOf(profilePrefs.getString("profile_name", userName) ?: userName)
    }
    var currentEmail by remember {
        mutableStateOf(profilePrefs.getString("profile_email", userEmail) ?: userEmail)
    }
    var profileImageTimestamp by remember {
        mutableStateOf(profilePrefs.getLong("profile_image_timestamp", 0L))
    }

    val profileImageFile = remember(profileImageTimestamp) {
        File(context.filesDir, "user_profile_avatar.jpg")
    }
    val hasProfileImage = profileImageFile.exists() && profileImageFile.length() > 0

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val targetFile = File(context.filesDir, "user_profile_avatar.jpg")
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val newTimestamp = System.currentTimeMillis()
                profilePrefs.edit().putLong("profile_image_timestamp", newTimestamp).apply()
                profileImageTimestamp = newTimestamp
                Toast.makeText(context, "Profile image updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier
            .width(320.dp)
            .testTag("profile_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Card Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                BurgundyTopBar,
                                BurgundyTopBar.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    // Top Bar inside Header: Settings Gear Icon & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO PLANNER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Settings Gear Icon Button
                            IconButton(
                                onClick = {
                                    onOpenSettings("general")
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .testTag("drawer_settings_icon")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = onCloseDrawer,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Profile Avatar & Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProfileEditDialog = true }
                    ) {
                        // Avatar Badge with Initials / Profile Image Frame
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasProfileImage) {
                                key(profileImageTimestamp) {
                                    AsyncImage(
                                        model = profileImageFile,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(BurgundyTopBar.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentName.take(2).uppercase(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BurgundyTopBar
                                    )
                                }
                            }

                            // Small edit camera badge
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Edit Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentEmail,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Sections
            Text(
                text = "NAVIGATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            DrawerItemRow(
                icon = Icons.Default.CalendarViewDay,
                label = "Day Schedule",
                isSelected = currentViewMode == PlannerViewMode.DAY,
                onClick = {
                    onViewModeSelect(PlannerViewMode.DAY)
                    onCloseDrawer()
                }
            )

            DrawerItemRow(
                icon = Icons.Default.CalendarMonth,
                label = "Month Calendar",
                isSelected = currentViewMode == PlannerViewMode.MONTH,
                onClick = {
                    onViewModeSelect(PlannerViewMode.MONTH)
                    onCloseDrawer()
                }
            )

            DrawerItemRow(
                icon = Icons.Default.FormatListNumbered,
                label = "Agenda View",
                isSelected = currentViewMode == PlannerViewMode.AGENDA,
                onClick = {
                    onViewModeSelect(PlannerViewMode.AGENDA)
                    onCloseDrawer()
                }
            )

            DrawerItemRow(
                icon = Icons.Default.BarChart,
                label = "Events Stats",
                isSelected = currentViewMode == PlannerViewMode.STATS,
                onClick = {
                    onViewModeSelect(PlannerViewMode.STATS)
                    onCloseDrawer()
                }
            )

            DrawerItemRow(
                icon = Icons.Default.CheckCircleOutline,
                label = "Tasks & To-Dos",
                isSelected = currentViewMode == PlannerViewMode.TASKS,
                onClick = {
                    onViewModeSelect(PlannerViewMode.TASKS)
                    onCloseDrawer()
                }
            )

            DrawerItemRow(
                icon = Icons.Default.AccountBalanceWallet,
                label = "My Wallet & Expenses",
                isSelected = currentViewMode == PlannerViewMode.WALLET,
                onClick = {
                    onViewModeSelect(PlannerViewMode.WALLET)
                    onCloseDrawer()
                }
            )

            DrawerItemRow(
                icon = Icons.Default.Checkroom,
                label = "My Closet (Wardrobe & Outfits)",
                isSelected = currentViewMode == PlannerViewMode.CLOSET,
                onClick = {
                    onViewModeSelect(PlannerViewMode.CLOSET)
                    onCloseDrawer()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Preferences & Settings Section
            Text(
                text = "PREFERENCES & SETTINGS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            DrawerItemRow(
                icon = Icons.Default.FormatQuote,
                label = "Daily Quotes settings",
                isSelected = false,
                onClick = {
                    onCloseDrawer()
                    onOpenSettings("quotes")
                }
            )

            DrawerItemRow(
                icon = Icons.Default.AutoStories,
                label = "Daily verse settings",
                isSelected = false,
                onClick = {
                    onCloseDrawer()
                    onOpenSettings("verses")
                }
            )

            DrawerItemRow(
                icon = Icons.Default.Settings,
                label = "General settings",
                isSelected = false,
                onClick = {
                    onCloseDrawer()
                    onOpenSettings("general")
                }
            )

            DrawerItemRow(
                icon = Icons.Default.Wallet,
                label = "Wallet settings",
                isSelected = false,
                onClick = {
                    onCloseDrawer()
                    onOpenSettings("wallet")
                }
            )

            DrawerItemRow(
                icon = Icons.Default.Storage,
                label = "Account and data settings",
                isSelected = false,
                onClick = {
                    onCloseDrawer()
                    onOpenSettings("account")
                }
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Daily Planner & Wallet v2.1.0",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }

    // Edit Profile Dialog
    if (showProfileEditDialog) {
        var editName by remember(showProfileEditDialog) { mutableStateOf(currentName) }
        var editEmail by remember(showProfileEditDialog) { mutableStateOf(currentEmail) }

        AlertDialog(
            onDismissRequest = { showProfileEditDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile Image Preview & Quick Click to Import
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, BurgundyTopBar.copy(alpha = 0.5f), CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") }
                            .testTag("edit_dialog_avatar_preview"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasProfileImage) {
                            key(profileImageTimestamp) {
                                AsyncImage(
                                    model = profileImageFile,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Text(
                                text = editName.take(2).uppercase(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = BurgundyTopBar
                            )
                        }

                        // Badge overlay
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Import Image",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Import / Remove Buttons Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("import_profile_image_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Profile Image", fontSize = 12.sp)
                        }

                        if (hasProfileImage) {
                            IconButton(
                                onClick = {
                                    if (profileImageFile.exists()) {
                                        profileImageFile.delete()
                                    }
                                    val newTimestamp = System.currentTimeMillis()
                                    profilePrefs.edit().putLong("profile_image_timestamp", newTimestamp).apply()
                                    profileImageTimestamp = newTimestamp
                                    Toast.makeText(context, "Profile image removed", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Photo",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = editName.trim().ifEmpty { currentName }
                        val trimmedEmail = editEmail.trim().ifEmpty { currentEmail }
                        currentName = trimmedName
                        currentEmail = trimmedEmail
                        profilePrefs.edit()
                            .putString("profile_name", trimmedName)
                            .putString("profile_email", trimmedEmail)
                            .apply()
                        showProfileEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DrawerItemRow(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    initialCurrency: String = "₱ PHP",
    isWalletPinEnabled: Boolean = false,
    walletPin: String = "",
    activeSection: String = "all",
    themeColor: String = "Default",
    themeMode: String = "System",
    monthlyQuotes: List<String> = emptyList(),
    monthlyVerses: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSaveCurrency: (String) -> Unit = {},
    onSaveWalletPinSettings: (Boolean, String) -> Unit = { _, _ -> },
    onSaveThemeSettings: (String, String) -> Unit = { _, _ -> },
    onSaveQuotesText: (String) -> Unit = {},
    onSaveQuotesList: (List<String>) -> Unit = {},
    onResetQuotesDefault: () -> Unit = {},
    onSaveVersesText: (String) -> Unit = {},
    onSaveVersesList: (List<String>) -> Unit = {},
    onResetVersesDefault: () -> Unit = {},
    onExportData: () -> Unit = {},
    onImportData: () -> Unit = {}
) {
    val context = LocalContext.current
    val sdf = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }

    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var selectedCurrency by remember { mutableStateOf(initialCurrency) }
    var selectedWeekStart by remember { mutableStateOf("Sunday") }
    var selectedThemeColor by remember { mutableStateOf(themeColor) }
    var selectedThemeMode by remember { mutableStateOf(themeMode) }
    var walletPinEnabledState by remember { mutableStateOf(isWalletPinEnabled) }
    var walletPinState by remember { mutableStateOf(walletPin) }
    
    var showImportQuotesModal by remember { mutableStateOf(false) }
    var showQuoteDateSpanDialog by remember { mutableStateOf(false) }
    var showVerseDateSpanDialog by remember { mutableStateOf(false) }

    // Quote Date Range State
    var quoteDateFromMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var quoteDateToMillis by remember { mutableStateOf(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)) }

    // Verse Date Range State
    var verseDateFromMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var verseDateToMillis by remember { mutableStateOf(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)) }

    val versePrefs = remember { context.getSharedPreferences("daily_verse_settings", Context.MODE_PRIVATE) }
    var verseImageTimestamp by remember {
        mutableStateOf(versePrefs.getLong("verse_profile_image_timestamp", 0L))
    }
    var verseBotName by remember {
        mutableStateOf(versePrefs.getString("verse_bot_name", "Sister Emma") ?: "Sister Emma")
    }
    var verseBotGender by remember {
        mutableStateOf(versePrefs.getString("verse_bot_gender", "Sister (Female)") ?: "Sister (Female)")
    }
    val verseImageFile = remember(verseImageTimestamp) {
        File(context.filesDir, "daily_verse_avatar.jpg")
    }
    val hasVerseProfileImage = verseImageFile.exists() && verseImageFile.length() > 0

    val verseImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val targetFile = File(context.filesDir, "daily_verse_avatar.jpg")
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val newTimestamp = System.currentTimeMillis()
                versePrefs.edit().putLong("verse_profile_image_timestamp", newTimestamp).apply()
                verseImageTimestamp = newTimestamp
                Toast.makeText(context, "Daily Verse profile image updated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import profile image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val quotePrefs = remember { context.getSharedPreferences("daily_quotes_settings", Context.MODE_PRIVATE) }
    var quoteImageTimestamp by remember {
        mutableStateOf(quotePrefs.getLong("quote_profile_image_timestamp", 0L))
    }
    var quoteBotName by remember {
        mutableStateOf(quotePrefs.getString("quote_bot_name", "Maya") ?: "Maya")
    }
    var quoteBotGender by remember {
        mutableStateOf(quotePrefs.getString("quote_bot_gender", "Girlfriend (Female)") ?: "Girlfriend (Female)")
    }
    val quoteImageFile = remember(quoteImageTimestamp) {
        File(context.filesDir, "daily_quote_avatar.jpg")
    }
    val hasQuoteProfileImage = quoteImageFile.exists() && quoteImageFile.length() > 0

    val quoteImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val targetFile = File(context.filesDir, "daily_quote_avatar.jpg")
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val newTimestamp = System.currentTimeMillis()
                quotePrefs.edit().putLong("quote_profile_image_timestamp", newTimestamp).apply()
                quoteImageTimestamp = newTimestamp
                Toast.makeText(context, "Daily Quotes profile image updated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import profile image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val currencies = listOf("₱ PHP", "$ USD", "€ EUR", "£ GBP", "¥ JPY")

    val (titleText, titleIcon) = remember(activeSection) {
        when (activeSection) {
            "general" -> "General settings" to Icons.Default.Settings
            "wallet" -> "Wallet settings" to Icons.Default.Wallet
            "account" -> "Account and data settings" to Icons.Default.Storage
            "quotes" -> "Daily Quotes settings" to Icons.Default.FormatQuote
            "verses" -> "Daily verse settings" to Icons.Default.AutoStories
            else -> "Settings & Preferences" to Icons.Default.Settings
        }
    }

    fun pickQuoteStartDate() {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = quoteDateFromMillis }
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = java.util.Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
                quoteDateFromMillis = newCal.timeInMillis
                if (quoteDateToMillis < quoteDateFromMillis) {
                    quoteDateToMillis = quoteDateFromMillis + (30L * 24 * 60 * 60 * 1000)
                }
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun pickQuoteEndDate() {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = quoteDateToMillis }
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = java.util.Calendar.getInstance().apply { set(year, month, day, 23, 59, 59) }
                if (newCal.timeInMillis >= quoteDateFromMillis) {
                    quoteDateToMillis = newCal.timeInMillis
                } else {
                    Toast.makeText(context, "End date must be on or after start date", Toast.LENGTH_SHORT).show()
                }
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun pickVerseStartDate() {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = verseDateFromMillis }
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = java.util.Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
                verseDateFromMillis = newCal.timeInMillis
                if (verseDateToMillis < verseDateFromMillis) {
                    verseDateToMillis = verseDateFromMillis + (30L * 24 * 60 * 60 * 1000)
                }
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun pickVerseEndDate() {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = verseDateToMillis }
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                val newCal = java.util.Calendar.getInstance().apply { set(year, month, day, 23, 59, 59) }
                if (newCal.timeInMillis >= verseDateFromMillis) {
                    verseDateToMillis = newCal.timeInMillis
                } else {
                    Toast.makeText(context, "End date must be on or after start date", Toast.LENGTH_SHORT).show()
                }
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = titleIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(titleText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: General settings
                if (activeSection == "general" || activeSection == "all") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (activeSection == "all") {
                            Text(
                                text = "General settings",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Sound Effects Switch (as requested)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sound", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Play subtle chime on task completion", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { soundEnabled = it }
                            )
                        }

                        // App Theme (Mode and Color)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        
                        Column {
                            Text("App Theme Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Select application color mode", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("System", "Light", "Dark").forEach { mode ->
                                    FilterChip(
                                        selected = selectedThemeMode == mode,
                                        onClick = { selectedThemeMode = mode },
                                        label = { Text(mode, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        Column {
                            Text("App Theme Color", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Select application accent color palette", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            val themes = listOf("Default", "Blue", "Purple", "Green", "Pink", "Orange")
                            // Because "Default" uses GoogleBluePrimary, I'll name it Blue if it is not default, but let's just show options
                            // Actually just use rows of chips
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                themes.forEach { theme ->
                                    FilterChip(
                                        selected = selectedThemeColor == theme,
                                        onClick = { selectedThemeColor = theme },
                                        label = { Text(theme, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Category Colors (as requested)
                        Column {
                            Text("Category Colors", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Preset color mappings for schedule & task categories", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))

                            val categoryColors = listOf(
                                Triple("Grape", "Plan", Color(0xFF8E24AA)),
                                Triple("Dark Green", "Exercise", Color(0xFF1B5E20)),
                                Triple("Sage", "Sports", Color(0xFF558B2F)),
                                Triple("Tomato", "Meal", Color(0xFFD32F2F)),
                                Triple("Banana", "Prepare / Clean / Service", Color(0xFFFBC02D)),
                                Triple("Peacock", "Work", Color(0xFF039BE5)),
                                Triple("Blue Green", "Meeting / Class", Color(0xFF00897B)),
                                Triple("Pink", "Study", Color(0xFFEC407A)),
                                Triple("Brown", "Sleep", Color(0xFF795548)),
                                Triple("Flamingo", "Travel", Color(0xFFE91E63)),
                                Triple("Grey", "Leisure Time, Rest / Others", Color(0xFF757575))
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                categoryColors.forEach { (colorName, catName, colorHex) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(colorHex)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = colorName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colorHex,
                                                modifier = Modifier.width(72.dp)
                                            )
                                            Text(
                                                text = catName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION: Daily Quotes settings (Date from / to span generation)
                if (activeSection == "quotes" || activeSection == "all") {
                    if (activeSection == "all") {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FormatQuote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Daily Quotes Settings",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Manage guide identity, date spans, auto-generate, or edit quotes",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // SECTION 1: Guide Persona & Avatar Card for Daily Quotes
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "GUIDE PERSONA & AVATAR",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = "Motivational Speaker",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Avatar + Info Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Avatar Circle
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasQuoteProfileImage) {
                                                AsyncImage(
                                                    model = quoteImageFile,
                                                    contentDescription = "Daily Quote Profile Image",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Brush.linearGradient(listOf(Color(0xFF0084FF), Color(0xFF00C6FF)))),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.FormatQuote,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = quoteBotName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (hasQuoteProfileImage) "Custom photo set" else "Default quote icon",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Photo Action Buttons
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalButton(
                                                onClick = { quoteImagePickerLauncher.launch("image/*") },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .testTag("import_quote_image_profile_btn")
                                            ) {
                                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Photo", fontSize = 11.sp)
                                            }

                                            if (hasQuoteProfileImage) {
                                                IconButton(
                                                    onClick = {
                                                        if (quoteImageFile.exists()) {
                                                            quoteImageFile.delete()
                                                        }
                                                        val newTimestamp = System.currentTimeMillis()
                                                        quotePrefs.edit().putLong("quote_profile_image_timestamp", newTimestamp).apply()
                                                        quoteImageTimestamp = newTimestamp
                                                        Toast.makeText(context, "Daily Quotes profile image reset", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .testTag("reset_quote_image_profile_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Remove quote profile image",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Bot Name Input Field
                                    OutlinedTextField(
                                        value = quoteBotName,
                                        onValueChange = { newName ->
                                            quoteBotName = newName
                                            quotePrefs.edit().putString("quote_bot_name", newName).apply()
                                        },
                                        label = { Text("Guide Name", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("quote_bot_name_input"),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Face,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )

                                    // Bot Gender / Title Choice Chips
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Title / Gender", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val genderOptions = listOf("Girlfriend (Female)", "Sister (Female)", "Brother (Male)", "Speaker (Neutral)")
                                            genderOptions.forEach { option ->
                                                val isSelected = quoteBotGender == option
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        quoteBotGender = option
                                                        quotePrefs.edit().putString("quote_bot_gender", option).apply()
                                                    },
                                                    label = {
                                                        Text(
                                                            text = option.split(" ")[0],
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(32.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Expert in public speaking, motivational quotes, and inspirational stories, with a warm, loving, and encouraging girlfriend persona.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                            // Date Span Selection Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "DATE RANGE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // From Date
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { pickQuoteStartDate() }
                                                .testTag("settings_quote_from_date_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text("From", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        text = sdf.format(java.util.Date(quoteDateFromMillis)),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        // To Date
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { pickQuoteEndDate() }
                                                .testTag("settings_quote_to_date_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text("To", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        text = sdf.format(java.util.Date(quoteDateToMillis)),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Actions Grid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Row 1: Primary actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val generated = com.example.util.QuoteUtils.generateQuotesForDateSpan(quoteDateFromMillis, quoteDateToMillis)
                                            onSaveQuotesList(generated)
                                            Toast.makeText(context, "Generated & saved ${generated.size} daily quotes!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f).testTag("quick_generate_quotes_btn"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Generate Span", fontSize = 11.sp)
                                    }

                                    FilledTonalButton(
                                        onClick = { showQuoteDateSpanDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("open_quotes_generator_modal_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Full Generator", fontSize = 11.sp)
                                    }
                                }

                                // Row 2: Import & Reset
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showImportQuotesModal = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("settings_import_quotes_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Import / Edit", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onResetQuotesDefault()
                                            Toast.makeText(context, "Reset to default 31 quotes", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("settings_reset_quotes_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reset Defaults", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION: Daily Verse settings (Date from / to span generation)
                if (activeSection == "verses" || activeSection == "all") {
                    if (activeSection == "all") {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AutoStories,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Daily Verse Settings",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Manage guide identity, avatar, and scripture schedule",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // SECTION 1: Guide Identity & Avatar Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "GUIDE PERSONA & AVATAR",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = "Devotional Guide",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Avatar + Info Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Avatar Circle
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasVerseProfileImage) {
                                                AsyncImage(
                                                    model = verseImageFile,
                                                    contentDescription = "Daily Verse Profile Image",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Brush.linearGradient(listOf(Color(0xFF0084FF), Color(0xFFA855F7)))),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MenuBook,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = verseBotName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (hasVerseProfileImage) "Custom photo set" else "Default scripture icon",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Photo Action Buttons
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalButton(
                                                onClick = { verseImagePickerLauncher.launch("image/*") },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .testTag("import_verse_image_profile_btn")
                                            ) {
                                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Photo", fontSize = 11.sp)
                                            }

                                            if (hasVerseProfileImage) {
                                                IconButton(
                                                    onClick = {
                                                        if (verseImageFile.exists()) {
                                                            verseImageFile.delete()
                                                        }
                                                        val newTimestamp = System.currentTimeMillis()
                                                        versePrefs.edit().putLong("verse_profile_image_timestamp", newTimestamp).apply()
                                                        verseImageTimestamp = newTimestamp
                                                        Toast.makeText(context, "Daily Verse profile image reset", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .testTag("reset_verse_image_profile_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Remove verse profile image",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Bot Name Input Field
                                    OutlinedTextField(
                                        value = verseBotName,
                                        onValueChange = { newName ->
                                            verseBotName = newName
                                            versePrefs.edit().putString("verse_bot_name", newName).apply()
                                        },
                                        label = { Text("Guide Name", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("verse_bot_name_input"),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Face,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )

                                    // Bot Gender / Title Choice Chips
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Title / Gender", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val genderOptions = listOf("Sister (Female)", "Brother (Male)", "Member (Neutral)")
                                            genderOptions.forEach { option ->
                                                val isSelected = verseBotGender == option
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        verseBotGender = option
                                                        versePrefs.edit().putString("verse_bot_gender", option).apply()
                                                    },
                                                    label = {
                                                        Text(
                                                            text = option.split(" ")[0],
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(32.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Answers with spiritual devotional insights and scripture cross-references.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                            // SECTION 2: Scripture Schedule & Actions Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "SCRIPTURE SCHEDULE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        letterSpacing = 0.5.sp
                                    )

                                    // Date Range Pickers
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // From Date
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { pickVerseStartDate() }
                                                .testTag("settings_verse_from_date_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("From Date", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        text = sdf.format(java.util.Date(verseDateFromMillis)),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        // To Date
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { pickVerseEndDate() }
                                                .testTag("settings_verse_to_date_btn")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text("To Date", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        text = sdf.format(java.util.Date(verseDateToMillis)),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Primary Action: Generate Verses
                                    Button(
                                        onClick = {
                                            val generated = com.example.util.VerseUtils.generateVersesForDateSpan(verseDateFromMillis, verseDateToMillis)
                                            onSaveVersesList(generated)
                                            Toast.makeText(context, "Generated & saved ${generated.size} daily scripture verses!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .testTag("quick_generate_verses_btn"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Generate Scripture Schedule", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    // Secondary Actions Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showVerseDateSpanDialog = true },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .testTag("open_verses_generator_modal_btn")
                                                .testTag("settings_import_verses_btn")
                                        ) {
                                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Manage / Edit", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                onResetVersesDefault()
                                                Toast.makeText(context, "Reset to default 31 scripture verses", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .testTag("settings_reset_verses_btn")
                                        ) {
                                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset Defaults", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (activeSection == "all") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                }

                // SECTION 2: Wallet settings
                if (activeSection == "wallet" || activeSection == "all") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (activeSection == "all") {
                            Text(
                                text = "Wallet settings",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Currency (as requested)
                        Column {
                            Text("Currency", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(currencies) { currency ->
                                    FilterChip(
                                        selected = selectedCurrency == currency,
                                        onClick = { selectedCurrency = currency },
                                        label = { Text(currency, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Wallet Password (as requested)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Wallet Password", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Require passcode to view My Wallet", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = walletPinEnabledState,
                                    onCheckedChange = { walletPinEnabledState = it }
                                )
                            }

                            if (walletPinEnabledState) {
                                OutlinedTextField(
                                    value = walletPinState,
                                    onValueChange = {
                                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                            walletPinState = it
                                        }
                                    },
                                    label = { Text("Set Wallet PIN (4-6 digits)", fontSize = 12.sp) },
                                    placeholder = { Text("e.g. 1234") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth().testTag("wallet_pin_input_settings")
                                )
                            }
                        }
                    }
                }

                if (activeSection == "all") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                }

                // SECTION 3: Account and data settings
                if (activeSection == "account" || activeSection == "all") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (activeSection == "all") {
                            Text(
                                text = "Account, Backup & Info",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Export All and Import Backup (Saved Data)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Saved Data (Backup & Restore)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Save data from the device to a local backup file or restore it easily anytime.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onExportData,
                                    modifier = Modifier.weight(1f).testTag("export_data_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Data (Export)", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = onImportData,
                                    modifier = Modifier.weight(1f).testTag("import_data_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore Data", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // App Information Details
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("App Information & Details", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("App Name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Daily Planner & Companion", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Version", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("1.2.0", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "A versatile personal helper featuring smart event scheduling, task tracking, secure wallet budgeting, closet style planning, personal AI dialog companions (Sister Emma & Maya), and an advanced public speaking notebook with hands-free teleprompter scrolling.",
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Developed by JJ Torrejos",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.testTag("developer_credit_text")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveCurrency(selectedCurrency)
                    onSaveWalletPinSettings(walletPinEnabledState, walletPinState)
                    onSaveThemeSettings(selectedThemeColor, selectedThemeMode)
                    onDismiss()
                },
                modifier = Modifier.testTag("settings_done_btn")
            ) {
                Text("Done")
            }
        }
    )

    if (showImportQuotesModal) {
        QuoteImportDialog(
            currentQuotes = monthlyQuotes,
            onDismiss = { showImportQuotesModal = false },
            onSaveQuotes = {
                onSaveQuotesText(it)
                showImportQuotesModal = false
            },
            onResetDefault = {
                onResetQuotesDefault()
                showImportQuotesModal = false
            }
        )
    }

    if (showQuoteDateSpanDialog) {
        QuoteDateSpanGeneratorDialog(
            currentQuotes = monthlyQuotes,
            initialStartDateMillis = quoteDateFromMillis,
            initialEndDateMillis = quoteDateToMillis,
            onDismiss = { showQuoteDateSpanDialog = false },
            onApplyQuotes = { quotes ->
                onSaveQuotesList(quotes)
                showQuoteDateSpanDialog = false
            },
            onResetDefault = {
                onResetQuotesDefault()
                showQuoteDateSpanDialog = false
            }
        )
    }

    if (showVerseDateSpanDialog) {
        VerseDateSpanGeneratorDialog(
            currentVerses = monthlyVerses,
            initialStartDateMillis = verseDateFromMillis,
            initialEndDateMillis = verseDateToMillis,
            onDismiss = { showVerseDateSpanDialog = false },
            onApplyVerses = { verses ->
                onSaveVersesList(verses)
                showVerseDateSpanDialog = false
            },
            onResetDefault = {
                onResetVersesDefault()
                showVerseDateSpanDialog = false
            }
        )
    }
}
