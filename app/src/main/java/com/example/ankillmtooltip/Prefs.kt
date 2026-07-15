package com.example.ankillmtooltip

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val FILE = "llm_tooltip_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_MODEL_URI = "model_uri"
    private const val KEY_GAMEPAD_KEYCODE = "gamepad_keycode"
    private const val KEY_SYSTEM_PROMPT = "system_prompt"
    private const val KEY_MAX_TOKENS = "max_tokens"
    private const val KEY_TEMPERATURE = "temperature"

    const val DEFAULT_SYSTEM_PROMPT =
        "You are a concise study assistant. Explain the selected text clearly " +
            "and briefly (roughly 3-6 sentences). Do not repeat the input text " +
            "back verbatim before explaining, and do not add filler preamble."

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, true)

    fun setEnabled(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun toggleEnabled(ctx: Context): Boolean {
        val newVal = !isEnabled(ctx)
        setEnabled(ctx, newVal)
        return newVal
    }

    /** content:// or file:// URI pointing at the .gguf file the user picked. */
    fun getModelUri(ctx: Context): String? = prefs(ctx).getString(KEY_MODEL_URI, null)

    fun setModelUri(ctx: Context, uri: String) {
        prefs(ctx).edit().putString(KEY_MODEL_URI, uri).apply()
    }

    /** Android KeyEvent.KEYCODE_BUTTON_* that toggles the feature. Default: Y/Triangle/X button. */
    fun getGamepadKeyCode(ctx: Context): Int =
        prefs(ctx).getInt(KEY_GAMEPAD_KEYCODE, android.view.KeyEvent.KEYCODE_BUTTON_Y)

    fun setGamepadKeyCode(ctx: Context, keyCode: Int) {
        prefs(ctx).edit().putInt(KEY_GAMEPAD_KEYCODE, keyCode).apply()
    }

    fun getSystemPrompt(ctx: Context): String =
        prefs(ctx).getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT

    fun setSystemPrompt(ctx: Context, value: String) {
        prefs(ctx).edit().putString(KEY_SYSTEM_PROMPT, value).apply()
    }

    fun getMaxTokens(ctx: Context): Int = prefs(ctx).getInt(KEY_MAX_TOKENS, 256)

    fun setMaxTokens(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt(KEY_MAX_TOKENS, value).apply()
    }

    fun getTemperature(ctx: Context): Float = prefs(ctx).getFloat(KEY_TEMPERATURE, 0.3f)

    fun setTemperature(ctx: Context, value: Float) {
        prefs(ctx).edit().putFloat(KEY_TEMPERATURE, value).apply()
    }
}
