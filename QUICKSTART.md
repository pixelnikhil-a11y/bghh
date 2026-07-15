# QuickStart: From ZIP to Running App

## 1. Extract and upload to GitHub (5 min)

```bash
unzip AnkiLLMTooltip.zip
cd AnkiLLMTooltip
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/anki-llm-tooltip.git
git branch -M main
git push -u origin main
```

(Or use GitHub's web interface: create repo → upload files)

## 2. Wait for Actions to build (3–5 min)

- Go to your GitHub repo → **Actions** tab
- The "Build APK" workflow should be running
- When it's done (green checkmark), click it → scroll to **Artifacts**
- Download `app-release.apk`

## 3. Host your model file

Pick **one** of these:

- **Hugging Face** (easiest for large files)
  1. Create account at https://huggingface.co
  2. Create a new model repo
  3. Upload your `granite4.gguf`
  4. Copy the URL: `https://huggingface.co/USERNAME/REPO/resolve/main/granite4.gguf`

- **GitHub Releases** (quick if model < 2GB)
  1. In your repo, go to **Releases** → **Create a new release**
  2. Upload your `.gguf` as an attachment
  3. Copy the download link

- **Your own server** (if you have one)
  1. Upload the file somewhere accessible
  2. Use the direct download URL

## 4. Install APK on phone

```bash
# Via USB with Android Debug Bridge (if you have adb installed)
adb install app-release.apk

# OR: just tap the APK file on your phone after enabling "Unknown sources"
```

## 5. Configure the app (2 min)

1. Open the app
2. Tap **"Load .gguf model"** → **"Download from URL"**
3. Paste your model URL → wait for download (1–10 min depending on size)
4. Tap **"Enable Accessibility permission"**
   - Android opens Settings → Accessibility
   - Find "Anki LLM Tooltip" → turn ON
5. Connect Bluetooth gamepad
6. Tap **"1. Load .gguf model"** button, then press the gamepad button you want as your toggle
   - The app will remember it
7. Go back to home

## 6. Use it

1. Open **AnkiDroid**
2. Review a card
3. Select some text by tapping and dragging
4. After a moment, the floating explanation bubble appears
5. Press your gamepad button anytime to toggle the feature on/off

---

## Troubleshooting

**"Actions failed" / APK didn't build**
- Check the Actions tab for error messages
- Most common: `git clone llama.cpp` timeout (large repo, just retry)
- Go to your workflow and click "Re-run failed jobs"

**Download hangs**
- Large models (500+ MB) take time
- Hugging Face usually faster than GitHub releases
- Make sure your phone is on WiFi

**Text selection doesn't trigger explanation**
- Go to Settings → Accessibility → make sure "Anki LLM Tooltip" is ON
- Try selecting text again inside AnkiDroid's review screen
- If still nothing: dm, we can try the AnkiDroid JS API fallback

**Gamepad button won't bind**
- Pair it in phone Settings first
- Make sure it's connected when you open the app
- Try pressing different buttons on the Gamepad within the app

---

That's it. Enjoy your on-device LLM explanations!
