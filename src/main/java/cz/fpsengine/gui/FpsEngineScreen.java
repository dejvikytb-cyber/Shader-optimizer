package cz.fpsengine.gui;

import cz.fpsengine.core.Advisor;
import cz.fpsengine.core.EngineState;
import cz.fpsengine.performance.monitor.FrameStats;
import cz.fpsengine.performance.monitor.JvmSample;
import cz.fpsengine.system.SystemInfo;
import cz.fpsengine.system.WindowsTweaks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Black/white settings screen with animated "liquid" visuals (sine waves, bubbles, a cursor-following glow,
 * stretchy toggle knobs and click ripples). Everything is drawn with GuiGraphics.fill, and clicks are handled by
 * invisible vanilla buttons, so no input-handling APIs are overridden.
 */
public final class FpsEngineScreen extends Screen {
    private static final int[] FPS_STEPS = {10, 20, 30, 60};
    private static final int BG = 0xFF050505, PANEL = 0xF2111111, CARD = 0xFF0B0B0B, TRACK_OFF = 0xFF2A2A2A;
    private static final int WHITE = 0xFFFFFFFF, DIM = 0xFF8A8A8A, BLACK = 0xFF000000;
    private static final long T0 = System.nanoTime();

    private final Screen parent;

    // layout
    private int px, py, pw, ph;
    private boolean showAdvice;
    private final int[] rowY = new int[4];
    private int gpuY, bottomY, statsY, adviceY;

    // hit areas for hover (x, y, w, h)
    private final int[][] hit = new int[7][4];
    private final float[] anim = new float[3];
    private final float[] hover = new float[7];

    private final List<double[]> ripples = new ArrayList<>(); // x, y, startSeconds
    private double glowX = -100, glowY = -100;
    private int lastMouseX, lastMouseY;
    private long lastNs = System.nanoTime();

    private volatile String actionMessage = "";
    private List<String> advice = List.of();
    private FrameStats stats = FrameStats.EMPTY;
    private long cacheNs;

