package com.amoled.keys

enum class KeyPage { Alpha, Sym }

enum class ShiftState { Off, Once, Locked }

sealed class KeyAction {
    data class Char(val char: kotlin.Char) : KeyAction()
    object Backspace : KeyAction()
    object BackspaceWord : KeyAction()
    object Enter : KeyAction()
    object Space : KeyAction()
    object ShiftTap : KeyAction()
    object SwitchToSym : KeyAction()
    object SwitchToAlpha : KeyAction()
    object Voice : KeyAction()
    data class Suggestion(val word: String) : KeyAction()
}

data class KeyboardUiState(
    val page: KeyPage = KeyPage.Alpha,
    val shiftState: ShiftState = ShiftState.Off,
    val showNumberRow: Boolean = true,
    val suggestions: List<String> = emptyList(),
    val composingWord: String = "",
    val isListening: Boolean = false,
    val enterLabel: String = "↵",
    val hapticEnabled: Boolean = true,
    val autocorrectEnabled: Boolean = true,
)

data class Key(
    val label: String,
    val shiftLabel: String = label.uppercase(),
    val symLabel: String = label,
    val action: KeyAction? = null,
    val widthWeight: Float = 1f,
    val isSpecial: Boolean = false,
)

object KeyLayouts {
    val numberRow = listOf(
        Key("1"), Key("2"), Key("3"), Key("4"), Key("5"),
        Key("6"), Key("7"), Key("8"), Key("9"), Key("0"),
    )

    val alphaRow1 = listOf(
        Key("q"), Key("w"), Key("e"), Key("r"), Key("t"),
        Key("y"), Key("u"), Key("i"), Key("o"), Key("p"),
    )

    val alphaRow2 = listOf(
        Key("a"), Key("s"), Key("d"), Key("f"), Key("g"),
        Key("h"), Key("j"), Key("k"), Key("l"),
    )

    val alphaRow3Letters = listOf(
        Key("z"), Key("x"), Key("c"), Key("v"), Key("b"), Key("n"), Key("m"),
    )

    val symRow1 = listOf(
        Key("1"), Key("2"), Key("3"), Key("4"), Key("5"),
        Key("6"), Key("7"), Key("8"), Key("9"), Key("0"),
    )

    val symRow2 = listOf(
        Key("!"), Key("@"), Key("#"), Key("$"), Key("%"),
        Key("^"), Key("&"), Key("*"), Key("-"), Key("+"),
    )

    val symRow3 = listOf(
        Key("("), Key(")"), Key("_"), Key("="), Key("["),
        Key("]"), Key("'"), Key("\""),
    )
}
