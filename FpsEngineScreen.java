package cz.fpsengine.gui;

import cz.fpsengine.core.Advisor;
import cz.fpsengine.core.EngineState;
import cz.fpsengine.system.SystemInfo;
import cz.fpsengine.system.WindowsTweaks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Minimal settings screen built from vanilla widgets (no extra config library needed). */
public final class FpsEngineScreen extends Screen {
    private static final int[] FPS_STEPS = {10, 20, 30, 60};

    private final Screen parent;
    private volatile String actionMessage = "";
    private List<String> advice = List.of();
    private long adviceStamp;
    private Button enabledBtn, prioBtn, throttleBtn, fpsBtn;

    public FpsEngineScreen(Screen parent) {
        super(Component.literal("FPS Engine"));
        this.parent = parent;
    }

    private static Component onOff(String name, boolean v) {
        return Component.literal(name + ": " + (v ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = 36;
        var cfg = EngineState.config();

        enabledBtn = addRenderableWidget(Button.builder(onOff("Optimization", cfg.enabled), b -> {
            EngineState.setEnabled(!EngineState.config().enabled);
            refresh();
        }).bounds(cx - 100, y, 200, 20).build());
        y += 24;

        prioBtn = addRenderableWidget(Button.builder(onOff("Boost process priority (Windows)", cfg.boostProcessPriority), b -> {
            EngineState.setBoostPriority(!EngineState.config().boostProcessPriority);
            refresh();
        }).bounds(cx - 100, y, 200, 20).build());
        prioBtn.active = SystemInfo.WINDOWS;
        y += 24;

        throttleBtn = addRenderableWidget(Button.builder(onOff("Throttle when window unfocused", cfg.throttleWhenUnfocused), b -> {
            EngineState.setThrottleUnfocused(!EngineState.config().throttleWhenUnfocused);
            refresh();
        }).bounds(cx - 100, y, 200, 20).build());
        y += 24;

        fpsBtn = addRenderableWidget(Button.builder(Component.literal("Unfocused FPS: " + cfg.unfocusedFps), b -> {
            int cur = EngineState.config().unfocusedFps;
            int next = FPS_STEPS[0];
            for (int i = 0; i < FPS_STEPS.length; i++) if (FPS_STEPS[i] == cur) next = FPS_STEPS[(i + 1) % FPS_STEPS.length];
            EngineState.setUnfocusedFps(next);
            refresh();
        }).bounds(cx - 100, y, 200, 20).build());
        y += 24;

        Button gpu = addRenderableWidget(Button.builder(Component.literal("Use high-performance GPU for Minecraft"), b ->
                WindowsTweaks.applyGpuPreferenceAsync(m -> actionMessage = m)).bounds(cx - 100, y, 200, 20).build());
        gpu.active = SystemInfo.WINDOWS;
        y += 24;

        addRenderableWidget(Button.builder(Component.literal("Re-scan system"), b -> {
            SystemInfo.probeAsync();
            actionMessage = "Scanning...";
        }).bounds(cx - 100, y, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx + 2, y, 98, 20).build());
    }

    private void refresh() {
        var cfg = EngineState.config();
        enabledBtn.setMessage(onOff("Optimization", cfg.enabled));
        prioBtn.setMessage(onOff("Boost process priority (Windows)", cfg.boostProcessPriority));
        throttleBtn.setMessage(onOff("Throttle when window unfocused", cfg.throttleWhenUnfocused));
        fpsBtn.setMessage(Component.literal("Unfocused FPS: " + cfg.unfocusedFps));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);

        int y = 36 + 24 * 6 + 8;
        int x = this.width / 2 - 150;
        if (!actionMessage.isEmpty()) {
            g.drawString(this.font, trim(actionMessage), x, y, 0xFF55FF55);
            y += 12;
        }
        long now = System.nanoTime();
        if (now - adviceStamp > 2_000_000_000L) { // recompute at most every 2 s, not every frame
            advice = Advisor.lines();
            adviceStamp = now;
        }
        int shown = 0;
        for (String a : advice) {
            if (shown++ >= 4) break;
            g.drawString(this.font, trim("- " + a), x, y, 0xFFCCCCCC);
            y += 12;
        }
        if (advice.size() > 4) g.drawString(this.font, "...more: /fpsengine check", x, y, 0xFF888888);
    }

    private static String trim(String s) {
        return s.length() > 70 ? s.substring(0, 67) + "..." : s;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
