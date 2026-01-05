package epiloguemixinbridge;

import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(C02PacketUseEntity.class)
public interface AccessorC02PacketUseEntity {
    @Accessor("hitVec")
    void setHitVec(Vec3 hitVec);
}
