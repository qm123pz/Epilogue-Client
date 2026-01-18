package epilogue.module.modules.combat;

import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.event.types.Priority;
import epilogue.events.AttackEvent;
import epilogue.events.MoveInputEvent;
import epilogue.events.PacketEvent;
import epilogue.events.TickEvent;
import epilogue.module.Module;
import epilogue.util.TimerUtil;
import epilogue.value.values.*;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

public class SuperKnockBack extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Legit", "LegitFast", "LessPacket", "Packet", "DoublePacket", "Wtap"});
    public final FloatValue delay = new FloatValue("Delay", 5.5F, 0.0F, 10.0F, () -> this.mode.getValue() == 5);
    public final FloatValue duration = new FloatValue("Duration", 1.5F, 1.0F, 5.0F, () -> this.mode.getValue() == 5);
    public final BooleanValue intelligent = new BooleanValue("Intelligent", false);
    public final BooleanValue onlyGround = new BooleanValue("Only Gound", true);
    @Getter
    private boolean shouldSprintReset;
    private EntityLivingBase target;
    private final TimerUtil timer = new TimerUtil();
    private boolean active = false;
    private boolean stopForward = false;
    private long delayTicks = 0L;
    private long durationTicks = 0L;

    public SuperKnockBack() {
        super("SuperKnockBack", false);
        this.shouldSprintReset = false;
        this.target = null;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        Entity targetEntity = event.getTarget();
        if (targetEntity instanceof EntityLivingBase) {
            this.target = (EntityLivingBase) targetEntity;
        }
    }

    private boolean canTrigger() {
        return !(mc.thePlayer.movementInput.moveForward < 0.8F)
                && !mc.thePlayer.isCollidedHorizontally
                && (!((float) mc.thePlayer.getFoodStats().getFoodLevel() <= 6.0F) || mc.thePlayer.capabilities.allowFlying) && (mc.thePlayer.isSprinting()
                || !mc.thePlayer.isUsingItem() && !mc.thePlayer.isPotionActive(Potion.blindness) && mc.gameSettings.keyBindSprint.isKeyDown());
    }

    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (this.active) {
            if (this.mode.getValue() != 5) {
                return;
            }
            if (!this.stopForward && !this.canTrigger()) {
                this.active = false;
                while (this.delayTicks > 0L) {
                    this.delayTicks -= 50L;
                }
                while (this.durationTicks > 0L) {
                    this.durationTicks -= 50L;
                }
            } else if (this.delayTicks > 0L) {
                this.delayTicks -= 50L;
            } else {
                if (this.durationTicks > 0L) {
                    this.durationTicks -= 50L;
                    this.stopForward = true;
                    mc.thePlayer.movementInput.moveForward = 0.0F;
                }
                if (this.durationTicks <= 0L) {
                    this.active = false;
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.mode.getValue() != 5) {
            return;
        }
        if (this.isEnabled() && !event.isCancelled() && event.getType() == EventType.SEND) {
            if (event.getPacket() instanceof C02PacketUseEntity
                    && ((C02PacketUseEntity) event.getPacket()).getAction() == C02PacketUseEntity.Action.ATTACK
                    && !this.active
                    && this.timer.hasTimeElapsed(500L)
                    && mc.thePlayer.isSprinting()) {
                this.timer.reset();
                this.active = true;
                this.stopForward = false;
                this.delayTicks = this.delayTicks + (long) (50.0F * this.delay.getValue());
                this.durationTicks = this.durationTicks + (long) (50.0F * this.duration.getValue());
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (this.mode.getValue() == 1) {
            if (this.target != null && this.isMoving()) {
                if (!this.onlyGround.getValue() || mc.thePlayer.onGround) {
                    mc.thePlayer.sprintingTicksLeft = 0;
                }
                this.target = null;
            }
            return;
        }
        EntityLivingBase entity = null;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            entity = (EntityLivingBase) mc.objectMouseOver.entityHit;
        }
        if (entity == null) {
            return;
        }
        double x = mc.thePlayer.posX - entity.posX;
        double z = mc.thePlayer.posZ - entity.posZ;
        float calcYaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI - 90.0);
        float diffY = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - entity.rotationYawHead));
        if (this.intelligent.getValue() && diffY > 120.0F) {
            return;
        }
        if (entity.hurtTime == 10) {
            switch (this.mode.getValue()) {
                case 0:
                    this.shouldSprintReset = true;
                    if (mc.thePlayer.isSprinting()) {
                        mc.thePlayer.setSprinting(false);
                        mc.thePlayer.setSprinting(true);
                    }
                    this.shouldSprintReset = false;
                    break;
                case 2:
                    if (mc.thePlayer.isSprinting()) {
                        mc.thePlayer.setSprinting(false);
                    }
                    mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;
                case 3:
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;
                case 4:
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                    mc.thePlayer.setSprinting(true);
                    break;
            }
        }
    }

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F;
    }

}