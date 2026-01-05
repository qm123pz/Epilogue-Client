package epilogue.hooks;

import epilogue.event.EventManager;
import epilogue.events.ChatGUIEvent;
import epilogue.ui.chat.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class GuiChatHooks {
    private GuiChatHooks() {
    }

    public static void onDrawScreen(GuiScreen self, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!(self instanceof net.minecraft.client.gui.GuiChat) && !(self instanceof GuiChat)) {
            return;
        }
        EventManager.call(new ChatGUIEvent(mouseX, mouseY));
    }
}
