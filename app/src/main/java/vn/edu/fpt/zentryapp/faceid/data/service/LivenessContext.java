package vn.edu.fpt.zentryapp.faceid.data.service;

import java.util.concurrent.atomic.AtomicBoolean;

public final class LivenessContext {
    private volatile long lastLivenessAtMs = 0L;
    private final AtomicBoolean gazeCentered = new AtomicBoolean(false);

    public void markLivenessNow() {
        lastLivenessAtMs = System.currentTimeMillis();
    }

    public boolean livenessVerifiedRecently() {
        long now = System.currentTimeMillis();
        return (now - lastLivenessAtMs) <= FaceIdConfig.LIVENESS_BOOST_WINDOW_MS;
    }

    public void setGazeCentered(boolean centered) {
        gazeCentered.set(centered);
    }

    public boolean straightGaze() {
        return gazeCentered.get();
    }
}