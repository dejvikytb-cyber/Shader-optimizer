package cz.fpsengine.system;

import cz.fpsengine.FPSengine;
import cz.fpsengine.util.ProcessRunner;
import org.lwjgl.opengl.GL11;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only hardware/OS probing. Windows queries run once on a background thread. */
public final class SystemInfo {
    public static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    public static final String POWER_SAVER = "a1841308-3541-4fab-bc81-f71556f20b4a";
    public static final String POWER_BALANCED = "381b4222-f694-41f0-9685-ff5bb260df2e";
    public static final String POWER_HIGH = "8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c";
    public static final String POWER_ULTIMATE = "e9a42b02-d5df-448d-aa00-03f14749eb61";
    private static final String GPU_PREF_KEY = "HKCU\\Software\\Microsoft\\DirectX\\UserGpuPreferences";

    public record Snapshot(boolean probed, String cpuName, String cpuVendor, List<String> gpus,
                           String powerPlanGuid, Boolean gameMode, Integer hags, boolean gpuPrefHigh) {
        static final Snapshot EMPTY = new Snapshot(false, "unknown", "unknown", List.of(), "", null, null, false);
    }

    private static volatile Snapshot snapshot = Snapshot.EMPTY;
    private static volatile String glRenderer = "unknown";
    private static volatile String glVendor = "unknown";

    private SystemInfo() {}

    public static Snapshot get() {
        return snapshot;
    }

    public static String glRenderer() {
        return glRenderer;
    }

    public static String glVendor() {
        return glVendor;
    }

    /** Must be called on the render thread with a current GL context. */
    public static void captureGl() {
        try {
            String r = GL11.glGetString(GL11.GL_RENDERER);
            String v = GL11.glGetString(GL11.GL_VENDOR);
            if (r != null) glRenderer = r;
            if (v != null) glVendor = v;
            FPSengine.LOGGER.info("OpenGL renderer: {} ({})", glRenderer, glVendor);
        } catch (Throwable t) {
            FPSengine.LOGGER.warn("Could not read GL renderer string", t);
        }
    }

    public static void probeAsync() {
        Thread t = new Thread(SystemInfo::probe, "FPSEngine-probe");
        t.setDaemon(true);
        t.start();
    }

    public static Path javaExecutable() {
        return ProcessHandle.current().info().command().map(Path::of).orElse(null);
    }

    private static void probe() {
        try {
            String ident = System.getenv("PROCESSOR_IDENTIFIER");
            String vendor = "unknown";
            if (ident != null) {
                String u = ident.toUpperCase(Locale.ROOT);
                if (u.contains("AUTHENTICAMD")) vendor = "AMD";
                else if (u.contains("GENUINEINTEL")) vendor = "Intel";
            }
            String cpuName = ident != null ? ident : "unknown";
            List<String> gpus = new ArrayList<>();
            String plan = "";
            Integer gameMode = null;
            Integer hags = null;
            boolean gpuPref = false;

            if (WINDOWS) {
                List<String> ps = ProcessRunner.run(15, "powershell", "-NoProfile", "-NonInteractive", "-Command",
                        "$c=(Get-CimInstance Win32_Processor | Select-Object -First 1).Name; "
                                + "$g=((Get-CimInstance Win32_VideoController).Name -join '|'); "
                                + "Write-Output $c; Write-Output $g");
                if (ps.size() >= 1 && !ps.get(0).isBlank()) cpuName = ps.get(0);
                if (ps.size() >= 2) for (String g : ps.get(1).split("\\|")) if (!g.isBlank()) gpus.add(g.trim());

                for (String l : ProcessRunner.run(5, "powercfg", "/getactivescheme")) {
                    Matcher m = Pattern.compile("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}").matcher(l);
                    if (m.find()) plan = m.group().toLowerCase(Locale.ROOT);
                }
                gameMode = regDword("HKCU\\Software\\Microsoft\\GameBar", "AutoGameModeEnabled");
                Integer g = regDword("HKLM\\SYSTEM\\CurrentControlSet\\Control\\GraphicsDrivers", "HwSchMode");
                hags = g;
                Path java = javaExecutable();
                if (java != null) {
                    for (String l : ProcessRunner.run(5, "reg", "query", GPU_PREF_KEY, "/v", java.toString())) {
                        if (l.contains("GpuPreference=2")) gpuPref = true;
                    }
                }
            }
            snapshot = new Snapshot(true, cpuName, vendor, List.copyOf(gpus), plan,
                    gameMode == null ? null : gameMode != 0, hags, gpuPref);
            FPSengine.LOGGER.info("System probe: cpu={} ({}), gpus={}, powerPlan={}", cpuName, vendor, gpus, plan);
        } catch (Throwable t) {
            FPSengine.LOGGER.warn("System probe failed", t);
        }
    }

    private static Integer regDword(String key, String value) {
        for (String l : ProcessRunner.run(5, "reg", "query", key, "/v", value)) {
            if (l.contains(value)) {
                String[] parts = l.trim().split("\\s+");
                String last = parts[parts.length - 1];
                try {
                    if (last.startsWith("0x")) return Integer.parseInt(last.substring(2), 16);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    public static String gpuPreferenceKey() {
        return GPU_PREF_KEY;
    }
}
