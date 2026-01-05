package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.movement.Jesus;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;

public final class WorldHooks {
    private WorldHooks() {
    }

    public static boolean onHandleMaterialAcceleration(Entity entity) {
        if (entity instanceof EntityPlayerSP && Epilogue.moduleManager != null) {
            Jesus jesus = (Jesus) Epilogue.moduleManager.modules.get(Jesus.class);
            if (jesus.isEnabled() && jesus.noPush.getValue()) {
                return false;
            }
        }
        return entity.isPushedByWater();
    }
}
