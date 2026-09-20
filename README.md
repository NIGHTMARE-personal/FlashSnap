# FlashNotes 📚⚡

> **Transform handwritten notes and textbook pages into intelligent study flashcards, master topics with Leitner Spaced Repetition, and test yourself under real-world negative-marking exam conditions.**

Built with **Modern Android (Kotlin & Jetpack Compose)**, **Material Design 3**, **Room Database**, **CameraX**, and **Google Gemini AI**.

---

## 🌟 Key Features

- 📸 **AI Document Scanner & Handwriting OCR**:
  - High-resolution camera viewfinder with document framing grid, pinch-to-zoom, torch toggle, and gallery import.
  - Automatic perspective and contrast enhancement for clean notebook captures.
  - Gemini AI handwriting recognition that transcribes diagrams, equations, formulas, and cursive notes into organized cards.

- 🗂️ **Leitner 5-Box Spaced Repetition Engine**:
  - Scientifically proven spaced interval review system (Boxes 1 through 5).
  - Cards advance upon correct answers and reset upon incorrect answers.
  - Interactive 5-box visualization shelf with quick filtering and review triggers.

- ⚡ **Smart Exam Cram Blitz Mode**:
  - Rapid-fire study session focusing on high-difficulty and low-box flashcards before tests and exams.

- 🎯 **Competitive Negative-Marking Exam Engine**:
  - Real exam simulation: **+15 XP** on first correct attempt, **+5 XP** on subsequent attempts, and **-10 XP** penalty with loss of life on incorrect answers.
  - 3-lives challenge with real-time score tracking, streak combos, and detailed post-quiz performance analysis.

- 🔔 **Automated Daily Study Reminders**:
  - Android notification scheduler with customizable study time presets (morning, afternoon, evening, night).
  - Direct notification actions to launch the day's recommended quiz deck.

- 💎 **Gamified XP & Level Progression**:
  - Dynamic student ranks, streak multiplier counters, sound effects (shutter, correct chime, wrong buzz), and tactile haptics.

- 💾 **Offline-First Room Persistence**:
  - Local database stores all decks, flashcards, review histories, captured notebook pages, and user statistics without requiring network access.

---

## 🛡️ Security & Privacy Precautions (GitHub-Ready)

This repository is pre-configured with industry-standard safety practices for open-source hosting:

1. **No Hardcoded Secrets**:
   - API keys and private tokens are loaded securely via the **Secrets Gradle Plugin** and `BuildConfig`.
   - The `.env` file is included in `.gitignore` and is **never committed** to the repository.

2. **Protected Signing Credentials**:
   - `debug.keystore`, `debug.keystore.base64`, and all `*.jks` / `*.keystore` files are strictly excluded via `.gitignore`.
   - Release signing configurations use environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`).

3. **Template Configurations Provided**:
   - `.env.example` provides a clear template for your Gemini API key.
   - `app/google-services.json.example` provides a sample Firebase schema if you wish to link your own Firebase project.

4. **Build Artifacts Excluded**:
   - All `build/`, `.gradle/`, `.kotlin/`, APKs, AABs, and IDE local cache files are excluded in `.gitignore`.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose with Material Design 3 (Dynamic Color, Edge-to-Edge)
- **Architecture**: Clean MVVM (Model - View - ViewModel) with unidirectional data flow (UDF)
- **Local Persistence**: Android Room Database (SQLite) with KSP compiler
- **Camera**: AndroidX CameraX (Lifecycle, Camera2, Viewfinder)
- **Image Loading**: Coil Compose
- **Concurrency**: Kotlin Coroutines & `StateFlow` / `collectAsStateWithLifecycle`
- **AI Integration**: Google Gemini AI REST API via OkHttp & Moshi

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 17** (configured as Gradle JDK in Android Studio)
- **Android SDK Platform 34+** (Min SDK: 24, Target SDK: 36)
- A physical Android device or emulator running Android 7.0 (API 24) or above

### Installation & Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/FlashNotes.git
   cd FlashNotes
   ```

2. **Configure Environment Variables**:
   Copy `.env.example` to `.env` in the root directory:
   ```bash
   cp .env.example .env
   ```
   Open `.env` and add your **Gemini API Key**:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
   > 💡 *You can obtain a free Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey).*

3. **Optional: Firebase Configuration**:
   If you wish to enable Firebase services:
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with package name `flashsnap.android`.
   - Download `google-services.json` and place it in the `app/` directory.

4. **Open in Android Studio**:
   - Launch Android Studio.
   - Select **Open** and choose the `FlashNotes` directory.
   - Wait for Gradle to download dependencies and sync.

5. **Build and Run**:
   - Select your target device or emulator in the toolbar.
   - Click **Run** (`Shift + F10`) or execute via command line:
     ```bash
     gradle :app:installDebug
     ```

---

## 📁 Project Structure

```
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/
│   │   │   │   │   ├── db/          # Room database, DAOs, and entities
│   │   │   │   │   ├── gemini/      # Gemini AI OCR & card generation service
│   │   │   │   │   ├── model/       # Data models (Deck, Flashcard, Profile)
│   │   │   │   │   └── repository/ # Local data repository
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/     # Compose screens (Camera, Decks, Quiz, Stats, Settings)
│   │   │   │   │   ├── theme/       # Material 3 Color scheme, Typography, Shapes
│   │   │   │   │   └── viewmodel/   # FlashNotesViewModel & State holders
│   │   │   │   ├── util/            # Audio sound effects & notification helpers
│   │   │   │   └── MainActivity.kt  # Root activity & navigation coordinator
│   │   │   └── res/                 # Vector drawables, launcher icons, XML resources
│   │   └── test/                    # Unit and Robolectric JVM test suite
│   ├── build.gradle.kts             # App-level dependencies & plugins
│   └── google-services.json.example # Firebase configuration template
├── gradle/                          # Gradle wrapper and version catalog (libs.versions.toml)
├── .env.example                     # Environment template for API keys
├── .gitignore                       # Security & build exclusions
├── build.gradle.kts                 # Project-level build script
└── settings.gradle.kts              # Project modules and repositories
```

---

## 🔒 Security Checklist for Contributors

- [x] Never commit `.env` or files containing plain-text keys.
- [x] Never commit private keystores or signing passwords.
- [x] Run unit tests before opening a pull request: `gradle :app:testDebugUnitTest`.
- [x] Verify build compilation: `gradle :app:assembleDebug`.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) - see the LICENSE file for details.
