package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.render.Xray;
import epiloguemixinbridge.IInvokerBlockModelRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class BlockModelRendererHooks {
    private BlockModelRendererHooks() {
    }

    public static void onRenderModel(
            BlockModelRenderer self,
            IBlockAccess iBlockAccess,
            IBakedModel iBakedModel,
            IBlockState iBlockState,
            BlockPos blockPos,
            WorldRenderer worldRenderer,
            boolean boolean6,
            CallbackInfoReturnable<Boolean> callbackInfoReturnable
    ) {
        if (Epilogue.moduleManager != null) {
            if (Epilogue.moduleManager.modules.get(Xray.class).isEnabled()) {
                boolean result = ((IInvokerBlockModelRenderer) self).callRenderModelAmbientOcclusion(
                        iBlockAccess, iBakedModel, iBlockState.getBlock(), blockPos, worldRenderer, boolean6
                );
                callbackInfoReturnable.setReturnValue(result);
            }
        }
    }
}
