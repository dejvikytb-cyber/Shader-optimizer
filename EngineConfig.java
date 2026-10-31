package cz.fpsengine.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cz.fpsengine.FPSengine;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Only stores the on/off switch. Never touches Sodium or Iris configuration. */
public final class EngineConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    /** Windows: AboveNormal priority for the game process. */
    public boolean boostProcessPriority = true;
    /** Sleep the render loop while the window is not focused (saves GPU/CPU for other apps; no in-game FPS gain). */
    public boolean throttleWhenUnfocused = true;
    public int unfocusedFps = 20;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("fpsengine.json");
    }

    public static EngineConfig load() {
        Path p = path();
        if (Files.isRegularFile(p)) {
            try {
                EngineConfig c = GSON.fromJson(Files.readString(p), EngineConfig.class);
                if (c != null) return c;
            } catch (Exception e) {
                FPSengine.LOGGER.warn("Could not read {}; using defaults", p, e);
            }
        }
        EngineConfig c = new EngineConfig();
        c.save();
        return c;
    }

    public void save() {
        try {
            Files.writeString(path(), GSON.toJson(this));
        } catch (IOException e) {
            FPSengine.LOGGER.warn("Could not write {}", path(), e);
        }
    }
}
