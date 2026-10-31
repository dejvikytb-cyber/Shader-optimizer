package cz.fpsengine.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

/** Read-only detection of installed rendering/performance mods and their versions. */
public final class CompatReport {
    private static final String[][] KNOWN = {
            {"sodium", "Sodium"}, {"iris", "Iris"}, {"lithium", "Lithium"},
            {"ferritecore", "FerriteCore"}, {"immediatelyfast", "ImmediatelyFast"},
            {"entityculling", "Entity Culling"}, {"moreculling", "More Culling"},
            {"modernfix", "ModernFix"}, {"indium", "Indium"}, {"dynamic_fps", "Dynamic FPS"}
    };

    private CompatReport() {}

    public static boolean isLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static List<String> lines() {
        List<String> out = new ArrayList<>();
        for (String[] k : KNOWN) {
            FabricLoader.getInstance().getModContainer(k[0]).ifPresent(c ->
                    out.add(k[1] + " " + c.getMetadata().getVersion().getFriendlyString()));
        }
        return out;
    }
}
