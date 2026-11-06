package cz.fpsengine.core;

import cz.fpsengine.compat.CompatReport;
import cz.fpsengine.compat.IrisBridge;
import cz.fpsengine.system.SystemInfo;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Turns measured system facts into concrete recommendations. It only advises; it changes nothing. */
public final class Advisor {
    private Advisor() {}

    public static List<String> lines() {
        List<String> out = new ArrayList<>();
        SystemInfo.Snapshot s = SystemInfo.get();
        String gl = SystemInfo.glRenderer().toLowerCase(Locale.ROOT);
        boolean nvidiaPresent = s.gpus().stream().anyMatch(g -> g.toLowerCase(Locale.ROOT).contains("nvidia")) || gl.contains("nvidia");

        if (!s.probed() && SystemInfo.WINDOWS) out.add("System scan still running, try again in a few seconds.");

        if (SystemInfo.WINDOWS && s.probed()) {
            if (nvidiaPresent && !gl.equals("unknown") && !gl.contains("nvidia")) {
                out.add("IMPORTANT: Minecraft renders on \"" + SystemInfo.glRenderer() + "\" although an NVIDIA GPU exists. Run /fpsengine gpu, restart the game.");
            } else if (s.gpus().size() > 1 && !s.gpuPrefHigh()) {
                out.add("Several GPUs detected. Run /fpsengine gpu so Windows always uses the high-performance GPU for Minecraft.");
            }
            if (SystemInfo.POWER_SAVER.equals(s.powerPlanGuid())) {
                out.add("Windows power plan is Power saver: switch to Balanced or High performance.");
            }
            if (Boolean.FALSE.equals(s.gameMode())) {
                out.add("Windows Game Mode is off: enable it in Settings > Gaming > Game Mode.");
            }
        }

        if (nvidiaPresent) {
            out.add("NVIDIA Control Panel > Manage 3D settings > Program Settings > javaw.exe: Power management = Prefer maximum performance, Threaded optimization = On, Shader Cache Size = Unlimited.");
        }
        if ("AMD".equals(s.cpuVendor())) {
            out.add("AMD CPU: keep Windows Game Mode on and the AMD chipset driver current (X3D chips rely on it to pick the V-Cache cores).");
        } else if ("Intel".equals(s.cpuVendor())) {
            out.add("Intel CPU: on 12th gen and newer (hybrid), keep Game Mode on and Windows power mode on Best performance so game threads stay on P-cores.");
        }

        long maxMb = Runtime.getRuntime().maxMemory() >> 20;
        long totalMb = totalRamMb();
        boolean iris = IrisBridge.installed();
        if (iris && maxMb < 4000) {
            out.add("Only " + maxMb + " MiB of heap is allocated. With shaders, 4-8 GiB is typical; raise it in your launcher (-Xmx).");
        } else if (totalMb > 0 && maxMb > totalMb * 0.6) {
            out.add("Heap (" + maxMb + " MiB) is over 60% of system RAM (" + totalMb + " MiB). Lower it to avoid swapping.");
        }
        for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
            String n = b.getName();
            if (n.equals("Copy") || n.startsWith("PS ")) {
                out.add("JVM uses the " + n + " collector. Use G1 (-XX:+UseG1GC) for smoother frametimes.");
                break;
            }
        }

        if (!CompatReport.isLoaded("sodium")) out.add("Sodium is not installed; it is the largest single FPS gain for rendering.");
        if (iris) {
            Boolean pack = IrisBridge.shaderPackInUse();
            out.add(pack == null ? "Iris installed (shader state unknown)." : pack ? "Iris shader pack active."
                    : "Iris installed, no shader pack active.");
            out.add("Shader FPS is dominated by GPU cost: shadow distance/resolution and the pack's quality profile are the biggest levers. FPS Engine never changes them.");
        }
        if (!CompatReport.isLoaded("immediatelyfast") && !CompatReport.isLoaded("entityculling")) {
            out.add("Optional, benchmark yourself: ImmediatelyFast (GUI/entity batching) and Entity Culling can lower CPU cost in busy scenes.");
        }
        return out;
    }

    private static long totalRamMb() {
        try {
            var os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean x) return x.getTotalMemorySize() >> 20;
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
