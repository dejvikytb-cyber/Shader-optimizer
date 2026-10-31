package cz.fpsengine.core;

import com.mojang.brigadier.CommandDispatcher;
import cz.fpsengine.compat.CompatReport;
import cz.fpsengine.performance.monitor.FrameStats;
import cz.fpsengine.performance.monitor.JvmSample;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class FpsCommand {
    private FpsCommand() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(ClientCommandManager.literal("fpsengine")
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    EngineState.setEnabled(true);
                    ctx.getSource().sendFeedback(Component.literal("FPS Engine: ON"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    EngineState.setEnabled(false);
                    ctx.getSource().sendFeedback(Component.literal("FPS Engine: OFF"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("status").executes(ctx -> {
                    for (String line : status()) ctx.getSource().sendFeedback(Component.literal(line));
                    return 1;
                })));
    }

    private static String[] status() {
        FrameStats s = EngineState.frameStats();
        JvmSample j = EngineState.jvm();
        String state = !EngineState.isHealthy() ? "DEACTIVATED (error, see log)" : EngineState.isEnabled() ? "ON" : "OFF";
        String frames = s.frames() == 0
                ? "Frames [measured]: not enough data yet"
                : String.format(Locale.ROOT, "Frames [measured]: avg %.0f FPS, 1%% low %.0f FPS, median %.1f ms, p99 %.1f ms, stutter %.1f%%",
                s.avgFps(), s.low1PercentFps(), s.medianMs(), s.p99Ms(), s.stutterRatio() * 100);
        String heap = j.heapMaxBytes() == 0
                ? "Heap [measured]: not sampled yet"
                : String.format(Locale.ROOT, "Heap [measured]: %d / %d MiB, GC time %.1f%%",
                j.heapUsedBytes() >> 20, j.heapMaxBytes() >> 20, j.gcTimeFraction() * 100);
        return new String[]{
                "FPS Engine: " + state,
                frames,
                heap,
                "Bottleneck [heuristic]: " + EngineState.bottleneck().label,
                "GPU load / VRAM: unavailable (no supported interface)",
                "Detected mods: " + (CompatReport.lines().isEmpty() ? "none of the known set" : String.join(", ", CompatReport.lines())),
                "Active render optimizations: none (phase 1 = measurement only)"
        };
    }
}
