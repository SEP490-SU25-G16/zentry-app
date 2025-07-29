package vn.edu.fpt.zentryapp.service;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AttendanceRoundScheduler {
    private static final String TAG = "AttendanceRoundScheduler";
    private static final long CALCULATE_DELAY_MS = 30 * 1000; // 30 seconds after round ends

    private final List<AttendanceModels.AttendanceRound> rounds;
    private final RoundExecutionCallback executionCallback;
    private final RoundCalculateCallback calculateCallback; // 🔧 THÊM callback cho calculate
    private final Runnable completionCallback;

    private ScheduledExecutorService scheduler;
    private Handler mainHandler;
    private boolean isRunning = false;

    public interface RoundExecutionCallback {
        void onRoundExecute(AttendanceModels.AttendanceRound round);
    }

    // 🔧 THÊM interface cho calculate callback
    public interface RoundCalculateCallback {
        void onRoundCalculate(AttendanceModels.AttendanceRound round);
    }

    // 🔧 CẬP NHẬT constructor để nhận calculate callback
    public AttendanceRoundScheduler(List<AttendanceModels.AttendanceRound> rounds,
                                    RoundExecutionCallback executionCallback,
                                    RoundCalculateCallback calculateCallback,
                                    Runnable completionCallback) {
        this.rounds = rounds;
        this.executionCallback = executionCallback;
        this.calculateCallback = calculateCallback; // 🔧 THÊM
        this.completionCallback = completionCallback;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void start() {
        if (isRunning) {
            Log.w(TAG, "Scheduler is already running");
            return;
        }

        scheduler = Executors.newScheduledThreadPool(2); // 🔧 TĂNG pool size cho calculate tasks
        isRunning = true;

        Log.d(TAG, "Starting scheduler with " + rounds.size() + " rounds");

        for (int i = 0; i < rounds.size(); i++) {
            AttendanceModels.AttendanceRound round = rounds.get(i);

            // Schedule execution (submit attendance)
            scheduleRoundExecution(round);

            // 🔧 SCHEDULE calculate attendance (30s after round ends)
            scheduleRoundCalculation(round);
        }

        // Schedule completion after last round
        if (!rounds.isEmpty()) {
            AttendanceModels.AttendanceRound lastRound = rounds.get(rounds.size() - 1);
            long completionDelay = lastRound.getExecutionTime().getTime() - System.currentTimeMillis() +
                    CALCULATE_DELAY_MS + 5000; // 5s after last calculate

            scheduler.schedule(() -> {
                mainHandler.post(() -> {
                    if (completionCallback != null) {
                        completionCallback.run();
                    }
                });
            }, Math.max(0, completionDelay), TimeUnit.MILLISECONDS);
        }
    }

    private void scheduleRoundExecution(AttendanceModels.AttendanceRound round) {
        long delay = round.getExecutionTime().getTime() - System.currentTimeMillis();

        if (delay <= 0) {
            Log.w(TAG, "Round " + round.getRoundNumber() + " execution time has passed, executing immediately");
            executeRound(round);
        } else {
            scheduler.schedule(() -> executeRound(round), delay, TimeUnit.MILLISECONDS);
            Log.d(TAG, "Scheduled round " + round.getRoundNumber() + " execution in " + delay + "ms");
        }
    }

    // 🔧 THÊM method để schedule calculate
    private void scheduleRoundCalculation(AttendanceModels.AttendanceRound round) {
        // Calculate 30s after round execution time
        long calculateDelay = round.getExecutionTime().getTime() - System.currentTimeMillis() + CALCULATE_DELAY_MS;

        if (calculateDelay <= CALCULATE_DELAY_MS) {
            Log.w(TAG, "Round " + round.getRoundNumber() + " calculate time has passed");
            return;
        }

        scheduler.schedule(() -> calculateRound(round), calculateDelay, TimeUnit.MILLISECONDS);
        Log.d(TAG, "Scheduled round " + round.getRoundNumber() + " calculation in " + calculateDelay + "ms");
    }

    private void executeRound(AttendanceModels.AttendanceRound round) {
        mainHandler.post(() -> {
            Log.d(TAG, "Executing round " + round.getRoundNumber());
            if (executionCallback != null) {
                executionCallback.onRoundExecute(round);
            }
        });
    }

    // 🔧 THÊM method để calculate round
    private void calculateRound(AttendanceModels.AttendanceRound round) {
        mainHandler.post(() -> {
            Log.d(TAG, "Calculating round " + round.getRoundNumber() + " (30s after execution)");
            if (calculateCallback != null) {
                calculateCallback.onRoundCalculate(round);
            }
        });
    }

    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        isRunning = false;
        Log.d(TAG, "Scheduler stopped");
    }

    public boolean isRunning() {
        return isRunning;
    }
}
