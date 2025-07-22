package vn.edu.fpt.zentryapp.student.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Overlay view for displaying face detection results
 */
public class FaceOverlayView extends View {
    private static final String TAG = "FaceOverlayView";
    
    private Rect faceRect;
    private boolean isSpoofed;
    private String message;
    
    private final Paint facePaint;
    private final Paint textPaint;
    private final Paint backgroundPaint;
    
    public FaceOverlayView(Context context) {
        this(context, null);
    }
    
    public FaceOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public FaceOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        
        // Initialize paints
        facePaint = new Paint();
        facePaint.setStyle(Paint.Style.STROKE);
        facePaint.setStrokeWidth(5);
        facePaint.setColor(Color.GREEN);
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40);
        textPaint.setAntiAlias(true);
        
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#80000000"));
        backgroundPaint.setStyle(Paint.Style.FILL);
    }
    
    /**
     * Set face detection result
     * @param faceRect Bounding box of detected face
     * @param isSpoofed Whether the face is spoofed
     * @param message Message to display
     */
    public void setFaceDetectionResult(Rect faceRect, boolean isSpoofed, String message) {
        this.faceRect = faceRect;
        this.isSpoofed = isSpoofed;
        this.message = message;
        
        // Update face paint color based on spoof detection
        if (isSpoofed) {
            facePaint.setColor(Color.RED);
        } else {
            facePaint.setColor(Color.GREEN);
        }
        
        // Request redraw
        invalidate();
    }
    
    /**
     * Clear the overlay
     */
    public void clear() {
        faceRect = null;
        message = null;
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (faceRect != null) {
            // Draw face bounding box
            canvas.drawRect(faceRect, facePaint);
            
            // Draw message if available
            if (message != null && !message.isEmpty()) {
                // Calculate text position
                float textX = faceRect.left;
                float textY = faceRect.bottom + 50;
                
                // Draw text background
                Rect textBounds = new Rect();
                textPaint.getTextBounds(message, 0, message.length(), textBounds);
                canvas.drawRect(
                        textX - 10,
                        textY - textBounds.height() - 10,
                        textX + textBounds.width() + 10,
                        textY + 10,
                        backgroundPaint);
                
                // Draw text
                canvas.drawText(message, textX, textY, textPaint);
            }
        }
    }
} 