package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.event.EventManager;
import epilogue.events.KnockbackEvent;
import epilogue.events.SafeWalkEvent;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class EntityHooks {
    private EntityHooks() {
    }

    public static void onSetVelocity(Entity self, double double1, double double2, double double3, CallbackInfo callbackInfo) {
        if (self instanceof EntityPlayerSP) {
            KnockbackEvent event = new KnockbackEvent(double1, double2, double3);
            EventManager.call(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
                self.motionX = event.getX();
                self.motionY = event.getY();
                self.motionZ = event.getZ();
            }
        }
    }

    public static void onSetAngles(Entity self, CallbackInfo callbackInfo) {
        if (self instanceof EntityPlayerSP && Epilogue.rotationManager != null && Epilogue.rotationManager.isRotated()) {
            callbackInfo.cancel();
        }
    }

    public static boolean onMoveEntity(Entity self, boolean boolean1) {
        if (self instanceof EntityPlayerSP) {
            SafeWalkEvent event = new SafeWalkEvent(boolean1);
            EventManager.call(event);
            return event.isSafeWalk();
        }
        return boolean1;
    }
}
