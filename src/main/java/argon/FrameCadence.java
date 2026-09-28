package argon;

import java.util.concurrent.locks.LockSupport;

/** Hybrid frame cadence. Unlimited caps (260 and above, or non-positive) stay vanilla. */
public final class FrameCadence {
    private static final long ONE_MS = 1_000_000L;
    private static final long ONE_SECOND = 1_000_000_000L;

    private static long last = System.nanoTime();
    private static int lastLimit = -1;
    private static long cachedInterval = 0;

    private FrameCadence() {
    }

    public static boolean shouldPace(int fps) {
        return fps > 0 && fps < 260;
    }

    public static void limit(int fps) {
        if (!shouldPace(fps)) {
            last = System.nanoTime();
            lastLimit = fps;
            return;
        }

        long now = System.nanoTime();

        // Recalculate frame interval only when FPS setting changes
        if (fps != lastLimit) {
            cachedInterval = ONE_SECOND / fps;
            last = now;
            lastLimit = fps;
        }

        long target = last + cachedInterval;

        // Catch up if falling behind by more than 1 frame
        if (target < now - cachedInterval) {
            target = now;
        }

        long remaining = target - now;

        // Sleep for the bulk of remaining time
        if (remaining > ONE_MS) {
            LockSupport.parkNanos(remaining - ONE_MS);
        }

        // Busy-spin for high-precision end timing
        while (System.nanoTime() < target) {
            Thread.onSpinWait();
        }

        last = target; // Use target instead of nanoTime() to prevent frame drift
    }
}