package epiloguemixinbridge;

import epilogue.hooks.GuiScreenHooks;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {
    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), cancellable = true)
    private void epilogue$inputFix$handleKeyboardInput(CallbackInfo ci) throws java.io.IOException {
        GuiScreenHooks.onHandleKeyboardInput((GuiScreen) (Object) this, ci);
    }
}
