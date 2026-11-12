package com.amoled.keys

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KeysSettingsScreen() }
    }
}

@Composable
private fun KeysSettingsScreen() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("keys_settings", Context.MODE_PRIVATE) }

    var haptic by remember { mutableStateOf(prefs.getBoolean("haptic", true)) }
    var autocorrect by remember { mutableStateOf(prefs.getBoolean("autocorrect", true)) }
    var numberRow by remember { mutableStateOf(prefs.getBoolean("number_row", true)) }

    var isEnabled by remember { mutableStateOf(isKeyboardEnabled(ctx)) }
    var isDefault by remember { mutableStateOf(isDefaultKeyboard(ctx)) }

    // Re-check on resume
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        isEnabled = isKeyboardEnabled(ctx)
        isDefault = isDefaultKeyboard(ctx)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 32.dp, vertical = 64.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = "keys",
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "amoled keyboard",
            fontSize = 14.sp,
            color = Color(0xFF666666),
        )

        Spacer(Modifier.height(48.dp))

        SectionLabel("setup")

        Spacer(Modifier.height(16.dp))

        SetupRow(
            step = "1",
            label = "enable keyboard",
            subtitle = if (isEnabled) "enabled" else "open settings →",
            done = isEnabled,
            onClick = { ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
        )

        Spacer(Modifier.height(16.dp))

        SetupRow(
            step = "2",
            label = "set as default",
            subtitle = if (isDefault) "active" else "switch input method →",
            done = isDefault,
            onClick = {
                val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                @Suppress("DEPRECATION")
                imm.showInputMethodPicker()
            },
        )

        Spacer(Modifier.height(48.dp))

        SectionLabel("settings")

        Spacer(Modifier.height(16.dp))

        ToggleRow("haptic feedback", haptic) {
            haptic = it
            prefs.edit().putBoolean("haptic", it).apply()
        }
        ToggleRow("autocorrect", autocorrect) {
            autocorrect = it
            prefs.edit().putBoolean("autocorrect", it).apply()
        }
        ToggleRow("number row", numberRow) {
            numberRow = it
            prefs.edit().putBoolean("number_row", it).apply()
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "Keys does not collect or transmit any keystrokes.",
            fontSize = 12.sp,
            color = Color(0xFF333333),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = Color(0xFF555555),
        letterSpacing = 1.sp,
    )
}

@Composable
private fun SetupRow(
    step: String,
    label: String,
    subtitle: String,
    done: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = step,
            fontSize = 13.sp,
            color = if (done) Color(0xFF00CC66) else Color(0xFF555555),
            modifier = Modifier.width(12.dp),
        )
        Column {
            Text(
                text = label,
                fontSize = 17.sp,
                color = if (done) Color(0xFF666666) else Color.White,
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = if (done) Color(0xFF00CC66) else Color(0xFF555555),
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 16.sp, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color(0xFF555555),
                uncheckedTrackColor = Color(0xFF1A1A1A),
                uncheckedBorderColor = Color(0xFF333333),
            ),
        )
    }
}

private fun isKeyboardEnabled(ctx: Context): Boolean {
    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    return imm.enabledInputMethodList.any { it.packageName == ctx.packageName }
}

private fun isDefaultKeyboard(ctx: Context): Boolean {
    val default = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    return default?.startsWith(ctx.packageName) == true
}
