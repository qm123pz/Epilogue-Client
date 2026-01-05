package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.movement.ViewClip;
import epilogue.module.modules.render.Chams;
import epilogue.module.modules.render.Xray;
import net.minecraft.client.renderer.chunk.SetVisibility;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class VisGraphHooks {
    private VisGraphHooks() {
    }

    public static void onFunc_178606_a(CallbackInfo callbackInfo) {
        if (Epilogue.moduleManager != null) {
            if (Epilogue.moduleManager.modules.get(Chams.class).isEnabled()
                    || Epilogue.moduleManager.modules.get(ViewClip.class).isEnabled()
                    || Epilogue.moduleManager.modules.get(Xray.class).isEnabled()) {
                callbackInfo.cancel();
            }
        }
    }

    public static void onComputeVisibility(CallbackInfoReturnable<SetVisibility> callbackInfoReturnable) {
        if (Epilogue.moduleManager != null) {
            if (Epilogue.moduleManager.modules.get(Chams.class).isEnabled()
                    || Epilogue.moduleManager.modules.get(ViewClip.class).isEnabled()
                    || Epilogue.moduleManager.modules.get(Xray.class).isEnabled()) {
                SetVisibility setVisibility = new SetVisibility();
                setVisibility.setAllVisible(true);
                callbackInfoReturnable.setReturnValue(setVisibility);
            }
        }
    }
}
