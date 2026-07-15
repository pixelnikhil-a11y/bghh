// llama_bridge.cpp
//
// Minimal JNI wrapper around llama.cpp for single-turn "explain this text"
// generation. Modeled on llama.cpp's examples/simple-chat.cpp. One handle
// = one loaded model + one context, reused across calls (context is reset
// per generate() call by re-seeding from scratch, which is simplest and
// safest for short, independent tooltip requests).
//
// This file intentionally keeps sampling simple (greedy-ish with temperature)
// rather than pulling in llama.cpp's full sampler-chain API, so it stays
// readable. Swap in common/sampling.h's llama_sampler_chain if you want
// top-p/top-k/repeat-penalty control later.

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>

#include "llama.h"

#define LOG_TAG "llama_bridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct EngineHandle {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    const llama_vocab* vocab = nullptr;
};

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_ankillmtooltip_LlamaEngine_nativeLoad(
        JNIEnv* env, jobject /*thiz*/, jstring jModelPath, jint nThreads, jint nCtx) {

    const char* modelPath = env->GetStringUTFChars(jModelPath, nullptr);

    ggml_backend_load_all();

    llama_model_params mparams = llama_model_default_params();
    llama_model* model = llama_model_load_from_file(modelPath, mparams);
    env->ReleaseStringUTFChars(jModelPath, modelPath);

    if (!model) {
        LOGE("failed to load model");
        return 0;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = static_cast<uint32_t>(nCtx);
    cparams.n_threads = nThreads;
    cparams.n_threads_batch = nThreads;

    llama_context* ctx = llama_init_from_model(model, cparams);
    if (!ctx) {
        LOGE("failed to create context");
        llama_model_free(model);
        return 0;
    }

    auto* handle = new EngineHandle();
    handle->model = model;
    handle->ctx = ctx;
    handle->vocab = llama_model_get_vocab(model);

    LOGI("model loaded ok, n_ctx=%d", nCtx);
    return reinterpret_cast<jlong>(handle);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_ankillmtooltip_LlamaEngine_nativeGenerate(
        JNIEnv* env, jobject /*thiz*/, jlong handlePtr,
        jstring jSystemPrompt, jstring jUserText, jint maxTokens, jfloat temperature) {

    auto* handle = reinterpret_cast<EngineHandle*>(handlePtr);
    if (!handle || !handle->ctx) {
        return env->NewStringUTF("[error: engine not loaded]");
    }

    const char* sysC = env->GetStringUTFChars(jSystemPrompt, nullptr);
    const char* userC = env->GetStringUTFChars(jUserText, nullptr);

    // Build a simple chat-formatted prompt via llama.cpp's built-in chat
    // template support (falls back to a plain format if the gguf carries
    // no template metadata).
    std::vector<llama_chat_message> messages;
    messages.push_back({ "system", sysC });
    messages.push_back({ "user", userC });

    std::vector<char> buf(4096);
    const char* tmpl = llama_model_chat_template(handle->model, nullptr);
    int32_t len = llama_chat_apply_template(
            tmpl, messages.data(), messages.size(), true, buf.data(), buf.size());
    if (len > static_cast<int32_t>(buf.size())) {
        buf.resize(len);
        len = llama_chat_apply_template(
                tmpl, messages.data(), messages.size(), true, buf.data(), buf.size());
    }
    std::string prompt(buf.data(), len > 0 ? len : 0);
    if (prompt.empty()) {
        prompt = std::string(sysC) + "\n\n" + userC + "\n";
    }

    env->ReleaseStringUTFChars(jSystemPrompt, sysC);
    env->ReleaseStringUTFChars(jUserText, userC);

    // fresh KV cache per request — simplest correctness story for
    // short, independent tooltip lookups
    llama_kv_self_clear(handle->ctx);

    const int nPromptTokens = -llama_tokenize(
            handle->vocab, prompt.c_str(), (int32_t)prompt.size(),
            nullptr, 0, true, true);
    std::vector<llama_token> promptTokens(nPromptTokens);
    llama_tokenize(handle->vocab, prompt.c_str(), (int32_t)prompt.size(),
                    promptTokens.data(), (int32_t)promptTokens.size(), true, true);

    llama_batch batch = llama_batch_get_one(promptTokens.data(), (int32_t)promptTokens.size());

    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    std::string result;
    int generated = 0;

    while (generated < maxTokens) {
        if (llama_decode(handle->ctx, batch) != 0) {
            LOGE("decode failed");
            break;
        }
        llama_token newToken = llama_sampler_sample(sampler, handle->ctx, -1);

        if (llama_vocab_is_eog(handle->vocab, newToken)) break;

        char piece[256];
        int n = llama_token_to_piece(handle->vocab, newToken, piece, sizeof(piece), 0, true);
        if (n > 0) result.append(piece, n);

        batch = llama_batch_get_one(&newToken, 1);
        generated++;
    }

    llama_sampler_free(sampler);

    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_ankillmtooltip_LlamaEngine_nativeUnload(
        JNIEnv* /*env*/, jobject /*thiz*/, jlong handlePtr) {
    auto* handle = reinterpret_cast<EngineHandle*>(handlePtr);
    if (!handle) return;
    if (handle->ctx) llama_free(handle->ctx);
    if (handle->model) llama_model_free(handle->model);
    delete handle;
}
