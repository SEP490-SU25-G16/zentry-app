# ZentryApp Project Overview

## Purpose
ZentryApp is an Android application for smart attendance tracking, designed for both lecturers and students at FPT University. The application uses modern facial recognition and anti-spoofing technology to authenticate users.

## Key Features
- Face ID registration, verification, and updating
- Anti-spoofing detection to prevent fake face attacks
- Student and lecturer specific functionality
- Attendance tracking

## Tech Stack
- **Platform**: Android 
- **Language**: Java
- **Minimum SDK**: 24
- **Target SDK**: 35
- **Compile SDK**: 35
- **Build System**: Gradle with Kotlin DSL
- **Viewbinding**: Enabled for UI components

## Key Dependencies
- **Face Recognition**:
  - TensorFlow Lite for on-device ML models
  - Google MediaPipe for face detection
  - Custom face embedding and spoof detection models
- **Networking**:
  - Retrofit2 for API calls
  - OkHttp3 for HTTP client
  - Gson for JSON parsing
- **UI Components**:
  - PinView (3rd party) for PIN input
  - AndroidX Navigation components
  - Camera X for camera functionality
  - Material Components

## Project Structure
The project follows a modular architecture with clear separation of concerns:
- `app/src/main/java/vn/edu/fpt/zentryapp/` - Main package
  - `auth/` - Authentication related classes
  - `helper/` - Utility and helper classes
  - `lecturer/` - Lecturer-specific functionality
  - `student/` - Student-specific functionality
    - `data/` - Data models, APIs, and services
      - `api/` - API interfaces
      - `service/` - Services including Face ID functionality
      - `model/` - Data models
    - `ui/` - UI components and fragments
  - `service/` - Common services
  - `notification/` - Notification handling

## Architecture
The application follows a clean architecture approach with:
- API interfaces for backend communication
- Service layer for business logic
- UI controllers for presentation
- Data models for structure