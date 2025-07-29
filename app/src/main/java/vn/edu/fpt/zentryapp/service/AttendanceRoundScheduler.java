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
    private static final long CALCULATE_DELAY_MS = 30 * 1000; // 30 seconds after round ends

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
            Log.e(TAG, "❌ FATAL: Rounds list is NULL");
            throw new IllegalArgumentException("Rounds list cannot be null");
        }

        if (executionCallback == null) {
            Log.e(TAG, "❌ FATAL: Execution callback is NULL");
            throw new IllegalArgumentException("Execution callback cannot be null");
        }

        this.rounds = rounds;
        this.executionCallback = executionCallback;
        this.calculateCallback = calculateCallback;
        this.completionCallback = completionCallback;
        this.mainHandler = new Handler(Looper.getMainLooper());

        Log.d(TAG, "📋 SCHEDULER CONFIGURATION:");
        Log.d(TAG, "  Total rounds: " + rounds.size());
        Log.d(TAG, "  Calculate delay: " + CALCULATE_DELAY_MS + "ms (" + (CALCULATE_DELAY_MS / 1000) + "s)");
        Log.d(TAG, "  Execution callback: " + (executionCallback != null ? "Set" : "NULL"));
        Log.d(TAG, "  Calculate callback: " + (calculateCallback != null ? "Set" : "NULL"));
        Log.d(TAG, "  Completion callback: " + (completionCallback != null ? "Set" : "NULL"));
        Log.d(TAG, "  Main handler: " + (mainHandler != null ? "Ready" : "NULL"));

        // Log round details
        logRoundDetails();

        Log.d(TAG, "AttendanceRoundScheduler initialized successfully");
        Log.d(TAG, "================================================");
    }

    private void logRoundDetails() {
        Log.d(TAG, "📅 ROUNDS SCHEDULE:");

        if (rounds.isEmpty()) {
            Log.w(TAG, "  No rounds to schedule");
            return;
        }

        long currentTime = System.currentTimeMillis();
        Date now = new Date(currentTime);

        Log.d(TAG, "  Current time: " + timeFormat.format(now));

        for (int i = 0; i < rounds.size(); i++) {
            AttendanceModels.AttendanceRound round = rounds.get(i);

            if (round == null) {
                Log.w(TAG, "  Round[" + i + "]: NULL");
                continue;
            }

            Date executionTime = round.getExecutionTime();
            long delay = executionTime.getTime() - currentTime;
            long calculateDelay = delay + CALCULATE_DELAY_MS;

            Log.d(TAG, "  Round[" + i + "]:");
            Log.d(TAG, "    Number: " + round.getRoundNumber());
            Log.d(TAG, "    Round ID: " + round.getRoundId());
            Log.d(TAG, "    Execution time: " + timeFormat.format(executionTime));
            Log.d(TAG, "    Execution delay: " + delay + "ms (" + formatDuration(delay) + ")");
            Log.d(TAG, "    Calculate delay: " + calculateDelay + "ms (" + formatDuration(calculateDelay) + ")");
            Log.d(TAG, "    Is last round: " + round.isLastRound());
            Log.d(TAG, "    Time status: " + (delay > 0 ? "FUTURE" : "PAST"));

            if (delay <= 0) {
                Log.w(TAG, "    ⚠️ Round execution time has already passed!");
            }
        }
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
        Log.d(TAG, "=== STARTING ATTENDANCE ROUND SCHEDULER ===");
        Log.d(TAG, "Start timestamp: " + timeFormat.format(new Date()));

        if (isRunning) {
            Log.w(TAG, "⚠️ Scheduler is already running, ignoring start request");
            return;
        }

        if (rounds.isEmpty()) {
            Log.w(TAG, "⚠️ No rounds to schedule, completing immediately");
            if (completionCallback != null) {
                completionCallback.run();
            }
            return;
        }

        Log.d(TAG, "🚀 Creating scheduler thread pool...");
        scheduler = Executors.newScheduledThreadPool(2);
        isRunning = true;

        Log.d(TAG, "✅ Scheduler thread pool created successfully");
        Log.d(TAG, "📋 Scheduling " + rounds.size() + " rounds...");

        // Schedule all rounds
        scheduleAllRounds();

        // Schedule completion
        scheduleCompletion();

        Log.d(TAG, "✅ All rounds and completion scheduled successfully");
        Log.d(TAG, "Scheduler is now RUNNING");
        Log.d(TAG, "=============================================");
    }

    private void scheduleAllRounds() {
        Log.d(TAG, "📅 SCHEDULING ALL ROUNDS:");

        for (int i = 0; i < rounds.size(); i++) {
            AttendanceModels.AttendanceRound round = rounds.get(i);

            Log.d(TAG, "🔄 Processing round " + (i + 1) + "/" + rounds.size());
            Log.d(TAG, "  Round number: " + round.getRoundNumber());
            Log.d(TAG, "  Round ID: " + round.getRoundId());

            // Schedule execution
            scheduleRoundExecution(round);

            // Schedule calculation
            scheduleRoundCalculation(round);

            Log.d(TAG, "✅ Round " + round.getRoundNumber() + " scheduled successfully");
        }

        Log.d(TAG, "All rounds scheduled");
    }

    private void scheduleRoundExecution(AttendanceModels.AttendanceRound round) {
        Log.d(TAG, "⏰ SCHEDULING ROUND EXECUTION:");
        Log.d(TAG, "  Round: " + round.getRoundNumber());

        long currentTime = System.currentTimeMillis();
        long executionTime = round.getExecutionTime().getTime();
        long delay = executionTime - currentTime;

        Log.d(TAG, "  Current time: " + timeFormat.format(new Date(currentTime)));
        Log.d(TAG, "  Execution time: " + timeFormat.format(round.getExecutionTime()));
        Log.d(TAG, "  Delay: " + delay + "ms (" + formatDuration(delay) + ")");

        if (delay <= 0) {
            Log.w(TAG, "  ⚠️ Round " + round.getRoundNumber() + " execution time has passed");
            Log.w(TAG, "  Executing immediately...");
            executeRound(round);
        } else {
            Log.d(TAG, "  📅 Scheduling execution in " + delay + "ms");

            try {
                scheduler.schedule(() -> executeRound(round), delay, TimeUnit.MILLISECONDS);
                Log.d(TAG, "  ✅ Round " + round.getRoundNumber() + " execution scheduled successfully");
            } catch (Exception e) {
                Log.e(TAG, "  ❌ Failed to schedule round " + round.getRoundNumber() + " execution", e);
            }
        }
    }

    private void scheduleRoundCalculation(AttendanceModels.AttendanceRound round) {
        Log.d(TAG, "🧮 SCHEDULING ROUND CALCULATION:");
        Log.d(TAG, "  Round: " + round.getRoundNumber());

        if (calculateCallback == null) {
            Log.w(TAG, "  ⚠️ Calculate callback is NULL, skipping calculation scheduling");
            return;
        }

        long currentTime = System.currentTimeMillis();
        long executionTime = round.getExecutionTime().getTime();
        long calculateTime = executionTime + CALCULATE_DELAY_MS;
        long calculateDelay = calculateTime - currentTime;

        Log.d(TAG, "  Current time: " + timeFormat.format(new Date(currentTime)));
        Log.d(TAG, "  Execution time: " + timeFormat.format(round.getExecutionTime()));
        Log.d(TAG, "  Calculate time: " + timeFormat.format(new Date(calculateTime)));
        Log.d(TAG, "  Calculate delay: " + calculateDelay + "ms (" + formatDuration(calculateDelay) + ")");

        if (calculateDelay <= CALCULATE_DELAY_MS) {
            Log.w(TAG, "  ⚠️ Round " + round.getRoundNumber() + " calculate time has passed or too close");
            Log.w(TAG, "  Skipping calculation scheduling");
            return;
        }

        Log.d(TAG, "  📅 Scheduling calculation in " + calculateDelay + "ms");

        try {
            scheduler.schedule(() -> calculateRound(round), calculateDelay, TimeUnit.MILLISECONDS);
            Log.d(TAG, "  ✅ Round " + round.getRoundNumber() + " calculation scheduled successfully");
        } catch (Exception e) {
            Log.e(TAG, "  ❌ Failed to schedule round " + round.getRoundNumber() + " calculation", e);
        }
    }

    private void scheduleCompletion() {
        Log.d(TAG, "🏁 SCHEDULING COMPLETION:");

        if (rounds.isEmpty()) {
            Log.w(TAG, "  No rounds, completion not needed");
            return;
        }

        if (completionCallback == null) {
            Log.w(TAG, "  ⚠️ Completion callback is NULL, skipping completion scheduling");
            return;
        }

        AttendanceModels.AttendanceRound lastRound = rounds.get(rounds.size() - 1);
        long currentTime = System.currentTimeMillis();
        long lastRoundTime = lastRound.getExecutionTime().getTime();
        long completionTime = lastRoundTime + CALCULATE_DELAY_MS + 5000; // 5s after last calculate
        long completionDelay = completionTime - currentTime;

        Log.d(TAG, "  Last round: " + lastRound.getRoundNumber());
        Log.d(TAG, "  Last round time: " + timeFormat.format(lastRound.getExecutionTime()));
        Log.d(TAG, "  Completion time: " + timeFormat.format(new Date(completionTime)));
        Log.d(TAG, "  Completion delay: " + completionDelay + "ms (" + formatDuration(completionDelay) + ")");

        if (completionDelay <= 0) {
            Log.w(TAG, "  ⚠️ Completion time has passed, scheduling for immediate execution");
            completionDelay = 1000; // 1 second delay
        }

        try {
            scheduler.schedule(() -> {
                mainHandler.post(() -> {
                    Log.d(TAG, "🏁 EXECUTING COMPLETION CALLBACK");
                    Log.d(TAG, "  Completion time: " + timeFormat.format(new Date()));

                    if (completionCallback != null) {
                        completionCallback.run();
                    }

                    Log.d(TAG, "✅ Completion callback executed");
                });
            }, Math.max(0, completionDelay), TimeUnit.MILLISECONDS);

            Log.d(TAG, "✅ Completion scheduled successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to schedule completion", e);
        }
    }

    private void executeRound(AttendanceModels.AttendanceRound round) {
        Log.d(TAG, "🚀 EXECUTING ROUND " + round.getRoundNumber());
        Log.d(TAG, "  Execution timestamp: " + timeFormat.format(new Date()));
        Log.d(TAG, "  Round ID: " + round.getRoundId());
        Log.d(TAG, "  Scheduled time: " + timeFormat.format(round.getExecutionTime()));
        Log.d(TAG, "  Is last round: " + round.isLastRound());

        // Calculate timing accuracy
        long currentTime = System.currentTimeMillis();
        long scheduledTime = round.getExecutionTime().getTime();
        long timingDiff = currentTime - scheduledTime;

        Log.d(TAG, "  Timing accuracy: " + timingDiff + "ms " +
                (timingDiff > 0 ? "LATE" : "EARLY"));

        if (Math.abs(timingDiff) > 5000) { // More than 5 seconds off
            Log.w(TAG, "  ⚠️ Significant timing deviation: " + formatDuration(Math.abs(timingDiff)));
        }

        mainHandler.post(() -> {
            Log.d(TAG, "📱 Posting execution to main handler");

            try {
                if (executionCallback != null) {
                    Log.d(TAG, "  Calling execution callback...");
                    executionCallback.onRoundExecute(round);
                    Log.d(TAG, "  ✅ Execution callback completed");
                } else {
                    Log.e(TAG, "  ❌ Execution callback is NULL!");
                }
            } catch (Exception e) {
                Log.e(TAG, "  ❌ Exception in execution callback", e);
            }
        });

        Log.d(TAG, "Round " + round.getRoundNumber() + " execution initiated");
    }

    private void calculateRound(AttendanceModels.AttendanceRound round) {
        Log.d(TAG, "🧮 CALCULATING ROUND " + round.getRoundNumber());
        Log.d(TAG, "  Calculation timestamp: " + timeFormat.format(new Date()));
        Log.d(TAG, "  Round ID: " + round.getRoundId());
        Log.d(TAG, "  Original execution time: " + timeFormat.format(round.getExecutionTime()));

        // Calculate timing from execution
        long currentTime = System.currentTimeMillis();
        long executionTime = round.getExecutionTime().getTime();
        long actualDelay = currentTime - executionTime;

        Log.d(TAG, "  Time since execution: " + actualDelay + "ms (" + formatDuration(actualDelay) + ")");
        Log.d(TAG, "  Expected delay: " + CALCULATE_DELAY_MS + "ms (" + (CALCULATE_DELAY_MS / 1000) + "s)");

        long delayDiff = actualDelay - CALCULATE_DELAY_MS;
        Log.d(TAG, "  Timing accuracy: " + delayDiff + "ms " +
                (delayDiff > 0 ? "LATE" : "EARLY"));

        mainHandler.post(() -> {
            Log.d(TAG, "📱 Posting calculation to main handler");

            try {
                if (calculateCallback != null) {
                    Log.d(TAG, "  Calling calculate callback...");
                    calculateCallback.onRoundCalculate(round);
                    Log.d(TAG, "  ✅ Calculate callback completed");
                } else {
                    Log.w(TAG, "  ⚠️ Calculate callback is NULL, skipping");
                }
            } catch (Exception e) {
                Log.e(TAG, "  ❌ Exception in calculate callback", e);
            }
        });

        Log.d(TAG, "Round " + round.getRoundNumber() + " calculation initiated");
    }

    public void stop() {
        Log.d(TAG, "=== STOPPING ATTENDANCE ROUND SCHEDULER ===");
        Log.d(TAG, "Stop timestamp: " + timeFormat.format(new Date()));
        Log.d(TAG, "Current running status: " + isRunning);

        if (!isRunning) {
            Log.w(TAG, "⚠️ Scheduler is not running, nothing to stop");
            return;
        }

        if (scheduler != null) {
            Log.d(TAG, "🛑 Shutting down scheduler...");
            Log.d(TAG, "  Scheduler status: " + (scheduler.isShutdown() ? "SHUTDOWN" : "RUNNING"));
            Log.d(TAG, "  Scheduler terminated: " + (scheduler.isTerminated() ? "YES" : "NO"));

            try {
                scheduler.shutdownNow();
                Log.d(TAG, "  ✅ Scheduler shutdown initiated");

                // Wait for termination with timeout
                boolean terminated = scheduler.awaitTermination(5, TimeUnit.SECONDS);
                Log.d(TAG, "  Scheduler terminated within 5s: " + terminated);

                if (!terminated) {
                    Log.w(TAG, "  ⚠️ Scheduler did not terminate gracefully within timeout");
                }

            } catch (InterruptedException e) {
                Log.e(TAG, "  ❌ Interrupted while waiting for scheduler termination", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.e(TAG, "  ❌ Exception during scheduler shutdown", e);
            }
        } else {
            Log.w(TAG, "  Scheduler is NULL, nothing to shutdown");
        }

        isRunning = false;
        Log.d(TAG, "✅ Scheduler stopped successfully");
        Log.d(TAG, "New running status: " + isRunning);
        Log.d(TAG, "==========================================");
    }

    public boolean isRunning() {
        boolean running = isRunning && scheduler != null && !scheduler.isShutdown();
        Log.v(TAG, "Scheduler running check: " + running +
                " (isRunning=" + isRunning +
                ", scheduler=" + (scheduler != null ? "exists" : "null") +
                ", shutdown=" + (scheduler != null ? scheduler.isShutdown() : "N/A") + ")");
        return running;
    }
}
