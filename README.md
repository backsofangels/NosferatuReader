# NosferatuLauncher

A minimalist and high-contrast e-reader specifically designed for **Nosferatu OS**.

## Purpose

NosferatuReader evolves from a simple invisible bridge into a complete native reading experience. Built with a brutalist and functional philosophy, it is optimized for tablet devices and designed to eliminate every distraction between the reader and the text.

## Features

* **Native Reading Engine**: Powered by the Readium toolkit, ensuring full support for the EPUB standard.
* **Brutalist Interface**: UI written entirely in Jetpack Compose, featuring high-contrast (Black/White) design for maximum legibility.
* **Library Management**: Automatic local file synchronization with metadata extraction (Title, Author, Covers).
* **Immersive Experience**: Full-screen reading mode with minimal progress indicators and on-demand typographic controls.
* **Persistence**: Automatically saves the last reading position and user preferences (font size) using Room and SharedPreferences.

## Requirements

* **Android SDK**: 25 (Nougat 7.1.2) or higher.
* **Target SDK**: 36 (Android 15+).
* **Target Environment**: Nosferatu OS (or any AOSP-based system).

## Project Structure
The project is organized into the following main packages:
* ``data``: Handles database entities and DAOs.
* ``library``: Scans the file system, manages covers, and handles configurations.
* ``parser``: Metadata extraction strategies for EPUB files.
* ``reader``: Implementation of the Readium-based reading activity.
* ``ui``: Compose components for the Home and Library screens.

## Build Instructions

This project uses the standard Gradle build system. You can build the APK using Android Studio or via the command line:

```bash
# Clean and sync tasks
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

 ##License
This project is distributed under the **Apache License 2.0**.
