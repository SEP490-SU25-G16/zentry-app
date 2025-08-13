package vn.edu.fpt.zentryapp.faceid.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

import vn.edu.fpt.zentryapp.faceid.data.service.FaceProcessingState;
import android.util.Log;

public class OvalFaceOverlayView extends View {
    private static final String TAG = "OvalFaceOverlayView";
    
    // Constants for oval dimensions - adjusted for better face fitting
    private static final float OVAL_WIDTH_RATIO = 0.65f;
    private static final float OVAL_HEIGHT_RATIO = 0.8f;
    
    // Constants for face positioning guidance
    private static final float IDEAL_FACE_OVAL_RATIO = 0.70f; // Ideal face size relative to oval
    private static final float MIN_FACE_OVAL_RATIO = 0.40f;   // Minimum acceptable face size (more lenient)
    private static final float MAX_FACE_OVAL_RATIO = 0.90f;   // Maximum acceptable face size (more lenient)
    
    // Positioning tolerance constants (more lenient)
    private static final float CENTERING_TOLERANCE = 0.25f;    // Increased from 0.15f
    private static final float ELLIPSE_TOLERANCE = 1.2f;       // Increased from 1.0f
    private static final float GUIDANCE_TOLERANCE = 0.2f;      // Increased from 0.15f
    private static final float SLIGHT_ADJUSTMENT_TOLERANCE = 0.15f; // Increased from 0.1f
    
    // Paint objects
    private final Paint overlayPaint;
    private final Paint ovalPaint;
    private final Paint progressPaint;
    private final Paint textPaint;
    private final Paint successPaint;
    private final Paint guidePaint;
    
    // Geometry
    private RectF ovalRect;
    private Path ovalPath;
    
    // State
    private FaceProcessingState currentState = FaceProcessingState.INITIALIZING;
    private String statusMessage = "";
    private float progressValue = 0f;
    private ValueAnimator progressAnimator;
    private ValueAnimator successAnimator;
    private float successAnimValue = 0f;
    
    // Face position tracking
    private android.graphics.Rect lastFaceRect;
    private boolean isGoodPosition = false;
    private String positionGuidance = "";
    
    public OvalFaceOverlayView(Context context) {
        this(context, null);
    }
    
    public OvalFaceOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public OvalFaceOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        
        // Enable hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null);
        
