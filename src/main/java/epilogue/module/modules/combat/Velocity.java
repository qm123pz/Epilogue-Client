package epilogue.module.modules.combat;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import epilogue.enums.DelayModules;
import epilogue.util.ItemUtil;
import epiloguemixinbridge.IAccessorEntity;
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
import java.util.Random;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();
    private static final DecimalFormat df = new DecimalFormat("0.000000000000");

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

    private boolean reduceActive = false;
    private int reduceVelocityTicks = 0;
    private int reduceOffGroundTicks = 0;
    private int reduceTicksSinceTeleport = 0;
    private boolean reduceReceiving = false;

    private boolean watchdogJump = false;
    private boolean watchdogActive = false;
    private boolean watchdogReceiving = false;
    private final ArrayList<Packet> watchdogPackets = new ArrayList<>();
    private int watchdogTicksSinceTeleport = 0;
    private int watchdogTicksSinceVelocity = 0;
    private int watchdogOffGroundTicks = 0;
    private boolean watchdogShouldJump = false;

    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Vanilla", "JumpReset", "Mix", "Watchdog"});

    public final PercentValue horizontal = new PercentValue("Horizontal", 100, () -> this.mode.getValue() == 0);
    public final PercentValue vertical = new PercentValue("Vertical", 100, () -> this.mode.getValue() == 0);
    public final PercentValue explosionHorizontal = new PercentValue("Explosion Horizontal", 100, () -> this.mode.getValue() == 0);
    public final PercentValue explosionVertical = new PercentValue("Explosion Vertical", 100, () -> this.mode.getValue() == 0);
    public final PercentValue chance = new PercentValue("Chance", 100);
    public final BooleanValue fakeCheck = new BooleanValue("Check Fake", true);

    public final BooleanValue airDelay = new BooleanValue("Air Delay", false, () -> this.mode.getValue() == 1);
    public final IntValue airDelayTicks = new IntValue("Air Delay Ticks", 3, 1, 20, () -> this.mode.getValue() == 1 && this.airDelay.getValue());

    public final BooleanValue mixDelay = new BooleanValue("Delay", true, () -> this.mode.getValue() == 2);
    public final IntValue mixDelayTicks = new IntValue("Delay Ticks", 1, 1, 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue());
    public final BooleanValue mixDelayOnlyInGround = new BooleanValue("Delay Only In Ground", true, () -> this.mode.getValue() == 2 && this.mixDelay.getValue());
    public final BooleanValue mixReduce = new BooleanValue("Reduce", false, () -> this.mode.getValue() == 2);
    public final BooleanValue mixJumpReset = new BooleanValue("Jump Reset", true, () -> this.mode.getValue() == 2);
    public final BooleanValue mixRotate = new BooleanValue("Rotate", false, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue());
    public final BooleanValue mixRotateOnlyInGround = new BooleanValue("Rotate Only In Ground", true, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());
    public final BooleanValue mixAutoMove = new BooleanValue("Auto Move", true, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());
    public final IntValue mixRotateTicks = new IntValue("Rotate Ticks", 3, 1, 20, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());

    public final PercentValue watchdogChance = new PercentValue("Chance", 100, () -> this.mode.getValue() == 3);
    public final BooleanValue watchdogLegitTiming = new BooleanValue("Legit Timing", false, () -> this.mode.getValue() == 3);

    public Velocity() {
        super("Velocity", false);
    }

    @EventTarget
    public void onPreMotion(PreMotionEvent event) {
        if (!this.isEnabled() || !this.isWatchdog()) return;

        watchdogShouldJump = false;
    }

    @EventTarget
    public void onMove(MoveEvent event) {
        if (!this.isEnabled() || !this.isWatchdog()) return;

        if (watchdogShouldJump) {
            event.setJump(true);
        }
    }

    private boolean isAuraBlocking() {
        try {
            Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);

            if (aura == null || !aura.isEnabled()) {
                return false;
            }

            int autoBlockValue = aura.autoBlock.getValue();

            if (autoBlockValue != 3 && autoBlockValue != 4) {
                return aura.isPlayerBlocking() || aura.blockingState;
            } else {
                return aura.isBlocking();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void processWatchdogPackets() {
        if (mc.getNetHandler() == null || watchdogPackets.isEmpty()) return;

        for (Packet packet : watchdogPackets) {
            try {
                packet.processPacket(mc.getNetHandler());
            } catch (Exception e) {
                try {
                    if (packet instanceof S12PacketEntityVelocity) {
                        packet.processPacket(mc.getNetHandler());
                    } else if (packet instanceof S08PacketPlayerPosLook) {
                        packet.processPacket(mc.getNetHandler());
                    } else if (packet instanceof S19PacketEntityStatus) {
                        packet.processPacket(mc.getNetHandler());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        watchdogPackets.clear();
    }

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

    private Entity getNearTarget() {
        try {
            Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
            if (aura != null && aura.isEnabled()) {
                Entity target = aura.getTarget();
                if (target != null) {
                    return target;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getNearestPlayerTarget();
    }

    private boolean isInWeb(Entity entity) {
        if (entity == null) return false;
        try {
            return ((IAccessorEntity) entity).getIsInWeb();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTargetInRaycastRange(Entity entity) {
        if (entity == null || mc.thePlayer == null) return false;
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        if (bb == null) return false;
        return RotationUtil.rayTrace(bb, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 3.0) != null;
    }

    private boolean watchdogIsTargetInRaycastRange(Entity target, double range) {
        if (target == null || mc.thePlayer == null) {
            return false;
        }
        AxisAlignedBB bb = target.getEntityBoundingBox();
        if (bb == null) return false;
        return RotationUtil.rayTrace(bb, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, (float)range) != null;
    }

    private boolean isMoving() {
        return mc.thePlayer != null && (Math.abs(mc.thePlayer.motionX) > 0.01 || Math.abs(mc.thePlayer.motionZ) > 0.01 ||
                mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0);
    }

    private boolean checks() {
        if (mc.thePlayer == null) return false;
        return isInWeb(mc.thePlayer) || mc.thePlayer.isInLava() || mc.thePlayer.isBurning() || mc.thePlayer.isInWater();
    }

    private boolean isInBadPosition() {
        if (mc.thePlayer == null) return false;
        return isInWeb(mc.thePlayer) || mc.thePlayer.isOnLadder() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava();
    }

    private boolean isVanilla() { return this.mode.getValue() == 0; }
    private boolean isJumpReset() { return this.mode.getValue() == 1; }
    private boolean isReduce() { return this.mode.getValue() == 2; }
    private boolean isWatchdog() { return this.mode.getValue() == 3; }

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
                    boolean doJumpReset = (this.mode.getValue() == 1) || (this.isReduce() && this.mixJumpReset.getValue());
                    boolean canDoJumpReset = doJumpReset && event.getY() > 0.0;

                    if (this.isReduce() && this.mixJumpReset.getValue() && this.mixRotate.getValue() && canDoJumpReset) {
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

        if (this.isWatchdog() && packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity vel = (S12PacketEntityVelocity) packet;
            if (vel.getEntityID() == mc.thePlayer.getEntityId()) {
                if (random.nextInt(100) > this.watchdogChance.getValue()) {
                    return;
                }

                Entity target = this.getNearTarget();

                if (watchdogReceiving ||
                        watchdogTicksSinceTeleport < 3 ||
                        isInWeb(mc.thePlayer) ||
                        mc.thePlayer.isSwingInProgress ||
                        isAuraBlocking() ||
                        (target != null && mc.thePlayer.getDistanceToEntity(target) <= 3.2)) {
                    return;
                }

                watchdogPackets.add(vel);
                watchdogActive = true;
                event.setCancelled(true);

                watchdogTicksSinceVelocity = 0;

                double verticalMotion = (double)vel.getMotionY() / 8000.0;
                if (verticalMotion > 0) {
                    watchdogShouldJump = true;
                }
            }
        }

        if (this.isWatchdog() && packet instanceof S08PacketPlayerPosLook) {
            S08PacketPlayerPosLook posPacket = (S08PacketPlayerPosLook) packet;
            if (watchdogActive) {
                watchdogPackets.add(posPacket);
                event.setCancelled(true);
            }
        }

        if (this.isWatchdog() && packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus status = (S19PacketEntityStatus) packet;
            if (status.getEntity(mc.theWorld) == mc.thePlayer && status.getOpCode() == 2) {
                watchdogTicksSinceVelocity = 0;
            }
        }

        if (this.isReduce() && this.mixReduce.getValue() && packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity vel = (S12PacketEntityVelocity) packet;
            if (vel.getEntityID() == mc.thePlayer.getEntityId()) {
                reduceReceiving = true;

                Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
                if (aura != null && aura.isEnabled()) {
                    aura.getTarget();
                }

                boolean inBadPos = isInBadPosition();
                boolean isBlocking = aura != null && aura.autoBlock.getValue() != 3 && aura.autoBlock.getValue() != 4 ?
                        (aura.isPlayerBlocking() || aura.blockingState) :
                        aura != null && aura.isBlocking();

                boolean reduceCondition = reduceTicksSinceTeleport >= 3 && !inBadPos && !isBlocking;

                if (reduceCondition) {
                    reduceActive = true;
                    reduceVelocityTicks = 0;
                    ChatUtil.sendRaw("Reduce Successfully");
                }
            }
        }

        if (this.isReduce() && this.mixReduce.getValue() && reduceActive && packet instanceof S32PacketConfirmTransaction) {
            event.setCancelled(true);
            return;
        }

        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity vel = (S12PacketEntityVelocity) packet;
            if (vel.getEntityID() != mc.thePlayer.getEntityId()) return;

            if (this.isReduce()) {
                if (this.mixDelay.getValue()) {
                    if (!this.mixDelayOnlyInGround.getValue() || mc.thePlayer.onGround) {
                        this.queueDelayedVelocity(event, vel, this.mixDelayTicks.getValue());
                        return;
                    }
                }
                return;
            }

            if (this.isJumpReset() && this.airDelay.getValue() && !mc.thePlayer.onGround) {
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

        if (this.isWatchdog()) {
            watchdogTicksSinceTeleport++;
            watchdogTicksSinceVelocity++;

            if (!mc.thePlayer.onGround) {
                watchdogOffGroundTicks++;
            } else {
                watchdogOffGroundTicks = 0;
            }

            Entity target = this.getNearTarget();

            if (this.watchdogIsTargetInRaycastRange(target, 3.0) &&
                    mc.thePlayer.isSwingInProgress &&
                    watchdogTicksSinceVelocity < 3 &&
                    !mc.thePlayer.onGround &&
                    watchdogTicksSinceTeleport > 3) {

                Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);

                if (aura != null && aura.isEnabled()) {

                    if (mc.getNetHandler() != null) {
                        boolean canAttack;
                        if (aura.autoBlock.getValue() == 3 || aura.autoBlock.getValue() == 4 || aura.autoBlock.getValue() == 5) {
                            canAttack = !(mc.thePlayer.isUsingItem() || aura.blockingState) && ItemUtil.isHoldingSword();
                        } else {
                            canAttack = true;
                        }
                        if (canAttack){
                            mc.getNetHandler().addToSendQueue(new net.minecraft.network.play.client.C0APacketAnimation());
                        }
                    }

                    if (mc.playerController != null) {
                        mc.playerController.attackEntity(mc.thePlayer, target);
                    }

                    String motionXStr = df.format(mc.thePlayer.motionX);
                    String motionZStr = df.format(mc.thePlayer.motionZ);
                    ChatUtil.sendRaw("§bSB-Coffee §fMotion X: " + motionXStr + " | Motion Z: " + motionZStr);
                }
            }

            if (watchdogActive && !watchdogReceiving && !watchdogPackets.isEmpty()) {
                boolean shouldRelease = false;

                if (mc.thePlayer.onGround) {
                    shouldRelease = true;
                }

                if (watchdogTicksSinceTeleport < 3) {
                    shouldRelease = true;
                }

                if (mc.thePlayer.isSwingInProgress) {
                    shouldRelease = true;
                }

                if (target != null && mc.thePlayer.getDistanceToEntity(target) <= 3.2) {
                    shouldRelease = true;
                }

                if (watchdogOffGroundTicks > 20) {
                    shouldRelease = true;
                }

                if (shouldRelease) {
                    watchdogReceiving = true;
                    processWatchdogPackets();
                    watchdogActive = false;
                    watchdogReceiving = false;
                }
            }

            if (this.watchdogLegitTiming.getValue() && watchdogActive) {
                try {
                    Thread.sleep(1 + random.nextInt(3));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (this.isReduce() && this.mixReduce.getValue()) {
            reduceReceiving = false;
            reduceTicksSinceTeleport++;
        }

        if (this.isReduce() && this.mixReduce.getValue() && event.getType() == EventType.PRE) {
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

                boolean inBadPos = isInBadPosition();
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
                    ChatUtil.sendRaw("Attack");
                    mc.thePlayer.motionX *= 0.6;
                    mc.thePlayer.motionZ *= 0.6;
                    mc.thePlayer.setSprinting(false);
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

        if (this.isReduce() && event.getType() == EventType.PRE) {
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

        if (event.getType() == EventType.POST) {
            if (this.isReduce()) {
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
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        if (this.isReduce()) {
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

        if (this.isWatchdog() && watchdogJump && mc.thePlayer.onGround) {
            mc.thePlayer.movementInput.jump = true;
            watchdogJump = false;
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
        this.endRotate();

        reduceActive = false;
        reduceVelocityTicks = 0;
        reduceOffGroundTicks = 0;
        reduceTicksSinceTeleport = 0;
        reduceReceiving = false;

        watchdogJump = false;
        watchdogActive = false;
        watchdogReceiving = false;
        watchdogPackets.clear();
        watchdogTicksSinceTeleport = 0;
        watchdogTicksSinceVelocity = 0;
        watchdogOffGroundTicks = 0;
        watchdogShouldJump = false;
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
        this.endRotate();

        if (Epilogue.delayManager.getDelayModule() == DelayModules.VELOCITY) {
            Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
        }
        Epilogue.delayManager.delayedPacket.clear();

        reduceActive = false;
        reduceTicksSinceTeleport = 0;
        reduceReceiving = false;

        if (!watchdogPackets.isEmpty()) {
            processWatchdogPackets();
        }
        watchdogActive = false;
        watchdogReceiving = false;
        watchdogJump = false;
        watchdogShouldJump = false;
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString();
        if (this.isWatchdog()) {
            String chanceStr = this.watchdogChance.getValue() + "%";
            if (this.watchdogLegitTiming.getValue()) {
                return new String[]{"Watchdog", chanceStr, "Legit"};
            }
            return new String[]{"Watchdog", chanceStr};
        }
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}