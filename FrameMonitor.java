package cz.fpsengine.performance.monitor;

import java.util.Arrays;

/**
 * Records frame-to-frame intervals in a fixed ring buffer (no allocation per frame).
 * Called only from the Minecraft client thread.
 */
public final class FrameMonitor {
    private static final int CAPACITY = 1200;
    private final long[] intervals = new long[CAPACITY];
    private int size;
    private int next;
    private long lastStart;
    private long workStart;
    private double workAvgNs;

    public void frameStart() {
        long now = System.nanoTime();
        if (lastStart != 0) {
            long dt = now - lastStart;
            // ignore pauses/loading hitches > 1 s so they do not poison the statistics
            if (dt < 1_000_000_000L) {
                intervals[next] = dt;
                next = (next + 1) % CAPACITY;
                if (size < CAPACITY) size++;
            }
        }
        lastStart = now;
        workStart = now;
    }

    /** Time spent inside Minecraft.runTick. Includes buffer swap, so it cannot separate CPU from GPU. */
    public void frameEnd() {
        long dt = System.nanoTime() - workStart;
        workAvgNs = workAvgNs == 0 ? dt : workAvgNs * 0.98 + dt * 0.02;
    }

    /** Used while the render loop is deliberately throttled so those frames do not pollute stats. */
    public void pause() {
        lastStart = 0;
        workStart = System.nanoTime();
    }

    public void reset() {
        size = 0;
        next = 0;
        lastStart = 0;
        workAvgNs = 0;
    }

    public FrameStats snapshot() {
        int n = size;
        if (n < 30) return FrameStats.EMPTY;
        long[] a = Arrays.copyOf(intervals, n);
        Arrays.sort(a);
        double sum = 0;
        for (long v : a) sum += v;
        double mean = sum / n;
        double median = a[n / 2];
        double p99 = a[Math.min(n - 1, (int) (n * 0.99))];
        int worst = Math.max(1, (int) Math.ceil(n * 0.01));
        double worstSum = 0;
        for (int i = n - worst; i < n; i++) worstSum += a[i];
        double low1 = 1e9 / (worstSum / worst);
        int stutters = 0;
        for (long v : a) if (v > 2 * median) stutters++;
        return new FrameStats(n, 1e9 / mean, low1, median / 1e6, p99 / 1e6,
                stutters / (double) n, workAvgNs / 1e6);
    }
}
