package epilogue.module.modules.combat;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import epilogue.enums.DelayModules;
import epilogue.mixin.IAccessorEntity;
import epilogue.util.MoveUtil;
import epilogue.util.RotationUtil;
import epilogue.value.values.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;

import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.world.World;
import epilogue.util.ChatUtil;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.*;

import epilogue.management.RotationState;
import epilogue.module.Module;
import net.minecraft.util.AxisAlignedBB;

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
    private int delayTicksLeft = 0;
    private int airDelayTicksLeft = 0;
    private boolean delayedVelocityActive = false;
    private int attackReduceTicksLeft = 0;
    private boolean attackReduceApplied = false;
    private boolean reduceActive = false;
    private int reduceVelocityTicks = 0;
    private int reduceOffGroundTicks = 0;
    private int reduceTicksSinceTeleport = 0;
    private boolean reduceReceiving = false;

    private EntityPlayer getNearestPlayerTarget() {
        if (mc.theWorld == null || mc.thePlayer == null) return null;
        EntityPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityPlayer o : mc.theWorld.playerEntities) {
            if (!(o instanceof EntityPlayer)) continue;
            if (o == mc.thePlayer || o.isDead) continue;
            double d = mc.thePlayer.getDistanceToEntity(o);
            if (d < bestDist) {
                bestDist = d;
                best = o;
            }
        }
        return best;
    }

    private boolean isInWeb(EntityPlayer player) {
        return player != null && ((IAccessorEntity) player).getIsInWeb();
    }

    private boolean isTargetInRaycastRange(Entity entity) {
        if (entity == null || mc.thePlayer == null) return false;
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        if (bb == null) return false;
        return RotationUtil.rayTrace(bb, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 3.0) != null;
    }

    public final ModeValue mode = new ModeValue("mode", 0, new String[]{"Vanilla", "JumpReset", "Reduce"});

    // Vanilla
    public final PercentValue horizontal = new PercentValue("Horizontal", 100, () -> this.mode.getValue() == 0);
    public final PercentValue vertical = new PercentValue("Vertical", 100, () -> this.mode.getValue() == 0);
    public final PercentValue explosionHorizontal = new PercentValue("Explosions Horizontal", 100, () -> this.mode.getValue() == 0);
    public final PercentValue explosionVertical = new PercentValue("Explosions Vertical", 100, () -> this.mode.getValue() == 0);
    public final PercentValue chance = new PercentValue("Change", 100);
    public final BooleanValue fakeCheck = new BooleanValue("Check Fake", true);

    // JumpReset
    public final BooleanValue airDelay = new BooleanValue("Air Delay", false, () -> this.mode.getValue() == 1);
    public final IntValue airDelayTicks = new IntValue("Air Delay Ticks", 3, 1, 20, () -> this.mode.getValue() == 1 && this.airDelay.getValue());

    // Mix
    public final BooleanValue mixDelay = new BooleanValue("Delay", true, () -> this.mode.getValue() == 2);
    public final IntValue mixDelayTicks = new IntValue("Delay Ticks", 1, 1, 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue());
    public final BooleanValue mixDelayOnlyInGround = new BooleanValue("Delay Only In Ground", true, () -> this.mode.getValue() == 2 && this.mixDelay.getValue());
    public final BooleanValue mixReduce = new BooleanValue("Reduce", false, () -> this.mode.getValue() == 2);
    public final BooleanValue mixJumpReset = new BooleanValue("Jump Reset", true, () -> this.mode.getValue() == 2);
    public final BooleanValue mixRotate = new BooleanValue("Rotate", false, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue());
    public final BooleanValue mixRotateOnlyInGround = new BooleanValue("Rotate Only In Ground", true, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());
    public final BooleanValue mixAutoMove = new BooleanValue("Auto Move", true, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());
    public final IntValue mixRotateTicks = new IntValue("Rotate Ticks", 3, 1, 20, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());

    public Velocity() {
        super("Velocity", false);
    }

    private boolean isMix() { return this.mode.getValue() == 2; }

    private void startRotate(double knockbackX, double knockbackZ) {
        endRotate();
        this.knockbackX = knockbackX;
        this.knockbackZ = knockbackZ;
        if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
            this.rotateTickCounter = 1;
            this.targetRotation = null;
        }
    }

    private void endRotate() {
        this.rotateTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
    }

    private void startDelayedVelocity(int ticks) {
        this.delayedVelocityActive = true;
        this.delayTicksLeft = Math.max(1, ticks);
    }

    private void queueDelayedVelocity(PacketEvent event, S12PacketEntityVelocity packet, int ticks) {
        Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
        Epilogue.delayManager.delayedPacket.offer(packet);
        event.setCancelled(true);
        this.startDelayedVelocity(ticks);
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            this.pendingExplosion = false;
            this.allowNext = true;
            this.endRotate();
            return;
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
                        if (this.mixRotateOnlyInGround.getValue() && mc.thePlayer.onGround) {
                            shouldRotate = true;
                        } else shouldRotate = !this.mixRotateOnlyInGround.getValue();
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
        if (!this.isEnabled() || mc.thePlayer == null) return;
        if (event.getType() != EventType.RECEIVE || event.isCancelled()) return;

        Packet<?> packet = event.getPacket();

        if (this.isMix() && this.mixReduce.getValue() && packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity vel = (S12PacketEntityVelocity) packet;
            if (vel.getEntityID() == mc.thePlayer.getEntityId()) {

                reduceReceiving = true;

                Entity target;
                Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
                if (aura != null && aura.isEnabled() && aura.getTarget() != null) {
                    target = aura.getTarget();
                } else {
                    target = getNearestPlayerTarget();
                }
                boolean rangeOk = (target == null) || (mc.thePlayer.getDistanceToEntity(target) > 3.2F);
                boolean isBlocking = aura != null && aura.autoBlock.getValue() != 3 && aura.autoBlock.getValue() != 4 ? (aura.isPlayerBlocking() || aura.blockingState) : aura != null && aura.isBlocking();
                boolean inBadPos = isInWeb(mc.thePlayer) || mc.thePlayer.isOnLadder() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava();
                if (reduceReceiving && reduceTicksSinceTeleport >= 3 && !inBadPos && !mc.thePlayer.isSwingInProgress && !isBlocking && rangeOk) {
                    reduceActive = true;
                    reduceVelocityTicks = 0;
                }
            }
        }

        if (this.isMix() && this.mixReduce.getValue() && reduceActive && packet instanceof S32PacketConfirmTransaction) {
            event.setCancelled(true);
            return;
        }

        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity vel = (S12PacketEntityVelocity) packet;
            if (vel.getEntityID() != mc.thePlayer.getEntityId()) return;

            if (this.isMix()) {
                if (this.mixDelay.getValue()) {
                    if (!this.mixDelayOnlyInGround.getValue() || mc.thePlayer.onGround) {
                        this.queueDelayedVelocity(event, vel, this.mixDelayTicks.getValue());
                        return;
                    }
                }

                return;
            }

            if (this.mode.getValue() == 1 && this.airDelay.getValue() && !mc.thePlayer.onGround) {
                Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                Epilogue.delayManager.delayedPacket.offer(vel);
                event.setCancelled(true);
                this.startDelayedVelocity(airDelayTicks.getValue());
                return;
            }
        }

        if (packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus p = (S19PacketEntityStatus) packet;
            World world = mc.theWorld;
            if (world != null) {
                Entity entity = p.getEntity(world);
                if (entity != null && entity.equals(mc.thePlayer) && p.getOpCode() == 2) {
                    this.allowNext = false;
                }
            }
        }

        if (packet instanceof S27PacketExplosion) {
            S27PacketExplosion p = (S27PacketExplosion) packet;
            if (p.func_149149_c() != 0.0F || p.func_149144_d() != 0.0F || p.func_149147_e() != 0.0F) {
                this.pendingExplosion = true;
                if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        if (this.isMix() && this.mixReduce.getValue()) {
            reduceReceiving = false;
            reduceTicksSinceTeleport++;
        }

        if (this.isMix() && this.mixReduce.getValue() && event.getType() == EventType.PRE) {
            if (!mc.thePlayer.onGround) {
                reduceOffGroundTicks++;
            } else {
                reduceOffGroundTicks = 0;
            }

            if (reduceActive) {
                reduceVelocityTicks++;
            }

            Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
            Entity target;
            if (aura != null && aura.isEnabled() && aura.getTarget() != null) {
                target = aura.getTarget();
            } else {
                target = getNearestPlayerTarget();
            }

            if (target != null &&
                    mc.thePlayer.isSwingInProgress &&
                    reduceVelocityTicks < 3 &&
                    !mc.thePlayer.onGround) {

                boolean inBadPos = isInWeb(mc.thePlayer) || mc.thePlayer.isOnLadder() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava();
                boolean inRaycast = isTargetInRaycastRange(target);
                boolean canReduce = !inBadPos && aura != null && aura.isEnabled() && inRaycast && reduceTicksSinceTeleport >= 3;
                if (canReduce) {
                    int ab = aura.autoBlock.getValue();
                    if (ab == 3 || ab == 4) {
                        boolean isBlocking = aura.isPlayerBlocking();
                        canReduce = !isBlocking;
                    }
                }

                if (canReduce) {
                    mc.thePlayer.swingItem();
                    mc.playerController.attackEntity(mc.thePlayer, target);
                    ChatUtil.sendRaw("[Velocity] AttackReduced!");
                }
            }

            boolean shouldReset = mc.thePlayer.onGround ||
                    mc.thePlayer.isSwingInProgress ||
                    (target != null && mc.thePlayer.getDistanceToEntity(target) <= 3.2F) ||
                    reduceOffGroundTicks > 20 ||
                    reduceTicksSinceTeleport < 3;

            if (shouldReset && reduceActive) {
                reduceActive = false;
            }
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

        if (this.isMix() && event.getType() == EventType.PRE) {

            int maxTick = this.mixRotateTicks.getValue();
            if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
                if (this.rotateTickCounter == 1) {
                    double deltaX = -this.knockbackX;
                    double deltaZ = -this.knockbackZ;
                    this.targetRotation = RotationUtil.getRotationsTo(deltaX, 0.0, deltaZ, event.getYaw(), event.getPitch());
                }
                if (this.targetRotation != null) {
                    event.setRotation(this.targetRotation[0], this.targetRotation[1], 2);
                    event.setPervRotation(this.targetRotation[0], 2);
                }
            }
        }

        if (this.isMix() && event.getType() == EventType.POST) {
            int maxTick = this.mixRotateTicks.getValue();
            if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
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
        if (!this.isEnabled() || mc.thePlayer == null) return;

        if (this.isMix()) {
            int maxTick = this.mixRotateTicks.getValue();
            if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
                if (this.mixAutoMove.getValue()) {
                    mc.thePlayer.movementInput.moveForward = 1.0F;
                }
                if (this.targetRotation != null && RotationState.isActived() && RotationState.getPriority() == 2.0F && MoveUtil.isForwardPressed()) {
                    Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
                    if (aura != null && aura.isEnabled() && aura.moveFix.getValue() == 2 && aura.rotations.getValue() != 3) {
                        MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
                    }
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
        this.delayTicksLeft = 0;
        this.airDelayTicksLeft = 0;
        this.delayedVelocityActive = false;
        this.attackReduceTicksLeft = 0;
        this.attackReduceApplied = false;
        this.endRotate();

        reduceActive = false;
        reduceVelocityTicks = 0;
        reduceOffGroundTicks = 0;
        reduceTicksSinceTeleport = 0;
        reduceReceiving = false;
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

        reduceActive = false;
        reduceTicksSinceTeleport = 0;
        reduceReceiving = false;
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString();
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}