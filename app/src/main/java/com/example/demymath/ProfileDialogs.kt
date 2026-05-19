package com.example.demymath

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.demymath.data.AppRepository
import com.example.demymath.data.User
import kotlinx.coroutines.launch

@Composable
fun ProfileDialogManager(
    type: ProfileDialogType?,
    repository: AppRepository,
    viewModel: SharedViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    var userList by remember { mutableStateOf(emptyList<User>()) }
    var selectedUserForAction by remember { mutableStateOf<User?>(null) }
    var isDeleteOperation by remember { mutableStateOf(false) }

    // Тільки завантаження списку
    LaunchedEffect(type) {
        if (type == ProfileDialogType.LOGIN || type == ProfileDialogType.DELETE_SELECT) {
            userList = repository.getAllUsers()
        }
    }

    when (type) {
        ProfileDialogType.CREATE -> HandleCreateDialog(repository, viewModel, currentUser, onDismiss, scope)

        ProfileDialogType.LOGIN -> LoginDialog(
            users = userList,
            onDismiss = onDismiss,
            onSelect = { user ->
                isDeleteOperation = false
                selectedUserForAction = user
            }
        )

        ProfileDialogType.EDIT -> HandleEditDialog(currentUser, repository, onDismiss, scope, context)

        ProfileDialogType.DELETE_SELECT -> LoginDialog(
            users = userList.filter { it.userId != 1 },
            onDismiss = onDismiss,
            onSelect = { user ->
                isDeleteOperation = true
                selectedUserForAction = user
            }
        )

        ProfileDialogType.RESET -> ResetProgressDialog(currentUser, repository, onDismiss, scope)
        ProfileDialogType.FIREBASE_BIND -> HandleFirebaseBindDialog(currentUser, repository, onDismiss, scope, context)
        ProfileDialogType.SYNC_CHOICE -> HandleSyncChoiceDialog(currentUser, repository, onDismiss, scope, context)
        else -> {}
    }

    // Запит пароля перед входом або видаленням
    selectedUserForAction?.let { targetUser ->
        PasswordVerificationDialog(
            title = if (isDeleteOperation) "Видалення профілю ${targetUser.displayName}" else "Вхід у профіль ${targetUser.displayName}",
            description = if (isDeleteOperation) "Це видалить весь прогрес безповоротно. Введіть пароль:" else "Для входу введіть пароль профілю:",
            onDismiss = { selectedUserForAction = null },
            onConfirm = { password ->
                scope.launch {
                    val isPasswordCorrect = repository.verifyUserPassword(targetUser.userId, password)
                    if (isPasswordCorrect) {
                        if (isDeleteOperation) {
                            repository.deleteUser(targetUser)
                            if (currentUser?.userId == targetUser.userId) viewModel.switchUser(1)
                            Toast.makeText(context, "Профіль видалено", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.switchUser(targetUser.userId)
                            Toast.makeText(context, "Вітаємо, ${targetUser.displayName}", Toast.LENGTH_SHORT).show()
                        }
                        selectedUserForAction = null
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Невірний пароль", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

// --- ІЗОЛЬОВАНІ КОМПОНЕНТИ ДІАЛОГІВ ДЛЯ ОПТИМІЗАЦІЇ КОДУ ---

@Composable
private fun HandleCreateDialog(
    repository: AppRepository,
    viewModel: SharedViewModel,
    currentUser: User?,
    onDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    CreateUserDialog(
        onDismiss = onDismiss,
        onConfirm = { name, pass, uri ->
            scope.launch {
                val newId = repository.createNewUser(User(displayName = name, password = pass, avatarUrі = uri))
                // Якщо створювали з-під Гостя (ID = 1), переносимо його прогрес на новий акаунт
                if (currentUser?.userId == 1) {
                    repository.migrateGuestProgressToUser(newId.toInt())
                }
                viewModel.switchUser(newId.toInt())
                onDismiss()
            }
        }
    )
}

@Composable
private fun HandleEditDialog(
    currentUser: User?,
    repository: AppRepository,
    onDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    var isVerified by remember { mutableStateOf(false) }

    if (!isVerified && currentUser != null) {
        PasswordVerificationDialog(
            title = "Редагування профілю",
            description = "Введіть поточний пароль для доступу до редагування:",
            onDismiss = onDismiss,
            onConfirm = { password ->
                scope.launch {
                    if (repository.verifyUserPassword(currentUser.userId, password)) {
                        isVerified = true
                    } else {
                        Toast.makeText(context, "Невірний пароль", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    } else {
        EditUserDialog(
            user = currentUser,
            onDismiss = onDismiss,
            onConfirm = { name, pass, uri ->
                scope.launch {
                    currentUser?.let {
                        repository.updateUser(it.copy(displayName = name, password = pass, avatarUrі = uri))
                    }
                    onDismiss()
                    Toast.makeText(context, "Оновлено", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun HandleFirebaseBindDialog(
    currentUser: User?,
    repository: AppRepository,
    onDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    FirebaseBindDialog(
        onDismiss = onDismiss,
        onConfirm = { email, pass ->
            scope.launch {
                currentUser?.let {
                    val result = repository.linkAccountWithFirebase(it.userId, email, pass)
                    if (result.isSuccess) {
                        Toast.makeText(context, "Хмару підключено!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Помилка: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
                onDismiss()
            }
        }
    )
}

@Composable
private fun HandleSyncChoiceDialog(
    currentUser: User?,
    repository: AppRepository,
    onDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    CloudSyncDialog(
        onDismiss = onDismiss,
        onUpload = {
            scope.launch {
                currentUser?.let {
                    val res = repository.syncWithCloud(it.userId, upload = true)
                    val msg = if (res.isSuccess) "Прогрес вивантажено" else "Помилка вивантаження"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }
        },
        onDownload = {
            scope.launch {
                currentUser?.let {
                    val res = repository.syncWithCloud(it.userId, upload = false)
                    val msg = if (res.isSuccess) "Прогрес завантажено з хмари" else "Помилка завантаження"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                onDismiss()
            }
        }
    )
}

@Composable
private fun ResetProgressDialog(
    currentUser: User?,
    repository: AppRepository,
    onDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Скинути прогрес?") },
        text = { Text("Це видалить ваші оцінки та нотатки. Ви впевнені?") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    currentUser?.let { repository.resetAllProgress(it.userId) }
                    onDismiss()
                }
            }) { Text("Так, видалити", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

@Composable
fun PasswordVerificationDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(description)
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(pass) }) { Text("Підтвердити") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

@Composable
fun CreateUserDialog(onDismiss: () -> Unit, onConfirm: (String, String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<String?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { avatarUri = it.toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Створення профілю") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ім'я") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Пароль") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { photoPickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (avatarUri == null) "Вибрати фото" else "Фото обрано ✅")
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, pass, avatarUri) }) { Text("Створити") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

@Composable
fun EditUserDialog(user: User?, onDismiss: () -> Unit, onConfirm: (String, String, String?) -> Unit) {
    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var pass by remember { mutableStateOf(user?.password ?: "") }
    var avatarUri by remember { mutableStateOf(user?.avatarUrі) }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { avatarUri = it.toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагування профілю") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Нове ім'я") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Новий пароль") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { photoPickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Змінити фото") }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, pass, avatarUri) }) { Text("Зберегти") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

@Composable
fun LoginDialog(users: List<User>, onDismiss: () -> Unit, onSelect: (User) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Виберіть профіль") },
        text = {
            LazyColumn {
                items(users) { user ->
                    ListItem(headlineContent = { Text(user.displayName) }, modifier = Modifier.clickable { onSelect(user) })
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun FirebaseBindDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Прив'язка до Firebase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Введіть пошту та пароль для синхронізації.")
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Пароль") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onConfirm(email, pass) }) { Text("Підключити") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

@Composable
fun CloudSyncDialog(onDismiss: () -> Unit, onUpload: () -> Unit, onDownload: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Синхронізація з хмарою") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onUpload, modifier = Modifier.fillMaxWidth()) { Text("Зберегти поточний прогрес у хмару ⬆️") }
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text("Завантажити прогрес із хмари ⬇️") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрити") } }
    )
}