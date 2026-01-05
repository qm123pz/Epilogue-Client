package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.render.Xray;
import net.minecraft.util.EnumWorldBlockLayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class BlockLayerHooks {
    private BlockLayerHooks() {
    }

    public static void onGetBlockLayer(CallbackInfoReturnable<EnumWorldBlockLayer> callbackInfoReturnable) {
        if (Epilogue.moduleManager != null) {
            if (Epilogue.moduleManager.modules.get(Xray.class).isEnabled()) {
                callbackInfoReturnable.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
            }
        }
    }
}
