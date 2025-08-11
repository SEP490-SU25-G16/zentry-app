package vn.edu.fpt.zentryapp.faceid.data.service;

/**
 * Lightweight runtime configuration for context-aware anti-spoofing fusion.
 */
public class AntiSpoofConfig {
    public float edgeWeight = 0.15f;
    public float lineWeight = 0.10f;
    public float bezelWeight = 0.25f;
    public float contextWeight = 1.0f;
    public float faceRatioTrigger = 0.15f;
    public float marginTrigger = 0.05f; // fraction of frame
    public float threshold = 0.60f;     // final spoof threshold
    public int   contextWidth = 320;    // keep aspect ratio
    public int   contextStride = 3;     // compute context every N frames
    public boolean debugLogs = true;
}


