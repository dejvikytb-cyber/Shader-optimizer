package cz.fpsengine.performance.monitor;

/** Immutable snapshot. All values are MEASURED from frame timestamps. */
public record FrameStats(int frames, double avgFps, double low1PercentFps,
                         double medianMs, double p99Ms, double stutterRatio,
                         double avgTickWorkMs) {
    public static final FrameStats EMPTY = new FrameStats(0, 0, 0, 0, 0, 0, 0);
}
