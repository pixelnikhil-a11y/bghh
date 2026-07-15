# Build on GitHub with GitHub Actions

This project is configured to auto-build on every push via GitHub Actions, so you don't need Android Studio locally. You just need to:

1. **Fork or create a GitHub repo** with this code
2. **Push to main branch** → GitHub Actions automatically clones llama.cpp, builds the APK, and saves it as an artifact
3. **Host your .gguf file** somewhere public (see "Where to host the model" below)
4. **Install the APK** on your phone and point the app to the model URL

## Step-by-step

### 1. Create a GitHub repository

- Go to https://github.com/new
- Create a repo (e.g. `anki-llm-tooltip`)
- Clone it locally, copy all files from this project into it, commit and push:
  ```bash
  git clone https://github.com/YOU/anki-llm-tooltip.git
  cd anki-llm-tooltip
  # Copy all files from the project here
  git add .
  git commit -m "Initial commit"
  git push origin main
  ```

### 2. Check the Actions tab

- Go to your repo on GitHub → **Actions** tab
- You should see the "Build APK" workflow running
- After 3–5 minutes it will finish
- Click the completed workflow → scroll down to **Artifacts** → download `app-release.apk`

That APK is your finished build. No Android Studio needed.

### 3. Where to host the model

You have a few options:

#### Option A: Hugging Face (recommended for large files)
- Create a repo on https://huggingface.co
- Upload your `granite4:1b-h.gguf` there
- In the app, use the URL: `https://huggingface.co/USERNAME/REPO/resolve/main/granite4:1b-h.gguf`
- (Replace USERNAME and REPO; Hugging Face free accounts allow repos up to 100 GB)

#### Option B: GitHub Releases (easier, but 2 GB per file limit)
- Go to your repo → **Releases** → **Create a new release**
- Upload your `.gguf` file as an attachment
- Copy the download link and paste it in the app
- (GitHub will host it for free, but there's a 2 GB per-file limit)

#### Option C: Your own server
- Upload the file to your own web server / VPS
- Use the direct download URL (e.g. `https://myserver.com/granite4.gguf`)

#### Option D: Local file (if your phone has the file already)
- Tap "Pick from device storage" in the app instead of using a URL
- Point the app to wherever it's stored on your phone

### 4. Install on your phone

1. Enable "Unknown sources" in Android settings (varies by phone)
2. Download the `app-release.apk` from GitHub Actions artifacts
3. Tap it to install, or use `adb install app-release.apk` from your PC
4. Open the app
5. Tap **"Load .gguf model"** → choose **"Download from URL"** → paste your Hugging Face / GitHub / server URL
6. Wait for the download (~1–10 min depending on model size and your connection)
7. Tap **"Enable Accessibility permission"** and follow Android's prompt to turn it on in Settings
8. Connect a Bluetooth gamepad and press the button you want as your toggle
9. Open AnkiDroid, review a card, select some text, and the overlay should appear

## Troubleshooting

### APK build failed in Actions
- Check the **Actions** tab → click the failed workflow → scroll down for error output
- Common issues:
  - llama.cpp clone timeout: the repo is large; retrying usually fixes it
  - Gradle sync issue: delete `build/` folder and retry
  - JNI/CMake issue: usually a sign that llama.cpp's C API changed; dm if you hit this

### Model download hangs
- Large models (500 MB+) will take time; don't close the app
- If it times out after 10+ min, try hosting it on a faster server (Hugging Face is usually fast)

### Text selection not detected in AnkiDroid
- Check that the Accessibility Service is enabled (Settings → Accessibility → Anki LLM Tooltip)
- Make sure you're selecting text inside AnkiDroid's reviewer (not the editor)
- If it still doesn't work, there's a fallback: modify the app to use AnkiDroid's JS API instead of accessibility events; dm for that variant

### Gamepad button not binding
- Make sure you connect the Bluetooth controller *before* opening the app
- Some phones require the gamepad to be paired in Settings first
- In the app, press the button when the "Waiting for button press…" message is visible

## Customizing

All settings live in the app's settings screen or in `Prefs.kt`:
- System prompt (the instruction to the model)
- Max tokens (how long the explanation can be)
- Temperature (randomness; lower = more deterministic)
- Hover delay (unused on mobile, kept for code parity)

Edit those constants in `Prefs.kt` before pushing to GitHub if you want different defaults.

## What happens after you build & install

The app runs a local, on-device llama.cpp inference. No network calls after the model is downloaded — your card content never leaves your phone. The accessibility service stays running in the background, watching for text selections while you review.
