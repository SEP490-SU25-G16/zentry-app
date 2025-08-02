# Face ID Implementation

## Overview
The Face ID feature in ZentryApp provides biometric authentication using facial recognition with anti-spoofing capabilities. It's primarily used for student authentication during attendance.

## Key Components

### 1. FaceIdService (Main Service)
- Core service that coordinates all face ID operations
- Handles face detection, embedding generation, and spoof detection
- Communicates with backend API for verification
- Manages model initialization and threading

### 2. Face Detection (using MediaPipe)
- Uses MediaPipe's face detector model
- Detects faces in camera frames
- Provides bounding boxes for detected faces
- Returns cropped face images

### 3. Face Embedding (using FaceNet)
- Generates 512-dimensional face embeddings
- Uses a TensorFlow Lite model (facenet_512.tflite)
- Normalizes and processes face images
- Supports GPU acceleration where available

### 4. Anti-Spoofing Detection
- Uses two complementary spoof detection models
- Models: spoof_model_scale_2_7.tflite and spoof_model_scale_4_0.tflite
- Combines results with an ensemble approach
- Categorizes faces as real or spoofed

### 5. Backend Integration
- API interfaces for registration, update, and verification
- Uses Retrofit for network communication
- Transmits face embeddings to server
- Receives verification results

## Flow
1. Camera captures frames
2. Face detection locates faces in frames
3. Spoof detection confirms real face presence
4. Face stabilization ensures quality captures
5. Face embedding generates biometric signature
6. Backend verifies identity against stored templates

## Key Files
- `FaceIdService.java` - Main service coordinating face processing
- `FaceDetector.java` - Face detection implementation
- `FaceEmbedding.java` - Face embedding generation
- `FaceSpoofDetector.java` - Anti-spoofing implementation
- `FaceIdApi.java` - Backend API interface
- `StudentSettingRegisterFaceIdFragment.java` - UI for registration
- `StudentSettingVerifyFaceIdFragment.java` - UI for verification
- `StudentSettingUpdateFaceIdFragment.java` - UI for updating

## Models
- Face detector: blaze_face_short_range.tflite
- Face embedding: facenet_512.tflite
- Spoof detection: spoof_model_scale_2_7.tflite and spoof_model_scale_4_0.tflite