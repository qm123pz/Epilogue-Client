package epilogue.module.modules.movement;

import epilogue.event.EventTarget;
import epilogue.events.TickEvent;
import epiloguemixinbridge.IAccessorEntityLivingBase;
import epilogue.module.Module;
import epilogue.util.KeyBindUtil;
import epilogue.value.values.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

public class Sprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean wasSprinting = false;
    public final BooleanValue foxFix = new BooleanValue("Fix CCB", true);

    public Sprint() {
        super("Sprint", true, true);
    }

    public boolean shouldApplyFovFix(IAttributeInstance attribute) {
        if (!this.foxFix.getValue()) {
            return false;
        } else {
            AttributeModifier attributeModifier = ((IAccessorEntityLivingBase) mc.thePlayer).getSprintingSpeedBoostModifier();
            return attribute.getModifier(attributeModifier.getID()) == null && this.wasSprinting;
        }
    }

    public boolean shouldKeepFov(boolean boolean2) {
        return this.foxFix.getValue() && !boolean2 && this.wasSprinting;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
                    break;
                case POST:
                    this.wasSprinting = mc.thePlayer.isSprinting();
            }
        }
    }

    @Override
    public void onDisabled() {
        this.wasSprinting = false;
        KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSprint.getKeyCode());
    }
}
