package cz.fpsengine.mixin;

import cz.fpsengine.core.EngineState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Frame boundary hook. If the target is missing the injector is skipped (defaultRequire = 0). */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void fpsengine$frameStart(boolean advanceGameTime, CallbackInfo ci) {
        EngineState.onFrameStart();
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void fpsengine$frameEnd(boolean advanceGameTime, CallbackInfo ci) {
        EngineState.onFrameEnd();
    }
}
