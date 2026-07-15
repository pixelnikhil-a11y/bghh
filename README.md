# Anki LLM Tooltip (AnkiDroid, on-device GGUF)

Port of your desktop "LLM Hover Tooltip (Ollama)" addon to AnkiDroid,
running a local GGUF model (e.g. granite4:1b-h) via llama.cpp instead of
Ollama, with a Bluetooth gamepad button as an on/off toggle.

## Quick start (GitHub Actions auto-build)

**You don't need Android Studio.** Push this repo to GitHub and Actions will build the APK for you:

1. Create a GitHub repo and push this code
2. Go to Actions tab → download the built `app-release.apk` (3–5 min)
3. Install on your phone
4. Point the app to your hosted `.gguf` file (Hugging Face, GitHub Releases, or your server)
5. Enable Accessibility permission and bind a gamepad button

**See [`GITHUB_SETUP.md`](GITHUB_SETUP.md) for detailed step-by-step instructions.**
