package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.render.dynamicisland.DynamicIsland;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class GuiPlayerTabOverlayHooks {
    private GuiPlayerTabOverlayHooks() {
    }

    public static void onRenderPlayerlist(int width, Scoreboard scoreboard, ScoreObjective scoreObjective, CallbackInfo ci) {
        if (Epilogue.moduleManager != null) {
            DynamicIsland dynamicIsland = (DynamicIsland) Epilogue.moduleManager.modules.get(DynamicIsland.class);
            if (dynamicIsland != null && dynamicIsland.isEnabled() && dynamicIsland.mode.getValue() != 1) {
                ci.cancel();
            }
        }
    }
}
