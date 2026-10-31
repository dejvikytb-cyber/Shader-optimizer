package cz.fpsengine.performance.bottleneck;

import cz.fpsengine.FPSengine;
import cz.fpsengine.performance.monitor.FrameStats;
import cz.fpsengine.performance.monitor.JvmSample;

/**
 * Classifies only what can be measured reliably without GPU timer queries:
 * memory pressure and frame pacing. A CPU-vs-GPU split is deliberately NOT guessed.
 * Uses separate enter/exit thresholds (hysteresis), a confirmation streak and a cooldown.
 */
public final class BottleneckClassifier {
    private static final int REQUIRED_STREAK = 3;
    private static final long COOLDOWN_NS = 30_000_000_000L;

    private Bottleneck current = Bottleneck.UNDETERMINED;
    private Bottleneck pending = Bottleneck.UNDETERMINED;
    private int streak;
    private long lastChange = Long.MIN_VALUE / 2;

    public Bottleneck current() {
        return current;
    }

    public void reset() {
        current = Bottleneck.UNDETERMINED;
        pending = Bottleneck.UNDETERMINED;
        streak = 0;
    }

    public void update(FrameStats s, JvmSample j, long nowNs) {
        if (s.frames() < 120) return; // insufficient data: keep current state
        Bottleneck candidate = evaluate(s, j);
        if (candidate == current) {
            pending = current;
            streak = 0;
            return;
        }
        if (candidate == pending) streak++;
        else {
            pending = candidate;
            streak = 1;
        }
        if (streak >= REQUIRED_STREAK && nowNs - lastChange >= COOLDOWN_NS) {
            FPSengine.LOGGER.info("Bottleneck state: {} -> {}", current.label, candidate.label);
            current = candidate;
            lastChange = nowNs;
            pending = candidate;
            streak = 0;
        }
    }

    private Bottleneck evaluate(FrameStats s, JvmSample j) {
        boolean wasMem = current == Bottleneck.MEMORY_PRESSURE;
        boolean wasPacing = current == Bottleneck.FRAME_PACING;

        double heapLimit = wasMem ? 0.80 : 0.90;
        double gcLimit = wasMem ? 0.02 : 0.05;
        if (j.heapFraction() > heapLimit || j.gcTimeFraction() > gcLimit) return Bottleneck.MEMORY_PRESSURE;

        double stutterLimit = wasPacing ? 0.015 : 0.03;
        double ratioLimit = wasPacing ? 2.0 : 2.5;
        double ratio = s.medianMs() > 0 ? s.p99Ms() / s.medianMs() : 0;
        if (s.stutterRatio() > stutterLimit || ratio > ratioLimit) return Bottleneck.FRAME_PACING;

        return Bottleneck.UNDETERMINED;
    }
}
