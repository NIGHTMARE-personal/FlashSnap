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

### Sovereign Multimodal Study Engine // Handwriting Vision OCR, Leitner 5-Box SRS, & Negative-Marking Exam Crucible

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20MD3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20MVVM%20%2B%20UDF-000000?style=for-the-badge)](https://developer.android.com/topic/architecture)
[![License](https://img.shields.io/badge/License-MIT-ddb568?style=for-the-badge)](LICENSE)
[![Studio](https://img.shields.io/badge/Architect-NIGHTMARE%20PROJECTS-1a1a1a?style=for-the-badge)](https://github.com/NIGHTMARE-personal)

</div>

---

## 1. Operational Overview

**FlashSnap** is an offline-first sovereign study platform engineered to eliminate the cognitive friction between analog handwriting and long-term memory retention. 

Most digital study apps suffer from two fatal failure modes:
1. **The Ingestion Bottleneck**: Manually typing flashcards on a mobile keyboard takes 10x longer than handwritten notation, leading to study fatigue before revision begins.
2. **The Passive Recognition Illusion**: Simple swipe-based flashcards reward vague familiarity rather than precise recall, resulting in catastrophic failure under actual exam pressure.

FlashSnap resolves both failure modes mechanically:
- **Optical Ingestion**: CameraX-driven high-contrast optical capture feeds raw handwritten notebook pages, textbook diagrams, equations, and tables directly into a multimodal neural transcription pipeline, generating structured question-and-answer pairs in seconds.
- **Negative-Marking Crucible**: Study sessions transition into high-stakes test simulations featuring strict negative marking, finite lives, and streak-multiplier mechanics to build genuine recall under pressure.
- **Leitner Spaced Repetition**: Memory degradation is systematically counteracted via an algorithmic 5-box shelf running exponential decay scheduling.

---

## 2. Core Subsystems & Technical Architecture

```
                                  [ PHYSICAL NOTEBOOK ]
                                            │ (CameraX Sensor Stream)
                                            ▼
                           ┌─────────────────────────────────┐
                           │   Document Ingestion Pipeline   │
                           │   - Perspective & Contrast Warp │
                           │   - 1-to-3 Multi-Page Batching  │
                           └────────────────┬────────────────┘
                                            │
                                            ▼
                           ┌─────────────────────────────────┐
                           │ Multimodal Neural OCR Service   │
                           │ - Handwriting & Cursive Parse   │
                           │ - Formula & Diagram Synthesis   │
                           └────────────────┬────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               FlashSnap State Machine                                 │
│                                                                                        │
│   ┌────────────────────────┐  ┌────────────────────────┐  ┌────────────────────────┐  │
│   │ Leitner 5-Box SRS Shelf│  │ Negative-Marking Engine│  │  Cram Blitz Engine     │  │
│   │ - Exponential Intervals│  │ - (+15 / +5 / -10 XP)  │  │ - Low-Box Filtration   │  │
│   │ - Strict Reset Logic   │  │ - 3-Heart Penalty Mode │  │ - High-Decay Prioritize│  │
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

### A. High-Throughput CameraX Ingestion
- Viewfinder calibrated with document framing guides, pinch-to-zoom hardware telemetry, torch toggling, and multi-page capture buffers.
- Pre-processing pipeline enhances contrast and isolates line work from paper shadows prior to neural inference.
- Generates structured schema containing card title, core concepts, step-by-step solutions, and distractors for multiple-choice verification.

### B. Leitner 5-Box Spaced Repetition Machine
- **Box 1 (Daily)**: Freshly ingested cards and failed recall attempts.
- **Box 2 (3-Day Interval)**: Intermediate reinforcement.
- **Box 3 (Weekly)**: Consolidating memory constructs.
- **Box 4 (Fortnightly)**: Long-term retention stabilization.
- **Box 5 (Monthly / Mastered)**: Permanent knowledge anchors.
- Any missed answer immediately drops the target card back to Box 1, preventing false confidence.

### C. Competitive Negative-Marking Exam Engine
- Modeled after competitive medical and engineering admissions exams (NEET, JEE, SAT):
  - **First-Pass Correct**: `+15 XP`
  - **Subsequent Pass Correct**: `+5 XP`
  - **Incorrect Penalty**: `-10 XP` and loss of 1 Heart (out of 3).
- Real-time combo multiplier scaling (`1.0x` to `2.5x`) rewards consistent accuracy while punishing reckless guessing.

### D. Offline-First Room Database & Duplex PDF Compiler
- Zero-cloud dependency for daily review: local Room SQLite database manages all decks, cards, history logs, and profile statistics.
- **Duplex A4 PDF Export**: Compiles digital decks into printable physical flashcard sheets with archival warm-paper aesthetics (`#F9F6F0`), cutting crop guidelines, and duplex mirror alignment for physical study.

---

## 3. Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose (Material Design 3, Dynamic Color, Edge-to-Edge) |
| **Architecture** | Clean Architecture / MVVM with Unidirectional Data Flow (`StateFlow`) |
| **Persistence** | AndroidX Room (SQLite) with KSP compiler code generation |
| **Camera & Vision** | AndroidX CameraX (`camera2`, `view`, `lifecycle`) |
| **Image Loading** | Coil Compose |
| **Inference API** | Google Gemini Multimodal REST API (via OkHttp3 & Moshi) |
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
│   │   │   │   │   └── repository/      # Room database & local SharedPreferences repository
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/      # Leitner visualizer, cram blitz, exam charts
│   │   │   │   │   ├── screens/         # Compose viewports (Camera, Decks, Quiz, Profile)
│   │   │   │   │   ├── theme/           # MD3 Color scheme, Typography, Surface palettes
│   │   │   │   │   └── viewmodel/       # MainViewModel & StateFlow coordination
│   │   │   │   ├── util/                # Duplex PDF exporter & synthesized audio feedback
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
- **Privacy First**: Optical camera frames are processed directly in-memory and stored only in internal application cache.

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

2. **Configure Environment**:
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
