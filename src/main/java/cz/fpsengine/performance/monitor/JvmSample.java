package cz.fpsengine.performance.monitor;

public record JvmSample(long heapUsedBytes, long heapMaxBytes, double gcTimeFraction) {
    public static final JvmSample EMPTY = new JvmSample(0, 0, 0);

    public double heapFraction() {
        return heapMaxBytes > 0 ? (double) heapUsedBytes / heapMaxBytes : 0;
    }
}
