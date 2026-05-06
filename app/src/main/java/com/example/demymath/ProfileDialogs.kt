package com.example.demymath

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
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
    userId: Int,
    onDismiss: () -> Unit,
    onUserChanged: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var userList by remember { mutableStateOf(emptyList<User>()) }
    var targetUser by remember { mutableStateOf<User?>(null) }

    val currentUser by produceState<User?>(initialValue = null, userId) {
        value = repository.getUserById(userId)
    }

    // Завантаження списку користувачів для діалогів вибору
    LaunchedEffect(type) {
        if (type == ProfileDialogType.LOGIN || type == ProfileDialogType.DELETE_SELECT) {
            userList = repository.getAllUsers()
        }
    }

    when (type) {
        ProfileDialogType.CREATE -> CreateUserDialog(
            onDismiss = onDismiss,
            onConfirm = { name, pass, uri ->
                scope.launch {
                    val newId = repository.createNewUser(User(displayName = name, password = pass, avatarUrі = uri))
                    onUserChanged(newId.toInt())
                    onDismiss()
                }
            }
        )

        ProfileDialogType.LOGIN -> LoginDialog(
            users = userList,
            onDismiss = onDismiss,
            onSelect = { user ->
                onUserChanged(user.userId)
                onDismiss()
            }
        )

        ProfileDialogType.EDIT -> EditUserDialog(
            user = currentUser,
            onDismiss = onDismiss,
            onConfirm = { name, pass, uri ->
                scope.launch {
                    currentUser?.let {
                        repository.updateUser(it.copy(displayName = name, password = pass, avatarUrі = uri))
                    }
                    onDismiss()
                    Toast.makeText(context, "Профіль оновлено", Toast.LENGTH_SHORT).show()
                }
            }
        )

        ProfileDialogType.DELETE_SELECT -> LoginDialog(
            users = userList.filter { it.userId != 1 },
            onDismiss = onDismiss,
            onSelect = { user ->
                targetUser = user
                // Ми не можемо просто змінити type тут, тому використовуємо допоміжний стан
            }
        )

        // Додаємо відсутню гілку підтвердження
        ProfileDialogType.DELETE_CONFIRM -> {
            // Ця гілка може знадобитися, якщо ви захочете викликати підтвердження напряму
            // Але оскільки ми використовуємо targetUser, обробимо його нижче
        }

        ProfileDialogType.RESET -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Скинути прогрес?") },
            text = { Text("Це видалить усі ваші оцінки та результати. Ви впевнені?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.resetAllProgress(userId)
                        onDismiss()
                    }
                }) { Text("Так, видалити", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
        )

        null -> { /* Нічого не робимо */ }
    }

    // Окреме вікно підтвердження, яке "спливає" поверх списку вибору
    targetUser?.let { user ->
        DeleteConfirmDialog(
            userName = user.displayName,
            onDismiss = { targetUser = null },
            onConfirm = { pass ->
                if (pass == user.password) {
                    scope.launch {
                        repository.deleteUser(user)
                        if (userId == user.userId) onUserChanged(1)
                        targetUser = null
                        onDismiss()
                        Toast.makeText(context, "Видалено", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Невірний пароль", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun CreateUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var avatarUri by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> avatarUri = uri.toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Створення профілю") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ім'я") })
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Пароль") })
                Button(onClick = { photoPickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (avatarUri == null) "Вибрати фото" else "Фото обрано ✅")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, pass, avatarUri) }) { Text("Створити") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

@Composable
fun EditUserDialog(
    user: User?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var pass by remember { mutableStateOf(user?.password ?: "") }
    var avatarUri by remember { mutableStateOf(user?.avatarUrі) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> avatarUri = uri.toString() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагування профілю") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Нове ім'я") })
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Новий пароль") })
                Button(onClick = { photoPickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Змінити фото")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, pass, avatarUri) }) { Text("Зберегти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}

@Composable
fun LoginDialog(
    users: List<User>,
    onDismiss: () -> Unit,
    onSelect: (User) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Виберіть профіль") },
        text = {
            LazyColumn {
                items(users) { user ->
                    ListItem(
                        headlineContent = { Text(user.displayName) },
                        modifier = Modifier.clickable { onSelect(user) }
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun DeleteConfirmDialog(
    userName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Видалити профіль $userName?") },
        text = {
            Column {
                Text("Це видалить весь прогрес безповоротно.")
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Пароль") },
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = { onConfirm(pass) }
            ) { Text("Видалити") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}