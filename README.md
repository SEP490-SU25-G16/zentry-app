# ZentryApp

## Introduction

ZentryApp is an Android application for smart attendance tracking, designed for both lecturers and students.

## Environment Requirements

* **Java JDK**: 11.0.19 (check via `java -version`)
* **Gradle Wrapper**: 8.11.1 (see `gradle/wrapper/gradle-wrapper.properties` → `distributionUrl=https://services.gradle.org/distributions/gradle-8.11.1-bin.zip`)
* **Android Gradle Plugin**: 7.4.2 (check `build.gradle` project-level)
* **Kotlin Plugin**: 1.8.10 (check `build.gradle` project-level or `gradle/libs.versions.toml`)
* **Android SDK**:

  * **compileSdk**: 35
  * **buildToolsVersion**: 35.x.x
  * **minSdkVersion**: 24
  * **targetSdkVersion**: 35
* **Android Studio**: Arctic Fox (2020.3.1) or later
* **Gradle JDK (Runtime)**: JetBrains Runtime 21.x (check via **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK** or via `./gradlew -version` under *JVM* line)

## Android SDK Setup

1. Create a `local.properties` file in the project root (not committed) with:

   ```properties
   sdk.dir=/path/to/Android/sdk
   ```
2. Or set an environment variable before building:

   ```bash
   export ANDROID_SDK_ROOT=/path/to/Android/sdk
   ```

## Installation

1. **Clone the repository**:

   ```bash
   git clone https://github.com/SEP490-SU25-G16/zentry-app.git
   cd zentry-app
   ```
2. **Ensure JDK is installed**:

   * Open a terminal and run `java -version` → should show `11.0.19`.
   * Or see Gradle JVM version: `./gradlew -version` (JVM line).
3. **Create or update `local.properties`** as described above.

## Project Configuration

* **Java Compatibility**:

  ```groovy
  android {
      compileOptions {
          sourceCompatibility = JavaVersion.VERSION_11
          targetCompatibility = JavaVersion.VERSION_11
      }
      kotlinOptions {
          jvmTarget = "11"
      }
  }
  ```
* **Plugins** (Kotlin DSL):

  ```kotlin
  plugins {
      alias(libs.plugins.android.application)
      alias(libs.plugins.kotlin.android)
  }
  ```

## Build & Run

1. **Clean and build**:

   ```bash
   ./gradlew clean build
   ```
2. **Open in Android Studio**:

   * File > Open > choose the `zentry-app` folder
   * Select the `app` module and click Run on an emulator or device

## Key Libraries

* **PinView**: 1.4.3
* **AndroidX Navigation** (fragment, UI)
* **AppCompat**, **Material**, **ConstraintLayout**, **Activity KTX**
* **JUnit**, **Espresso** for testing

## Contact

For questions or issues, please reach out:

* **Email**: [khanhtlhe176617@fpt.edu.vn](mailto:khanhtlhe176617@fpt.edu.vn)
* **GitHub Issues**: [https://github.com/SEP490-SU25-G16/zentry-app/issues](https://github.com/SEP490-SU25-G16/zentry-app/issues)
