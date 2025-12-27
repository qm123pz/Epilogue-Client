package epilogue.module.modules.combat;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import epilogue.value.values.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.world.World;
import epilogue.enums.DelayModules;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.*;
import epilogue.management.RotationState;
import epilogue.module.Module;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int chanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean jumpFlag = false;
    private int rotateTickCounter = 0;
    private float[] targetRotation = null;
    private double knockbackX = 0.0;
    private double knockbackZ = 0.0;
    private boolean airRotateActive = false;
    private int advancedTimeWindowTicks = 0;
    private int delayTicksLeft = 0;
    private int airDelayTicksLeft = 0;
    private boolean delayedVelocityActive = false;
    private int attackReduceTicksLeft = 0;
    private boolean attackReduceApplied = false;

    public final ModeValue mode = new ModeValue("mode", 0, new String[]{"Vanilla", "JumpReset", "Mix"});
    public final PercentValue horizontal = new PercentValue("Horizontal", 100, () -> this.mode.getValue() == 0);
    public final PercentValue vertical = new PercentValue("Vertical", 100, () -> this.mode.getValue() == 0);
    public final PercentValue explosionHorizontal = new PercentValue("Explosions Horizontal", 100, () -> this.mode.getValue() == 0);
    public final PercentValue explosionVertical = new PercentValue("Explosions Vertical", 100, () -> this.mode.getValue() == 0);
    public final PercentValue chance = new PercentValue("Change", 100);
    public final BooleanValue fakeCheck = new BooleanValue("Check Fake", true);
    //jump reset
    public final BooleanValue airDelay = new BooleanValue("Air Delay", false, () -> this.mode.getValue() == 1);
    public final IntValue airDelayTicks = new IntValue("Air Delay Ticks", 3, 1 , 20, () -> this.mode.getValue() == 1 && this.airDelay.getValue());
    //mix
    public final BooleanValue mixDelay = new BooleanValue("Delay", false, () -> this.mode.getValue() == 2);
    public final IntValue mixDelayTicks = new IntValue("Delay Ticks", 3, 1 , 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue());
    public final BooleanValue mixDelayOnlyInGround = new BooleanValue("Delay Only In Ground", true, () -> this.mode.getValue() == 2 && this.mixDelay.getValue());
    //smart air delay
    public final BooleanValue mixSmartAirDelay = new BooleanValue("Smart Air Delay", true, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue());
    public final IntValue test1 = new IntValue("air1", 4, 1 , 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final IntValue test2 = new IntValue("air2", 3, 1 , 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final IntValue test3 = new IntValue("air3", 3, 1 , 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final IntValue test4 = new IntValue("air4", 5, 1 , 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final IntValue test5 = new IntValue("air5", 5, 1 , 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final IntValue test6 = new IntValue("air6", 5, 1 , 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final IntValue test7 = new IntValue("air7", 5, 1 , 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final IntValue test8 = new IntValue("air8", 5, 1 , 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final IntValue test9 = new IntValue("air9", 1, 1 , 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final FloatValue hz1 = new FloatValue("hz1", 0.1F, 0.01F, 1.0F, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final FloatValue hz2 = new FloatValue("hz2", 0.18F, 0.01F, 1.0F, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final FloatValue hz3 = new FloatValue("hz3", 0.22F, 0.01F, 1.0F, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final FloatValue hz4 = new FloatValue("hz4", 0.32F, 0.01F, 1.0F, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final FloatValue hz5 = new FloatValue("hz5", 0.40F, 0.01F, 1.0F, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final FloatValue hz6 = new FloatValue("hz6", 0.60F, 0.01F, 1.0F, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final FloatValue hz7 = new FloatValue("hz7", 0.78F, 0.01F, 1.0F, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final FloatValue hz8 = new FloatValue("hz8", 0.83F, 0.01F, 1.0F, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    public final FloatValue hz9 = new FloatValue("hz9", 1.00F, 0.01F, 1.0F, () -> this.mode.getValue() == 2 && this.mixDelay.getValue() && this.mixDelayOnlyInGround.getValue() && this.mixSmartAirDelay.getValue());
    //smart rotate jr
    public final BooleanValue mixJumpReset = new BooleanValue("Jump Reset", true, () -> this.mode.getValue() == 2);
    public final BooleanValue mixRotate = new BooleanValue("Rotate", false, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue());
    public final BooleanValue mixRotateOnlyInAir = new BooleanValue("Rotate Only In Air", false, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue() && !this.mixRotateOnlyInGround.getValue());
    public final BooleanValue mixRotateOnlyInGround = new BooleanValue("Rotate Only In Ground", true, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue() && !this.mixRotateOnlyInAir.getValue());
    public final BooleanValue mixAutoMove = new BooleanValue("Auto Move", true, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());
    public final IntValue mixRotateTicks = new IntValue("Rotate Ticks", 3, 1, 20, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());
    //attack reduce(hit select)
    public final BooleanValue mixAttackReduce = new BooleanValue("Attack Reduce(Hit Select)", false, () -> this.mode.getValue() == 2);

    public Velocity() {
        super("Velocity", false);
    }

    private boolean isMix() {
        return this.mode.getValue() == 2;
    }

    private void startRotate(double knockbackX, double knockbackZ) {
        this.endRotate();
        this.knockbackX = knockbackX;
        this.knockbackZ = knockbackZ;
        if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
            this.rotateTickCounter = 1;
            this.targetRotation = null;
            this.airRotateActive = true;
        }
    }

    private boolean isAuraTargetHitByRay() {
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        if (aura == null || !aura.isEnabled()) {
            return true;
        }
        if (aura.target == null || aura.target.getBox() == null) {
            return true;
        }
        return epilogue.util.RotationUtil.rayTrace(aura.target.getBox(), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, aura.attackRange.getValue()) != null;
    }

    private void updateRotateGates() {
        if (!this.airRotateActive || this.rotateTickCounter <= 0 || this.rotateTickCounter > this.mixRotateTicks.getValue()) {
            return;
        }
    }

    private void endRotate() {
        if (!this.airRotateActive && this.rotateTickCounter <= 0) {
            return;
        }

        this.rotateTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
        this.airRotateActive = false;
    }

    private void startDelayedVelocity(int ticks) {
        this.delayedVelocityActive = true;
        this.delayTicksLeft = Math.max(1, ticks);
    }

    private void queueDelayedVelocity(PacketEvent event, S12PacketEntityVelocity packet, int ticks) {
        Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
        Epilogue.delayManager.delayedPacket.offer((Packet<INetHandlerPlayClient>) (Packet<?>) packet);
        event.setCancelled(true);
        this.startDelayedVelocity(ticks);
    }

    private int getMixSmartAirDelayTicks(S12PacketEntityVelocity packet) {
        double mx = Math.abs(packet.getMotionX() / 8000.0);
        double mz = Math.abs(packet.getMotionZ() / 8000.0);
        double hz = Math.sqrt(mx * mx + mz * mz);

        if (hz >= this.hz9.getValue()) {
            return this.test9.getValue();
        }
        if (hz >= this.hz8.getValue()) {
            return this.test8.getValue();
        }
        if (hz >= this.hz7.getValue()) {
            return this.test7.getValue();
        }
        if (hz >= this.hz6.getValue()) {
            return this.test6.getValue();
        }
        if (hz >= this.hz5.getValue()) {
            return this.test5.getValue();
        }
        if (hz >= this.hz4.getValue()) {
            return this.test4.getValue();
        }
        if (hz >= this.hz3.getValue()) {
            return this.test3.getValue();
        }
        if (hz >= this.hz2.getValue()) {
            return this.test2.getValue();
        }
        if (hz >= this.hz1.getValue()) {
            return this.test1.getValue();
        }
        return 1;
    }

    private void startAttackReduce() {
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        if (aura == null || !aura.isEnabled()) {
            return;
        }
        if (this.attackReduceTicksLeft <= 0) {
        }
        this.attackReduceTicksLeft = 1;
        Aura.attackBlocked = true;
        Aura.swingBlocked = true;
        this.attackReduceApplied = true;
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            this.pendingExplosion = false;
            this.allowNext = true;
            this.endRotate();
            return;
        }

        if (this.isMix() && this.mixAttackReduce.getValue() && event.getY() > 0.0) {
            this.startAttackReduce();
        }

        if (!this.allowNext || !this.fakeCheck.getValue()) {
            this.allowNext = true;
            if (this.pendingExplosion) {
                this.pendingExplosion = false;
                this.handleExplosion(event);
            } else {
                this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
                if (this.chanceCounter >= 100) {
                    boolean doJumpReset = (this.mode.getValue() == 1) || (this.isMix() && this.mixJumpReset.getValue());
                    boolean canDoJumpReset = doJumpReset && event.getY() > 0.0;

                    if (this.isMix() && this.mixJumpReset.getValue() && this.mixRotate.getValue() && canDoJumpReset) {
                        boolean shouldRotate;
                        if (this.mixRotateOnlyInAir.getValue()) {
                            shouldRotate = !mc.thePlayer.onGround;
                        } else if (this.mixRotateOnlyInGround.getValue()) {
                            shouldRotate = mc.thePlayer.onGround;
                        } else {
                            shouldRotate = true;
                        }
                        if (shouldRotate) {
                            this.startRotate(event.getX(), event.getZ());
                        }
                    }

                    this.jumpFlag = canDoJumpReset;
                    this.applyVanilla(event);
                    this.chanceCounter = 0;
                }
            }
        }
    }

    private void applyVanilla(KnockbackEvent event) {
        if (this.horizontal.getValue() > 0) {
            event.setX(event.getX() * this.horizontal.getValue() / 100.0);
            event.setZ(event.getZ() * this.horizontal.getValue() / 100.0);
        } else {
            event.setX(mc.thePlayer.motionX);
            event.setZ(mc.thePlayer.motionZ);
        }
        if (this.vertical.getValue() > 0) {
            event.setY(event.getY() * this.vertical.getValue() / 100.0);
        } else {
            event.setY(mc.thePlayer.motionY);
        }
    }

    private void handleExplosion(KnockbackEvent event) {
        if (this.explosionHorizontal.getValue() > 0) {
            event.setX(event.getX() * this.explosionHorizontal.getValue() / 100.0);
            event.setZ(event.getZ() * this.explosionHorizontal.getValue() / 100.0);
        } else {
            event.setX(mc.thePlayer.motionX);
            event.setZ(mc.thePlayer.motionZ);
        }
        if (this.explosionVertical.getValue() > 0) {
            event.setY(event.getY() * this.explosionVertical.getValue() / 100.0);
        } else {
            event.setY(mc.thePlayer.motionY);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (event.getType() != EventType.RECEIVE || event.isCancelled()) {
            return;
        }

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() != mc.thePlayer.getEntityId()) {
                return;
            }

            if (this.isMix()) {
                boolean handled = false;

                if (this.mixDelay.getValue() && this.mixSmartAirDelay.getValue() && !mc.thePlayer.onGround) {
                    int ticks = this.getMixSmartAirDelayTicks(packet);
                    this.queueDelayedVelocity(event, packet, ticks);
                    return;
                }

                if (!handled && this.mixDelay.getValue()) {
                    if (!this.mixDelayOnlyInGround.getValue() || mc.thePlayer.onGround) {
                        this.queueDelayedVelocity(event, packet, this.mixDelayTicks.getValue());
                        return;
                    }
                }

                return;
            }

            if (this.mode.getValue() == 1 && this.airDelay.getValue() && !mc.thePlayer.onGround) {
                Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                Epilogue.delayManager.delayedPacket.offer((Packet<INetHandlerPlayClient>) (Packet<?>) packet);
                event.setCancelled(true);
                this.startDelayedVelocity(airDelayTicks.getValue());
                return;
            }
            return;
        }

        if (event.getPacket() instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
            World world = mc.theWorld;
            if (world != null) {
                Entity entity = packet.getEntity(world);
                if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
                    this.allowNext = false;
                }
            }
            return;
        }

        if (event.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
            if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
                this.pendingExplosion = true;
                if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer != null && mc.thePlayer.onGround) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (event.getType() == EventType.POST) {
            if (this.attackReduceTicksLeft > 0) {
                this.attackReduceTicksLeft--;
                if (this.attackReduceTicksLeft <= 0) {
                    if (this.attackReduceApplied) {
                        Aura.attackBlocked = false;
                        Aura.swingBlocked = false;
                    }
                    this.attackReduceApplied = false;
                }
            }
        }

        if (!this.isMix()) {
            return;
        }

        if (event.getType() == EventType.PRE) {
            if (this.advancedTimeWindowTicks > 0) {
                this.advancedTimeWindowTicks--;
            }
            if (mc.thePlayer.onGround && mc.gameSettings != null && mc.gameSettings.keyBindJump != null && mc.gameSettings.keyBindJump.isKeyDown()) {
                this.advancedTimeWindowTicks = 12;
            }
            int maxTick = this.mixRotateTicks.getValue();
            if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
                if (this.rotateTickCounter == 1) {
                    double deltaX = -this.knockbackX;
                    double deltaZ = -this.knockbackZ;
                    this.targetRotation = epilogue.util.RotationUtil.getRotationsTo(deltaX, 0.0, deltaZ, event.getYaw(), event.getPitch());
                }
                if (this.targetRotation != null) {
                    event.setRotation(this.targetRotation[0], this.targetRotation[1], 2);
                    event.setPervRotation(this.targetRotation[0], 2);
                }
            }
            return;
        }

        if (event.getType() == EventType.POST) {
            int maxTick = this.mixRotateTicks.getValue();
            if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
                this.updateRotateGates();
                this.rotateTickCounter++;
                if (this.rotateTickCounter > maxTick) {
                    this.endRotate();
                }
            }

            if (this.delayedVelocityActive) {
                if (this.airDelayTicksLeft > 0) {
                    this.airDelayTicksLeft--;
                    if (this.airDelayTicksLeft <= 0) {
                        Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
                        this.delayedVelocityActive = false;
                    }
                } else if (this.delayTicksLeft > 0) {
                    this.delayTicksLeft--;
                    if (this.delayTicksLeft <= 0) {
                        Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
                        this.delayedVelocityActive = false;
                    }
                } else {
                    this.delayedVelocityActive = false;
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || !this.isMix()) {
            return;
        }
        int maxTick = this.mixRotateTicks.getValue();
        if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
            if (this.mixAutoMove.getValue()) {
                mc.thePlayer.movementInput.moveForward = 1.0F;
            }
            if (this.targetRotation != null && RotationState.isActived() && RotationState.getPriority() == 2.0F && epilogue.util.MoveUtil.isForwardPressed()) {
                Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
                if (aura != null && aura.isEnabled() && aura.moveFix.getValue() == 2 && aura.rotations.getValue() != 3) {
                    epilogue.util.MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.jumpFlag = false;
        this.rotateTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
        this.airRotateActive = false;
        this.advancedTimeWindowTicks = 0;
        this.delayTicksLeft = 0;
        this.airDelayTicksLeft = 0;
        this.delayedVelocityActive = false;
        this.attackReduceTicksLeft = 0;
        this.attackReduceApplied = false;
        this.endRotate();
    }

    @Override
    public void onDisabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.jumpFlag = false;
        this.rotateTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
        this.airRotateActive = false;
        this.advancedTimeWindowTicks = 0;
        this.delayTicksLeft = 0;
        this.airDelayTicksLeft = 0;
        this.delayedVelocityActive = false;
        if (this.attackReduceTicksLeft > 0) {
            if (this.attackReduceApplied) {
                Aura.attackBlocked = false;
                Aura.swingBlocked = false;
            }
        }
        this.attackReduceTicksLeft = 0;
        this.attackReduceApplied = false;
        this.endRotate();

        if (Epilogue.delayManager.getDelayModule() == DelayModules.VELOCITY) {
            Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
        }
        Epilogue.delayManager.delayedPacket.clear();
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString();
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}