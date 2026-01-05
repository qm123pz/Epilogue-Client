package epiloguemixinbridge;

import epilogue.hooks.KeyBindingHooks;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({KeyBinding.class})
public abstract class MixinKeyBinding {
    @Inject(
            method = {"isPressed"},
            at = {@At("RETURN")},
            cancellable = true
    )
    private void isPressed(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        KeyBindingHooks.onIsPressed((KeyBinding) (Object) this, callbackInfoReturnable);
    }
}
