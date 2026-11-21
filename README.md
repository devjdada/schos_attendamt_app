# School OS Attendance (Native Android)

**Application Identity:** `School OS Attendance`  
**GitHub Repository:** `schos_attendamt_app`  
**Target Platform:** Native Android (Min SDK 26 / Target SDK 34)  
**Programming Language:** Kotlin  
**UI Framework:** Jetpack Compose with Material 3  
**Build System:** Gradle Kotlin DSL  

---

## Overview

School OS Attendance is a high-performance native Android application engineered specifically for institutional attendance management, RFID / NFC card terminals, and rapid classroom roll-call workflows.

The application is structured as an independent repository with its own Git lifecycle, build pipeline, and GitHub remote.

## Technology Stack

- **Language:** Kotlin 1.9.22
- **UI Toolkit:** Jetpack Compose (BOM 2024.02.00)
- **Design System:** Material Design 3 (Material 3) with dynamic light and dark theme adaptation
- **Architecture:** AndroidX, ComponentActivity, declarative Composable screens
- **Build System:** Gradle 8.4 with Android Gradle Plugin (AGP) 8.2.2 and Kotlin DSL (`build.gradle.kts`)

## Project Structure

```text
schos_attendamt_app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/schoolos/attendance/
│   │   │   │   ├── AttendanceApplication.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   └── AttendanceLandingScreen.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   └── res/
│   │   │       ├── values/
│   │   │       │   ├── colors.xml
│   │   │       │   └── strings.xml
│   │   │       └── drawable/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (LTS)
- Android SDK Platform 34 (API Level 34)

### Building the Project

```bash
# On Windows
gradlew.bat assembleDebug

# On macOS / Linux
./gradlew assembleDebug
```
