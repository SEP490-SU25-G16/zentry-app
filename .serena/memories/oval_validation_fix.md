# Oval Validation Fix

## Issue
During the liveness challenge phase, the oval boundary was effectively shrinking to a circle, causing the "Face not within oval boundary" error:

```
2025-08-07 19:36:44.104 29045-29045 RegisterFaceIdFragment  vn.edu.fpt.zentryapp E  ❌ Frame processing error: Face not within oval boundary
```

This error was causing the face registration process to fail.

## Root Cause
The issue was due to inconsistent validation logic between different phases of the face registration process:

1. During initial face detection, the `OvalFaceOverlayView.validateFaceWithinOval()` method was using a tolerance of 1.2f for the ellipse equation validation.

2. During the liveness challenge, the `FaceIdService.checkFaceWithinOval()` method was using a stricter tolerance of 1.0f.

This inconsistency caused faces that were initially considered within the oval to be rejected during the liveness challenge, even though the user hadn't moved.

## Fix
The fix involved making the oval validation consistent throughout the entire process:

1. Modified `FaceIdService.checkFaceWithinOval()` to use the same ellipse tolerance (1.2f) as in `OvalFaceOverlayView.validateFaceWithinOval()`.

2. Updated the face size ratio thresholds to match those in `OvalFaceOverlayView`:
   - `MIN_FACE_OVAL_RATIO = 0.40f`
   - `MAX_FACE_OVAL_RATIO = 0.90f`

3. Applied the same fix to the `checkFaceWithinOvalFallback()` method, which is used as a fallback validation for registration.

4. Added comments explaining that these values need to match those in `OvalFaceOverlayView` to ensure consistent validation.

## Code Changes

### In FaceIdService.java:

```java
// FIXED: Use same tolerance as OvalFaceOverlayView (1.2f) to ensure consistent validation
// This prevents the "Face not within oval boundary" error during liveness challenge
float ELLIPSE_TOLERANCE = 1.2f; // Must match OvalFaceOverlayView.ELLIPSE_TOLERANCE
boolean isWithinEllipse = ellipseValue <= ELLIPSE_TOLERANCE;

// FIXED: Use same size ratio thresholds as OvalFaceOverlayView for consistency
float MIN_FACE_OVAL_RATIO = 0.40f; // Must match OvalFaceOverlayView.MIN_FACE_OVAL_RATIO
float MAX_FACE_OVAL_RATIO = 0.90f; // Must match OvalFaceOverlayView.MAX_FACE_OVAL_RATIO
boolean isGoodSize = widthRatio >= MIN_FACE_OVAL_RATIO && widthRatio <= MAX_FACE_OVAL_RATIO && 
                    heightRatio >= MIN_FACE_OVAL_RATIO && heightRatio <= MAX_FACE_OVAL_RATIO;
```

## Result
Now the oval boundary validation is consistent throughout the entire process, including during the liveness challenge. The user can complete the face registration without encountering the "Face not within oval boundary" error.