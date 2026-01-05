package epiloguemixinbridge;

import epilogue.hooks.RenderManagerHooks;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({RenderManager.class})
public abstract class MixinRenderManager {
    @Inject(
            method = {"renderEntityStatic"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void renderEntityStatic(Entity entity, float float2, boolean boolean3, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        RenderManagerHooks.onRenderEntityStatic(entity, float2, boolean3, callbackInfoReturnable);
    }

    @Inject(
            method = {"renderEntityStatic"},
            at = {@At("RETURN")}
    )
    private void renderEntityStaticPost(Entity entity, float float2, boolean boolean3, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        RenderManagerHooks.onRenderEntityStaticPost(entity, float2, boolean3, callbackInfoReturnable);
    }
}