    public FpsEngineScreen(Screen parent) {
        super(Component.literal("FPS Engine"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pw = Math.min(320, this.width - 16);
        showAdvice = this.height >= 320;
        ph = showAdvice ? 306 : 272;
        px = (this.width - pw) / 2;
        py = Math.max(4, (this.height - ph) / 2);

        int y = py + 44;
        for (int i = 0; i < 4; i++) rowY[i] = y + i * 24;
        gpuY = py + 148;
        bottomY = py + 174;
        statsY = py + 204;
        adviceY = py + 274;

        var cfg = EngineState.config();
        anim[0] = cfg.enabled ? 1f : 0f;
        anim[1] = cfg.boostProcessPriority ? 1f : 0f;
        anim[2] = cfg.throttleWhenUnfocused ? 1f : 0f;

        int rx = px + 10, rw = pw - 20;
        hit[0] = new int[]{rx, rowY[0], rw, 22};
        hit[1] = new int[]{rx, rowY[1], rw, 22};
        hit[2] = new int[]{rx, rowY[2], rw, 22};
        hit[3] = new int[]{rx, rowY[3], rw, 22};
        hit[4] = new int[]{rx, gpuY, rw, 22};
        int half = (rw - 6) / 2;
        hit[5] = new int[]{rx, bottomY, half, 22};
        hit[6] = new int[]{rx + half + 6, bottomY, half, 22};

        addInvisible(0, b -> EngineState.setEnabled(!EngineState.config().enabled));
        addInvisible(1, b -> EngineState.setBoostPriority(!EngineState.config().boostProcessPriority));
        addInvisible(2, b -> EngineState.setThrottleUnfocused(!EngineState.config().throttleWhenUnfocused));
        addInvisible(3, b -> {
            int cur = EngineState.config().unfocusedFps;
            int next = FPS_STEPS[0];
            for (int i = 0; i < FPS_STEPS.length; i++) if (FPS_STEPS[i] == cur) next = FPS_STEPS[(i + 1) % FPS_STEPS.length];
            EngineState.setUnfocusedFps(next);
        });
        Button gpu = addInvisible(4, b -> WindowsTweaks.applyGpuPreferenceAsync(m -> actionMessage = m));
        gpu.active = SystemInfo.WINDOWS;
        addInvisible(5, b -> {
            SystemInfo.probeAsync();
            actionMessage = "Scanning system...";
        });
        addInvisible(6, b -> onClose());
    }

    private Button addInvisible(int i, Button.OnPress press) {
        int[] h = hit[i];
        return addRenderableWidget(Button.builder(Component.empty(), b -> {
            ripples.add(new double[]{lastMouseX, lastMouseY, seconds()});
            press.onPress(b);
        }).bounds(h[0], h[1], h[2], h[3]).build());
    }

    private static double seconds() {
        return (System.nanoTime() - T0) / 1e9;
    }

    // ---------------------------------------------------------------- drawing helpers

    private static int alpha(int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | 0xFFFFFF;
    }

    private static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
        g.fill(x, y + r, x + w, y + h - r, color);
    }

    private static void circle(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    private boolean over(int i, int mx, int my) {
        int[] h = hit[i];
        return mx >= h[0] && mx < h[0] + h[2] && my >= h[1] && my < h[1] + h[3];
    }

    private void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(this.font, s, x, y, color);
    }

    private void textRight(GuiGraphics g, String s, int xRight, int y, int color) {
        g.drawString(this.font, s, xRight - this.font.width(s), y, color);
    }

    private void textCenter(GuiGraphics g, String s, int cx, int y, int color) {
        g.drawString(this.font, s, cx - this.font.width(s) / 2, y, color);
    }

    private static String trim(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    // ---------------------------------------------------------------- liquid background

    private void liquid(GuiGraphics g, double t) {
        // cursor-following soft glow
        int gx = (int) glowX, gy = (int) glowY;
        circle(g, gx, gy, 70, alpha(6));
        circle(g, gx, gy, 50, alpha(8));
        circle(g, gx, gy, 32, alpha(10));
        circle(g, gx, gy, 16, alpha(14));

        // rising bubbles
        for (int i = 0; i < 12; i++) {
            double speed = 14 + (i * 7) % 18;
            double yy = this.height + 30 - ((t * speed + i * 61) % (this.height + 60));
            double xx = ((i * 97) % Math.max(1, this.width)) + Math.sin(t * 0.8 + i) * 10;
            int r = 4 + (i * 5) % 11;
            circle(g, (int) xx, (int) yy, r, alpha(14));
            circle(g, (int) xx - r / 3, (int) yy - r / 3, Math.max(1, r / 4), alpha(40));
        }

        // layered sine waves along the bottom
        int[] a = {0x1A, 0x22, 0x30};
        for (int layer = 0; layer < 3; layer++) {
            double base = this.height - 22 - layer * 12;
            double amp = 7 + layer * 3;
            int c = alpha(a[layer]);
            for (int x = 0; x < this.width; x += 2) {
                double y = base + Math.sin(x * 0.02 + t * (0.9 + layer * 0.35) + layer * 2.1) * amp
                        + Math.sin(x * 0.047 - t * 1.4) * amp * 0.4;
                g.fill(x, (int) y, x + 2, this.height, c);
            }
        }
    }

    // ---------------------------------------------------------------- widgets

    private void toggleRow(GuiGraphics g, int i, String label, boolean on, int mx, int my, float dt) {
        int[] h = hit[i];
        float target = on ? 1f : 0f;
        float prev = anim[i];
        anim[i] += (target - anim[i]) * Math.min(1f, dt * 12f);
        if (Math.abs(target - anim[i]) < 0.005f) anim[i] = target;
        boolean disabled = i == 1 && !SystemInfo.WINDOWS;

        float hv = hover[i] += ((over(i, mx, my) ? 1f : 0f) - hover[i]) * Math.min(1f, dt * 14f);
        rrect(g, h[0], h[1], h[2], h[3], 6, 0xFF000000 | (int) (0x0E + hv * 0x10) * 0x010101);
        text(g, label, h[0] + 8, h[1] + 7, disabled ? 0xFF555555 : WHITE);

        // track + stretchy liquid knob
        int tw = 34, th = 16, tx = h[0] + h[2] - tw - 8, ty = h[1] + 3;
        int track = on ? WHITE : TRACK_OFF;
        if (anim[i] != target) track = mix(TRACK_OFF, WHITE, anim[i]);
        rrect(g, tx, ty, tw, th, 8, track);
        double moving = Math.abs(target - anim[i]);
        int extra = (int) (moving * 10);
        int knobW = 12 + extra;
        int baseX = tx + 2 + (int) (anim[i] * (tw - 4 - 12));
        int kx = target > anim[i] ? baseX - extra : baseX;
        kx = Math.max(tx + 2, Math.min(kx, tx + tw - 2 - knobW));
        rrect(g, kx, ty + 2, knobW, 12, 6, anim[i] > 0.5f ? BLACK : WHITE);
    }

    private static int mix(int c0, int c1, float t) {
        int r = (int) (((c0 >> 16) & 255) * (1 - t) + ((c1 >> 16) & 255) * t);
        int gch = (int) (((c0 >> 8) & 255) * (1 - t) + ((c1 >> 8) & 255) * t);
        int b = (int) ((c0 & 255) * (1 - t) + (c1 & 255) * t);
        return 0xFF000000 | (r << 16) | (gch << 8) | b;
    }

    private void pill(GuiGraphics g, int i, String label, boolean active, int mx, int my, float dt) {
        int[] h = hit[i];
        float hv = hover[i] += ((active && over(i, mx, my) ? 1f : 0f) - hover[i]) * Math.min(1f, dt * 14f);
        int bg = active ? mix(WHITE, 0xFFC8C8C8, hv) : 0xFF1E1E1E;
        rrect(g, h[0], h[1], h[2], h[3], 11, bg);
        textCenter(g, label, h[0] + h[2] / 2, h[1] + 7, active ? BLACK : 0xFF666666);
    }

    private void stepperRow(GuiGraphics g, int mx, int my, float dt) {
        int[] h = hit[3];
        float hv = hover[3] += ((over(3, mx, my) ? 1f : 0f) - hover[3]) * Math.min(1f, dt * 14f);
        boolean active = EngineState.config().throttleWhenUnfocused;
        rrect(g, h[0], h[1], h[2], h[3], 6, 0xFF000000 | (int) (0x0E + hv * 0x10) * 0x010101);
        text(g, "Unfocused FPS limit", h[0] + 8, h[1] + 7, active ? WHITE : 0xFF555555);
        String v = EngineState.config().unfocusedFps + " FPS";
        int vw = this.font.width(v) + 14;
        rrect(g, h[0] + h[2] - vw - 8, h[1] + 3, vw, 16, 8, active ? WHITE : TRACK_OFF);
        textCenter(g, v, h[0] + h[2] - vw / 2 - 8, h[1] + 7, active ? BLACK : 0xFF777777);
    }

    // ---------------------------------------------------------------- main render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        double t = seconds();
        glowX += (mouseX - glowX) * Math.min(1.0, dt * 5.0);
        glowY += (mouseY - glowY) * Math.min(1.0, dt * 5.0);

        if (now - cacheNs > 500_000_000L) { // refresh stats and advice twice a second, not every frame
            stats = EngineState.frameStats();
            if (showAdvice) advice = Advisor.lines();
            cacheNs = now;
        }

        g.fill(0, 0, this.width, this.height, BG);
        liquid(g, t);

        // panel
        rrect(g, px, py, pw, ph, 12, PANEL);
        rrect(g, px, py, pw, 1, 0, alpha(40));
        g.drawString(this.font, Component.literal("FPS ENGINE").withStyle(ChatFormatting.BOLD), px + 14, py + 12, WHITE);
        boolean enabled = EngineState.isEnabled();
        String badge = !EngineState.isHealthy() ? "ERROR" : enabled ? "ACTIVE" : "OFF";
        int bw = this.font.width(badge) + 12;
        rrect(g, px + pw - bw - 14, py + 9, bw, 14, 7, enabled ? WHITE : TRACK_OFF);
        textCenter(g, badge, px + pw - bw / 2 - 14, py + 12, enabled ? BLACK : DIM);
        String sub = actionMessage.isEmpty() ? "Shader-safe. Never changes visual settings." : actionMessage;
        text(g, trim(sub, 52), px + 14, py + 27, actionMessage.isEmpty() ? DIM : WHITE);

        var cfg = EngineState.config();
        toggleRow(g, 0, "Optimization", cfg.enabled, mouseX, mouseY, dt);
        toggleRow(g, 1, "Boost process priority", cfg.boostProcessPriority, mouseX, mouseY, dt);
        toggleRow(g, 2, "Throttle when unfocused", cfg.throttleWhenUnfocused, mouseX, mouseY, dt);
        stepperRow(g, mouseX, mouseY, dt);
        pill(g, 4, "Use high-performance GPU", SystemInfo.WINDOWS, mouseX, mouseY, dt);
        pill(g, 5, "Re-scan", true, mouseX, mouseY, dt);
        pill(g, 6, "Done", true, mouseX, mouseY, dt);

        // live stats card
        rrect(g, px + 10, statsY, pw - 20, 62, 8, CARD);
        int sx = px + 20, sy = statsY + 7;
        JvmSample j = EngineState.jvm();
        if (stats.frames() == 0) {
            text(g, "Collecting frame data...", sx, sy, DIM);
        } else {
            text(g, String.format(Locale.ROOT, "%.0f FPS avg   %.0f 1%% low", stats.avgFps(), stats.low1PercentFps()), sx, sy, WHITE);
            text(g, String.format(Locale.ROOT, "%.1f ms median   %.1f ms p99   %.1f%% stutter", stats.medianMs(), stats.p99Ms(), stats.stutterRatio() * 100), sx, sy + 12, 0xFFCCCCCC);
        }
        text(g, j.heapMaxBytes() == 0 ? "Heap: sampling..." : String.format(Locale.ROOT, "Heap %d/%d MiB   GC %.1f%%", j.heapUsedBytes() >> 20, j.heapMaxBytes() >> 20, j.gcTimeFraction() * 100), sx, sy + 24, 0xFFCCCCCC);
        text(g, trim("GPU: " + SystemInfo.glRenderer(), 48), sx, sy + 36, 0xFFCCCCCC);
        text(g, "Bottleneck: " + EngineState.bottleneck().label, sx, sy + 48 - 1, DIM);

        if (showAdvice) {
            int ay = adviceY;
            int n = 0;
            for (String a : advice) {
                if (n++ >= 2) break;
                text(g, trim("- " + a, 52), px + 14, ay, DIM);
                ay += 11;
            }
        }

        // click ripples (expanding fading circles)
        ripples.removeIf(r -> t - r[2] > 0.6);
        for (double[] r : ripples) {
            double p = (t - r[2]) / 0.6;
            circle(g, (int) r[0], (int) r[1], (int) (8 + p * 48), alpha((int) ((1 - p) * 48)));
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
