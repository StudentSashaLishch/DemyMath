package com.example.demymath

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.demymath.data.AppDao
import com.example.demymath.data.AppRepository
import com.example.demymath.data.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class SharedViewModel(private val repository: AppRepository) : ViewModel() {

    // 1. Поточний ID користувача (реактивний)
    private val _currentUserId = MutableStateFlow(1)
    val currentUserId: StateFlow<Int> = _currentUserId.asStateFlow()

    // 2. Потік даних поточного користувача (авто-оновлюється при зміні ID)
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<User?> = _currentUserId.flatMapLatest { id ->
        repository.getUserById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 3. Статистика (авто-оновлюється при зміні ID)
    @OptIn(ExperimentalCoroutinesApi::class)
    val finishedTopicsCount: StateFlow<Int> = _currentUserId.flatMapLatest { id ->
        repository.getFinishedTopicsCount(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFinishedTopics(lang: String): StateFlow<List<AppDao.TopicWithMark>> =
        currentUserId.flatMapLatest { id ->
            repository.getFinishedTopicsWithNames(id, lang)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Метод для зміни користувача
    fun switchUser(newId: Int) {
        _currentUserId.value = newId
    }
}

// Фабрика для створення ViewModel з репозиторієм
class SharedViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SharedViewModel(repository) as T
    }
}