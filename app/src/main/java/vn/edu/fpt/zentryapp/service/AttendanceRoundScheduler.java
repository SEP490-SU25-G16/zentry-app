package vn.edu.fpt.zentryapp.service;

import android.util.Log;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class AttendanceRoundScheduler {
    private static final String TAG = "AttendanceRoundScheduler";

    private final ScheduledExecutorService executor;
    private final List<AttendanceModels.AttendanceRound> rounds;
    private final Consumer<AttendanceModels.AttendanceRound> onRoundTrigger;
    private final Runnable onAllRoundsComplete;

    private ScheduledFuture<?> currentSchedule;
    private int currentRoundIndex = 0;

    public AttendanceRoundScheduler(List<AttendanceModels.AttendanceRound> rounds,
                                    Consumer<AttendanceModels.AttendanceRound> onRoundTrigger,
                                    Runnable onAllRoundsComplete) {
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.rounds = rounds;
        this.onRoundTrigger = onRoundTrigger;
        this.onAllRoundsComplete = onAllRoundsComplete;
    }

    public void start() {
        if (rounds.isEmpty()) {
            Log.w(TAG, "No rounds to schedule");
            return;
        }

        scheduleNextRound();
    }

    public void stop() {
        if (currentSchedule != null && !currentSchedule.isCancelled()) {
            currentSchedule.cancel(true);
        }
        executor.shutdown();
    }

    private void scheduleNextRound() {
        if (currentRoundIndex >= rounds.size()) {
            Log.d(TAG, "All rounds completed");
            onAllRoundsComplete.run();
            return;
        }

        AttendanceModels.AttendanceRound round = rounds.get(currentRoundIndex);
        long delay = calculateDelay(round.getTimestamp());

        Log.d(TAG, "Scheduling round " + round.getRoundNumber() + " in " + delay + " ms");

        currentSchedule = executor.schedule(() -> {
            Log.d(TAG, "Executing round " + round.getRoundNumber());
            onRoundTrigger.accept(round);

            currentRoundIndex++;

            if (round.isLastRound()) {
                Log.d(TAG, "Last round completed, stopping scheduler");
                onAllRoundsComplete.run();
            } else {
                scheduleNextRound();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private long calculateDelay(Date targetTime) {
        long currentTime = System.currentTimeMillis();
        long targetTimeMs = targetTime.getTime();
        return Math.max(0, targetTimeMs - currentTime);
    }

    public boolean isRunning() {
        return currentSchedule != null && !currentSchedule.isCancelled();
    }

    public int getCurrentRound() {
        return currentRoundIndex;
    }

    public int getTotalRounds() {
        return rounds.size();
    }
}
