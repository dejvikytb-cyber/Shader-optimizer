package cz.fpsengine.core;

import cz.fpsengine.FPSengine;
import cz.fpsengine.config.EngineConfig;
import cz.fpsengine.performance.bottleneck.Bottleneck;
import cz.fpsengine.performance.bottleneck.BottleneckClassifier;
import cz.fpsengine.performance.monitor.FrameMonitor;
import cz.fpsengine.performance.monitor.FrameStats;
import cz.fpsengine.performance.monitor.JvmMonitor;
import cz.fpsengine.performance.monitor.JvmSample;
import cz.fpsengine.system.SystemInfo;
import cz.fpsengine.system.WindowsTweaks;
import org.lwjgl.glfw.GLFW;

/** Central state. Hot-path calls happen on the Minecraft client thread. */
public final class EngineState {
    private static final long SAMPLE_INTERVAL_NS = 5_000_000_000L;

    private static EngineConfig config = new EngineConfig();
    private static final FrameMonitor FRAMES = new FrameMonitor();
    private static final JvmMonitor JVM = new JvmMonitor();
    private static final BottleneckClassifier CLASSIFIER = new BottleneckClassifier();

    private static volatile boolean healthy = true;
    private static boolean glProbed;
    private static long lastSample;
    private static JvmSample lastJvm = JvmSample.EMPTY;

    private EngineState() {}

    public static void init() {
        config = EngineConfig.load();
        lastSample = System.nanoTime();
        SystemInfo.probeAsync();
        if (isEnabled() && config.boostProcessPriority) WindowsTweaks.setPriority(true);
    }

    public static EngineConfig config() {
        return config;
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
            FRAMES.reset();
            CLASSIFIER.reset();
            WindowsTweaks.setPriority(false); // restore
        } else {
            lastSample = System.nanoTime();
            if (config.boostProcessPriority) WindowsTweaks.setPriority(true);
        }
    }

    public static void setBoostPriority(boolean value) {
        config.boostProcessPriority = value;
        config.save();
        if (isEnabled()) WindowsTweaks.setPriority(value);
    }

    public static void setThrottleUnfocused(boolean value) {
        config.throttleWhenUnfocused = value;
        config.save();
    }

    public static void setUnfocusedFps(int fps) {
        config.unfocusedFps = Math.max(5, Math.min(60, fps));
        config.save();
    }

    public static void disableAfterFailure() {
        healthy = false;
    }

    private static boolean windowUnfocused() {
        long w = GLFW.glfwGetCurrentContext();
        return w != 0L && GLFW.glfwGetWindowAttrib(w, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE;
    }

    public static void onFrameStart() {
        if (!isEnabled()) return;
        try {
            if (!glProbed) {
                glProbed = true;
                SystemInfo.captureGl();
            }
            if (config.throttleWhenUnfocused && windowUnfocused()) {
                FRAMES.pause();
                try {
                    Thread.sleep(Math.max(1, 1000 / Math.max(5, config.unfocusedFps)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
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
            WindowsTweaks.setPriority(false);
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
