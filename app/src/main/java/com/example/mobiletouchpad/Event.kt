package com.example.mobiletouchpad

import android.util.Log
import androidx.compose.ui.geometry.Offset

val clickEvent = { event: String ->
    println(event)
}

val dragStart = { offset: Offset ->
    println("startOffset: $offset")
}

val dragMove = { change: Offset, delta: Offset ->
    println("changeOffset: $change, delta: $delta")
}

val dragEnd = {
    println("releaseOffset")
}