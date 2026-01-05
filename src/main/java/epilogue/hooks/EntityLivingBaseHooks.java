package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.event.EventManager;
import epilogue.events.StrafeEvent;
import epilogue.management.RotationState;
import epilogue.module.modules.movement.Jesus;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public final class EntityLivingBaseHooks {
    private EntityLivingBaseHooks() {
    }

    public static float onJump(EntityLivingBase self, float float1) {
        return (Entity) self instanceof EntityPlayerSP && RotationState.isActived()
                ? RotationState.getSmoothedYaw() * (float) (Math.PI / 180.0)
                : float1;
    }

    public static void onMoveEntityWithHeading(EntityLivingBase self, EntityLivingBase entityLivingBase, float float2, float float3, float float4) {
        if ((Entity) self instanceof EntityPlayerSP) {
            StrafeEvent event = new StrafeEvent(float2, float3, float4);
            EventManager.call(event);
            float2 = event.getStrafe();
            float3 = event.getForward();
            float4 = event.getFriction();
            boolean actived = RotationState.isActived();
            float yaw = self.rotationYaw;
            if (actived) {
                self.rotationYaw = RotationState.getSmoothedYaw();
            }
            entityLivingBase.moveFlying(float2, float3, float4);
            if (actived) {
                self.rotationYaw = yaw;
            }
        } else {
            entityLivingBase.moveFlying(float2, float3, float4);
        }
    }

    public static float onMoveEntityWithHeadingStore(EntityLivingBase self, float float1) {
        if (self instanceof EntityPlayerSP && float1 == (float) EnchantmentHelper.getDepthStriderModifier(self)) {
            if (Epilogue.moduleManager == null) {
                return float1;
            }
            Jesus jesus = (Jesus) Epilogue.moduleManager.modules.get(Jesus.class);
            if (jesus.isEnabled() && (!jesus.groundOnly.getValue() || self.onGround)) {
                return Math.max(float1, jesus.speed.getValue());
            }
        }
        return float1;
    }
}
