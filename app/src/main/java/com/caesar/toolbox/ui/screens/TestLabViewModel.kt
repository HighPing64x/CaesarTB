package com.caesar.toolbox.ui.screens

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 测试实验室 ViewModel — 验证状态管理全链路
 */
class TestLabViewModel : ViewModel() {

    private val _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _reversedText = MutableStateFlow("")
    val reversedText: StateFlow<String> = _reversedText.asStateFlow()

    private val _useDarkCard = MutableStateFlow(false)
    val useDarkCard: StateFlow<Boolean> = _useDarkCard.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun increment() {
        _counter.value++
    }

    fun decrement() {
        _counter.value--
    }

    fun resetCounter() {
        _counter.value = 0
        _toastMessage.value = "计数器已归零"
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun reverseText() {
        _reversedText.value = _inputText.value.reversed()
    }

    fun clearText() {
        _inputText.value = ""
        _reversedText.value = ""
    }

    fun toggleCardStyle() {
        _useDarkCard.value = !_useDarkCard.value
    }

    fun consumeToast() {
        _toastMessage.value = null
    }
}
