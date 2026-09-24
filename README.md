# FlashSnap

<div align="center">

```
  ███████╗██╗      █████╗ ███████╗██╗  ██╗███████╗███╗   ██╗ █████╗ ██████╗ 
  ██╔════╝██║     ██╔══██╗██╔════╝██║  ██║██╔════╝████╗  ██║██╔══██╗██╔══██╗
  █████╗  ██║     ███████║███████╗███████║███████╗██╔██╗ ██║███████║██████╔╝
  ██╔══╝  ██║     ██╔══██║╚════██║██╔══██║╚════██║██║╚██╗██║██╔══██║██╔═══╝ 
  ██║     ███████╗██║  ██║███████║██║  ██║███████║██║ ╚████║██║  ██║██║     
  ╚═╝     ╚══════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═══╝╚═╝  ╚═╝╚═╝     
```

### Sovereign Multimodal Study Engine // On-Device Google ML Kit OCR, Hardware CameraX 2K Pipeline, Leitner 5-Box SRS, & Negative-Marking Exam Crucible

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20MD3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![On-Device OCR](https://img.shields.io/badge/OCR-Google%20ML%20Kit%20(100%25%20Offline)-FF6F00?style=for-the-badge&logo=google&logoColor=white)](https://developers.google.com/ml-kit/vision/text-recognition)
[![Camera Engine](https://img.shields.io/badge/CameraX-Hardware%20ImageCapture%202K-00C853?style=for-the-badge)](https://developer.android.com/training/camerax)
[![License](https://img.shields.io/badge/License-MIT-ddb568?style=for-the-badge)](LICENSE)
[![Studio](https://img.shields.io/badge/Architect-NIGHTMARE%20PROJECTS-1a1a1a?style=for-the-badge)](https://github.com/NIGHTMARE-personal)

</div>

---

## 1. Operational Overview

**FlashSnap** is an offline-first sovereign study platform engineered to eliminate the friction between analog handwriting and long-term memory retention. 

Most digital study apps suffer from two fatal failure modes:
1. **The Ingestion Bottleneck**: Manually typing flashcards on a mobile keyboard takes 10x longer than handwritten notation, leading to study fatigue before revision begins.
2. **The Passive Recognition Illusion**: Simple swipe-based flashcards reward vague familiarity rather than precise recall, resulting in catastrophic failure under actual exam pressure.

FlashSnap resolves both failure modes mechanically:
- **Optical Ingestion & On-Device ML Kit OCR**: CameraX-driven hardware sensor capture feeds raw handwritten notebook pages, textbook diagrams, equations, and tables directly into an on-device Google ML Kit OCR engine—operating 100% offline with zero latency and zero API requirements.
- **Hardware Sensor & Filter Modes**: Full 12MP/48MP native sensor capture with interactive tap-to-focus and dynamic document filtering (HD Original, Clean Notes, High-Contrast B&W).
- **Dual-Tier Intelligence Fallback**: Instant local ML Kit transcription backed by optional multimodal cloud Gemini synthesis for conceptual question generation.
- **Dynamic Quiz Generator**: Automatically converts flashcard collections into multiple-choice examinations testing actual conceptual definitions with intelligent distractor sampling.
- **Negative-Marking Crucible**: Study sessions transition into high-stakes test simulations featuring strict negative marking, finite lives, and streak-multiplier mechanics.
- **Leitner Spaced Repetition**: Memory degradation is systematically counteracted via an algorithmic 5-box shelf running exponential decay scheduling.

---

## 2. Core Subsystems & Technical Architecture

```
                                  [ PHYSICAL NOTEBOOK ]
                                             │
                                             ▼
                 ┌────────────────────────────────────────────────────────┐
                 │       CameraX Hardware ImageCapture Pipeline           │
                 │   - CAPTURE_MODE_MAXIMIZE_QUALITY (95% JPEG)           │
                 │   - Interactive Tap-to-Focus & Auto-Exposure Metering  │
                 │   - Filter Modes: [HD Original | Clean Notes | B&W]    │
                 │   - 2K Resolution Ceiling (2048px Full HD+)            │
                 └───────────────────────────┬────────────────────────────┘
                                             │
                                             ▼
                 ┌────────────────────────────────────────────────────────┐
                 │       Dual-Tier Document Transcription Pipeline        │
                 ├────────────────────────────────────────────────────────┤
                 │ [Tier 1: On-Device Primary // 100% Offline]            │
                 │  • Google ML Kit Text Recognition (v16.0.1)            │
                 │  • Verbatim block, line, and bounding box geometry     │
                 │  • Zero network latency & zero API key dependency       │
                 ├────────────────────────────────────────────────────────┤
                 │ [Tier 2: Cloud Synthesis // Multimodal Gemini]         │
                 │  • Conceptual extraction & question generation         │
                 │  • Automatic failover to Tier 1 if offline/delayed     │
                 └───────────────────────────┬────────────────────────────┘
                                             │
                                             ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               FlashSnap State Machine                                 │
│                                                                                        │
│   ┌────────────────────────┐  ┌────────────────────────┐  ┌────────────────────────┐  │
│   │ Leitner 5-Box SRS Shelf│  │ Negative-Marking Engine│  │ Dynamic Quiz Generator │  │
│   │ - Exponential Intervals│  │ - (+15 / +5 / -10 XP)  │  │ - Distractor Sampling  │  │
│   │ - Strict Reset Logic   │  │ - 3-Heart Penalty Mode │  │ - Conceptual Matching  │  │
│   └───────────┬────────────┘  └───────────┬────────────┘  └───────────┬────────────┘  │
│               │                           │                           │               │
└───────────────┼───────────────────────────┼───────────────────────────┼───────────────┘
                ▼                           ▼                           ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        Local Storage & Hardware Interop Layer                         │
│                                                                                        │
│   • Room SQLite Database (Decks, Cards, Review Logs, User Profiles)                   │
│   • Offline Synthesized Audio (Low-latency ToneGenerator & Dynamic Haptics)           │
│   • Duplex Vector PDF Generator (Printable A4 double-sided physical cards)            │
│   • Android Notification Scheduler (AlarmManager study cadence triggers)              │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### A. High-Throughput CameraX Sensor Pipeline (`FullScreenCameraScreen.kt`)
- **Native Hardware Capture**: Replaced low-resolution preview buffer captures with true CameraX `ImageCapture` running `CAPTURE_MODE_MAXIMIZE_QUALITY` with 95% JPEG encoding, utilizing the device's full 12MP/48MP sensor with proper EXIF orientation matrix transforms.
- **Interactive Tap-to-Focus & Auto-Exposure**: Users can tap any point on the viewfinder (e.g., a specific fraction, formula, or cursive line). Features an animated golden focus ring with automatic 3-second auto-cancel metering.
- **Document Filter Mode Selector**:
  - 📷 **HD Original (Default)**: 100% true native sensor color, exposure, and sharpness.
  - 📄 **Clean Notes**: Subtle paper background brightening and ink sharpening for multi-color pen notes.
  - 🖨️ **B&W Scan**: High-contrast monochrome binarization for faded ink or printed handouts.
- **2K Resolution Upgrade**: Maximum image processing ceiling raised from 1280px to **2048px (2K / Full HD+)**, preserving tiny fractions, subscripts, and cursive strokes.

### B. Google ML Kit On-Device OCR Subsystem (`OfflineOcrProcessor.kt`)
- **100% Offline Optical Character Recognition**: Integrated Google ML Kit (`com.google.mlkit:text-recognition:16.0.1`). Operates entirely locally on-device without requiring an internet connection or an API key.
- **Spatial Geometry & Coordinate Extraction**: Accurately extracts verbatim text blocks, individual lines, normalized bounding box coordinates (`relX, relY, relWidth, relHeight`), confidence scores, and subject categorization.
- **Deterministic Failover Guarantee (`GeminiFlashcardService.kt`)**: Cloud Gemini synthesis calls automatically fail over to on-device ML Kit OCR whenever network calls are offline, rate-limited, or delayed.
- **Instant Background Transcription**: Capturing a document automatically triggers background ML Kit OCR, instantly indexing the page's extracted text and classifying its subject.

### C. Dynamic Quiz Generator (`DynamicQuizGenerator.kt`)
- **Automated MCQ Synthesis**: Dynamically compiles multiple-choice quiz questions directly from flashcard decks.
- **Intelligent Distractor Sampling**: Samples believable incorrect options from unrelated cards within the same subject.
- **Zero Meta Questions**: Strictly filters out page numbers and formatting noise, testing pure conceptual comprehension.

### D. Leitner 5-Box Spaced Repetition Machine
- **Box 1 (Daily)**: Freshly ingested cards and failed recall attempts.
- **Box 2 (3-Day Interval)**: Intermediate reinforcement.
- **Box 3 (Weekly)**: Consolidating memory constructs.
- **Box 4 (Fortnightly)**: Long-term retention stabilization.
- **Box 5 (Monthly / Mastered)**: Permanent knowledge anchors.
- Any missed answer immediately drops the target card back to Box 1, preventing false confidence.

### E. Competitive Negative-Marking Exam Engine
- Modeled after competitive admissions examinations (NEET, JEE, SAT):
  - **First-Pass Correct**: `+15 XP`
  - **Subsequent Pass Correct**: `+5 XP`
  - **Incorrect Penalty**: `-10 XP` and loss of 1 Heart (out of 3).
- Real-time combo multiplier scaling (`1.0x` to `2.5x`) rewards consistent accuracy while punishing reckless guessing.

### F. Offline-First Room Database & Duplex PDF Compiler
- Zero-cloud dependency for daily review: local Room SQLite database manages all decks, cards, history logs, and profile statistics.
- **Duplex A4 PDF Export**: Compiles digital decks into printable physical flashcard sheets with archival warm-paper aesthetics (`#F9F6F0`), cutting crop guidelines, and duplex mirror alignment for physical study.

---

## 3. Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose (Material Design 3, Dynamic Color, Edge-to-Edge) |
| **Architecture** | Clean Architecture / MVVM with Unidirectional Data Flow (`StateFlow`) |
| **On-Device OCR** | **Google ML Kit Text Recognition (`16.0.1`)** (100% offline, zero latency) |
| **Camera & Vision** | **AndroidX CameraX** (`camera2`, `view`, `lifecycle`) with `ImageCapture` Max Quality |
| **Cloud Inference** | Google Gemini Multimodal REST API (Optional failover synthesis via OkHttp3 & Moshi) |
| **Persistence** | AndroidX Room (SQLite) with KSP compiler code generation |
| **Image Loading** | Coil Compose |
| **Hardware Cues** | Android `ToneGenerator` (zero-binary audio synthesis) & `Vibrator` haptics |
| **Document Output**| Android Native `PdfDocument` with A4 coordinate mapping |

---

## 4. Repository Structure

```
FlashSnap/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/
│   │   │   │   │   ├── gemini/          # Multimodal neural OCR & card synthesis
│   │   │   │   │   ├── model/           # Deck, Flashcard, QuizQuestion data models
│   │   │   │   │   ├── notification/    # Daily study reminder AlarmManager triggers
│   │   │   │   │   ├── quiz/            # DynamicQuizGenerator (MCQ distractor sampling)
│   │   │   │   │   └── repository/      # Room database & local SharedPreferences repository
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/      # Leitner visualizer, cram blitz, exam charts
│   │   │   │   │   ├── screens/         # Compose viewports (FullScreenCamera, Decks, Quiz, Study, Pages)
│   │   │   │   │   ├── theme/           # MD3 Color scheme, Typography, Surface palettes
│   │   │   │   │   └── viewmodel/       # MainViewModel & StateFlow coordination
│   │   │   │   ├── util/                # OfflineOcrProcessor (ML Kit), Duplex PDF exporter, audio
│   │   │   │   └── MainActivity.kt      # Root activity & navigation coordinator
│   │   │   └── res/                     # Vector XML resources & launcher icons
│   │   └── test/                        # JVM unit & Robolectric test suite
│   ├── build.gradle.kts                 # Application build rules & dependency graph
│   └── google-services.json.example     # Firebase configuration schema template
├── gradle/                              # Gradle wrapper & Version Catalog (libs.versions.toml)
├── .env.example                         # Local development environment template
├── .gitignore                           # Strict security, keystore, & build exclusions
├── build.gradle.kts                     # Top-level multiplatform build script
├── settings.gradle.kts                  # Project declaration & plugin management
└── LICENSE                              # MIT License (NIGHTMARE PROJECTS)
```

---

## 5. Security & OPSEC Doctrine

- **Zero Hardcoded Secrets**: Loaded at compile time via Gradle Secrets Plugin into `BuildConfig`.
- **Keystore Isolation**: Debug and release `.keystore` / `*.jks` files are strictly gitignored.
- **Privacy First**: Optical camera frames are processed directly in-memory and stored only in internal application cache. On-device OCR processes images locally without transmitting data.

---

## 6. Build & Setup Instructions

### Prerequisites
- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 17** configured as Gradle JDK
- **Android SDK Platform 34+** (Min SDK: 24, Target SDK: 36)
- Physical device or emulator running Android 7.0+ (API 24+)

### Local Compilation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/NIGHTMARE-personal/FlashSnap.git
   cd FlashSnap
   ```

2. **Configure Environment** *(Optional: Only needed for cloud Gemini synthesis; on-device ML Kit OCR works without keys)*:
   ```bash
   cp .env.example .env
   ```
   Provide your Gemini API key in `.env`:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. **Compile and Install Debug APK**:
   ```bash
   ./gradlew :app:installDebug
   ```

---

## 7. Engineering Attribution

- **Lead Architect & Maintainer**: **NIGHTMARE**
- **Engineering Studio**: **NIGHTMARE-PROJECTS**
- **Repository**: [https://github.com/NIGHTMARE-personal/FlashSnap](https://github.com/NIGHTMARE-personal/FlashSnap)

Licensed under the [MIT License](LICENSE).
