package epilogue.hooks;

import epilogue.management.RotationState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.WeakHashMap;

public final class RenderManagerHooks {
    private static final Map<EntityPlayerSP, RotationSnapshot> SNAPSHOTS = new WeakHashMap<>();

    private RenderManagerHooks() {
    }

    public static void onRenderEntityStatic(Entity entity, float float2, boolean boolean3, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (entity instanceof EntityPlayerSP && RotationState.isRotated(1)) {
            EntityPlayerSP player = (EntityPlayerSP) entity;
            SNAPSHOTS.put(player, new RotationSnapshot(player));
            player.prevRenderYawOffset = RotationState.getPrevRenderYawOffset();
            player.renderYawOffset = RotationState.getRenderYawOffset();
            player.prevRotationYawHead = RotationState.getPrevRotationYawHead();
            player.rotationYawHead = RotationState.getRotationYawHead();
            player.prevRotationPitch = RotationState.getPrevRotationPitch();
            player.rotationPitch = RotationState.getRotationPitch();
        }
    }

    public static void onRenderEntityStaticPost(Entity entity, float float2, boolean boolean3, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (entity instanceof EntityPlayerSP && RotationState.isRotated(1)) {
            EntityPlayerSP player = (EntityPlayerSP) entity;
            RotationSnapshot snapshot = SNAPSHOTS.remove(player);
            if (snapshot != null) {
                snapshot.restore(player);
            }
        }
    }

    private static final class RotationSnapshot {
        private final float prevRenderYawOffset;
        private final float renderYawOffset;
        private final float prevRotationYawHead;
        private final float rotationYawHead;
        private final float prevRotationPitch;
        private final float rotationPitch;

        private RotationSnapshot(EntityPlayerSP player) {
            this.prevRenderYawOffset = player.prevRenderYawOffset;
            this.renderYawOffset = player.renderYawOffset;
            this.prevRotationYawHead = player.prevRotationYawHead;
            this.rotationYawHead = player.rotationYawHead;
            this.prevRotationPitch = player.prevRotationPitch;
            this.rotationPitch = player.rotationPitch;
        }

        private void restore(EntityPlayerSP player) {
            player.prevRenderYawOffset = prevRenderYawOffset;
            player.renderYawOffset = renderYawOffset;
            player.prevRotationYawHead = prevRotationYawHead;
            player.rotationYawHead = rotationYawHead;
            player.prevRotationPitch = prevRotationPitch;
            player.rotationPitch = rotationPitch;
        }
    }
}
