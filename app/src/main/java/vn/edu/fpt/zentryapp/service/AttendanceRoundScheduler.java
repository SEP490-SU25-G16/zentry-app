package vn.edu.fpt.zentryapp.service;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AttendanceRoundScheduler {
    private static final String TAG = "AttendanceRoundScheduler";
    private static final long SCAN_DELAY_MS = 20 * 1000; // 20 seconds after execution time
    private static final long CALCULATE_DELAY_MS = 40 * 1000; // 40 seconds after scan starts
    private static final long MAX_LATE_TOLERANCE_MS = 0; // 1 phút tolerance
    private final List<AttendanceModels.AttendanceRound> rounds;
    private final RoundExecutionCallback executionCallback;
    private final RoundCalculateCallback calculateCallback;
    private final Runnable completionCallback;
    private ScheduledExecutorService scheduler;
    private Handler mainHandler;
    private boolean isRunning = false;
    private final SimpleDateFormat timeFormat;

    public interface RoundExecutionCallback {
        void onRoundExecute(AttendanceModels.AttendanceRound round);
    }

    public interface RoundCalculateCallback {
        void onRoundCalculate(AttendanceModels.AttendanceRound round);
    }

    public AttendanceRoundScheduler(List<AttendanceModels.AttendanceRound> rounds,
                                    RoundExecutionCallback executionCallback,
                                    RoundCalculateCallback calculateCallback,
                                    Runnable completionCallback) {
        Log.d(TAG, "=== INITIALIZING ATTENDANCE ROUND SCHEDULER ===");

        this.timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

        // Validate inputs
        if (rounds == null) {
            throw new IllegalArgumentException("Rounds list cannot be null");
        }
        if (executionCallback == null) {
            throw new IllegalArgumentException("Execution callback cannot be null");
        }

        this.rounds = rounds;
        this.executionCallback = executionCallback;
        this.calculateCallback = calculateCallback;
        this.completionCallback = completionCallback;
        this.mainHandler = new Handler(Looper.getMainLooper());

        Log.d(TAG, "Scheduler initialized with " + rounds.size() + " rounds");
        logRoundDetails();
    }

    private void logRoundDetails() {
        if (rounds.isEmpty()) {
            Log.w(TAG, "No rounds to schedule");
            return;
        }

        long currentTime = System.currentTimeMillis();
        Log.d(TAG, "Current time: " + timeFormat.format(new Date(currentTime)));
        Log.d(TAG, "Late tolerance: " + formatDuration(MAX_LATE_TOLERANCE_MS));

        int validRounds = 0;
        int skippedRounds = 0;

        for (int i = 0; i < rounds.size(); i++) {
            AttendanceModels.AttendanceRound round = rounds.get(i);
            if (round == null) continue;

            // Tính toán timeline
            long executionTime = round.getExecutionTime().getTime();
            long scanTime = executionTime + SCAN_DELAY_MS;
            long calculateTime = scanTime + CALCULATE_DELAY_MS;

            long scanDelay = scanTime - currentTime;
            long calculateDelay = calculateTime - currentTime;

            // Check skip status
            boolean willSkipScan = false;
            boolean willSkipCalculate = false;

            if (scanDelay <= 0 && Math.abs(scanDelay) > MAX_LATE_TOLERANCE_MS) {
                willSkipScan = true;
                skippedRounds++;
            } else {
                validRounds++;
            }

            if (calculateDelay <= 0 && Math.abs(calculateDelay) > MAX_LATE_TOLERANCE_MS) {
                willSkipCalculate = true;
            }

            Log.d(TAG, "Round[" + i + "] #" + round.getRoundNumber() + ":");
            Log.d(TAG, "  Execution: " + timeFormat.format(round.getExecutionTime()));
            Log.d(TAG, "  Scan: " + timeFormat.format(new Date(scanTime)) +
                    " (delay: " + formatDuration(scanDelay) + ")" +
                    (willSkipScan ? " ❌ WILL SKIP" : " ✅ WILL EXECUTE"));
            Log.d(TAG, "  Calculate: " + timeFormat.format(new Date(calculateTime)) +
                    " (delay: " + formatDuration(calculateDelay) + ")" +
                    (willSkipCalculate ? " ❌ WILL SKIP" : " ✅ WILL EXECUTE"));
        }

        Log.d(TAG, "📊 Summary: " + validRounds + " valid rounds, " + skippedRounds + " will be skipped");
    }


    private String formatDuration(long milliseconds) {
        if (milliseconds < 0) {
            return "PAST (" + formatDuration(-milliseconds) + " ago)";
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    public void start() {
        Log.d(TAG, "=== STARTING SCHEDULER ===");

        if (isRunning) {
            Log.w(TAG, "Scheduler already running");
            return;
        }

        if (rounds.isEmpty()) {
            Log.w(TAG, "No rounds to schedule, completing immediately");
            if (completionCallback != null) {
                completionCallback.run();
            }
            return;
        }

        scheduler = Executors.newScheduledThreadPool(2);
        isRunning = true;

        scheduleAllRounds();
        scheduleCompletion();

        Log.d(TAG, "Scheduler started with " + rounds.size() + " rounds");
    }

    private void scheduleAllRounds() {
        for (AttendanceModels.AttendanceRound round : rounds) {
            scheduleRoundExecution(round);
            scheduleRoundCalculation(round);
        }
    }

    private void scheduleRoundExecution(AttendanceModels.AttendanceRound round) {
        long currentTime = System.currentTimeMillis();
        // Round time đã là UTC+7 từ mapping layer
        long executionTime = round.getExecutionTime().getTime();
        long scanTime = executionTime + SCAN_DELAY_MS; // +10s sau execution time
        long delay = scanTime - currentTime;

        Log.d(TAG, "Scheduling round " + round.getRoundNumber() +
                " scan in " + formatDuration(delay));

        if (delay <= 0) {
            // ✅ KIỂM TRA: Round đã quá thời gian tolerance chưa?
            long timePassed = Math.abs(delay); // Thời gian đã qua kể từ scan time

            if (timePassed > MAX_LATE_TOLERANCE_MS) {
                // ❌ Quá tolerance time -> SKIP round này
                Log.w(TAG, "⏰ Round " + round.getRoundNumber() +
                        " đã qua " + formatDuration(timePassed) +
                        " (tolerance: " + formatDuration(MAX_LATE_TOLERANCE_MS) +
                        "), bỏ qua round này");
                return; // Không execute round này
            }

            // ✅ Vẫn trong tolerance time -> execute ngay
            Log.w(TAG, "⚡ Round " + round.getRoundNumber() +
                    " đã qua " + formatDuration(timePassed) +
                    " nhưng vẫn trong tolerance (" + formatDuration(MAX_LATE_TOLERANCE_MS) +
                    "), thực hiện ngay lập tức");
            executeRound(round);
        } else {
            // ✅ Round chưa tới -> schedule bình thường
            Log.d(TAG, "✅ Round " + round.getRoundNumber() +
                    " sẽ thực hiện sau " + formatDuration(delay));
            scheduler.schedule(() -> executeRound(round), delay, TimeUnit.MILLISECONDS);
        }
    }



    private void scheduleRoundCalculation(AttendanceModels.AttendanceRound round) {
        if (calculateCallback == null) {
            Log.d(TAG, "Calculate callback null, bỏ qua calculate cho round " + round.getRoundNumber());
            return;
        }

        long currentTime = System.currentTimeMillis();
        long executionTime = round.getExecutionTime().getTime();
        long scanTime = executionTime + SCAN_DELAY_MS;
        long calculateTime = scanTime + CALCULATE_DELAY_MS; // +30s sau scan
        long delay = calculateTime - currentTime;

        Log.d(TAG, "Scheduling calculate for round " + round.getRoundNumber() +
                " in " + formatDuration(delay));

        if (delay <= 0) {
            // ✅ KIỂM TRA: Calculate time đã quá tolerance chưa?
            long timePassed = Math.abs(delay);

            if (timePassed > MAX_LATE_TOLERANCE_MS) {
                // ❌ Quá tolerance time -> SKIP calculate
                Log.w(TAG, "⏰ Calculate time cho round " + round.getRoundNumber() +
                        " đã qua " + formatDuration(timePassed) +
                        " (tolerance: " + formatDuration(MAX_LATE_TOLERANCE_MS) +
                        "), bỏ qua calculate");
                return;
            }

            // ✅ Vẫn trong tolerance time -> calculate ngay
            Log.w(TAG, "⚡ Calculate time cho round " + round.getRoundNumber() +
                    " đã qua " + formatDuration(timePassed) +
                    " nhưng vẫn trong tolerance, thực hiện ngay lập tức");
            calculateRound(round);
        } else {
            // ✅ Calculate time chưa tới -> schedule bình thường
            Log.d(TAG, "✅ Calculate cho round " + round.getRoundNumber() +
                    " sẽ thực hiện sau " + formatDuration(delay));
            scheduler.schedule(() -> calculateRound(round), delay, TimeUnit.MILLISECONDS);
        }
    }


    private void scheduleCompletion() {
        if (rounds.isEmpty() || completionCallback == null) return;

        AttendanceModels.AttendanceRound lastRound = rounds.get(rounds.size() - 1);
        long currentTime = System.currentTimeMillis();

        long lastExecutionTime = lastRound.getExecutionTime().getTime();
        long lastScanTime = lastExecutionTime + SCAN_DELAY_MS;
        long lastCalculateTime = lastScanTime + CALCULATE_DELAY_MS;
        long completionTime = lastCalculateTime + 5000; // +5s sau calculate cuối
        long delay = completionTime - currentTime;

        scheduler.schedule(() -> {
            mainHandler.post(() -> {
                Log.d(TAG, "Executing completion callback");
                if (completionCallback != null) {
                    completionCallback.run();
                }
            });
        }, Math.max(1000, delay), TimeUnit.MILLISECONDS);
    }

    private void executeRound(AttendanceModels.AttendanceRound round) {
        Log.d(TAG, "🚀 EXECUTING ROUND " + round.getRoundNumber());

        mainHandler.post(() -> {
            try {
                executionCallback.onRoundExecute(round);
                Log.d(TAG, "✅ Round " + round.getRoundNumber() + " executed");
            } catch (Exception e) {
                Log.e(TAG, "❌ Exception in execution callback", e);
            }
        });
    }

    private void calculateRound(AttendanceModels.AttendanceRound round) {
        Log.d(TAG, "🧮 CALCULATING ROUND " + round.getRoundNumber());

        mainHandler.post(() -> {
            try {
                if (calculateCallback != null) {
                    calculateCallback.onRoundCalculate(round);
                    Log.d(TAG, "✅ Round " + round.getRoundNumber() + " calculated");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Exception in calculate callback", e);
            }
        });
    }

    public void stop() {
        Log.d(TAG, "=== STOPPING SCHEDULER ===");

        if (!isRunning) return;

        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        isRunning = false;
        Log.d(TAG, "✅ Scheduler stopped");
    }
}
