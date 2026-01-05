package epiloguemixinbridge;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiScreen.class)
public interface IInvokerGuiScreen {
    @Invoker("keyTyped")
    void callKeyTyped(char typedChar, int keyCode) throws java.io.IOException;
}
