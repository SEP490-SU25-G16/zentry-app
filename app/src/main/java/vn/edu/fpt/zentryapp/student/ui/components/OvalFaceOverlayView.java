package vn.edu.fpt.zentryapp.student.ui.components;

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

import vn.edu.fpt.zentryapp.student.data.service.FaceProcessingState;

/**
 * Custom overlay view for face detection with oval guide and progress ring
 */
public class OvalFaceOverlayView extends View {
    private static final String TAG = "OvalFaceOverlayView";
    
    // Constants for oval dimensions
    private static final float OVAL_WIDTH_RATIO = 0.65f;
    private static final float OVAL_HEIGHT_RATIO = 0.8f;
    
    // Paint objects
    private final Paint overlayPaint;
    private final Paint ovalPaint;
    private final Paint progressPaint;
    private final Paint textPaint;
    private final Paint successPaint;
    
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
        
        // Draw status message
        if (statusMessage != null && !statusMessage.isEmpty()) {
            canvas.drawText(statusMessage, getWidth() / 2f, 
                    ovalRect.bottom + 80, textPaint);
        }
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
        progressValue = 0f;
        updateOvalColor();
        invalidate();
    }
    
    /**
     * Set oval color programmatically
     * @param color Color integer
     */
    public void setOvalColor(int color) {
        ovalPaint.setColor(color);
        invalidate();
    }
    
    /**
     * Get the oval rect in absolute coordinates
     */
    public RectF getOvalRect() {
        return ovalRect;
    }
} 