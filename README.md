# AylisPlayer 🎵

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org)
[![Design](https://img.shields.io/badge/Design-Material--3-orange.svg)](https://m3.material.io)

**AylisPlayer** is a modern, feature-rich Android media powerhouse built with Material 3 Expressive design. It combines local music playback, online streaming, a reactive audio visualizer, a wallpaper hub, custom typography, and automatic in-app updates.

---

## ✨ Features

- 🎵 **Local & Online Music Playback**: Powered by Google Media3 / ExoPlayer with YouTube Music integration via NewPipeExtractor.
- 🎨 **Reactive Audio Visualizer**: Highly customizable OpenGL and Canvas audio visualizer with reactive spectrums, motion blur, and custom shader effects.
- 🌌 **Ambient Glow Effect**: Dynamic ambient background lighting synchronized with album art.
- 🖼️ **Wallpaper & Photo Hub**: Explore high-resolution artwork powered by Wallhaven with instant downloads.
- ✍️ **Typography Customization**: Custom font downloader allowing personalized UI typefaces.
- 🔄 **In-App Updater**: Automatic update checker using GitHub Releases API with support for pre-releases.
- 🌐 **Multi-Language Support**: Fully localized in English, Russian, Ukrainian, Portuguese (Brazil), and Vietnamese.

---

## 🛠️ Built With & Third-Party Attributions

Special thanks to the open-source projects, libraries, and developers that made AylisPlayer possible:

| Library / Service | License | Description / Author | Link |
| :--- | :--- | :--- | :--- |
| **Avee Open Player** | Open Source | Original base codebase by Azy-Kun | [GitHub](https://github.com/Azy-Kun/AveeOpenPlayer_1.0.34) |
| **Google Media3 / ExoPlayer** | Apache 2.0 | Audio and video playback engine by Google | [GitHub](https://github.com/androidx/media) |
| **NewPipeExtractor** | GPL-3.0 | Lightweight streaming extractor by TeamNewPipe | [GitHub](https://github.com/TeamNewPipe/NewPipeExtractor) |
| **Glide** | BSD/MIT/Apache 2.0 | Efficient media and image loading framework by Bumptech | [GitHub](https://github.com/bumptech/glide) |
| **Retrofit & OkHttp** | Apache 2.0 | Type-safe HTTP client for Android by Square | [GitHub](https://github.com/square/okhttp) |
| **Moshi** | Apache 2.0 | Modern JSON library for Android by Square | [GitHub](https://github.com/square/moshi) |
| **AnimatedBottomBar** | MIT | Customizable bottom navigation bar | [GitHub](https://github.com/DroppingCircle/AnimatedBottomBar) |
| **Jaudiotagger** | LGPL | Audio metadata tagging library | [GitHub](https://github.com/ijabz/jaudiotagger) |
| **Wallhaven** | Service API | High-resolution wallpaper community | [Wallhaven](https://wallhaven.cc) |
| **Cubiq (TheCubiq)** | Acknowledgement | Contributor and open-source enthusiast | [GitHub](https://github.com/TheCubiq) |

---

## 🚀 Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/HeyHaku/AylisPlayer.git
   ```
2. Open the project in **Android Studio** (JDK 17+ recommended).
3. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📄 License

Distributed under the **GNU General Public License v3.0 (GPL-3.0)**. See `LICENSE` and `NOTICE` for details.
