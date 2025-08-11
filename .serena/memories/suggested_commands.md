# Suggested Commands

## Build and Run
```bash
# Clean and build the project
./gradlew clean build

# Install the app on a connected device/emulator
./gradlew installDebug

# Run specific tasks
./gradlew <task-name>
```

## Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Code Quality
```bash
# Run lint check
./gradlew lint

# Format code (if configured)
./gradlew spotlessApply
```

## Android Studio
The project can be opened in Android Studio for development:
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the project directory and select it
4. Wait for Gradle sync to complete
5. Run the app by clicking the "Run" button

## Git Commands
```bash
# Check status
git status

# Create a new branch
git checkout -b feature/branch-name

# Stage changes
git add .

# Commit changes
git commit -m "Commit message"

# Push changes
git push origin feature/branch-name
```

## Directory Navigation
```bash
# List files in a directory
dir

# Change directory
cd <directory-path>

# Move up one directory
cd ..
```

## Debugging
```bash
# View logs
adb logcat

# Filter logs for app
adb logcat | findstr vn.edu.fpt.zentryapp
```