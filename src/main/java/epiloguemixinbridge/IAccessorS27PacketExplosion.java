package epiloguemixinbridge;

import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(Side.CLIENT)
@Mixin(S27PacketExplosion.class)
public interface IAccessorS27PacketExplosion {
    @Accessor("field_149152_f")
    float getField_149152_f();

    @Accessor("field_149152_f")
    void setField_149152_f(float f);

    @Accessor("field_149153_g")
    float getField_149153_g();

    @Accessor("field_149153_g")
    void setField_149153_g(float f);

    @Accessor("field_149159_h")
    float getField_149159_h();

    @Accessor("field_149159_h")
    void setField_149159_h(float f);
}