        // Initialize paints
        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.parseColor("#80000000")); // Semi-transparent black
        
        ovalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ovalPaint.setStyle(Paint.Style.STROKE);
        ovalPaint.setStrokeWidth(5);
        ovalPaint.setColor(Color.WHITE);
        
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(10);
        progressPaint.setColor(Color.parseColor("#4CAF50")); // Material Green
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        successPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        successPaint.setStyle(Paint.Style.STROKE);
        successPaint.setStrokeWidth(10);
        successPaint.setColor(Color.parseColor("#4CAF50")); // Material Green
        
        guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        guidePaint.setColor(Color.WHITE);
        guidePaint.setTextSize(36);
        guidePaint.setTextAlign(Paint.Align.CENTER);
        guidePaint.setAlpha(200);
        
        // Initialize paths
        ovalPath = new Path();
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        // Calculate oval dimensions
        float ovalWidth = w * OVAL_WIDTH_RATIO;
        float ovalHeight = h * OVAL_HEIGHT_RATIO;
        
        // Center oval in view
        float left = (w - ovalWidth) / 2;
        float top = (h - ovalHeight) / 2;
        float right = left + ovalWidth;
        float bottom = top + ovalHeight;
        
        ovalRect = new RectF(left, top, right, bottom);
        
        // Create oval path
        ovalPath = new Path();
        ovalPath.addOval(ovalRect, Path.Direction.CW);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (ovalRect == null) {
            return;
        }
        
        // Save canvas state
        canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        
        // Draw semi-transparent overlay
        canvas.drawColor(overlayPaint.getColor());
        
        // Create hole in overlay using PorterDuff
        Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawOval(ovalRect, clearPaint);
        
        // Restore canvas
        canvas.restore();
        
        // Draw oval outline
        canvas.drawOval(ovalRect, ovalPaint);
        
        // Draw ideal face position indicators if not in stabilizing or stable state
        if (currentState != FaceProcessingState.FACE_STABILIZING && 
            currentState != FaceProcessingState.FACE_STABLE) {
            drawPositionGuides(canvas);
        }
        
        // Draw progress arc if in stabilizing state
        if (currentState == FaceProcessingState.FACE_STABILIZING && progressValue > 0) {
            float sweepAngle = 360 * progressValue;
            canvas.drawArc(ovalRect, -90, sweepAngle, false, progressPaint);
        }
        
        // Draw success animation if in stable state
        if (currentState == FaceProcessingState.FACE_STABLE && successAnimValue > 0) {
            // Animate oval stroke
            ovalPaint.setColor(Color.parseColor("#4CAF50")); // Green
            ovalPaint.setStrokeWidth(5 + 5 * successAnimValue);
            canvas.drawOval(ovalRect, ovalPaint);
            
            // Draw expanding circles for success animation
            successPaint.setAlpha((int)(255 * (1 - successAnimValue)));
            float expandRatio = 0.1f * successAnimValue;
            RectF expandedRect = new RectF(
                    ovalRect.left - ovalRect.width() * expandRatio,
                    ovalRect.top - ovalRect.height() * expandRatio,
                    ovalRect.right + ovalRect.width() * expandRatio,
                    ovalRect.bottom + ovalRect.height() * expandRatio
            );
            canvas.drawOval(expandedRect, successPaint);
        }
        
        // Draw position guidance message if available
        if (positionGuidance != null && !positionGuidance.isEmpty() && 
            currentState != FaceProcessingState.FACE_STABLE) {
            canvas.drawText(positionGuidance, getWidth() / 2f, 
                    ovalRect.top - 40, guidePaint);
        }
        
        // Draw status message
        if (statusMessage != null && !statusMessage.isEmpty()) {
            canvas.drawText(statusMessage, getWidth() / 2f, 
                    ovalRect.bottom + 80, textPaint);
        }
    }
    
    /**
     * Draw position guides to help user position face correctly
     */
    private void drawPositionGuides(Canvas canvas) {
        // Draw ideal face size indicator
        Paint idealSizePaint = new Paint(ovalPaint);
        idealSizePaint.setStyle(Paint.Style.STROKE);
        idealSizePaint.setStrokeWidth(2);
        idealSizePaint.setColor(Color.parseColor("#AAFFFFFF")); // Translucent white
        idealSizePaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        
        // Draw ideal face oval indicator
        RectF idealRect = new RectF(
                ovalRect.left + ovalRect.width() * (1 - IDEAL_FACE_OVAL_RATIO) / 2,
                ovalRect.top + ovalRect.height() * (1 - IDEAL_FACE_OVAL_RATIO) / 2,
                ovalRect.right - ovalRect.width() * (1 - IDEAL_FACE_OVAL_RATIO) / 2,
                ovalRect.bottom - ovalRect.height() * (1 - IDEAL_FACE_OVAL_RATIO) / 2
        );
        
        canvas.drawOval(idealRect, idealSizePaint);
    }
    
    /**
     * Update the current state and message
     */
    public void updateState(FaceProcessingState state, String message) {
        this.currentState = state;
        this.statusMessage = message;
        
        // Reset progress when state changes
        if (state != FaceProcessingState.FACE_STABILIZING) {
            stopProgressAnimation();
            progressValue = 0f;
        }
        
        // Start success animation when face is stable
        if (state == FaceProcessingState.FACE_STABLE) {
            startSuccessAnimation();
        } else {
            stopSuccessAnimation();
        }
        
        // Update oval color based on state
        updateOvalColor();
        
        invalidate();
    }
    
    /**
     * Update face position feedback
     * @param faceRect The detected face rectangle
     * @return true if face is positioned well
     */
    public boolean updateFacePosition(android.graphics.Rect faceRect) {
        if (faceRect == null || ovalRect == null) {
            positionGuidance = "Position your face in the oval";
            isGoodPosition = false;
            lastFaceRect = null;
            invalidate();
            return false;
        }
        
        lastFaceRect = faceRect;
        
        // Calculate face center relative to oval center
        float faceCenterX = faceRect.exactCenterX();
        float faceCenterY = faceRect.exactCenterY();
        float ovalCenterX = ovalRect.centerX();
        float ovalCenterY = ovalRect.centerY();
        
        // Calculate face width and height relative to oval
        float faceWidth = faceRect.width();
        float faceHeight = faceRect.height();
        float widthRatio = faceWidth / ovalRect.width();
        float heightRatio = faceHeight / ovalRect.height();
        
        // Check if face is properly centered
        float xOffset = Math.abs(faceCenterX - ovalCenterX) / (ovalRect.width() / 2);
        float yOffset = Math.abs(faceCenterY - ovalCenterY) / (ovalRect.height() / 2);
        boolean isCentered = xOffset < 0.45f && yOffset < 0.45f; // Much more lenient centering check
        
        // Check if face size is appropriate
        boolean isGoodSize = 
            widthRatio >= MIN_FACE_OVAL_RATIO && 
            widthRatio <= MAX_FACE_OVAL_RATIO &&
            heightRatio >= MIN_FACE_OVAL_RATIO && 
            heightRatio <= MAX_FACE_OVAL_RATIO;
        
        // Calculate ellipse equation value
        // (x-h)²/a² + (y-k)²/b² ≤ 1 for points inside ellipse
        float a = ovalRect.width() / 2; // semi-major axis
        float b = ovalRect.height() / 2; // semi-minor axis
        float ellipseValue = (float) (
            Math.pow(faceCenterX - ovalCenterX, 2) / Math.pow(a, 2) +
            Math.pow(faceCenterY - ovalCenterY, 2) / Math.pow(b, 2)
        );
        boolean isWithinEllipse = ellipseValue <= ELLIPSE_TOLERANCE;
        
        // Debug logging
        Log.d(TAG, "updateFacePosition: ellipseValue=" + String.format("%.4f", ellipseValue) + 
              ", widthRatio=" + String.format("%.4f", widthRatio) + ", heightRatio=" + String.format("%.4f", heightRatio) + 
              ", xOffset=" + String.format("%.4f", xOffset) + ", yOffset=" + String.format("%.4f", yOffset) +
              ", isWithinEllipse=" + isWithinEllipse + ", isGoodSize=" + isGoodSize + ", isCentered=" + isCentered);
        
        // Determine guidance message - Simplified logic
        if (!isWithinEllipse) {
            if (faceCenterX < ovalCenterX - a * GUIDANCE_TOLERANCE) {
                positionGuidance = "Move face right";
            } else if (faceCenterX > ovalCenterX + a * GUIDANCE_TOLERANCE) {
                positionGuidance = "Move face left";
            } else if (faceCenterY < ovalCenterY - b * GUIDANCE_TOLERANCE) {
                positionGuidance = "Move face down";
            } else if (faceCenterY > ovalCenterY + b * GUIDANCE_TOLERANCE) {
                positionGuidance = "Move face up";
            } else {
                positionGuidance = "Position face within oval";
            }
        } else if (!isGoodSize) {
            if (widthRatio < MIN_FACE_OVAL_RATIO || heightRatio < MIN_FACE_OVAL_RATIO) {
                positionGuidance = "Move closer to camera";
            } else if (widthRatio > MAX_FACE_OVAL_RATIO || heightRatio > MAX_FACE_OVAL_RATIO) {
                positionGuidance = "Move away from camera";
            } else {
                positionGuidance = "Adjust face position";
            }
        } else {
            // If face is within ellipse and good size, consider it good enough
            positionGuidance = "Perfect position";
        }
        
        // Simplified good position check - if face is within ellipse and good size, it's good
        isGoodPosition = isWithinEllipse && isGoodSize;
        
        Log.d(TAG, "updateFacePosition: final result=" + isGoodPosition + ", guidance=" + positionGuidance);
        
        invalidate();
        return isGoodPosition;
    }
    
    /**
     * Check if face position is good
     */
    public boolean isGoodFacePosition() {
        return isGoodPosition;
    }
    
    /**
     * Update oval color based on current state
     */
    private void updateOvalColor() {
        switch (currentState) {
            case FACE_DETECTED:
                ovalPaint.setColor(Color.YELLOW);
                ovalPaint.setPathEffect(null);
                break;
            case FACE_REAL:
            case FACE_STABILIZING:
                ovalPaint.setColor(Color.parseColor("#4CAF50")); // Green
                ovalPaint.setPathEffect(null);
                break;
            case FACE_SPOOFED:
                ovalPaint.setColor(Color.RED);
                ovalPaint.setPathEffect(null);
                break;
            case ERROR:
                ovalPaint.setColor(Color.RED);
                ovalPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
                break;
            default:
                ovalPaint.setColor(Color.WHITE);
                ovalPaint.setPathEffect(null);
                break;
        }
    }
    
    /**
     * Start progress animation for face stabilization
     */
    public void startProgressAnimation(long durationMs) {
        stopProgressAnimation();
        
        progressAnimator = ValueAnimator.ofFloat(0f, 1f);
        progressAnimator.setDuration(durationMs);
        progressAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            progressValue = (float) animation.getAnimatedValue();
            invalidate();
        });
        progressAnimator.start();
    }
    
    /**
     * Stop progress animation
     */
    public void stopProgressAnimation() {
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
        }
    }
    
    /**
     * Start success animation
     */
    private void startSuccessAnimation() {
        stopSuccessAnimation();
        
        successAnimator = ValueAnimator.ofFloat(0f, 1f);
        successAnimator.setDuration(800);
        successAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        successAnimator.addUpdateListener(animation -> {
            successAnimValue = (float) animation.getAnimatedValue();
            invalidate();
        });
        successAnimator.start();
    }
    
    /**
     * Stop success animation
     */
    private void stopSuccessAnimation() {
        if (successAnimator != null && successAnimator.isRunning()) {
            successAnimator.cancel();
        }
        successAnimValue = 0f;
    }
    
    /**
     * Clear the overlay
     */
    public void clear() {
        stopProgressAnimation();
        stopSuccessAnimation();
        currentState = FaceProcessingState.READY;
        statusMessage = "";
        positionGuidance = "";
        progressValue = 0f;
        isGoodPosition = false;
        lastFaceRect = null;
        updateOvalColor();
        invalidate();
    }
    
    /**
     * Set oval color programmatically
     * @param color Color integer
     */
    public void setOvalColor(int color) {
        ovalPaint.setColor(color);
        // Reset path effect to ensure solid lines unless explicitly changed
        ovalPaint.setPathEffect(null);
        invalidate();
    }
    
    /**
     * Get the oval rect in absolute coordinates
     */
    public RectF getOvalRect() {
        return ovalRect;
    }
    
    /**
     * Get the last face rect
     */
    public android.graphics.Rect getLastFaceRect() {
        return lastFaceRect;
    }
    
    /**
     * Check if a face is properly positioned within the oval boundary
     * This can be called externally to validate face position
     * 
     * @param faceRect The face rectangle to check
     * @return true if face is properly positioned
     */
    public boolean validateFaceWithinOval(android.graphics.Rect faceRect) {
        if (faceRect == null || ovalRect == null) {
            return false;
        }
        
        // Calculate face center relative to oval center
        float faceCenterX = faceRect.exactCenterX();
        float faceCenterY = faceRect.exactCenterY();
        
        // Calculate ellipse parameters
        float centerX = ovalRect.centerX();
        float centerY = ovalRect.centerY();
        float a = ovalRect.width() / 2; // semi-major axis
        float b = ovalRect.height() / 2; // semi-minor axis
        
        // Ellipse equation: (x-h)²/a² + (y-k)²/b² ≤ 1
        float ellipseValue = (float) (
            Math.pow(faceCenterX - centerX, 2) / Math.pow(a, 2) +
            Math.pow(faceCenterY - centerY, 2) / Math.pow(b, 2)
        );
        
        // Check face size relative to oval
        float faceWidth = faceRect.width();
        float faceHeight = faceRect.height();
        float widthRatio = faceWidth / ovalRect.width();
        float heightRatio = faceHeight / ovalRect.height();
        
        boolean isWithinEllipse = ellipseValue <= ELLIPSE_TOLERANCE;
        boolean hasSuitableSize = 
            widthRatio >= MIN_FACE_OVAL_RATIO && 
            widthRatio <= MAX_FACE_OVAL_RATIO &&
            heightRatio >= MIN_FACE_OVAL_RATIO && 
            heightRatio <= MAX_FACE_OVAL_RATIO;
        
        return isWithinEllipse && hasSuitableSize;
    }
}
