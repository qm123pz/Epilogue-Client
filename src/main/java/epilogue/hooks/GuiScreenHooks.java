package epilogue.hooks;

import epiloguemixinbridge.IInvokerGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class GuiScreenHooks {
    private GuiScreenHooks() {
    }

    public static void onHandleKeyboardInput(GuiScreen self, CallbackInfo ci) {
        try {
            if (Keyboard.getEventKeyState() || (Keyboard.getEventKey() == 0 && Character.isDefined(Keyboard.getEventCharacter()))) {
                ((IInvokerGuiScreen) self).callKeyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
            }
            Minecraft.getMinecraft().dispatchKeypresses();
            ci.cancel();
        } catch (java.io.IOException ignored) {
        }
    }
}
