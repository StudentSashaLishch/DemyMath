package com.example.demymath

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class GraphState {
    var offset by mutableStateOf(Offset.Zero)
    var scale by mutableStateOf(1f)
}