# Code Style and Conventions

## Java Conventions
- **Naming**:
  - Classes: PascalCase (e.g., `FaceDetector`, `StudentSettingFragment`)
  - Methods/Variables: camelCase (e.g., `processFaceImage`, `currentFrameBitmap`)
  - Constants: UPPER_SNAKE_CASE (e.g., `CAMERA_PERMISSION_REQUEST_CODE`)
  - Private class members are prefixed with nothing (e.g., `faceDetector`, not `mFaceDetector`)

- **Documentation**:
  - Javadoc style comments for classes and public methods
  - Classes usually have a brief description comment
  - Method parameters documented with @param tags when needed
  - Return values documented with @return tags when needed

- **Logging**:
  - Android Log API used with TAG constants
  - Different log levels (d, i, w, e) used appropriately
  - Debug logs include emoji symbols for better readability in logcat

## Architecture
- **Package Structure**:
  - Organized by feature (lecturer, student) and then by layer (data, ui)
  - Clear separation between UI and data components
  - API interfaces and models separated from implementation

- **Design Patterns**:
  - Callback pattern for asynchronous operations
  - Builder pattern for some component initialization
  - Factory pattern for service initialization (e.g., FaceIdServiceManager)
  - State pattern for UI state management

## UI Components
- **Layout**:
  - XML layouts using ConstraintLayout for most UIs
  - ViewBinding used instead of findViewById
  - Component IDs follow a structured naming convention

- **Fragment/Activity**:
  - Lifecycle methods properly handled
  - Resources cleaned up in onDestroy/onDestroyView
  - Permissions requested appropriately

## Error Handling
- Try-catch blocks for specific operations that might fail
- Appropriate error states and user feedback
- Null checks before critical operations
- Fragment attachment checks (isAdded()) before UI updates

## Concurrency
- Executors used for background processing
- Handler used for main thread operations
- CountDownLatch for synchronization between threads
- AtomicBoolean for thread-safe state flags