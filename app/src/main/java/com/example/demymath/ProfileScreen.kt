package com.example.demymath

import android.widget.Toast
import androidx.benchmark.traceprocessor.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.demymath.data.AppRepository
import com.example.demymath.data.User
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: SharedViewModel,
    repository: AppRepository,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var activeDialog by remember { mutableStateOf<ProfileDialogType?>(null) }

    // Підписка на дані через ViewModel
    val currentUser by viewModel.currentUser.collectAsState()
    val finishedTopics by viewModel.finishedTopicsCount.collectAsState()
    val totalTopics by repository.getTotalTopicsCount().collectAsState(initial = 0)

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        // 1. ВЕРХНЯ ПАНЕЛЬ
        ProfileTopBar(
            isDarkTheme = isDarkTheme,
            onThemeChange = onThemeChange,
            onAction = { action ->
                when(action) {
                    ProfileAction.Create -> activeDialog = ProfileDialogType.CREATE
                    ProfileAction.Login -> activeDialog = ProfileDialogType.LOGIN
                    ProfileAction.Logout -> viewModel.switchUser(1) // Повернення до гостя
                    ProfileAction.Edit -> {
                        if (currentUser?.userId != 1) activeDialog = ProfileDialogType.EDIT
                        else Toast.makeText(context, "Профіль гостя не можна редагувати", Toast.LENGTH_SHORT).show()
                    }
                    ProfileAction.Delete -> activeDialog = ProfileDialogType.DELETE_SELECT
                    ProfileAction.FirebaseBind -> {
                        if (currentUser?.userId != 1) activeDialog = ProfileDialogType.FIREBASE_BIND
                        else Toast.makeText(context, "Гість не може синхронізуватись", Toast.LENGTH_SHORT).show()
                    }
                    ProfileAction.Sync -> {
                        if (currentUser?.firebaseUid != null) activeDialog = ProfileDialogType.SYNC_CHOICE
                        else Toast.makeText(context, "Спочатку підключіть Firebase акаунт", Toast.LENGTH_SHORT).show()
                    }
                    else -> Toast.makeText(context, "В розробці", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // 2. ОСНОВНИЙ КОНТЕНТ
        ProfileContent(
            userName = currentUser?.displayName ?: "Гість",
            avatarUri = currentUser?.avatarUrі,
            finishedTopics = finishedTopics,
            totalTopics = totalTopics
        )

        Spacer(Modifier.weight(1f))

        // 3. КНОПКА СКИНУТИ
        OutlinedButton(
            onClick = { activeDialog = ProfileDialogType.RESET },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Скинути мій прогрес")
        }
    }

    ProfileDialogManager(
        type = activeDialog,
        repository = repository,
        viewModel = viewModel,
        onDismiss = { activeDialog = null }
    )
}

@Composable
fun ProfileTopBar(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onAction: (ProfileAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Меню")
            }
            ProfileDropdownMenu(
                expanded = expanded,
                onDismiss = { expanded = false },
                onAction = { action ->
                    expanded = false
                    onAction(action)
                }
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Темна тема", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(8.dp))
            Switch(checked = isDarkTheme, onCheckedChange = onThemeChange)
        }
    }
}

@Composable
fun ProfileDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAction: (ProfileAction) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        ProfileMenuItem("Додати профіль", Icons.Default.GroupAdd) { onAction(ProfileAction.Create) }
        ProfileMenuItem("Зайти у профіль", Icons.Default.Login) { onAction(ProfileAction.Login) }
        ProfileMenuItem("Вийти", Icons.Default.Logout) { onAction(ProfileAction.Logout) }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        ProfileMenuItem("Редагувати", Icons.Default.Edit) { onAction(ProfileAction.Edit) }
        ProfileMenuItem("Видалити", Icons.Default.DeleteForever, isError = true) { onAction(ProfileAction.Delete) }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        ProfileMenuItem("Firebase Прив'язка", Icons.Default.CloudDone) { onAction(ProfileAction.FirebaseBind) }
        ProfileMenuItem("Синхронізація", Icons.Default.CloudSync) { onAction(ProfileAction.Sync) }
    }
}

@Composable
private fun ProfileMenuItem(
    text: String,
    icon: ImageVector,
    isError: Boolean = false,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = if(isError) MaterialTheme.colorScheme.error else LocalContentColor.current) },
        onClick = onClick
    )
}

@Composable
fun ProfileContent(
    userName: String,
    avatarUri: String?,
    finishedTopics: Int,
    totalTopics: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Аватар
        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUri != null) {
                AsyncImage(model = avatarUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(64.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(text = userName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        // Картка прогресу
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Прогрес навчання", style = MaterialTheme.typography.titleMedium)
                Text(text = "$finishedTopics / $totalTopics", fontSize = 36.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)

                val progress = if (totalTopics > 0) finishedTopics.toFloat() / totalTopics else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)
                )
            }
        }
    }
}

// Перерахування для типів дій
enum class ProfileAction { Create, Login, Logout, Edit, Delete, FirebaseBind, Sync }

enum class ProfileDialogType {
    CREATE, LOGIN, EDIT, DELETE_SELECT, DELETE_CONFIRM, RESET, FIREBASE_BIND, SYNC_CHOICE
}