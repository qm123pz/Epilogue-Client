package epilogue.module.modules.combat;

import com.google.common.base.CaseFormat;
import epilogue.util.ChatUtil;
import epilogue.value.values.*;
import epiloguemixinbridge.IAccessorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import epilogue.Epilogue;
import epilogue.enums.BlinkModules;
import epilogue.enums.DelayModules;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.*;
import epilogue.module.Module;
import epilogue.module.modules.movement.LongJump;
import epilogue.util.MoveUtil;
import epilogue.util.RotationUtil;
import epilogue.management.RotationState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int chanceCounter = 0;
    private int delayChanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean delayFlag = false;
    private boolean delayActive = false;
    private boolean jumpFlag = false;
    private long blinkStartTime = System.currentTimeMillis();
    private long delayStartTime = 0L;
    private boolean shouldCancelAttack;
    private boolean shouldSprintReset = false;

    public final ModeValue mode = new ModeValue("Mode", 2, new String[]{"Vanilla", "Jump", "Prediction"});
    public final PercentValue chance = new PercentValue("Chance", 100);
    public final PercentValue horizontal = new PercentValue("Horizontal", 100, () -> this.mode.getValue() == 0);
    public final PercentValue vertical = new PercentValue("Vertical", 100, () -> this.mode.getValue() == 0);
    public final PercentValue explosionHorizontal = new PercentValue("Explosions Horizontal", 100, () -> this.mode.getValue() == 0);
    public final PercentValue explosionVertical = new PercentValue("Explosions Vertical", 100, () -> this.mode.getValue() == 0);
    public final BooleanValue fakeCheck = new BooleanValue("Check Fake", true);
    public final IntValue delayTicks = new IntValue("Delay Ticks", 1, 1, 20, () -> this.mode.getValue() == 2);
    public final PercentValue delayChance = new PercentValue("Delay Change", 100, () -> this.mode.getValue() == 2);
    public final BooleanValue reduce = new BooleanValue("Reduce", true, () -> this.mode.getValue() == 2);
    public final BooleanValue reduceOnlyNoBlocking = new BooleanValue("Reduce Only Not Blocking", true, () -> this.mode.getValue() == 2 && this.reduce.getValue());
    public final BooleanValue reduceOnlyMoving = new BooleanValue("Only Moving", false, () -> this.mode.getValue() == 2 && this.reduce.getValue());
    public final PercentValue reduceChange = new PercentValue("Reduce Chance", 100, () -> this.mode.getValue() == 2 && this.reduce.getValue());
    public final BooleanValue jumpReset = new BooleanValue("Jump Reset", true, () -> this.mode.getValue() == 2);
    public final BooleanValue sprintReset = new BooleanValue("Sprint Reset", true, () -> this.mode.getValue() == 2);
    public final IntValue sprintResetStartHurtTime = new IntValue("Start HurtTime", 7, 1, 10, () -> this.mode.getValue() == 2);
    public final IntValue sprintResetEndHurtTime = new IntValue("End HurtTime", 7, 1, 10, () -> this.mode.getValue() == 2);
    public final BooleanValue blink = new BooleanValue("Blink", true, () -> this.mode.getValue() == 2);
    public final BooleanValue dbg = new BooleanValue("Debug", true, () -> this.mode.getValue() == 2 && this.reduce.getValue());
    public final BooleanValue jrDbg = new BooleanValue("Debug", true, () -> this.mode.getValue() == 1);

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

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;
    }

    private List<EntityLivingBase> getEntitiesInRange() {
        List<EntityLivingBase> entities = new ArrayList<>();
        double range = 3.0;

        AxisAlignedBB boundingBox = mc.thePlayer.getEntityBoundingBox().expand(range, range, range);
        List<Entity> loadedEntities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, boundingBox);
        for (Entity entity : loadedEntities) {
            if (entity instanceof EntityLivingBase && entity != mc.thePlayer) {
                double distance = mc.thePlayer.getDistanceToEntity(entity);
                if (distance <= range) {
                    entities.add((EntityLivingBase) entity);
                }
            }
        }

        entities.sort(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)));

        return entities;
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            this.pendingExplosion = false;
            this.allowNext = true;
            return;
        }

        if (this.mode.getValue() == 2) {
            boolean can =
                    //((!this.fakeCheck.getValue() && this.allowNext) || (this.fakeCheck.getValue() && !this.allowNext)) &&
                            (this.chance.getValue() == 100 || (Math.random() * 100) < chance.getValue());
            if (this.jumpReset.getValue() && mc.thePlayer.onGround && can) {
                mc.thePlayer.movementInput.jump = true;
                if (dbg.getValue()) ChatUtil.sendFormatted("JumpReseted");
                shouldSprintReset = true;
            }

            if (shouldSprintReset && sprintReset.getValue() && can) {
                if (mc.thePlayer.hurtTime >= sprintResetStartHurtTime.getValue()) {
                mc.thePlayer.setSprinting(false);
                shouldSprintReset = true;
                if (dbg.getValue()) ChatUtil.sendFormatted("SprintReseted - SetSprinting(false)");
            }
                if (mc.thePlayer.hurtTime >= sprintResetEndHurtTime.getValue()) {
                    mc.thePlayer.setSprinting(true);
                    shouldSprintReset = false;
                    if (dbg.getValue()) ChatUtil.sendFormatted("SprintReseted - SetSprinting(True)");
                }
            }

            if (reduce.getValue()) {
                boolean shouldActivate = !this.reduceOnlyMoving.getValue() || this.isMoving();

                if (shouldActivate) {
                    int currentChance = this.reduceChange.getValue();
                    boolean chancePassed = currentChance == 100 || (Math.random() * 100) < currentChance;

                    if (chancePassed) {
                        List<EntityLivingBase> nearbyEntities = getEntitiesInRange();
                        if (!nearbyEntities.isEmpty() && nearbyEntities.get(0) instanceof EntityPlayer) {
                            Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);

                            float aimYaw = mc.thePlayer.rotationYaw;
                            float aimPitch = mc.thePlayer.rotationPitch;
                            if (aura != null && aura.isEnabled() && RotationState.isActived()) {
                                aimYaw = RotationState.getRotationYawHead();
                                aimPitch = RotationState.getRotationPitch();
                            }

                            boolean aimed = RotationUtil.watchdogIsTargetInRaycastRange(
                                    nearbyEntities.get(0),
                                    4.5
                            );

                            if (aura != null && aura.isEnabled() && RotationState.isActived()) {
                                aimed = RotationUtil.rayTrace(nearbyEntities.get(0).getEntityBoundingBox(), aimYaw, aimPitch, 4.5) != null;
                            }

                            if (!aimed) {
                                return;
                            }

                            if (reduceOnlyNoBlocking.getValue()) {
                                if (aura == null) {
                                    return;
                                }
                                boolean blockedMode = aura.autoBlock.getValue() == 1 || aura.autoBlock.getValue() == 3;
                                if (blockedMode) {
                                    if (aura.isPlayerBlocking()) return;
                                }
                            }

                            mc.thePlayer.swingItem();
                            mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(nearbyEntities.get(0), C02PacketUseEntity.Action.ATTACK));

                            double reduceFactor = 0.6;
                            event.setX(event.getX() * reduceFactor);
                            event.setZ(event.getZ() * reduceFactor);

                            mc.thePlayer.setSprinting(false);
                            this.shouldCancelAttack = true;
                            if (dbg.getValue()) ChatUtil.sendFormatted("Reduced");
                        }
                    }
                }
            }

            boolean shouldProcess = (!this.fakeCheck.getValue() && this.allowNext) || (this.fakeCheck.getValue() && !this.allowNext);
            if (shouldProcess) {
                this.allowNext = true;
                if (this.pendingExplosion) {
                    this.pendingExplosion = false;
                    this.handleExplosion(event);
                } else {
                    this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
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
                    boolean jumpMode = this.mode.getValue() == 1 && event.getY() > 0.0;
                    this.jumpFlag = jumpMode;
                    if (jumpMode) {
                        this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
                    } else {
                        this.applyVanilla(event);
                    }
                }
            }
        }
    }

    private void applyMotion(KnockbackEvent event, int horizontalPct, int verticalPct) {
        if (horizontalPct > 0) {
            event.setX(event.getX() * horizontalPct / 100.0);
            event.setZ(event.getZ() * horizontalPct / 100.0);
        } else {
            event.setX(mc.thePlayer.motionX);
            event.setZ(mc.thePlayer.motionZ);
        }
        if (verticalPct > 0) {
            event.setY(event.getY() * verticalPct / 100.0);
        } else {
            event.setY(mc.thePlayer.motionY);
        }
    }

    private void applyVanilla(KnockbackEvent event) {
        this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
    }

    private void handleExplosion(KnockbackEvent event) {
        this.applyMotion(event, this.explosionHorizontal.getValue(), this.explosionVertical.getValue());
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }
        if (event.getType() == EventType.RECEIVE && !event.isCancelled()) {
            if (event.getPacket() instanceof S12PacketEntityVelocity) {
                S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
                if (packet.getEntityID() != mc.thePlayer.getEntityId()) {
                    return;
                }
                if (this.mode.getValue() == 2) {
                    LongJump longJump = (LongJump) Epilogue.moduleManager.modules.get(LongJump.class);
                    boolean canStartJump = longJump != null && longJump.isEnabled() && longJump.canStartJump();
                    if (
                            //!this.delayFlag &&
                                    !this.isInLiquidOrWeb()
                                            //&& !this.pendingExplosion
                            //&& !this.allowNext
                            //&& !canStartJump
                        ) {
                        this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();
                        if (this.delayChanceCounter >= 100) {
                            Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                            if (dbg.getValue()) ChatUtil.sendFormatted("Delayed");
                            Epilogue.delayManager.delayedPacket.offer(packet);
                            event.setCancelled(true);
                            this.delayFlag = true;
                            this.delayStartTime = System.currentTimeMillis();
                            if (this.blink.getValue()) {
                                this.blinkStartTime = System.currentTimeMillis();
                                Epilogue.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                                if (dbg.getValue()) ChatUtil.sendFormatted("Blinked");
                            }
                            this.delayChanceCounter = 0;
                        }
                    }
                }
            } else if (event.getPacket() instanceof S19PacketEntityStatus) {
                S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
                World world = mc.theWorld;
                if (world != null) {
                    Entity entity = packet.getEntity(world);
                    if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
                        this.allowNext = false;
                    }
                }
            } else if (event.getPacket() instanceof S27PacketExplosion) {
                S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
                if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
                    this.pendingExplosion = true;
                    if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 2 && this.reduce.getValue() && this.shouldCancelAttack && mc.thePlayer.hurtTime > 0) {
            this.shouldCancelAttack = false;
        }

        if (event.getType() != EventType.POST || this.mode.getValue() != 2) {
            return;
        }
        if (this.delayFlag) {
            boolean shouldRelease = false;
            int delayValue = this.delayTicks.getValue();
            if (delayValue >= 1 && delayValue <= 3) {
                long requiredDelay = delayValue == 1 ? 60L : (delayValue == 2 ? 95L : 100L);
                if (System.currentTimeMillis() - this.delayStartTime >= requiredDelay) {
                    shouldRelease = true;
                }
            } else {
                shouldRelease = this.canDelay() || this.isInLiquidOrWeb() || Epilogue.delayManager.getDelay() >= delayValue;
            }
            if (shouldRelease) {
                Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
                this.delayFlag = false;
                Epilogue.blinkManager.setBlinkState(false, BlinkModules.BLINK);
            }
        }
        if (this.delayActive) {
            MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
            this.delayActive = false;
        }
        if (this.blink.getValue()) {
            Epilogue.blinkManager.setBlinkState(System.currentTimeMillis() - this.blinkStartTime < 95, BlinkModules.BLINK);
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
                if(dbg.getValue() && mode.getValue() == 2 || jrDbg.getValue()) ChatUtil.sendFormatted("JumpReseted");
            }
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
        this.delayFlag = false;
        this.delayActive = false;
        this.blinkStartTime = System.currentTimeMillis();
        this.delayStartTime = 0L;
        this.jumpFlag = false;
    }

    @Override
    public void onDisabled() {
        this.shouldCancelAttack = false;
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.delayChanceCounter = 0;
        this.delayFlag = false;
        this.delayActive = false;
        this.delayStartTime = 0L;
        this.jumpFlag = false;
        if (Epilogue.delayManager.getDelayModule() == DelayModules.VELOCITY) {
            Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
        }
        Epilogue.delayManager.delayedPacket.clear();
        Epilogue.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString();
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}