package com.example.ankillmtooltip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val pickModelLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) importModel(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnPickModel).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Choose how to load the model")
                .setItems(arrayOf("Pick from device storage", "Download from URL")) { _, which ->
                    when (which) {
                        0 -> pickModelLauncher.launch(arrayOf("application/octet-stream"))
                        1 -> promptDownloadUrl()
                    }
                }
                .show()
        }

        findViewById<Button>(R.id.btnOpenAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        refreshStatus()
    }

    private fun refreshStatus() {
        val status = findViewById<TextView>(R.id.statusText)
        val modelOk = Prefs.getModelUri(this) != null
        status.text = if (modelOk) "Model imported. Enable Accessibility below."
        else "Pick your .gguf file to get started."
    }

    private fun promptDownloadUrl() {
        val input = EditText(this).apply {
            hint = "https://huggingface.co/... or your model URL"
        }
        AlertDialog.Builder(this)
            .setTitle("Download model from URL")
            .setView(input)
            .setPositiveButton("Download") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) downloadModel(url)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun downloadModel(urlString: String) {
        Toast.makeText(this, "Downloading model…", Toast.LENGTH_SHORT).show()
        scope.launch {
            try {
                val url = URL(urlString)
                val dest = File(filesDir, "model.gguf")
                url.openStream().use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output, bufferSize = 8 * 1024 * 1024)
                    }
                }
                Prefs.setModelUri(this@MainActivity, dest.absolutePath)
                Toast.makeText(this@MainActivity, "Model downloaded", Toast.LENGTH_SHORT).show()
                refreshStatus()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Download failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Copies the picked .gguf into app-private storage (llama.cpp needs a
     *  real filesystem path, not a content:// uri) and remembers the path. */
    private fun importModel(uri: Uri) {
        Toast.makeText(this, "Copying model…", Toast.LENGTH_SHORT).show()
        scope.launch {
            try {
                val dest = File(filesDir, "model.gguf")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output, bufferSize = 8 * 1024 * 1024)
                    }
                }
                Prefs.setModelUri(this@MainActivity, dest.absolutePath)
                Toast.makeText(this@MainActivity, "Model imported", Toast.LENGTH_SHORT).show()
                refreshStatus()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Import failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ---- gamepad "learn button" -----------------------------------

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.device != null &&
            (event.device.sources and android.view.InputDevice.SOURCE_GAMEPAD) ==
            android.view.InputDevice.SOURCE_GAMEPAD
        ) {
            Prefs.setGamepadKeyCode(this, keyCode)
            findViewById<Button>(R.id.btnLearnGamepad).text =
                "Bound: ${KeyEvent.keyCodeToString(keyCode)}"
            findViewById<TextView>(R.id.gamepadStatus).text =
                "This button now toggles the tooltip on/off while reviewing in AnkiDroid."
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
