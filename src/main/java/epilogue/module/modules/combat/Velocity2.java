package epilogue.module.modules.combat;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import epilogue.enums.BlinkModules;
import epilogue.enums.DelayModules;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.*;
import epilogue.mixin.IAccessorEntity;
import epilogue.module.Module;
import epilogue.util.ChatUtil;
import epilogue.util.RotationUtil;
import epilogue.value.values.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Velocity2 extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private int rotateTickCounter = 0;
    private float[] targetRotation = null;
    private double knockbackX = 0.0;
    private double knockbackZ = 0.0;
    private boolean jumpFlag = false;

    private int attackCounter = 0;
    private boolean attackReduceApplied = false;
    private int attackReduceTicksLeft = 0;
    private boolean canAttackReduce = false;

    private final Deque<double[]> recentKnockbacks = new LinkedList<>();

    private boolean buffer3Active = false;
    private final Queue<S12PacketEntityVelocity> bufferPacketQueue = new LinkedList<>();
    private int bufferRemainingTicks = 0;
    private boolean bufferActive = false;

    private boolean delayedVelocityActive = false;
    private int delayTicksLeft = 0;

    private boolean attackReduce3Active = false;
    private int attackReduce3TicksLeft = 0;

    private boolean hasReceivedVelocity = false;
    private boolean fallDamage = false;

    private final Random random = new Random();
    private int hitsCount = 0;
    private int ticksCount = 0;
    private boolean autoReduceActive = false;

    public final ModeValue mode = new ModeValue("mode", 0, new String[]{"Hypixel"});

    public final BooleanValue rotate = new BooleanValue("Rotate", true);
    public final IntValue rotateTick = new IntValue("RotateTick", 3, 1, 12, () -> this.rotate.getValue());
    public final BooleanValue autoMove = new BooleanValue("AutoMove", true, () -> this.rotate.getValue());

    public final BooleanValue airRotate = new BooleanValue("AirRotate", false, () -> this.rotate.getValue());
    public final IntValue airRotateTicks = new IntValue("AirRotateTicks", 2, 1, 10, () -> this.rotate.getValue() && this.airRotate.getValue());

    public final BooleanValue jumpReset = new BooleanValue("JumpReset", true, () -> this.mode.getValue() == 0);

    public final BooleanValue buffer = new BooleanValue("Buffer", true);
    public final BooleanValue buffer2 = new BooleanValue("Buffer2", false);
    public final BooleanValue buffer3 = new BooleanValue("Buffer3", false);
    public final IntValue bufferTicks = new IntValue("BufferTicks", 1, 1, 20, () -> this.buffer.getValue() || this.buffer2.getValue() || this.buffer3.getValue());

    public final BooleanValue smartAirDelay = new BooleanValue("Smart Air Delay", false);

    public final FloatValue airLess1 = new FloatValue("airLess1", 0.1f, 0.01f, 1.0f, () -> this.smartAirDelay.getValue());
    public final FloatValue airLess2 = new FloatValue("airLess2", 0.18f, 0.01f, 1.0f, () -> this.smartAirDelay.getValue());
    public final FloatValue airLess3 = new FloatValue("airLess3", 0.22f, 0.01f, 1.0f, () -> this.smartAirDelay.getValue());
    public final FloatValue airLess4 = new FloatValue("airLess4", 0.32f, 0.01f, 1.0f, () -> this.smartAirDelay.getValue());
    public final FloatValue airLess5 = new FloatValue("airLess5", 0.4f, 0.01f, 1.0f, () -> this.smartAirDelay.getValue());
    public final FloatValue airLess6 = new FloatValue("airLess6", 0.6f, 0.01f, 1.0f, () -> this.smartAirDelay.getValue());
    public final FloatValue airLess7 = new FloatValue("airLess7", 0.78f, 0.01f, 1.0f, () -> this.smartAirDelay.getValue());
    public final FloatValue airLess8 = new FloatValue("airLess8", 0.83f, 0.01f, 1.0f, () -> this.smartAirDelay.getValue());
    public final FloatValue airLess9 = new FloatValue("airLess9", 1.0f, 0.01f, 1.0f, () -> this.smartAirDelay.getValue());

    public final IntValue delayStep1 = new IntValue("delayStep1", 4, 1, 20, () -> this.smartAirDelay.getValue());
    public final IntValue delayStep2 = new IntValue("delayStep2", 3, 1, 20, () -> this.smartAirDelay.getValue());
    public final IntValue delayStep3 = new IntValue("delayStep3", 3, 1, 20, () -> this.smartAirDelay.getValue());
    public final IntValue delayStep4 = new IntValue("delayStep4", 5, 1, 20, () -> this.smartAirDelay.getValue());
    public final IntValue delayStep5 = new IntValue("delayStep5", 5, 1, 20, () -> this.smartAirDelay.getValue());
    public final IntValue delayStep6 = new IntValue("delayStep6", 5, 1, 20, () -> this.smartAirDelay.getValue());
    public final IntValue delayStep7 = new IntValue("delayStep7", 5, 1, 20, () -> this.smartAirDelay.getValue());
    public final IntValue delayStep8 = new IntValue("delayStep8", 5, 1, 20, () -> this.smartAirDelay.getValue());
    public final IntValue delayStep9 = new IntValue("delayStep9", 1, 1, 20, () -> this.smartAirDelay.getValue());

    public final BooleanValue attackReduce3 = new BooleanValue("AttackReduce3", false);
    public final BooleanValue sprintReset3 = new BooleanValue("SprintReset3", true, () -> this.attackReduce3.getValue());
    public final FloatValue reduce3Factor = new FloatValue("Reduce3-Factor", 0.6f, 0.1f, 1.0f, () -> this.attackReduce3.getValue());

    public final BooleanValue autoReduce = new BooleanValue("AutoReduce", false);
    public final FloatValue reduceRange = new FloatValue("Reduce Range", 3.0f, 0.5f, 6.0f, () -> this.autoReduce.getValue());
    public final PercentValue reduceJumpChance = new PercentValue("Reduce Jump Chance", 100, () -> this.autoReduce.getValue());
    public final IntValue reduceHitsUntilJump = new IntValue("Hits Until Jump", 2, 1, 10, () -> this.autoReduce.getValue());
    public final IntValue reduceTicksUntilJump = new IntValue("Ticks Until Jump", 2, 1, 20, () -> this.autoReduce.getValue());
    public final BooleanValue reduceDebug = new BooleanValue("Reduce Debug", false, () -> this.autoReduce.getValue());

    public final BooleanValue reduceMode = new BooleanValue("Reduce", true);
    public final ModeValue reduceType = new ModeValue("Reduce-Type", 0, new String[]{"Attackreduce", "Reduce2"}, () -> this.reduceMode.getValue());
    public final FloatValue reduceFactor = new FloatValue("Reduce-Factor", 0.6f, 0.1f, 1.0f, () -> this.reduceMode.getValue());
    public final IntValue attackTick = new IntValue("AttackTick", 4, 1, 10, () -> this.reduceMode.getValue() && this.reduceType.getValue() == 0);
    public final BooleanValue sprintReset = new BooleanValue("SprintReset", true, () -> this.reduceMode.getValue());

    public final PercentValue horizontal = new PercentValue("horizontal", 100);
    public final PercentValue vertical = new PercentValue("vertical", 100);
    public final PercentValue explosionHorizontal = new PercentValue("explosions-horizontal", 100);
    public final PercentValue explosionVertical = new PercentValue("explosions-vertical", 100);

    public final BooleanValue showLog = new BooleanValue("LOG", true);

    public Velocity2() {
        super("Velocity2", false);
    }

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer != null && (mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb());
    }

    private void applyVanilla(KnockbackEvent event) {
        if (this.horizontal.getValue() > 0) {
            event.setX(event.getX() * this.horizontal.getValue() / 100.0);
            event.setZ(event.getZ() * this.horizontal.getValue() / 100.0);
        }
        if (this.vertical.getValue() > 0) {
            event.setY(event.getY() * this.vertical.getValue() / 100.0);
        }
    }

    private void handleExplosion(KnockbackEvent event) {
        if (this.explosionHorizontal.getValue() > 0) {
            event.setX(event.getX() * this.explosionHorizontal.getValue() / 100.0);
            event.setZ(event.getZ() * this.explosionHorizontal.getValue() / 100.0);
        }
        if (this.explosionVertical.getValue() > 0) {
            event.setY(event.getY() * this.explosionVertical.getValue() / 100.0);
        }
    }

    private int getSmartAirDelayTicks(S12PacketEntityVelocity packet) {
        double mx = Math.abs(packet.getMotionX() / 8000.0);
        double mz = Math.abs(packet.getMotionZ() / 8000.0);
        double hz = Math.sqrt(mx * mx + mz * mz);

        if (hz >= this.airLess9.getValue()) {
            return this.delayStep9.getValue();
        }
        if (hz >= this.airLess8.getValue()) {
            return this.delayStep8.getValue();
        }
        if (hz >= this.airLess7.getValue()) {
            return this.delayStep7.getValue();
        }
        if (hz >= this.airLess6.getValue()) {
            return this.delayStep6.getValue();
        }
        if (hz >= this.airLess5.getValue()) {
            return this.delayStep5.getValue();
        }
        if (hz >= this.airLess4.getValue()) {
            return this.delayStep4.getValue();
        }
        if (hz >= this.airLess3.getValue()) {
            return this.delayStep3.getValue();
        }
        if (hz >= this.airLess2.getValue()) {
            return this.delayStep2.getValue();
        }
        if (hz >= this.airLess1.getValue()) {
            return this.delayStep1.getValue();
        }
        return 1;
    }

    private Entity raycastEntity(double range, float yaw, float pitch) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 lookVec = mc.thePlayer.getLook(1.0f);
        Vec3 endPos = eyePos.addVector(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range);
        Entity best = null;
        double bestDist = range;

        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof Entity)) {
                continue;
            }
            Entity e = (Entity) o;
            if (!(e instanceof EntityPlayer) || e == mc.thePlayer || e.isDead) {
                continue;
            }
            float border = e.getCollisionBorderSize();
            MovingObjectPosition intercept = e.getEntityBoundingBox().expand(border, border, border).calculateIntercept(eyePos, endPos);
            if (intercept == null) {
                continue;
            }
            double dist = eyePos.distanceTo(intercept.hitVec);
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }

        return best;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }
        if (this.reduceMode.getValue() && this.reduceType.getValue() == 0) {
            ++this.attackCounter;
        } else if (this.reduceMode.getValue() && this.reduceType.getValue() == 1 && this.attackReduceApplied) {
            event.setCancelled(true);
            this.canAttackReduce = true;
        }
        if (this.autoReduceActive) {
            ++this.hitsCount;
        }
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            this.pendingExplosion = false;
            this.allowNext = true;
            return;
        }

        if (mc.thePlayer.hurtTime <= 0) {
            return;
        }

        if (this.pendingExplosion) {
            this.pendingExplosion = false;
            this.handleExplosion(event);
            return;
        }

        boolean airborne = !mc.thePlayer.onGround;
        double kbX = event.getX();
        double kbZ = event.getZ();
        double kbMag = Math.sqrt(kbX * kbX + kbZ * kbZ);

        boolean packetCancelled = false;

        if (this.buffer3.getValue()) {
            this.buffer3Active = true;
            this.bufferRemainingTicks = this.bufferTicks.getValue();
            Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
            event.setCancelled(true);
            packetCancelled = true;
        }

        if (!packetCancelled && this.buffer2.getValue() && (airborne ? kbMag > 0.2 : kbMag > 0.15)) {
            this.bufferActive = true;
            this.bufferRemainingTicks = this.bufferTicks.getValue();
            this.applyVanilla(event);
            Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
            event.setCancelled(true);
            packetCancelled = true;
        }

        if (!packetCancelled && this.buffer.getValue()) {
            this.bufferActive = true;
            this.bufferRemainingTicks = this.bufferTicks.getValue();
            this.bufferPacketQueue.offer(new S12PacketEntityVelocity(mc.thePlayer.getEntityId(), (int) (kbX * 8000.0), (int) (event.getY() * 8000.0), (int) (kbZ * 8000.0)));
            event.setCancelled(true);
            packetCancelled = true;
        }

        if (this.attackReduce3.getValue() && event.getY() > 0.0) {
            this.attackReduce3Active = true;
            this.attackReduce3TicksLeft = 1;
        }

        if (this.autoReduce.getValue() && event.getY() > 0.0) {
            double vx = event.getX();
            double vz = event.getZ();
            double vy = event.getY();
            this.fallDamage = Math.abs(vx) < 0.01 && Math.abs(vz) < 0.01 && vy < 0.0;
            this.hasReceivedVelocity = true;
            this.autoReduceActive = true;
            this.ticksCount = 0;
        }

        if (this.rotate.getValue() && event.getY() > 0.0) {
            int maxTick = this.rotateTick.getValue();
            if (this.airRotate.getValue() && !mc.thePlayer.onGround) {
                maxTick = this.airRotateTicks.getValue();
            }

            boolean shouldRotate = mc.thePlayer.onGround || (this.airRotate.getValue() && !mc.thePlayer.onGround);
            if (shouldRotate) {
                this.knockbackX = event.getX();
                this.knockbackZ = event.getZ();
                if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
                    this.rotateTickCounter = 1;
                    if (this.showLog.getValue()) {
                        ChatUtil.sendFormatted(String.format("%s[SmartRot] Triggered&r", Epilogue.clientName));
                    }
                }
            }
        }

        if (this.jumpReset.getValue() && event.getY() > 0.0) {
            this.jumpFlag = true;
            if (this.showLog.getValue()) {
                ChatUtil.sendFormatted(String.format("%s[JumpReset] Triggered&r", Epilogue.clientName));
            }
        }

        if (!packetCancelled) {
            this.applyVanilla(event);
        }

        if (this.showLog.getValue()) {
            ChatUtil.sendFormatted(String.format("%sReleases X:%.3f Z:%.3f Y:%.3f&r", Epilogue.clientName, event.getX(), event.getZ(), event.getY()));
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (event.getType() == EventType.PRE) {
            int maxTick = this.rotateTick.getValue();
            if (this.airRotate.getValue() && !mc.thePlayer.onGround) {
                maxTick = this.airRotateTicks.getValue();
            }

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

            if (this.autoReduce.getValue()
                    && this.hasReceivedVelocity
                    && mc.thePlayer.hurtTime == 9
                    && mc.thePlayer.isSprinting()
                    && (mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f)) {
                Entity target = this.raycastEntity(this.reduceRange.getValue(), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                if (target != null) {
                    mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
                    mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                    mc.thePlayer.motionX *= 0.6;
                    mc.thePlayer.motionZ *= 0.6;
                    mc.thePlayer.setSprinting(false);
                    this.hasReceivedVelocity = false;
                    this.autoReduceActive = false;
                    if (this.reduceDebug.getValue()) {
                        ChatUtil.sendFormatted("&7[&bVelocity2&7] AutoReduce hit &f" + target.getName());
                    }
                }
            }
        }

        if (event.getType() == EventType.POST) {
            if ((this.buffer3Active || this.bufferActive) && this.bufferRemainingTicks > 0) {
                --this.bufferRemainingTicks;
                if (this.bufferRemainingTicks <= 0) {
                    if (Epilogue.delayManager.getDelayModule() == DelayModules.VELOCITY) {
                        Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
                    }
                    if (this.buffer.getValue() && !this.bufferPacketQueue.isEmpty()) {
                        S12PacketEntityVelocity p = this.bufferPacketQueue.poll();
                        if (p != null) {
                            mc.thePlayer.motionX = p.getMotionX() / 8000.0;
                            mc.thePlayer.motionY = p.getMotionY() / 8000.0;
                            mc.thePlayer.motionZ = p.getMotionZ() / 8000.0;
                        }
                    }
                    this.buffer3Active = false;
                    this.bufferActive = false;
                }
            }

            int maxTick = this.rotateTick.getValue();
            if (this.airRotate.getValue() && !mc.thePlayer.onGround) {
                maxTick = this.airRotateTicks.getValue();
            }

            if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
                ++this.rotateTickCounter;
                if (this.rotateTickCounter > maxTick) {
                    this.rotateTickCounter = 0;
                    this.targetRotation = null;
                    this.knockbackX = 0.0;
                    this.knockbackZ = 0.0;
                }
            }

            if (this.reduceMode.getValue() && this.reduceType.getValue() == 1 && this.attackReduceTicksLeft > 0) {
                --this.attackReduceTicksLeft;
                if (this.attackReduceTicksLeft <= 0 && this.attackReduceApplied) {
                    this.attackReduceApplied = false;
                    this.canAttackReduce = false;
                }
            }

            if (this.attackReduce3TicksLeft > 0) {
                --this.attackReduce3TicksLeft;
                if (this.attackReduce3TicksLeft <= 0 && this.attackReduce3Active) {
                    float factor = this.reduce3Factor.getValue();
                    mc.thePlayer.motionX *= factor;
                    mc.thePlayer.motionZ *= factor;
                    if (this.sprintReset3.getValue()) {
                        mc.thePlayer.setSprinting(false);
                    }
                    ChatUtil.sendFormatted(Epilogue.clientName + "\u00a7aAttackReduce3 reduced velocity by " + (int) ((1.0f - factor) * 100.0f) + "%");
                    this.attackReduce3Active = false;
                }
            }

            if (this.delayedVelocityActive && this.delayTicksLeft > 0) {
                --this.delayTicksLeft;
                if (this.delayTicksLeft <= 0) {
                    Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
                    this.delayedVelocityActive = false;
                }
            }

            if (this.autoReduceActive) {
                ++this.ticksCount;
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !mc.thePlayer.isPotionActive(Potion.moveSpeed) && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
                if (this.showLog.getValue()) {
                    ChatUtil.sendFormatted(String.format("%s[JumpReset] Executed&r", Epilogue.clientName));
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        int maxTick = this.rotateTick.getValue();
        if (this.airRotate.getValue() && !mc.thePlayer.onGround) {
            maxTick = this.airRotateTicks.getValue();
        }

        if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick && this.autoMove.getValue()) {
            mc.thePlayer.movementInput.moveForward = 1.0f;
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || !this.autoReduce.getValue()) {
            return;
        }

        if (this.random.nextInt(100) > this.reduceJumpChance.getValue()) {
            return;
        }

        boolean hitsCondition = this.hitsCount >= this.reduceHitsUntilJump.getValue();
        boolean ticksCondition = this.ticksCount >= this.reduceTicksUntilJump.getValue();
        boolean shouldJump = mc.thePlayer.hurtTime == 9 && mc.thePlayer.isSprinting() && !this.fallDamage && (hitsCondition || ticksCondition);

        if (shouldJump && mc.thePlayer.onGround && (mc.gameSettings == null || mc.gameSettings.keyBindJump == null || !mc.gameSettings.keyBindJump.isKeyDown()) && !this.isInLiquidOrWeb()) {
            mc.thePlayer.jump();
            this.hitsCount = 0;
            this.ticksCount = 0;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            return;
        }

        if (event.getType() == EventType.RECEIVE) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity p = (S12PacketEntityVelocity) event.getPacket();
                if (p.getEntityID() == mc.thePlayer.getEntityId()) {
                    if (this.smartAirDelay.getValue() && !mc.thePlayer.onGround) {
                        int ticks = this.getSmartAirDelayTicks(p);
                        Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                        Epilogue.delayManager.delayedPacket.offer((Packet<INetHandlerPlayClient>) (Packet<?>) p);
                        event.setCancelled(true);
                        this.delayedVelocityActive = true;
                        this.delayTicksLeft = ticks;
                        return;
                    }

                    if (this.reduceMode.getValue()) {
                        if (this.reduceType.getValue() == 0) {
                            if (this.attackCounter >= this.attackTick.getValue()) {
                                float factor = this.reduceFactor.getValue();
                                mc.thePlayer.motionX *= factor;
                                mc.thePlayer.motionZ *= factor;
                                if (this.sprintReset.getValue()) {
                                    mc.thePlayer.setSprinting(false);
                                }
                                ChatUtil.sendFormatted(Epilogue.clientName + "\u00a7aSuccessfully reduced velocity by " + (int) ((1.0f - factor) * 100.0f) + "%");
                                this.attackCounter = 0;
                            }
                        } else if (this.reduceType.getValue() == 1) {
                            this.attackReduceTicksLeft = 2;
                            this.attackReduceApplied = true;
                            float factor = this.reduceFactor.getValue();
                            mc.thePlayer.motionX *= factor;
                            mc.thePlayer.motionZ *= factor;
                            if (this.sprintReset.getValue()) {
                                mc.thePlayer.setSprinting(false);
                            }
                            ChatUtil.sendFormatted(Epilogue.clientName + "\u00a7aReduce2 activated! Velocity reduced by " + (int) ((1.0f - factor) * 100.0f) + "%");
                        }
                    }
                }
            } else if (event.getPacket() instanceof S19PacketEntityStatus) {
                S19PacketEntityStatus p = (S19PacketEntityStatus) event.getPacket();
                World w = mc.theWorld;
                if (w != null) {
                    Entity entity = p.getEntity(w);
                    if (entity != null && entity.equals(mc.thePlayer) && p.getOpCode() == 2) {
                        this.allowNext = false;
                    }
                }
            } else if (event.getPacket() instanceof S27PacketExplosion) {
                S27PacketExplosion p = (S27PacketExplosion) event.getPacket();
                if (p.func_149149_c() != 0.0f || p.func_149144_d() != 0.0f || p.func_149147_e() != 0.0f) {
                    this.pendingExplosion = true;
                    if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onWorld(LoadWorldEvent event) {
    }

    @Override
    public void onEnabled() {
        this.resetFlags();
    }

    @Override
    public void onDisabled() {
        this.resetFlags();
        if (Epilogue.delayManager.getDelayModule() == DelayModules.VELOCITY) {
            Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
        }
        Epilogue.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }

    private void resetFlags() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.jumpFlag = false;
        this.rotateTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
        this.attackCounter = 0;
        this.attackReduceApplied = false;
        this.attackReduceTicksLeft = 0;
        this.canAttackReduce = false;
        this.recentKnockbacks.clear();
        this.buffer3Active = false;
        this.bufferPacketQueue.clear();
        this.bufferRemainingTicks = 0;
        this.bufferActive = false;
        this.delayedVelocityActive = false;
        this.delayTicksLeft = 0;
        this.attackReduce3Active = false;
        this.attackReduce3TicksLeft = 0;
        this.hasReceivedVelocity = false;
        this.fallDamage = false;
        this.hitsCount = 0;
        this.ticksCount = 0;
        this.autoReduceActive = false;
    }

    @Override
    public String[] getSuffix() {
        if (this.reduceMode.getValue()) {
            return new String[]{"Reduce"};
        }
        return new String[0];
    }
}
