package epiloguemixinbridge;

import epilogue.hooks.GuiChatHooks;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public class MixinGuiChat {
    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GuiChatHooks.onDrawScreen((GuiScreen) (Object) this, mouseX, mouseY, partialTicks, ci);
    }
}
