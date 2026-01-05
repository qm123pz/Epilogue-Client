package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.event.EventManager;
import epilogue.event.types.EventType;
import epilogue.events.LivingUpdateEvent;
import epilogue.events.MoveInputEvent;
import epilogue.events.PlayerUpdateEvent;
import epilogue.events.UpdateEvent;
import epilogue.management.RotationState;
import epilogue.module.modules.movement.NoSlow;
import epilogue.module.modules.player.NoDebuff;
import epiloguemixinbridge.IAccessorEntityLivingBase;
import epiloguemixinbridge.IAccessorEntityPlayerSP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

public final class EntityPlayerSPHooks {
    private static final Map<EntityPlayerSP, RotationOverrideState> STATES = new WeakHashMap<>();

    private EntityPlayerSPHooks() {
    }

    public static void onUpdate(EntityPlayerSP self, CallbackInfo callbackInfo) {
        if (self.worldObj.isBlockLoaded(new BlockPos(self.posX, 0.0, self.posZ))) {
            IAccessorEntityPlayerSP accessor = (IAccessorEntityPlayerSP) self;
            UpdateEvent event = new UpdateEvent(EventType.PRE, accessor.getLastReportedYaw(), accessor.getLastReportedPitch(), self.rotationYaw, self.rotationPitch);
            EventManager.call(event);
            RotationState.applyState(event.isRotated() && !self.isRiding(), event.getNewYaw(), event.getNewPitch(), event.getPreYaw(), event.isRotating());
            RotationOverrideState state = STATES.computeIfAbsent(self, k -> new RotationOverrideState());
            if (event.isRotated()) {
                state.pendingYaw = self.rotationYaw;
                state.pendingPitch = self.rotationPitch;
                state.overrideYaw = event.getNewYaw();
                state.overridePitch = event.getNewPitch();
            } else {
                state.pendingYaw = Float.NaN;
                state.pendingPitch = Float.NaN;
                state.overrideYaw = Float.NaN;
                state.overridePitch = Float.NaN;
            }
        }
    }

    public static void onPostUpdate(EntityPlayerSP self, CallbackInfo callbackInfo) {
        if (self.worldObj.isBlockLoaded(new BlockPos(self.posX, 0.0, self.posZ))) {
            RotationOverrideState state = STATES.get(self);
            if (state != null && !Float.isNaN(state.pendingYaw) && !Float.isNaN(state.pendingPitch)) {
                IAccessorEntityPlayerSP accessor = (IAccessorEntityPlayerSP) self;
                accessor.setLastReportedYaw(self.rotationYaw);
                accessor.setLastReportedPitch(self.rotationPitch);
                self.rotationYaw = self.rotationYaw + MathHelper.wrapAngleTo180_float(state.pendingYaw - self.rotationYaw);
                self.rotationPitch = state.pendingPitch;
                self.prevRotationYaw = self.rotationYaw;
                self.prevRotationPitch = self.rotationPitch;
                accessor.setPrevRenderArmYaw(self.rotationYaw - (accessor.getRenderArmYaw() - accessor.getPrevRenderArmYaw()) * 2.0F);
                accessor.setRenderArmYaw(self.rotationYaw);
            }
            IAccessorEntityPlayerSP accessor = (IAccessorEntityPlayerSP) self;
            EventManager.call(new UpdateEvent(EventType.POST, accessor.getLastReportedYaw(), accessor.getLastReportedPitch(), self.rotationYaw, self.rotationPitch));
        }
    }

    public static boolean onRiding(EntityPlayerSP entityPlayerSP) {
        RotationOverrideState state = STATES.get(entityPlayerSP);
        if (state != null && !Float.isNaN(state.overrideYaw) && !Float.isNaN(state.overridePitch)) {
            entityPlayerSP.rotationYaw = state.overrideYaw;
            entityPlayerSP.rotationPitch = state.overridePitch;
        }
        return entityPlayerSP.isRiding();
    }

    public static void onMotionUpdate(CallbackInfo callbackInfo) {
        EventManager.call(new PlayerUpdateEvent());
    }

    public static void onLivingUpdate(CallbackInfo callbackInfo) {
        EventManager.call(new LivingUpdateEvent());
    }

    public static void onUpdateMove(CallbackInfo callbackInfo) {
        EventManager.call(new MoveInputEvent());
    }

    public static boolean onIsUsing(EntityPlayerSP entityPlayerSP) {
        NoSlow noSlow = (NoSlow) Epilogue.moduleManager.modules.get(NoSlow.class);
        return (!noSlow.isEnabled() || !noSlow.isAnyActive()) && entityPlayerSP.isUsingItem();
    }

    public static boolean onCheckPotion(EntityPlayerSP entityPlayerSP, Potion potion) {
        if (potion == Potion.confusion && Epilogue.moduleManager != null) {
            NoDebuff noDebuff = (NoDebuff) Epilogue.moduleManager.modules.get(NoDebuff.class);
            if (noDebuff.isEnabled() && noDebuff.nausea.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entityPlayerSP).getActivePotionsMap().containsKey(potion.id);
    }

    private static final class RotationOverrideState {
        private float overrideYaw = Float.NaN;
        private float overridePitch = Float.NaN;
        private float pendingYaw = Float.NaN;
        private float pendingPitch = Float.NaN;
    }
}
