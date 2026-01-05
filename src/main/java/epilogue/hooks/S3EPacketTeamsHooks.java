package epilogue.hooks;

import epiloguemixinbridge.IAccessorS3EPacketTeams;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S3EPacketTeams;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class S3EPacketTeamsHooks {
    private S3EPacketTeamsHooks() {
    }

    public static void onReadPacketData(S3EPacketTeams self, PacketBuffer buf, CallbackInfo ci) {
        IAccessorS3EPacketTeams accessor = (IAccessorS3EPacketTeams) self;
        if (accessor.getColor() > 15) {
            accessor.setColor(-1);
        }
    }
}
