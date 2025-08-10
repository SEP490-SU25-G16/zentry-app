package vn.edu.fpt.zentryapp.faceid.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class SharedExecutors {
    private static final int CPU_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors());
    private static final int CORE_POOL_SIZE = Math.min(2, CPU_COUNT); // modest parallelism for mobile
    private static final int MAX_POOL_SIZE = Math.min(4, CPU_COUNT);
    private static final long KEEP_ALIVE_SECONDS = 30L;

    private static final BlockingQueue<Runnable> ML_QUEUE = new LinkedBlockingQueue<>(64); // bounded to avoid OOM

    private static final ThreadFactory NAMED_FACTORY = r -> {
        Thread t = new Thread(r);
        t.setName("faceid-ml-exec");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    };

    private static final ThreadPoolExecutor ML_EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            ML_QUEUE,
            NAMED_FACTORY,
            new ThreadPoolExecutor.DiscardOldestPolicy() // drop oldest on pressure
    );

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private SharedExecutors() {}

    public static ExecutorService getMlExecutor() {
        return ML_EXECUTOR;
    }

    public static Handler getMainHandler() {
        return MAIN_HANDLER;
    }
}
