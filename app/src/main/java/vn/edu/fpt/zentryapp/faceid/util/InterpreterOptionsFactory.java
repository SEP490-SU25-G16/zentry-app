package vn.edu.fpt.zentryapp.faceid.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.gpu.GpuDelegate;

import vn.edu.fpt.zentryapp.faceid.data.service.TFLiteGpuDelegateManager;

public final class InterpreterOptionsFactory {
    private static final String TAG = "InterpreterOptionsFactory";

    private static volatile boolean capabilityChecked = false;
    private static volatile boolean gpuUsable = false;
    private static volatile boolean nnapiUsable = false;

    private InterpreterOptionsFactory() {}

    public static synchronized void probe(Context context) {
        if (capabilityChecked) return;
        // GPU
        try {
            GpuDelegate probe = new GpuDelegate();
            probe.close();
            gpuUsable = true;
        } catch (Throwable t) {
            gpuUsable = false;
        }
        // NNAPI
        try {
            nnapiUsable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
        } catch (Throwable t) {
            nnapiUsable = false;
        }
        capabilityChecked = true;
        Log.d(TAG, "Capabilities - GPU:" + gpuUsable + ", NNAPI:" + nnapiUsable);
    }

    public static Interpreter.Options createCpuOptions() {
        Interpreter.Options opts = new Interpreter.Options();
        int threads = Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors()));
        opts.setNumThreads(threads);
        return opts;
    }

    public static Interpreter.Options createBestOptions(Context context) {
        probe(context);
        if (gpuUsable) {
            try {
                Interpreter.Options opts = new Interpreter.Options();
                GpuDelegate gpu = new GpuDelegate();
                opts.addDelegate(gpu);
                return opts;
            } catch (Throwable t) {
                Log.w(TAG, "GPU delegate creation failed, falling back", t);
            }
        }
        if (nnapiUsable) {
            try {
                Interpreter.Options opts = new Interpreter.Options();
                NnApiDelegate nnapi = new NnApiDelegate();
                opts.addDelegate(nnapi);
                return opts;
            } catch (Throwable t) {
                Log.w(TAG, "NNAPI delegate creation failed, falling back to CPU", t);
            }
        }
        return createCpuOptions();
    }
}
