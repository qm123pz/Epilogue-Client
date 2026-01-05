package epilogue.hooks;

import epilogue.event.EventManager;
import epilogue.events.SwapItemEvent;
import epiloguemixinbridge.IAccessorKeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class KeyBindingHooks {
    private KeyBindingHooks() {
    }

    public static void onIsPressed(KeyBinding self, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (callbackInfoReturnable.getReturnValue()) {
            Minecraft mc = Minecraft.getMinecraft();
            String keyDescription = ((IAccessorKeyBinding) self).getKeyDescription();
            for (int i = 0; i < 9; i++) {
                if (mc.gameSettings.keyBindsHotbar[i].getKeyDescription().equals(keyDescription)) {
                    SwapItemEvent event = new SwapItemEvent(i, 0);
                    EventManager.call(event);
                    if (event.isCancelled()) {
                        callbackInfoReturnable.setReturnValue(false);
                    }
                }
            }
        }
    }
}
