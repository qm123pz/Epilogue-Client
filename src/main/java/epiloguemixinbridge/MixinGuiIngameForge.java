package epiloguemixinbridge;

import epilogue.hooks.GuiIngameForgeHooks;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin({GuiIngameForge.class})
public abstract class MixinGuiIngameForge {
    @Inject(
            method = {"renderGameOverlay"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/GuiIngameForge;renderTitle(IIF)V",
                    shift = At.Shift.AFTER,
                    remap = false
            )}
    )
    private void renderGameOverlay(float float1, CallbackInfo callbackInfo) {
        GuiIngameForgeHooks.onRenderGameOverlay(float1, callbackInfo);
    }

    @Redirect(
            method = {"renderExperience"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;experience:F"
            )
    )
    private float renderExperience(EntityPlayerSP entityPlayerSP) {
        return GuiIngameForgeHooks.onRenderExperience(entityPlayerSP);
    }

    @Redirect(
            method = {"renderExperience"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;experienceLevel:I"
            )
    )
    private int renderExperienceLevel(EntityPlayerSP entityPlayerSP) {
        return GuiIngameForgeHooks.onRenderExperienceLevel(entityPlayerSP);
    }
}
