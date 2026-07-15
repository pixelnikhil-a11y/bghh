package com.example.ankillmtooltip

/**
 * Thin Kotlin wrapper around the native llama.cpp bridge (see
 * cpp/llama_bridge.cpp). One LlamaEngine = one loaded model + context.
 * All calls except [load] block the calling thread, so run this from a
 * background coroutine/thread — never from the accessibility service's
 * main-thread callbacks directly.
 */
class LlamaEngine {

    private var nativeHandle: Long = 0L

    /** @param modelPath absolute filesystem path to the .gguf file (copy
     *  content:// picks into app-private storage first — see
     *  MainActivity.importModel). */
    @Synchronized
    fun load(modelPath: String, nThreads: Int = 4, contextSize: Int = 2048): Boolean {
        if (nativeHandle != 0L) return true
        nativeHandle = nativeLoad(modelPath, nThreads, contextSize)
        return nativeHandle != 0L
    }

    @Synchronized
    fun isLoaded(): Boolean = nativeHandle != 0L

    /** Runs one blocking generation. Returns the model's reply text. */
    @Synchronized
    fun generate(systemPrompt: String, userText: String, maxTokens: Int, temperature: Float): String {
        check(nativeHandle != 0L) { "Model not loaded" }
        return nativeGenerate(nativeHandle, systemPrompt, userText, maxTokens, temperature)
    }

    @Synchronized
    fun unload() {
        if (nativeHandle != 0L) {
            nativeUnload(nativeHandle)
            nativeHandle = 0L
        }
    }

    private external fun nativeLoad(modelPath: String, nThreads: Int, contextSize: Int): Long
    private external fun nativeGenerate(
        handle: Long,
        systemPrompt: String,
        userText: String,
        maxTokens: Int,
        temperature: Float
    ): String
    private external fun nativeUnload(handle: Long)

    companion object {
        init {
            System.loadLibrary("llama_bridge")
        }
    }
}
