package cz.fpsengine;

import cz.fpsengine.core.EngineState;
import cz.fpsengine.core.FpsCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FPSengine implements ClientModInitializer {
    public static final String MOD_ID = "fpsengine";
    public static final Logger LOGGER = LoggerFactory.getLogger("FPS Engine");

    @Override
    public void onInitializeClient() {
        try {
            EngineState.init();
            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> FpsCommand.register(dispatcher));
            LOGGER.info("FPS Engine initialised (enabled={})", EngineState.isEnabled());
        } catch (Throwable t) {
            LOGGER.error("FPS Engine failed to initialise; it will stay inactive", t);
            EngineState.disableAfterFailure();
        }
    }
}
