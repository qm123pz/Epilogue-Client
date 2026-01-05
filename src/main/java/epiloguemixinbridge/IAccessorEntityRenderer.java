package epiloguemixinbridge;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(Side.CLIENT)
@Mixin({EntityRenderer.class})
public interface IAccessorEntityRenderer {
    @org.spongepowered.asm.mixin.gen.Accessor("thirdPersonDistance")
    float getThirdPersonDistance();

    @Invoker
    void callSetupCameraTransform(float float1, int integer);
}
