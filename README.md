# ThreadsPulse for Android

**English** | [繁體中文](README.zh-TW.md)

An advanced enhancement module and media toolkit for Meta's Threads (`com.instagram.barcelona`) on Android. Current Version: **1.27.0**

This repository is the official Android port of the popular iOS tweak [ThreadsPulse](https://github.com/mumuisdog/ThreadsPulse), bringing comprehensive ad blocking, tracking removal, media saving, and interface customizations to Android via **LSPosed (Root)** and **LSPatch / GitHub Actions (Non-Root)**.

---

## Features

| Category | Feature | Description |
|---|---|---|
| **Ads & Privacy** | Block Ads | Remove sponsored posts and advertisements from your feed |
| | Disable Screenshot Detection | Bypass screenshot / screen recording detection callbacks and remove screen capture restrictions |
| | Block Telemetry / Analytics | Prevent analytics events (dwell time, clicks, telemetry batches) from reporting |
| | Strip URL Tracking Parameters | Automatically remove tracking tags (`xmt`, `igshid`, `utm_*`, etc.) and unwrap `l.threads.com` redirection links |
| **Media Saving** | In-App Download Action | Access a dedicated ThreadsPulse download trigger from the post options menu |
| | Dedicated Post Downloader | Paste any Threads URL or use Android's native "Share" target to inspect and download |
| | Highest Quality Images | Download full-resolution original photos |
| | High Quality Video Downloads | Extract 1080p DASH and progressive MP4 video streams |
| | Multi-threaded Downloads | Save multi-image/video carousel posts with up to 4 parallel download workers |
| | Custom Gallery Album | Automatically organize downloaded items under a dedicated `Threads` album in your gallery |
| **Player** | Disable Autoplay | Stop videos from playing automatically while scrolling your feed |
| | Playback Speed Controls | Choose default playback rate (1.0x, 1.25x, 1.5x, 2.0x) |
| **Home Feed** | Retain Feed Position | Retain your scroll location without auto-refreshing when switching back from background |
| | Disable Pull-to-Refresh | Prevent accidental page refreshes when swiping down at the top of the feed |
| | Tab Reselect Behavior | Retain scroll position when tapping the Home tab; prevent unwanted jump-to-top and reloads |
| **Suggestions** | Hide Suggested Accounts | Hide "Suggested for you" cards from both the feed and profile views |
| | Hide Follow Recommendations | Hide suggested accounts in the search and explore sections |
| | Hide Feedback Prompts | Suppress "Are you interested in this post?" survey banners |
| **Direct Messaging** | Ghost Reading (No Seen) | Read direct messages without sending read receipts |
| | Hide Typing Indicator | Prevent the typing animation ("...") from showing to recipients |
| | Mark Seen on Reply | Optional toggle: send read receipt only once you actually send a reply |
| **Interaction** | Confirm Like | Prompt for confirmation before liking to prevent accidental taps |
| | Open External Browser | Open external links in your system browser rather than the in-app browser |
| | Quick Settings Entry | Long-press the Home tab icon inside Threads to launch the ThreadsPulse settings dashboard |
| | Backup & Restore | Export and import all configuration settings via JSON |

---

## 🎨 Theme Colors

Featuring the 6 signature color themes from ThreadsPulse:
- **Coral**
- **Ocean**
- **Mint**
- **Lavender**
- **Sakura**
- **Amber**

---

## 🚀 Installation & Usage

### 1. Rooted Setup (LSPosed)

1. Ensure your device is rooted with Magisk / KernelSU / APatch and has **LSPosed** installed.
2. Grab the latest `ThreadsPulse-Release.apk` from the Releases section.
3. In LSPosed Manager, enable **ThreadsPulse** and select **Threads (`com.instagram.barcelona`)** as the target scope.
4. Force close Threads and launch it.

---

### 2. Non-Root Setup (Automated GitHub Actions CI/CD)

You can build a patched Threads APK directly on GitHub without rooting your device:

1. Navigate to the **Actions** tab of this repository.
2. Select the **Build Patched Threads APK (LSPatch Non-Root)** workflow.
3. Click **Run workflow**:
   - Provide a direct download URL to a clean Threads base APK in the `threads_apk_url` input field, or configure `THREADS_APK_URL` in your repository secrets.
4. The workflow will build the module, embed it into the Threads base APK via LSPatch, sign the package, and publish a ready-to-install `Threads_Pulse_Patched.apk` in the run's Artifacts.

---

### 3. Local Non-Root Setup (LSPatch App)

1. Obtain a clean Threads base APK and `ThreadsPulse-Release.apk`.
2. Open the [LSPatch](https://github.com/LSPosed/LSPatch) application on your device.
3. Select **Patch an application** → Choose Threads APK → Select **ThreadsPulse** as the embedded module.
4. Install the resulting patched APK.

---

## 🛠️ Building From Source

Requires JDK 17 and Android SDK 34.

```bash
# Clone the repository
git clone https://github.com/csie1b/ThreadsPulse-Android.git
cd ThreadsPulse-Android

# Linux / macOS
chmod +x ./gradlew
./gradlew assembleRelease

# Windows (PowerShell)
.\gradlew.bat assembleRelease
```

---

## 📄 License & Disclaimer

- This project is for personal educational and reverse engineering research purposes only.
- Inspired by and adapted from the iOS project [ThreadsPulse](https://github.com/mumuisdog/ThreadsPulse).
- Not affiliated with Meta, Instagram, or Threads.
