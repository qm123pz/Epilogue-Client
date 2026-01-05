package epiloguemixinbridge;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import epilogue.hooks.ItemRendererHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
    @Inject(
            method = "renderItemInFirstPerson",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onScaleFirstPersonItem(float partialTicks, CallbackInfo ci) {
        ItemRendererHooks.onScaleFirstPersonItem(partialTicks, ci);
    }

    @Inject(method = "renderItemInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;doBlockTransformations()V"))
    private void onRenderBlockingItem(float partialTicks, CallbackInfo ci) {
        ItemRendererHooks.onRenderBlockingItem((ItemRenderer) (Object) this, partialTicks, ci);
    }
}
