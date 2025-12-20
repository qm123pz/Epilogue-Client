package epilogue.module.modules.combat;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.world.World;
import epilogue.enums.BlinkModules;
import epilogue.enums.DelayModules;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.*;
import epilogue.mixin.IAccessorEntity;
import epilogue.module.Module;
import epilogue.module.modules.movement.LongJump;
import epilogue.util.MoveUtil;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import epilogue.value.values.PercentValue;

import java.util.ArrayList;
import java.util.List;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int chanceCounter = 0;
    private int delayChanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean reverseFlag = false;
    private boolean delayActive = false;
    private long lastAttackTime = 0L;
    private long blinkStartTime = System.currentTimeMillis();
    private final long blinkDuration = 95L;
    private long reverseStartTime = 0L;
    private boolean jumpFlag = false;
    private int blinkTicks = 0;
    private int rotatoTickCounter = 0;
    private float[] targetRotation = null;
    private double knockbackX = 0.0;
    private double knockbackZ = 0.0;
    private boolean isSmartRotActive = false;
    private int smartRotDuration = 2;
    private boolean attackReduceTriggered = false;
    private boolean attackReduceSuccess = false;

    public final ModeValue mode = new ModeValue("mode", 0, new String[]{"Vanilla", "JumpReset", "Prediction"});
    public final PercentValue horizontal = new PercentValue("Horizontal", 100);
    public final PercentValue vertical = new PercentValue("Vertical", 100);
    public final PercentValue explosionHorizontal = new PercentValue("Explosions Horizontal", 100);
    public final PercentValue explosionVertical = new PercentValue("Explosions Vertical", 100);
    public final PercentValue chance = new PercentValue("Change", 100);
    public final BooleanValue fakeCheck = new BooleanValue("Check Fake", true);
    public final BooleanValue smartRotJumpReset = new BooleanValue("Smart Rotate", false, () -> this.mode.getValue() == 2);
    public final IntValue rotateTicks = new IntValue("Rotate Ticks", 2, 1, 10, () -> this.mode.getValue() == 2 && this.smartRotJumpReset.getValue());
    public final BooleanValue autoJump = new BooleanValue("Auto Jump", true, () -> this.mode.getValue() == 2 && this.smartRotJumpReset.getValue());
    public final BooleanValue reduce2 = new BooleanValue("Reduce2", false, () -> this.mode.getValue() == 2);
    public final FloatValue reduce2Factor = new FloatValue("Reduce2 Factor", 0.78f, 0.1f, 1.0f, () -> this.mode.getValue() == 2 && this.reduce2.getValue());
    public final BooleanValue keepVertical = new BooleanValue("Keep Vertical", true, () -> this.mode.getValue() == 2 && this.reduce2.getValue());
    public final BooleanValue attackReduce = new BooleanValue("Attack Reduce", false, () -> this.mode.getValue() == 2);
    public final IntValue delayTicks = new IntValue("Delay Ticks", 3, 1, 20, () -> this.mode.getValue() == 2);
    public final PercentValue delayChance = new PercentValue("Chance", 100, () -> this.mode.getValue() == 2);
    public final BooleanValue jumpReset = new BooleanValue("Jump Reset", true, () -> this.mode.getValue() == 2);
    public final IntValue reduceHurtTime = new IntValue("Reduce HurtTime", 10, 1, 10, () -> this.mode.getValue() == 2);
    public final FloatValue reduceFactor = new FloatValue("Reduce Factor", 0.6f, 0.1f, 1.0f, () -> this.mode.getValue() == 2);
    public final BooleanValue blink = new BooleanValue("Blink", true, () -> this.mode.getValue() == 2);
    public final BooleanValue showBlinkTicks = new BooleanValue("Show Blink Ticks", false, () -> this.mode.getValue() == 2 && this.blink.getValue());
    public final BooleanValue airDelay = new BooleanValue("Air Delay", false, () -> this.mode.getValue() == 1);

    public Velocity() {
        super("Velocity", false);
    }

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer != null && (mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb());
    }

    private boolean canDelay() {
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        return mc.thePlayer.onGround && (aura == null || !aura.isEnabled() || !aura.shouldAutoBlock());
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            this.pendingExplosion = false;
            this.allowNext = true;
            this.attackReduceTriggered = false;
            return;
        }

        if (this.mode.getValue() == 2) {
            if (!this.allowNext || !this.fakeCheck.getValue()) {
                this.allowNext = true;
                if (this.pendingExplosion) {
                    this.pendingExplosion = false;
                    this.handleExplosion(event);
                    return;
                }

                if (this.smartRotJumpReset.getValue() && event.getY() > 0.0) {
                    this.knockbackX = event.getX();
                    this.knockbackZ = event.getZ();
                    if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
                        this.rotatoTickCounter = 1;
                        this.isSmartRotActive = true;
                        this.smartRotDuration = this.rotateTicks.getValue();
                    }
                }

                if (this.jumpReset.getValue() && event.getY() > 0.0) {
                    this.jumpFlag = true;
                    this.attackReduceSuccess = this.attackReduce.getValue() && this.attackReduceTriggered;
                }

                if (this.attackReduce.getValue() && this.attackReduceTriggered) {
                    this.applyVanilla(event);
                    this.attackReduceTriggered = false;
                }

                if (this.reduce2.getValue() && !this.attackReduceTriggered) {
                    this.applyIntaveFactorReduction(event);
                }

                if (!this.attackReduce.getValue() && !this.reduce2.getValue()) {
                    this.applyVanilla(event);
                }
            }
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
                    this.jumpFlag = this.mode.getValue() == 1 && event.getY() > 0.0;
                    if (this.mode.getValue() == 1 && event.getY() > 0.0) {
                        this.applyVanilla(event);
                    } else {
                        this.applyVanilla(event);
                    }
                    this.chanceCounter = 0;
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE && this.isEnabled() && mc.thePlayer != null && this.isSmartRotActive && this.rotatoTickCounter > 0 && this.rotatoTickCounter <= this.smartRotDuration) {
            if (this.rotatoTickCounter == 1) {
                double deltaX = -this.knockbackX;
                double deltaZ = -this.knockbackZ;
                double dist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
                float pitch = (float) (-(Math.atan2(0.0, dist) * 180.0 / Math.PI));
                this.targetRotation = new float[]{yaw, pitch};
            }
            if (this.targetRotation != null) {
                event.setRotation(this.targetRotation[0], this.targetRotation[1], 2);
                event.setPervRotation(this.targetRotation[0], 2);
            }
        }

        if (event.getType() == EventType.POST && this.isEnabled() && mc.thePlayer != null && this.mode.getValue() == 2) {
            if (this.isSmartRotActive) {
                ++this.rotatoTickCounter;
                if (this.rotatoTickCounter > this.smartRotDuration) {
                    this.isSmartRotActive = false;
                    this.rotatoTickCounter = 0;
                    this.targetRotation = null;
                    this.knockbackX = 0.0;
                    this.knockbackZ = 0.0;
                    if (this.autoJump.getValue() && mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !this.isInLiquidOrWeb()) {
                        mc.thePlayer.jump();
                    }
                }
            }

            if (this.reduceFactor.getValue() < 1.0f && mc.thePlayer.hurtTime == this.reduceHurtTime.getValue() && System.currentTimeMillis() - this.lastAttackTime <= 8000L) {
                mc.thePlayer.motionX *= this.reduceFactor.getValue();
                mc.thePlayer.motionZ *= this.reduceFactor.getValue();
            }

            if (this.reverseFlag) {
                boolean shouldRelease = false;
                int delayValue = this.delayTicks.getValue();
                if (delayValue >= 1 && delayValue <= 3) {
                    long requiredDelayMs = delayValue == 1 ? 55L : (delayValue == 2 ? 60L : 100L);
                    if (System.currentTimeMillis() - this.reverseStartTime >= requiredDelayMs) {
                        shouldRelease = true;
                    }
                } else {
                    shouldRelease = this.canDelay() || this.isInLiquidOrWeb() || Epilogue.delayManager.getDelay() >= delayValue;
                }
                if (shouldRelease) {
                    Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
                    this.reverseFlag = false;
                    Epilogue.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                }
            }

            if (this.delayActive) {
                MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                this.delayActive = false;
            }

            if (this.blink.getValue()) {
                if (System.currentTimeMillis() - this.blinkStartTime < this.blinkDuration) {
                    Epilogue.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                    this.blinkTicks++;
                } else {
                    Epilogue.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                    this.blinkTicks = 0;
                }
            } else {
                this.blinkTicks = 0;
            }
        }
    }

    private void applyIntaveFactorReduction(KnockbackEvent event) {
        float factor = this.reduce2Factor.getValue();
        event.setX(event.getX() * factor);
        event.setZ(event.getZ() * factor);
        mc.thePlayer.motionX *= factor;
        mc.thePlayer.motionZ *= factor;
        if (!this.keepVertical.getValue()) {
            event.setY(event.getY() * factor);
            mc.thePlayer.motionY *= factor;
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

            if (this.mode.getValue() == 2) {
                LongJump longJump = (LongJump) Epilogue.moduleManager.modules.get(LongJump.class);
                boolean canStartJump = longJump != null && longJump.isEnabled() && longJump.canStartJump();
                if (!(this.reverseFlag || this.isInLiquidOrWeb() || this.pendingExplosion || (this.allowNext && this.fakeCheck.getValue()) || canStartJump)) {
                    this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();
                    if (this.delayChanceCounter >= 100) {
                        Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                        Epilogue.delayManager.delayedPacket.offer((Packet<INetHandlerPlayClient>) (Packet<?>) packet);
                        event.setCancelled(true);
                        this.reverseFlag = true;
                        this.reverseStartTime = System.currentTimeMillis();
                        if (this.blink.getValue()) {
                            this.blinkStartTime = System.currentTimeMillis();
                            Epilogue.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                        }
                        this.delayChanceCounter = 0;
                        return;
                    }
                }
                return;
            }

            if (this.mode.getValue() == 1 && this.airDelay.getValue() && mc.thePlayer != null && !mc.thePlayer.onGround) {
                Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                Epilogue.delayManager.delayedPacket.offer((Packet<INetHandlerPlayClient>) (Packet<?>) packet);
                event.setCancelled(true);
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
                    if (this.mode.getValue() == 2 && this.attackReduce.getValue()) {
                        this.attackReduceTriggered = true;
                    }
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
    public void onSendPacket(PacketEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 2 && event.getType() == EventType.SEND && !event.isCancelled() && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() == C02PacketUseEntity.Action.ATTACK) {
                this.lastAttackTime = System.currentTimeMillis();
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }
        if (this.mode.getValue() == 2 && this.attackReduce.getValue() && this.attackReduceTriggered && mc.thePlayer.hurtTime > 0 && mc.thePlayer.hurtTime <= 10) {
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer != null && mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
            }
            this.attackReduceSuccess = false;
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.onDisabled();
    }

    @Override
    public void onEnabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.delayChanceCounter = 0;
        this.reverseFlag = false;
        this.delayActive = false;
        this.lastAttackTime = 0L;
        this.blinkStartTime = System.currentTimeMillis();
        this.reverseStartTime = 0L;
        this.jumpFlag = false;
        this.blinkTicks = 0;
        this.attackReduceTriggered = false;
        this.attackReduceSuccess = false;
        this.rotatoTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
        this.isSmartRotActive = false;
    }

    @Override
    public void onDisabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.delayChanceCounter = 0;
        this.reverseFlag = false;
        this.delayActive = false;
        this.lastAttackTime = 0L;
        this.reverseStartTime = 0L;
        this.jumpFlag = false;
        this.blinkTicks = 0;
        this.attackReduceTriggered = false;
        this.attackReduceSuccess = false;
        this.rotatoTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
        this.isSmartRotActive = false;
        if (Epilogue.delayManager.getDelayModule() == DelayModules.VELOCITY) {
            Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
        }
        Epilogue.delayManager.delayedPacket.clear();
        Epilogue.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString();
        if (this.mode.getValue() == 2) {
            List<String> suffix = new ArrayList<>();
            suffix.add(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName));

            if (this.smartRotJumpReset.getValue()) {
                suffix.add("SmartRotate");
                suffix.add("Ticks:" + this.rotateTicks.getValue());
            }

            if (this.reduce2.getValue()) {
                suffix.add(String.format("ReduceFactor:%.2f", this.reduce2Factor.getValue()));
            }

            if (this.attackReduce.getValue()) {
                suffix.add("AttackReduce");
            }

            if (this.showBlinkTicks.getValue()) {
                suffix.add("BlinkTicks:" + this.blinkTicks);
            }

            return suffix.toArray(new String[0]);
        }
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}