package epiloguemixinbridge;

import epilogue.hooks.WorldRendererHooks;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

@SideOnly(Side.CLIENT)
@Mixin({WorldRenderer.class})
public abstract class MixinWorldRenderer {
    @Redirect(
            method = {"putColorMultiplier"},
            at = @At(
                    value = "INVOKE",
                    target = "java/nio/IntBuffer.put(II)Ljava/nio/IntBuffer;",
                    remap = false
            )
    )
    private IntBuffer putColorMultiplier(IntBuffer intBuffer, int integer2, int integer3) {
        return WorldRendererHooks.onPutColorMultiplier(intBuffer, integer2, integer3);
    }
}
