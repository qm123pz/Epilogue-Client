package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.combat.KeepSprint;
import net.minecraft.entity.player.EntityPlayer;

public final class EntityPlayerHooks {
    private EntityPlayerHooks() {
    }

    public static double onAttackTargetEntityWithCurrentItem(double speed) {
        if (Epilogue.moduleManager == null) {
            return speed;
        }
        KeepSprint keepSprint = (KeepSprint) Epilogue.moduleManager.modules.get(KeepSprint.class);
        return keepSprint.isEnabled() && keepSprint.shouldKeepSprint()
                ? speed + (1.0 - speed) * (1.0 - keepSprint.slowdown.getValue().doubleValue() / 100.0)
                : speed;
    }

    public static void onSetSprinting(EntityPlayer entityPlayer, boolean boolean2) {
        if (Epilogue.moduleManager != null) {
            KeepSprint keepSprint = (KeepSprint) Epilogue.moduleManager.modules.get(KeepSprint.class);
            if (!keepSprint.isEnabled() || !keepSprint.shouldKeepSprint()) {
                entityPlayer.setSprinting(boolean2);
            }
        }
    }
}
