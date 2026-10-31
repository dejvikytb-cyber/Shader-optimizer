package cz.fpsengine.core;

import cz.fpsengine.FPSengine;
import cz.fpsengine.config.EngineConfig;
import cz.fpsengine.performance.bottleneck.Bottleneck;
import cz.fpsengine.performance.bottleneck.BottleneckClassifier;
import cz.fpsengine.performance.monitor.FrameMonitor;
import cz.fpsengine.performance.monitor.FrameStats;
import cz.fpsengine.performance.monitor.JvmMonitor;
import cz.fpsengine.performance.monitor.JvmSample;

/** Central state. All hot-path calls happen on the Minecraft client thread. */
public final class EngineState {
    private static final long SAMPLE_INTERVAL_NS = 5_000_000_000L;

    private static EngineConfig config = new EngineConfig();
    private static final FrameMonitor FRAMES = new FrameMonitor();
    private static final JvmMonitor JVM = new JvmMonitor();
    private static final BottleneckClassifier CLASSIFIER = new BottleneckClassifier();

    private static volatile boolean healthy = true;
    private static long lastSample;
    private static JvmSample lastJvm = JvmSample.EMPTY;

    private EngineState() {}

    public static void init() {
        config = EngineConfig.load();
        lastSample = System.nanoTime();
    }

    public static boolean isEnabled() {
        return config.enabled && healthy;
    }

    public static boolean isHealthy() {
        return healthy;
    }

    public static void setEnabled(boolean value) {
        config.enabled = value;
        config.save();
        if (!value) {
            // Phase 1 modifies no game state; just drop collected data.
            FRAMES.reset();
            CLASSIFIER.reset();
        } else {
            lastSample = System.nanoTime();
        }
    }

    public static void disableAfterFailure() {
        healthy = false;
    }

    public static void onFrameStart() {
        if (!isEnabled()) return;
        try {
            FRAMES.frameStart();
        } catch (Throwable t) {
            fail(t);
        }
    }

    public static void onFrameEnd() {
        if (!isEnabled()) return;
        try {
            FRAMES.frameEnd();
            long now = System.nanoTime();
            if (now - lastSample >= SAMPLE_INTERVAL_NS) {
                lastSample = now;
                lastJvm = JVM.sample();
                CLASSIFIER.update(FRAMES.snapshot(), lastJvm, now);
            }
        } catch (Throwable t) {
            fail(t);
        }
    }

    private static void fail(Throwable t) {
        if (healthy) {
            FPSengine.LOGGER.error("FPS Engine hit an error and has deactivated itself", t);
            healthy = false;
        }
    }

    public static FrameStats frameStats() {
        return FRAMES.snapshot();
    }

    public static JvmSample jvm() {
        return lastJvm;
    }

    public static Bottleneck bottleneck() {
        return CLASSIFIER.current();
    }
}
