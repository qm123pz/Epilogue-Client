package epilogue.mixin;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {
 
    @Shadow
    protected net.minecraft.client.Minecraft mc;

    @Shadow
    protected abstract void keyTyped(char typedChar, int keyCode) throws java.io.IOException;

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), cancellable = true)
    private void epilogue$inputFix$handleKeyboardInput(CallbackInfo ci) throws java.io.IOException {
        if (Keyboard.getEventKeyState() || (Keyboard.getEventKey() == 0 && Character.isDefined(Keyboard.getEventCharacter()))) {
            this.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
        }
        this.mc.dispatchKeypresses();
        ci.cancel();
    }
}