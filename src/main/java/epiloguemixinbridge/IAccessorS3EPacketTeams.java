package epiloguemixinbridge;

import net.minecraft.network.play.server.S3EPacketTeams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(S3EPacketTeams.class)
public interface IAccessorS3EPacketTeams {
    @Accessor("color")
    int getColor();

    @Accessor("color")
    void setColor(int color);
}
