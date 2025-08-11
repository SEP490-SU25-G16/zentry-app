package vn.edu.fpt.zentryapp.faceid.data.service;

import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

/**
 * Singleton manager for TensorFlow Lite GPU Delegate
 * to avoid creating multiple instances and improve performance
 */
public class TFLiteGpuDelegateManager {
    private static final String TAG = "TFLiteGpuDelegateManager";
    
    private static TFLiteGpuDelegateManager instance;
    private GpuDelegate gpuDelegate;
    private boolean gpuAvailable = false;
    
    private TFLiteGpuDelegateManager() {
        try {
            gpuDelegate = new GpuDelegate();
            gpuAvailable = true;
            Log.d(TAG, "GPU Delegate created successfully");
        } catch (Exception e) {
            Log.d(TAG, "GPU not available: " + e.getMessage());
            gpuAvailable = false;
        }
    }
    
    public static synchronized TFLiteGpuDelegateManager getInstance() {
        if (instance == null) {
            instance = new TFLiteGpuDelegateManager();
        }
        return instance;
    }
    
    public Interpreter.Options getInterpreterOptions() {
        Interpreter.Options options = new Interpreter.Options();
        
        if (gpuAvailable && gpuDelegate != null) {
            options.addDelegate(gpuDelegate);
        } else {
            options.setNumThreads(4); // Fallback to CPU
        }
        
        return options;
    }
    
    public void close() {
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
    }
} 