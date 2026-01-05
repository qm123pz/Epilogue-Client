package epiloguemixinbridge;

import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityPlayerSP.class)
public interface IAccessorEntityPlayerSP {
    @Accessor("lastReportedYaw")
    float getLastReportedYaw();

    @Accessor("lastReportedYaw")
    void setLastReportedYaw(float value);

    @Accessor("lastReportedPitch")
    float getLastReportedPitch();

    @Accessor("lastReportedPitch")
    void setLastReportedPitch(float value);

    @Accessor("renderArmYaw")
    float getRenderArmYaw();

    @Accessor("renderArmYaw")
    void setRenderArmYaw(float value);

    @Accessor("prevRenderArmYaw")
    float getPrevRenderArmYaw();

    @Accessor("prevRenderArmYaw")
    void setPrevRenderArmYaw(float value);
}
