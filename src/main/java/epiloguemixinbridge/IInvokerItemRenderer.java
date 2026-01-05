package epiloguemixinbridge;

import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemRenderer.class)
public interface IInvokerItemRenderer {
    @Invoker("doBlockTransformations")
    void callDoBlockTransformations();
}
