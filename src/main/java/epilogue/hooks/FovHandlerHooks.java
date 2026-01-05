package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.movement.Sprint;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;

public final class FovHandlerHooks {
    private FovHandlerHooks() {
    }

    public static boolean onFovChange(EntityPlayer entityPlayer) {
        boolean sprinting = entityPlayer.isSprinting();
        if (entityPlayer instanceof EntityPlayerSP && Epilogue.moduleManager != null) {
            Sprint sprint = (Sprint) Epilogue.moduleManager.modules.get(Sprint.class);
            return sprint.isEnabled() && sprint.shouldKeepFov(sprinting) || sprinting;
        }
        return sprinting;
    }
}
