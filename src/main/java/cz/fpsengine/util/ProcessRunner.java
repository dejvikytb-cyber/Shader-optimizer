package cz.fpsengine.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs a short external command (no shell) and returns its output lines. Never call from the render thread. */
public final class ProcessRunner {
    private ProcessRunner() {}

    public static List<String> run(int timeoutSeconds, String... command) {
        List<String> lines = new ArrayList<>();
        Process p = null;
        try {
            p = new ProcessBuilder(command).redirectErrorStream(true).start();
            p.getOutputStream().close();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = r.readLine()) != null) lines.add(line.trim());
            }
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) p.destroyForcibly();
        } catch (Exception e) {
            if (p != null) p.destroyForcibly();
        }
        return lines;
    }

    public static int exitCode(int timeoutSeconds, String... command) {
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            p.getOutputStream().close();
            p.getInputStream().readAllBytes();
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return -1;
            }
            return p.exitValue();
        } catch (Exception e) {
            return -1;
        }
    }
}
