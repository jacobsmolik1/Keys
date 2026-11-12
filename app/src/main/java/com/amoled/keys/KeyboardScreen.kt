package com.amoled.keys

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BG = Color(0xFF000000)
private val KEY_COLOR = Color(0xFF1C1C1C)
private val SPECIAL_COLOR = Color(0xFF0D0D0D)
private val KEY_TEXT = Color(0xFFE8E8E8)
private val DIM_TEXT = Color(0xFF888888)
private val PRESSED_COLOR = Color(0xFF303030)
private val DIVIDER = Color(0xFF1A1A1A)

@Composable
fun KeyboardScreen(state: KeyboardUiState, onKey: (KeyAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG)
    ) {
        SuggestionBar(state.suggestions, onKey)
        when (state.page) {
            KeyPage.Alpha -> AlphaPage(state, onKey)
            KeyPage.Sym -> SymPage(state, onKey)
        }
    }
}

@Composable
private fun SuggestionBar(suggestions: List<String>, onKey: (KeyAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(BG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val slots = (suggestions + listOf("", "", "")).take(3)
        slots.forEachIndexed { i, word ->
            if (i > 0) {
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(DIVIDER))
            }
            var isPressed by remember(word) { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isPressed && word.isNotEmpty()) PRESSED_COLOR else BG)
                    .then(
                        if (word.isNotEmpty()) Modifier.pointerInput(word) {
                            detectTapGestures(
                                onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                                onTap = { onKey(KeyAction.Suggestion(word)) },
                            )
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (word.isNotEmpty()) {
                    Text(
                        text = word,
                        fontSize = 15.sp,
                        color = KEY_TEXT,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlphaPage(state: KeyboardUiState, onKey: (KeyAction) -> Unit) {
    val shifted = state.shiftState != ShiftState.Off
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (state.showNumberRow) {
            KeyRow {
                for (key in KeyLayouts.numberRow) {
                    KeyBtn(label = key.label, action = KeyAction.Char(key.label[0]), onKey = onKey)
                }
            }
        }
        KeyRow {
            for (key in KeyLayouts.alphaRow1) {
                val label = if (shifted) key.label.uppercase() else key.label
                KeyBtn(label = label, action = KeyAction.Char(key.label[0]), onKey = onKey)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Spacer(modifier = Modifier.weight(0.5f))
            for (key in KeyLayouts.alphaRow2) {
                val label = if (shifted) key.label.uppercase() else key.label
                KeyBtn(label = label, action = KeyAction.Char(key.label[0]), onKey = onKey, weight = 1f)
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val shiftLabel = if (state.shiftState == ShiftState.Locked) "⇪" else "⇧"
            val shiftTint = when (state.shiftState) {
                ShiftState.Off -> DIM_TEXT
                ShiftState.Once -> KEY_TEXT
                ShiftState.Locked -> Color.White
            }
            SpecialBtn(label = shiftLabel, labelColor = shiftTint, weight = 1.5f, action = KeyAction.ShiftTap, onKey = onKey)
            for (key in KeyLayouts.alphaRow3Letters) {
                val label = if (shifted) key.label.uppercase() else key.label
                KeyBtn(label = label, action = KeyAction.Char(key.label[0]), onKey = onKey, weight = 1f)
            }
            BackspaceBtn(weight = 1.5f, onKey = onKey)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            SpecialBtn(label = "?123", weight = 1.5f, action = KeyAction.SwitchToSym, onKey = onKey, fontSize = 12)
            MicBtn(isListening = state.isListening, weight = 1f, onKey = onKey)
            SpaceBtn(weight = 5f, onKey = onKey)
            KeyBtn(label = ".", action = KeyAction.Char('.'), onKey = onKey, weight = 1f)
            SpecialBtn(label = state.enterLabel, weight = 1.5f, action = KeyAction.Enter, onKey = onKey, fontSize = 12)
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SymPage(state: KeyboardUiState, onKey: (KeyAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        KeyRow {
            for (key in KeyLayouts.symRow1) {
                KeyBtn(label = key.label, action = KeyAction.Char(key.label[0]), onKey = onKey)
            }
        }
        KeyRow {
            for (key in KeyLayouts.symRow2) {
                KeyBtn(label = key.label, action = KeyAction.Char(key.label[0]), onKey = onKey)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (key in KeyLayouts.symRow3) {
                KeyBtn(label = key.label, action = KeyAction.Char(key.label[0]), onKey = onKey, weight = 1f)
            }
            BackspaceBtn(weight = 2f, onKey = onKey)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            SpecialBtn(label = "ABC", weight = 1.5f, action = KeyAction.SwitchToAlpha, onKey = onKey, fontSize = 12)
            MicBtn(isListening = state.isListening, weight = 1f, onKey = onKey)
            SpaceBtn(weight = 5f, onKey = onKey)
            KeyBtn(label = ".", action = KeyAction.Char('.'), onKey = onKey, weight = 1f)
            SpecialBtn(label = state.enterLabel, weight = 1.5f, action = KeyAction.Enter, onKey = onKey, fontSize = 12)
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        content = content,
    )
}

@Composable
private fun RowScope.KeyBtn(
    label: String,
    action: KeyAction,
    onKey: (KeyAction) -> Unit,
    weight: Float = 1f,
    fontSize: Int = 17,
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .weight(weight)
            .height(50.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (isPressed) PRESSED_COLOR else KEY_COLOR)
            .pointerInput(action) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = { onKey(action) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = fontSize.sp, color = KEY_TEXT, fontWeight = FontWeight.Normal)
    }
}

@Composable
private fun RowScope.SpecialBtn(
    label: String,
    weight: Float,
    action: KeyAction,
    onKey: (KeyAction) -> Unit,
    fontSize: Int = 17,
    labelColor: Color = KEY_TEXT.copy(alpha = 0.85f),
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .weight(weight)
            .height(50.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (isPressed) PRESSED_COLOR else SPECIAL_COLOR)
            .pointerInput(action) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = { onKey(action) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = fontSize.sp, color = labelColor, fontWeight = FontWeight.Normal)
    }
}

@Composable
private fun RowScope.BackspaceBtn(weight: Float, onKey: (KeyAction) -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .weight(weight)
            .height(50.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (isPressed) PRESSED_COLOR else SPECIAL_COLOR)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = { onKey(KeyAction.Backspace) },
                    onLongPress = { onKey(KeyAction.BackspaceWord) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("⌫", fontSize = 20.sp, color = KEY_TEXT.copy(alpha = 0.8f))
    }
}

@Composable
private fun RowScope.MicBtn(isListening: Boolean, weight: Float, onKey: (KeyAction) -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .weight(weight)
            .height(50.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (isPressed) PRESSED_COLOR else SPECIAL_COLOR)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = { onKey(KeyAction.Voice) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isListening) "●" else "mic",
            fontSize = if (isListening) 14.sp else 12.sp,
            color = if (isListening) Color(0xFFFF4444) else KEY_TEXT.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun RowScope.SpaceBtn(weight: Float, onKey: (KeyAction) -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .weight(weight)
            .height(50.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (isPressed) PRESSED_COLOR else KEY_COLOR)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = { onKey(KeyAction.Space) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("space", fontSize = 13.sp, color = DIM_TEXT)
    }
}
