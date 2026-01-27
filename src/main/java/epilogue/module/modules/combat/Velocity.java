package epilogue.module.modules.combat;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import epilogue.enums.ChatColors;
import epilogue.enums.DelayModules;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.*;
import epilogue.management.RotationState;
import epilogue.module.Module;
import epilogue.module.modules.movement.LongJump;
import epilogue.util.ChatUtil;
import epilogue.util.MoveUtil;
import epilogue.util.RotationUtil;
import epilogue.value.values.*;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.ModeValue;
import epiloguemixinbridge.IAccessorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int chanceCounter = 0;
    private int delayChanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean jumpFlag = false;
    private boolean delayFlag = false;
    private int rotatoTickCounter = 0;
    private float[] targetRotation = null;
    private double knockbackX = 0;
    private double knockbackZ = 0;
    private boolean hasReceivedVelocity = false;
    private boolean polarAbsorbed = false;
    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Packet", "Prediction"});
    public final IntValue delayTicks = new IntValue("Delay Ticks", 3, 1, 20, () -> this.mode.getValue() == 1);
    public final PercentValue delayChance = new PercentValue("Delay Chance", 100, () -> this.mode.getValue() == 1);
    public final PercentValue chance = new PercentValue("Chance", 100);
    public final PercentValue horizontal = new PercentValue("Horizontal", 100);
    public final PercentValue vertical = new PercentValue("Vertical", 100);
    public final PercentValue explosionHorizontal = new PercentValue("Explosions Horizontal", 100);
    public final PercentValue explosionVertical = new PercentValue("Explosions Vertical", 100);
    public final BooleanValue checkDamage = new BooleanValue("Check Damage", true);
    public final BooleanValue rotate = new BooleanValue("Rotate", false, () -> this.mode.getValue() == 1);
    public final IntValue rotateTick = new IntValue("Rotate Tick", 3, 1, 12, () -> this.mode.getValue() == 1 && this.rotate.getValue());
    public final BooleanValue autoMove = new BooleanValue("Auto Move", false, () -> this.mode.getValue() == 1 && this.rotate.getValue());
    public final BooleanValue dbg = new BooleanValue("Debug", false);
    private boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    private boolean canDelay() {
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        return mc.thePlayer.onGround && (!aura.isEnabled() || !aura.shouldAutoBlock());
    }

    public Velocity() {
        super("Velocity", false);
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled()) {
            this.pendingExplosion = false;
            this.allowNext = true;
        } else if (!this.allowNext || !(Boolean) this.checkDamage.getValue()) {
            this.allowNext = true;
            if (this.pendingExplosion) {
                this.pendingExplosion = false;
                if (this.explosionHorizontal.getValue() > 0) {
                    event.setX(event.getX() * (double) this.explosionHorizontal.getValue() / 100.0);
                    event.setZ(event.getZ() * (double) this.explosionHorizontal.getValue() / 100.0);
                } else {
                    event.setX(mc.thePlayer.motionX);
                    event.setZ(mc.thePlayer.motionZ);
                }
                if (this.explosionVertical.getValue() > 0) {
                    event.setY(event.getY() * (double) this.explosionVertical.getValue() / 100.0);
                } else {
                    event.setY(mc.thePlayer.motionY);
                }
            } else {
                this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
                if (this.chanceCounter >= 100) {
                    this.jumpFlag = (this.mode.getValue() == 1) && event.getY() > 0.0;
                    if (this.mode.getValue() == 1 && this.rotate.getValue() && event.getY() > 0.0) {
                        this.knockbackX = event.getX();
                        this.knockbackZ = event.getZ();
                        if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
                            this.rotatoTickCounter = 1;
                        }
                    }
                    if (this.horizontal.getValue() > 0) {
                        event.setX(event.getX() * (double) this.horizontal.getValue() / 100.0);
                        event.setZ(event.getZ() * (double) this.horizontal.getValue() / 100.0);
                    } else {
                        event.setX(mc.thePlayer.motionX);
                        event.setZ(mc.thePlayer.motionZ);
                    }
                    if (this.vertical.getValue() > 0) {
                        event.setY(event.getY() * (double) this.vertical.getValue() / 100.0);
                    } else {
                        event.setY(mc.thePlayer.motionY);
                    }
                }
            }
        }
    }


    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                if (packet.getEntityID() == mc.thePlayer.getEntityId()) {
                    LongJump longJump = (LongJump) Epilogue.moduleManager.modules.get(LongJump.class);
                    if (this.mode.getValue() == 1
                            && !this.delayFlag
                            && !this.canDelay()
                            && !this.isInLiquidOrWeb()
                            && !this.pendingExplosion
                            && (!this.allowNext || !(Boolean) this.checkDamage.getValue())
                            && (!longJump.isEnabled() || !longJump.canStartJump())) {
                        this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();
                        if (this.delayChanceCounter >= 100) {
                            Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                            Epilogue.delayManager.delayedPacket.offer(packet);
                            event.setCancelled(true);
                            this.delayFlag = true;
                            return;
                        }
                    }

                    if (this.mode.getValue() == 1) {
                        hasReceivedVelocity = true;
                        if (!mc.thePlayer.onGround) {
                            if (!polarAbsorbed) {
                                event.setCancelled(true);
                                polarAbsorbed = true;
                                return;
                            }
                        }
                    }

                    if (this.dbg.getValue()) {
                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sVelocity (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                                        Epilogue.clientName,
                                        mc.thePlayer.ticksExisted,
                                        (double) packet.getMotionX() / 8000.0,
                                        (double) packet.getMotionY() / 8000.0,
                                        (double) packet.getMotionZ() / 8000.0
                                )
                        );
                    }
                }
            } else if (!(event.getPacket() instanceof S27PacketExplosion)) {
                if (event.getPacket() instanceof S19PacketEntityStatus) {
                    S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
                    Entity entity = packet.getEntity(mc.theWorld);
                    if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
                        this.allowNext = false;
                    }
                }
            } else {
                S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
                if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
                    this.pendingExplosion = true;
                    if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                        event.setCancelled(true);
                    }
                    if (this.dbg.getValue()) {
                        ChatUtil.sendFormatted(
                                String.format(
                                        "%sExplosion (&otick: %d, x: %.2f, y: %.2f, z: %.2f&r)&r",
                                        Epilogue.clientName,
                                        mc.thePlayer.ticksExisted,
                                        mc.thePlayer.motionX + (double) packet.func_149149_c(),
                                        mc.thePlayer.motionY + (double) packet.func_149144_d(),
                                        mc.thePlayer.motionZ + (double) packet.func_149147_e()
                                )
                        );
                    }
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (hasReceivedVelocity && mc.thePlayer.onGround) {
                polarAbsorbed = false;
            }

            int maxTick = this.rotateTick.getValue();
            if (this.rotatoTickCounter > 0 && this.rotatoTickCounter <= maxTick) {
                if (this.rotatoTickCounter == 1) {
                    double deltaX = -this.knockbackX;
                    double deltaZ = -this.knockbackZ;
                    this.targetRotation = RotationUtil.getRotationsTo(deltaX, 0, deltaZ, event.getYaw(), event.getPitch());
                }
                if (this.targetRotation != null) {
                    event.setRotation(this.targetRotation[0], this.targetRotation[1], 2);
                    event.setPervRotation(this.targetRotation[0], 2);
                }
            }
        }
        if (event.getType() == EventType.POST) {
            if (this.delayFlag
                    && (
                    this.canDelay()
                            || this.isInLiquidOrWeb()
                            || Epilogue.delayManager.getDelay() >= (long) this.delayTicks.getValue()
            )) {
                Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
                this.delayFlag = false;
            }
            int maxTick = this.rotateTick.getValue();
            if (this.rotatoTickCounter > 0 && this.rotatoTickCounter <= maxTick) {
                this.rotatoTickCounter++;
                if (this.rotatoTickCounter > maxTick) {
                    this.rotatoTickCounter = 0;
                    this.targetRotation = null;
                    this.knockbackX = 0;
                    this.knockbackZ = 0;
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled() && this.rotatoTickCounter > 0 && this.rotatoTickCounter <= this.rotateTick.getValue()) {
            if (this.autoMove.getValue()) {
                mc.thePlayer.movementInput.moveForward = 1.0F;
            }
            if (this.targetRotation != null && RotationState.isActived() && RotationState.getPriority() == 2.0F && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.onDisabled();
    }

    @Override
    public void onDisabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.rotatoTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0;
        this.knockbackZ = 0;
        hasReceivedVelocity = false;
        polarAbsorbed = false;
    }

    @Override
    public String[] getSuffix() {
        boolean predictionMode = this.mode.getValue() == 1;
        return predictionMode && this.horizontal.getValue() == 100 && this.vertical.getValue() == 100
                ? new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())}
                : new String[]{
                ChatColors.formatColor(String.format(this.mode.getValue() == 3 ? "&m%d%%&r" : "%d%%", this.horizontal.getValue())),
                String.format("%d%%", this.vertical.getValue())
        };
    }
}