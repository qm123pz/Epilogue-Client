package epilogue.module.modules.movement;

import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.event.types.Priority;
import epilogue.events.TickEvent;
import epiloguemixinbridge.IAccessorEntityLivingBase;
import epilogue.module.Module;
import epilogue.value.values.IntValue;
import net.minecraft.client.Minecraft;

public class NoJumpDelay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final IntValue delay = new IntValue("delay", 3, 0, 8);

    public NoJumpDelay() {
        super("NoJumpDelay", false);
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            ((IAccessorEntityLivingBase) mc.thePlayer)
                    .setJumpTicks(Math.min(((IAccessorEntityLivingBase) mc.thePlayer).getJumpTicks(), this.delay.getValue() + 1));
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.delay.getValue().toString()};
    }
}
