package vn.edu.fpt.zentryapp.faceid.util;

import android.graphics.RectF;
import android.util.Log;

import java.util.concurrent.atomic.AtomicReference;


/**
 * Maps rectangles/points between PreviewView (view space) and analyzer Bitmap (bitmap space).
 * Assumes PreviewView uses a center-crop like behavior (FILL_CENTER): scale = max(viewW/bitmapW, viewH/bitmapH).
 * Supports horizontal mirroring for front camera preview.
 */
public final class CoordinateMapper {
    private static final String TAG = "CoordinateMapper";
    private static final CoordinateMapper INSTANCE = new CoordinateMapper();

    public static CoordinateMapper getInstance() { return INSTANCE; }

    public static final class Mapping {
        public final int viewWidth;
        public final int viewHeight;
        public final int bitmapWidth;
        public final int bitmapHeight;
        public final boolean mirrorX; // true for front camera mirrored preview

        public Mapping(int viewWidth, int viewHeight, int bitmapWidth, int bitmapHeight, boolean mirrorX) {
            this.viewWidth = Math.max(1, viewWidth);
            this.viewHeight = Math.max(1, viewHeight);
            this.bitmapWidth = Math.max(1, bitmapWidth);
            this.bitmapHeight = Math.max(1, bitmapHeight);
            this.mirrorX = mirrorX;
        }
    }

    private final AtomicReference<Mapping> current = new AtomicReference<>();

    private CoordinateMapper() {}

    public void updateMapping(int viewWidth, int viewHeight, int bitmapWidth, int bitmapHeight, boolean mirrorX) {
        current.set(new Mapping(viewWidth, viewHeight, bitmapWidth, bitmapHeight, mirrorX));
            Log.d(TAG, "Updated mapping: view=" + viewWidth + "x" + viewHeight +
                    ", bmp=" + bitmapWidth + "x" + bitmapHeight + ", mirrorX=" + mirrorX);

    }

    /**
     * Standardized updater that derives effective mirroring from preview and bitmap states.
     * Use this to ensure consistent mapping across flows (front camera mirrored preview, optional
     * bitmap mirroring in analyzer).
     *
     * @param viewWidth         width of the PreviewView in pixels
     * @param viewHeight        height of the PreviewView in pixels
     * @param bitmapWidth       width of the analyzer Bitmap in pixels
     * @param bitmapHeight      height of the analyzer Bitmap in pixels
     * @param isPreviewMirrored whether the PreviewView is mirrored horizontally (front camera)
     * @param isBitmapMirrored  whether the analyzer Bitmap has been horizontally mirrored already
     */
    public void updateMappingWithPolicy(int viewWidth, int viewHeight, int bitmapWidth, int bitmapHeight,
                                        boolean isPreviewMirrored, boolean isBitmapMirrored) {
        boolean effectiveMirrorX = isPreviewMirrored ^ isBitmapMirrored;
        updateMapping(viewWidth, viewHeight, bitmapWidth, bitmapHeight, effectiveMirrorX);

            Log.d(TAG, "updateMappingWithPolicy: previewMirrored=" + isPreviewMirrored +
                    ", bitmapMirrored=" + isBitmapMirrored + ", effectiveMirrorX=" + effectiveMirrorX);

    }

    public Mapping getMapping() { return current.get(); }

    /**
     * Map a rectangle from view coordinates to bitmap coordinates using the current mapping.
     * Returns null if mapping not available.
     */
    public RectF mapViewRectToBitmap(RectF viewRect) {
        Mapping m = current.get();
        if (m == null || viewRect == null) return null;

        float scale = Math.max((float) m.viewWidth / (float) m.bitmapWidth,
                               (float) m.viewHeight / (float) m.bitmapHeight);
        float displayW = m.bitmapWidth * scale;
        float displayH = m.bitmapHeight * scale;
        float offsetX = (m.viewWidth - displayW) * 0.5f;
        float offsetY = (m.viewHeight - displayH) * 0.5f;

        // Map each edge by converting to display space then to bitmap space
        float left = viewRect.left - offsetX;
        float right = viewRect.right - offsetX;
        if (m.mirrorX) {
            // Mirror around display center
            left = displayW - left;
            right = displayW - right;
            // swap after mirror if needed
            float tmp = left; left = right; right = tmp;
        }

        float top = viewRect.top - offsetY;
        float bottom = viewRect.bottom - offsetY;

        // Convert to bitmap coordinates
        left /= scale;
        right /= scale;
        top /= scale;
        bottom /= scale;

        // Normalize and clamp
        float l = Math.min(left, right);
        float r = Math.max(left, right);
        float t = Math.min(top, bottom);
        float b = Math.max(top, bottom);

        l = Math.max(0f, Math.min(l, m.bitmapWidth));
        r = Math.max(0f, Math.min(r, m.bitmapWidth));
        t = Math.max(0f, Math.min(t, m.bitmapHeight));
        b = Math.max(0f, Math.min(b, m.bitmapHeight));

        return new RectF(l, t, r, b);
    }

    /**
     * Map a rectangle from bitmap coordinates to view coordinates using the current mapping.
     * Returns null if mapping not available.
     */
    public RectF mapBitmapRectToView(RectF bitmapRect) {
        Mapping m = current.get();
        if (m == null || bitmapRect == null) return null;

        float scale = Math.max((float) m.viewWidth / (float) m.bitmapWidth,
                               (float) m.viewHeight / (float) m.bitmapHeight);
        float displayW = m.bitmapWidth * scale;
        float displayH = m.bitmapHeight * scale;
        float offsetX = (m.viewWidth - displayW) * 0.5f;
        float offsetY = (m.viewHeight - displayH) * 0.5f;

        // Scale to display space
        float left = bitmapRect.left * scale;
        float right = bitmapRect.right * scale;
        float top = bitmapRect.top * scale;
        float bottom = bitmapRect.bottom * scale;

        if (m.mirrorX) {
            // Mirror around display center
            left = displayW - left;
            right = displayW - right;
            float tmp = left; left = right; right = tmp;
        }

        // Translate into view space
        left += offsetX;
        right += offsetX;
        top += offsetY;
        bottom += offsetY;

        // Normalize and clamp
        float l = Math.min(left, right);
        float r = Math.max(left, right);
        float t = Math.min(top, bottom);
        float b = Math.max(top, bottom);

        l = Math.max(0f, Math.min(l, m.viewWidth));
        r = Math.max(0f, Math.min(r, m.viewWidth));
        t = Math.max(0f, Math.min(t, m.viewHeight));
        b = Math.max(0f, Math.min(b, m.viewHeight));

        return new RectF(l, t, r, b);
    }
}

