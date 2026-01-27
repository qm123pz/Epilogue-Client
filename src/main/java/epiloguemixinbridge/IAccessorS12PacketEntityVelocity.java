package epiloguemixinbridge;

import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(Side.CLIENT)
@Mixin(S12PacketEntityVelocity.class)
public interface IAccessorS12PacketEntityVelocity {
    @Accessor("motionX")
    void setMotionX(int x);

    @Accessor("motionY")
    void setMotionY(int y);

    @Accessor("motionZ")
    void setMotionZ(int z);
}