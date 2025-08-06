package vn.edu.fpt.zentryapp.student.data.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Extracts detailed facial landmarks for eye blink detection and gaze estimation
 */
public class FaceLandmarkExtractor {
    private static final String TAG = "FaceLandmarkExtractor";
    
    // ML Kit Face Detector with high accuracy settings
    private final FaceDetector faceDetector;
    private final ExecutorService executor;
    private final Handler mainHandler;
    
    // Landmarks for left and right eyes
    private List<PointF> leftEyePoints = new ArrayList<>();
    private List<PointF> rightEyePoints = new ArrayList<>();
    
    // Face landmarks and contours
    private Map<Integer, PointF> faceLandmarks = new HashMap<>();
    private Map<Integer, List<PointF>> faceContours = new HashMap<>();
    
    // Face detection results
    private float leftEyeOpenProbability = 1.0f;
    private float rightEyeOpenProbability = 1.0f;
    private float[] headEulerAngles = new float[3]; // Pitch, roll, yaw
    
    // Cached face and eye regions
    private Bitmap lastLeftEyeRegion;
    private Bitmap lastRightEyeRegion;
    
    /**
     * Creates a new face landmark extractor
     * @param context Application context
     */
    public FaceLandmarkExtractor(Context context) {
        // Configure face detector for high accuracy and detailed landmarks
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();
        
        this.faceDetector = FaceDetection.getClient(options);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "Face landmark extractor initialized");
    }
    
    /**
     * Callback for face landmark extraction results
     */
    public interface LandmarkExtractionCallback {
        void onLandmarksExtracted(boolean success);
    }
    
    /**
     * Extract landmarks from a face image
     * 
     * @param faceBitmap The face bitmap
     * @param faceRect The face bounding box
     * @param callback Callback for extraction results
     */
    public void extractLandmarks(Bitmap faceBitmap, Rect faceRect, LandmarkExtractionCallback callback) {
        executor.execute(() -> {
            try {
                // Create input image from bitmap
                InputImage image = InputImage.fromBitmap(faceBitmap, 0);
                
                // Process image with ML Kit face detector
                Task<List<Face>> faceDetectionTask = faceDetector.process(image);
                
                // Wait for detection to complete
                List<Face> faces = Tasks.await(faceDetectionTask);
                
                if (faces == null || faces.isEmpty()) {
                    Log.w(TAG, "No faces detected in extractLandmarks");
                    runOnMainThread(() -> callback.onLandmarksExtracted(false));
                    return;
                }
                
                // Get the face with the highest confidence or the one closest to the provided rect
                Face face = selectBestFace(faces, faceRect);
                
                // Extract face data
                processFace(face, faceBitmap);
                
                // Extract eye regions for gaze estimation
                extractEyeRegions(faceBitmap, face);
                
                runOnMainThread(() -> callback.onLandmarksExtracted(true));
                
            } catch (Exception e) {
                Log.e(TAG, "Error extracting landmarks", e);
                runOnMainThread(() -> callback.onLandmarksExtracted(false));
            }
        });
    }
    
    /**
     * Process face to extract landmarks, contours, and eye states
     */
    private void processFace(Face face, Bitmap faceBitmap) {
        // Clear previous data
        leftEyePoints.clear();
        rightEyePoints.clear();
        faceLandmarks.clear();
        faceContours.clear();
        
        // Get face landmarks
        if (face.getLandmark(FaceLandmark.LEFT_EYE) != null) {
            faceLandmarks.put(FaceLandmark.LEFT_EYE, face.getLandmark(FaceLandmark.LEFT_EYE).getPosition());
        }
        if (face.getLandmark(FaceLandmark.RIGHT_EYE) != null) {
            faceLandmarks.put(FaceLandmark.RIGHT_EYE, face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition());
        }
        if (face.getLandmark(FaceLandmark.NOSE_BASE) != null) {
            faceLandmarks.put(FaceLandmark.NOSE_BASE, face.getLandmark(FaceLandmark.NOSE_BASE).getPosition());
        }
        if (face.getLandmark(FaceLandmark.LEFT_EAR) != null) {
            faceLandmarks.put(FaceLandmark.LEFT_EAR, face.getLandmark(FaceLandmark.LEFT_EAR).getPosition());
        }
        if (face.getLandmark(FaceLandmark.RIGHT_EAR) != null) {
            faceLandmarks.put(FaceLandmark.RIGHT_EAR, face.getLandmark(FaceLandmark.RIGHT_EAR).getPosition());
        }
        if (face.getLandmark(FaceLandmark.MOUTH_LEFT) != null) {
            faceLandmarks.put(FaceLandmark.MOUTH_LEFT, face.getLandmark(FaceLandmark.MOUTH_LEFT).getPosition());
        }
        if (face.getLandmark(FaceLandmark.MOUTH_RIGHT) != null) {
            faceLandmarks.put(FaceLandmark.MOUTH_RIGHT, face.getLandmark(FaceLandmark.MOUTH_RIGHT).getPosition());
        }
        if (face.getLandmark(FaceLandmark.MOUTH_BOTTOM) != null) {
            faceLandmarks.put(FaceLandmark.MOUTH_BOTTOM, face.getLandmark(FaceLandmark.MOUTH_BOTTOM).getPosition());
        }
        
        // Extract face contours
        if (face.getContour(FaceContour.LEFT_EYE) != null) {
            faceContours.put(FaceContour.LEFT_EYE, new ArrayList<>(face.getContour(FaceContour.LEFT_EYE).getPoints()));
            leftEyePoints.addAll(face.getContour(FaceContour.LEFT_EYE).getPoints());
        }
        if (face.getContour(FaceContour.RIGHT_EYE) != null) {
            faceContours.put(FaceContour.RIGHT_EYE, new ArrayList<>(face.getContour(FaceContour.RIGHT_EYE).getPoints()));
            rightEyePoints.addAll(face.getContour(FaceContour.RIGHT_EYE).getPoints());
        }
        
        // Get eye open probabilities
        if (face.getLeftEyeOpenProbability() != null) {
            leftEyeOpenProbability = face.getLeftEyeOpenProbability();
        }
        if (face.getRightEyeOpenProbability() != null) {
            rightEyeOpenProbability = face.getRightEyeOpenProbability();
        }
        
        // Get head pose
        headEulerAngles[0] = face.getHeadEulerAngleX(); // Pitch
        headEulerAngles[1] = face.getHeadEulerAngleY(); // Roll
        headEulerAngles[2] = face.getHeadEulerAngleZ(); // Yaw
        
        Log.d(TAG, "Face landmarks extracted. Left eye points: " + leftEyePoints.size() 
                + ", Right eye points: " + rightEyePoints.size()
                + ", Eye open probabilities: [" + leftEyeOpenProbability + ", " + rightEyeOpenProbability + "]"
                + ", Head pose: [" + headEulerAngles[0] + ", " + headEulerAngles[1] + ", " + headEulerAngles[2] + "]");
    }
    
    /**
     * Extract eye regions for gaze estimation
     */
    private void extractEyeRegions(Bitmap faceBitmap, Face face) {
        try {
            // Get eye landmarks
            FaceLandmark leftEyeLandmark = face.getLandmark(FaceLandmark.LEFT_EYE);
            FaceLandmark rightEyeLandmark = face.getLandmark(FaceLandmark.RIGHT_EYE);
            
            if (leftEyeLandmark == null || rightEyeLandmark == null) {
                Log.w(TAG, "Eye landmarks not found");
                return;
            }
            
            // Calculate eye regions with padding
            PointF leftEyePosition = leftEyeLandmark.getPosition();
            PointF rightEyePosition = rightEyeLandmark.getPosition();
            
            int eyeSize = (int) (faceBitmap.getWidth() * 0.2); // 20% of face width
            
            Rect leftEyeRect = new Rect(
                    (int) (leftEyePosition.x - eyeSize/2),
                    (int) (leftEyePosition.y - eyeSize/2),
                    (int) (leftEyePosition.x + eyeSize/2),
                    (int) (leftEyePosition.y + eyeSize/2)
            );
            
            Rect rightEyeRect = new Rect(
                    (int) (rightEyePosition.x - eyeSize/2),
                    (int) (rightEyePosition.y - eyeSize/2),
                    (int) (rightEyePosition.x + eyeSize/2),
                    (int) (rightEyePosition.y + eyeSize/2)
            );
            
            // Ensure eye regions are within face bitmap bounds
            leftEyeRect.left = Math.max(0, leftEyeRect.left);
            leftEyeRect.top = Math.max(0, leftEyeRect.top);
            leftEyeRect.right = Math.min(faceBitmap.getWidth(), leftEyeRect.right);
            leftEyeRect.bottom = Math.min(faceBitmap.getHeight(), leftEyeRect.bottom);
            
            rightEyeRect.left = Math.max(0, rightEyeRect.left);
            rightEyeRect.top = Math.max(0, rightEyeRect.top);
            rightEyeRect.right = Math.min(faceBitmap.getWidth(), rightEyeRect.right);
            rightEyeRect.bottom = Math.min(faceBitmap.getHeight(), rightEyeRect.bottom);
            
            // Extract eye regions
            if (leftEyeRect.width() > 0 && leftEyeRect.height() > 0) {
                lastLeftEyeRegion = Bitmap.createBitmap(
                        faceBitmap,
                        leftEyeRect.left,
                        leftEyeRect.top,
                        leftEyeRect.width(),
                        leftEyeRect.height()
                );
            }
            
            if (rightEyeRect.width() > 0 && rightEyeRect.height() > 0) {
                lastRightEyeRegion = Bitmap.createBitmap(
                        faceBitmap,
                        rightEyeRect.left,
                        rightEyeRect.top,
                        rightEyeRect.width(),
                        rightEyeRect.height()
                );
            }
            
            Log.d(TAG, "Eye regions extracted. Left eye: " + leftEyeRect + ", Right eye: " + rightEyeRect);
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting eye regions", e);
        }
    }
    
    /**
     * Select the best face from detected faces
     */
    private Face selectBestFace(List<Face> faces, Rect targetRect) {
        if (faces.size() == 1) {
            return faces.get(0);
        }
        
        // If we have a target rectangle, find the face that best matches it
        if (targetRect != null) {
            Face bestMatch = null;
            float bestIou = 0;
            
            for (Face face : faces) {
                Rect faceRect = face.getBoundingBox();
                float iou = calculateIoU(faceRect, targetRect);
                
                if (iou > bestIou) {
                    bestIou = iou;
                    bestMatch = face;
                }
            }
            
            if (bestMatch != null) {
                return bestMatch;
            }
        }
        
        // Otherwise, return the face with the largest width (typically the closest/most prominent)
        Face largest = faces.get(0);
        for (Face face : faces) {
            if (face.getBoundingBox().width() > largest.getBoundingBox().width()) {
                largest = face;
            }
        }
        
        return largest;
    }
    
    /**
     * Calculate Intersection over Union for two rectangles
     */
    private float calculateIoU(Rect rect1, Rect rect2) {
        int intersectionLeft = Math.max(rect1.left, rect2.left);
        int intersectionTop = Math.max(rect1.top, rect2.top);
        int intersectionRight = Math.min(rect1.right, rect2.right);
        int intersectionBottom = Math.min(rect1.bottom, rect2.bottom);
        
        if (intersectionLeft >= intersectionRight || intersectionTop >= intersectionBottom) {
            return 0;
        }
        
        int intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop);
        int rect1Area = rect1.width() * rect1.height();
        int rect2Area = rect2.width() * rect2.height();
        
        return (float) intersectionArea / (rect1Area + rect2Area - intersectionArea);
    }
    
    /**
     * Get the 6 key points for EAR calculation for left eye
     * Points are arranged as:
     * 0: left corner
     * 1: top left
     * 2: top right
     * 3: right corner
     * 4: bottom right
     * 5: bottom left
     */
    public List<PointF> getLeftEyeEARPoints() {
        if (leftEyePoints.size() < 6) {
            return new ArrayList<>();
        }
        
        List<PointF> earPoints = new ArrayList<>();
        
        // For ML Kit, the eye contour typically has 8 points
        // We need to select the 6 points that correspond to the EAR calculation
        if (leftEyePoints.size() >= 8) {
            // Left corner (0)
            earPoints.add(leftEyePoints.get(0));
            // Top left (1)
            earPoints.add(leftEyePoints.get(1));
            // Top right (2)
            earPoints.add(leftEyePoints.get(2));
            // Right corner (3)
            earPoints.add(leftEyePoints.get(3));
            // Bottom right (4)
            earPoints.add(leftEyePoints.get(5));
            // Bottom left (5)
            earPoints.add(leftEyePoints.get(7));
        } else {
            // If we don't have enough points, use what we have
            earPoints.addAll(leftEyePoints);
        }
        
        return earPoints;
    }
    
    /**
     * Get the 6 key points for EAR calculation for right eye
     * Points are arranged the same as left eye
     */
    public List<PointF> getRightEyeEARPoints() {
        if (rightEyePoints.size() < 6) {
            return new ArrayList<>();
        }
        
        List<PointF> earPoints = new ArrayList<>();
        
        if (rightEyePoints.size() >= 8) {
            // Left corner (0)
            earPoints.add(rightEyePoints.get(0));
            // Top left (1)
            earPoints.add(rightEyePoints.get(1));
            // Top right (2)
            earPoints.add(rightEyePoints.get(2));
            // Right corner (3)
            earPoints.add(rightEyePoints.get(3));
            // Bottom right (4)
            earPoints.add(rightEyePoints.get(5));
            // Bottom left (5)
            earPoints.add(rightEyePoints.get(7));
        } else {
            earPoints.addAll(rightEyePoints);
        }
        
        return earPoints;
    }
    
    /**
     * Get the left eye open probability
     */
    public float getLeftEyeOpenProbability() {
        return leftEyeOpenProbability;
    }
    
    /**
     * Get the right eye open probability
     */
    public float getRightEyeOpenProbability() {
        return rightEyeOpenProbability;
    }
    
    /**
     * Get the head pose in Euler angles [pitch, roll, yaw]
     */
    public float[] getHeadEulerAngles() {
        return headEulerAngles;
    }
    
    /**
     * Get the last extracted left eye region
     */
    public Bitmap getLeftEyeRegion() {
        return lastLeftEyeRegion;
    }
    
    /**
     * Get the last extracted right eye region
     */
    public Bitmap getRightEyeRegion() {
        return lastRightEyeRegion;
    }
    
    /**
     * Check if eyes are closed based on probabilities
     */
    public boolean areEyesClosed() {
        // Both eyes have low open probability
        return leftEyeOpenProbability < 0.3f && rightEyeOpenProbability < 0.3f;
    }
    
    /**
     * Check if all required landmarks are available for eye tracking
     */
    public boolean hasRequiredLandmarks() {
        return !leftEyePoints.isEmpty() && !rightEyePoints.isEmpty();
    }
    
    /**
     * Run a runnable on the main thread
     */
    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
    
    /**
     * Release resources
     */
    public void close() {
        faceDetector.close();
        executor.shutdown();
    }
}
