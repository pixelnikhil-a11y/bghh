package com.example.ankillmtooltip

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs while AnkiDroid is in the foreground (scoped to its package via
 * accessibility_service_config.xml). Two jobs:
 *
 *  1. Notice when the user selects text inside AnkiDroid's review WebView
 *     and, if the feature is on, ask the LLM to explain it and show a
 *     floating bubble via OverlayController.
 *
 *  2. Intercept the chosen Bluetooth gamepad button (flagRequestFilterKeyEvents
 *     lets an AccessibilityService see key events system-wide) and use it
 *     purely as an on/off toggle for (1) — it does not consume/block the
 *     key from also reaching AnkiDroid, since gamepad buttons are usually
 *     mapped to answer grades there and you don't want to steal those.
 *     If you DO want to consume the toggle key so it doesn't also trigger
 *     an AnkiDroid answer button, flip CONSUME_TOGGLE_KEY to true.
 */
class TooltipAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "TooltipAccessibility"
        private const val CONSUME_TOGGLE_KEY = false
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var engine: LlamaEngine? = null
    private var lastSelectedText: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "connected")
        ensureEngineLoading()
    }

    private fun ensureEngineLoading() {
        val modelPath = Prefs.getModelUri(this) ?: run {
            Log.w(TAG, "no model imported yet — open the app and pick a .gguf file")
            return
        }
        if (engine?.isLoaded() == true) return
        scope.launch {
            val e = LlamaEngine()
            val ok = e.load(modelPath)
            if (ok) {
                engine = e
                Log.i(TAG, "model loaded")
            } else {
                Log.e(TAG, "model failed to load from $modelPath")
            }
        }
    }

    // ---- 1. Text selection capture -----------------------------------

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!Prefs.isEnabled(this)) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> handlePossibleSelection()
            else -> { /* ignore window state / content changes for now */ }
        }
    }

    private fun handlePossibleSelection() {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return
        val selected = findSelectedText(root) ?: return
        if (selected.isBlank() || selected == lastSelectedText) return
        lastSelectedText = selected
        onTextSelected(selected)
    }

    /** Walks the node tree looking for a node reporting a text selection
     *  range, and returns the selected substring. AnkiDroid's reviewer is
     *  a WebView, so selected text surfaces as a standard text-selection
     *  accessibility node. */
    private fun findSelectedText(node: AccessibilityNodeInfo): String? {
        val start = node.textSelectionStart
        val end = node.textSelectionEnd
        val text = node.text?.toString()
        if (start in 0..<end && text != null && end <= text.length) {
            return text.substring(start, end)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findSelectedText(child)
            if (result != null) return result
        }
        return null
    }

    private fun onTextSelected(text: String) {
        OverlayController.showLoading(this)
        scope.launch {
            val e = engine
            if (e == null || !e.isLoaded()) {
                withContext(Dispatchers.Main) {
                    OverlayController.showError(this@TooltipAccessibilityService, "Model not loaded yet")
                }
                return@launch
            }
            val result = try {
                e.generate(
                    systemPrompt = Prefs.getSystemPrompt(this@TooltipAccessibilityService),
                    userText = text,
                    maxTokens = Prefs.getMaxTokens(this@TooltipAccessibilityService),
                    temperature = Prefs.getTemperature(this@TooltipAccessibilityService)
                )
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    OverlayController.showError(this@TooltipAccessibilityService, "Error: ${ex.message}")
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                OverlayController.showResult(this@TooltipAccessibilityService, result)
            }
        }
    }

    // ---- 2. Gamepad on/off toggle --------------------------------------

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val toggleCode = Prefs.getGamepadKeyCode(this)
        if (event.keyCode == toggleCode && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val nowEnabled = Prefs.toggleEnabled(this)
            if (!nowEnabled) OverlayController.hide()
            Toast.makeText(
                this,
                if (nowEnabled) "LLM tooltip: ON" else "LLM tooltip: OFF",
                Toast.LENGTH_SHORT
            ).show()
            return CONSUME_TOGGLE_KEY
        }
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() {}
}
