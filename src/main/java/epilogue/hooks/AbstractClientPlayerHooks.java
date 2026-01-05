package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.movement.Sprint;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

public final class AbstractClientPlayerHooks {
    private AbstractClientPlayerHooks() {
    }

    public static double onGetFovModifier(AbstractClientPlayer self, IAttributeInstance iAttributeInstance) {
        double attributeValue = iAttributeInstance.getAttributeValue();
        if (((Entity) self) instanceof EntityPlayerSP && Epilogue.moduleManager != null) {
            Sprint sprint = (Sprint) Epilogue.moduleManager.modules.get(Sprint.class);
            return sprint.isEnabled() && sprint.shouldApplyFovFix(iAttributeInstance) ? attributeValue * 1.300000011920929 : attributeValue;
        }
        return attributeValue;
    }
}
