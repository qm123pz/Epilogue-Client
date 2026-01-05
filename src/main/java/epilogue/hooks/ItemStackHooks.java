package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.render.ESP;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class ItemStackHooks {
    private ItemStackHooks() {
    }

    public static void onHasEffect(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (Epilogue.moduleManager != null) {
            ESP esp = (ESP) Epilogue.moduleManager.modules.get(ESP.class);
            if (esp.isEnabled() && !esp.isGlowEnabled()) {
                callbackInfoReturnable.setReturnValue(false);
            }
        }
    }
}
