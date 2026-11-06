package cz.fpsengine.system;

import cz.fpsengine.FPSengine;
import cz.fpsengine.util.ProcessRunner;

import java.nio.file.Path;
import java.util.function.Consumer;

/** Windows-only, process-scoped or per-user changes. Everything here is opt-in via settings or command. */
public final class WindowsTweaks {
    private WindowsTweaks() {}

    /** AboveNormal priority for this game process only. Reverts automatically when the game exits. */
    public static void setPriority(boolean boost) {
        if (!SystemInfo.WINDOWS) return;
        String cls = boost ? "AboveNormal" : "Normal";
        Thread t = new Thread(() -> {
            int code = ProcessRunner.exitCode(10, "powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "(Get-Process -Id " + ProcessHandle.current().pid() + ").PriorityClass = '" + cls + "'");
            FPSengine.LOGGER.info("Process priority -> {} (exit {})", cls, code);
        }, "FPSEngine-priority");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Sets Windows' per-app "High performance" GPU preference for this Java executable
     * (same as Settings > System > Display > Graphics). Writes only under HKCU. Needs a game restart.
     */
    public static void applyGpuPreferenceAsync(Consumer<String> done) {
        if (!SystemInfo.WINDOWS) {
            done.accept("Only available on Windows.");
            return;
        }
        Path java = SystemInfo.javaExecutable();
        if (java == null) {
            done.accept("Could not determine the Java executable path.");
            return;
        }
        Thread t = new Thread(() -> {
            int code = ProcessRunner.exitCode(10, "reg", "add", SystemInfo.gpuPreferenceKey(),
                    "/v", java.toString(), "/t", "REG_SZ", "/d", "GpuPreference=2;", "/f");
            if (code == 0) {
                FPSengine.LOGGER.info("GPU preference set to High performance for {}", java);
                done.accept("High-performance GPU set for " + java.getFileName() + ". Restart Minecraft for it to apply.");
            } else {
                done.accept("Failed to write the GPU preference (exit " + code + "). Set it manually in Windows Settings > Display > Graphics.");
            }
        }, "FPSEngine-gpupref");
        t.setDaemon(true);
        t.start();
    }
}
