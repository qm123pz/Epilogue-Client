package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.render.Scoreboard;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class GuiIngameScoreboardHooks {
    private GuiIngameScoreboardHooks() {
    }

    public static void onRenderScoreboard(ScoreObjective scoreObjective, ScaledResolution scaledResolution, CallbackInfo ci) {
        if (Epilogue.moduleManager != null) {
            Scoreboard scoreboard = (Scoreboard) Epilogue.moduleManager.modules.get(Scoreboard.class);
            if (scoreboard != null && scoreboard.shouldHideOriginalScoreboard()) {
                ci.cancel();
            }
        }
    }
}
