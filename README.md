# ZentryApp

## Introduction

ZentryApp is an Android application designed for smart attendance tracking, tailored for both lecturers and students.

## Environment Requirements

* **Java (Gradle JVM)**: 11.0.19 (check via `./gradlew -version`)
* **Gradle Wrapper**: 8.11.1 (see `gradle/wrapper/gradle-wrapper.properties` for `distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip`)
* **Android Gradle Plugin & Kotlin Plugin**: Refer to the `build.gradle` (project-level) or `gradle/libs.versions.toml` if using Version Catalog.
* **Android SDK**:

  * **compileSdk**: 35
  * **buildToolsVersion**: 35.x.x
  * **minSdkVersion**: 24
  * **targetSdkVersion**: 35
* **Android Studio**: Arctic Fox or later
* **Android SDK Location**:

  * Create a `local.properties` file with:

    ```properties
    sdk.dir=/path/to/Android/sdk
    ```
  * Or export an environment variable:

    ```bash
    export ANDROID_SDK_ROOT=/path/to/Android/sdk
    ```

## Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/SEP490-SU25-G16/zentry-app.git
   cd zentry-app
   ```
2. \*\*Create \*\***`local.properties`** (if not already present) with the SDK path as above.
3. **(Optional)** Install JDK 11 and configure system `JAVA_HOME`/`PATH` if you prefer a system JDK:

   ```bash
   # Windows (PowerShell, run as Admin)
   setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-11.0.19.1-hotspot"
   setx PATH "%JAVA_HOME%\bin;%PATH%"
   ```

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
* **Plugins** (KTS syntax):

  ```kotlin
  plugins {
      alias(libs.plugins.android.application)
      alias(libs.plugins.kotlin.android)
  }
  ```

## Build & Run

1. **Build the project**:

   ```bash
   ./gradlew clean build
   ```
2. **Open in Android Studio**:

   * File > Open > select the `zentry-app` directory
   * Choose the `app` module and click Run on an emulator or physical device.

## Key Libraries

* PinView 1.4.3
* AndroidX Navigation (Fragment, UI)
* AppCompat, Material, ConstraintLayout, Activity KTX
* JUnit, Espresso for testing

## Contact

For questions or issues, please reach out:

* **Email**: [khanhtlhe176617@fpt.edu.vn](mailto:khanhtlhe176617@fpt.edu.vn)
* **GitHub Issues**: [https://github.com/SEP490-SU25-G16/zentry-app/issues](https://github.com/SEP490-SU25-G16/zentry-app/issues)
