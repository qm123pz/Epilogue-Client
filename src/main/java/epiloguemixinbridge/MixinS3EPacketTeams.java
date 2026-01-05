package epiloguemixinbridge;

import epilogue.hooks.S3EPacketTeamsHooks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S3EPacketTeams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(S3EPacketTeams.class)
public abstract class MixinS3EPacketTeams {
    @Inject(method = "readPacketData", at = @At("RETURN"))
    private void viaforge$fixS3E(PacketBuffer buf, CallbackInfo ci) {
        S3EPacketTeamsHooks.onReadPacketData((S3EPacketTeams) (Object) this, buf, ci);
    }
}
