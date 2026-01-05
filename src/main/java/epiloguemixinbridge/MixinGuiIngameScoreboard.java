package epiloguemixinbridge;

import epilogue.hooks.GuiIngameScoreboardHooks;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin({GuiIngame.class})
public abstract class MixinGuiIngameScoreboard {
    @Inject(
            method = {"renderScoreboard"},
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderScoreboard(ScoreObjective scoreObjective, ScaledResolution scaledResolution, CallbackInfo ci) {
        GuiIngameScoreboardHooks.onRenderScoreboard(scoreObjective, scaledResolution, ci);
    }
}
