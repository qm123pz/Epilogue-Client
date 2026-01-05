package epilogue.module.modules.combat;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import epilogue.enums.BlinkModules;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.event.types.Priority;
import epilogue.events.*;
import epilogue.management.RotationState;
import epiloguemixinbridge.IAccessorPlayerControllerMP;
import epilogue.module.Module;
import epilogue.module.modules.combat.rotation.OPRotationSystem;
import epilogue.module.modules.combat.rotation.Rotation;
import epilogue.module.modules.combat.rotation.RotationUtils;
import epilogue.module.modules.movement.NoSlow;
import epilogue.module.modules.player.Scaffold;
import epilogue.module.modules.player.BedNuker;
import epilogue.value.values.BooleanValue;

import epilogue.util.*;
import epilogue.value.values.*;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.*;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;

import java.util.ArrayList;
import java.util.Random;

public class Aura extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean rotationBlocked = false;
    public static boolean attackBlocked = false;
    public static boolean swingBlocked = false;
    private static final float SIM_MAX_YAW_STEP = 45.0F;
    private static final float SIM_MAX_PITCH_STEP = 45.0F;
    private static final float SMOOTH_BACK_MIN_SPEED = 3.9F;
    private static final float SMOOTH_BACK_MAX_SPEED = 23.4F;
    private static final float SMOOTH_BACK_NOISE = 0.35F;
    private final TimerUtil timer = new TimerUtil();
    private final OPRotationSystem simRotationSystem = new OPRotationSystem();
    private Rotation lastAuraRotation = null;
    private boolean smoothBackActive = false;
    private Rotation cameraRotation = null;
    private Rotation smoothBackTargetRotation = null;
    private Rotation smoothBackRotation = null;
    private int postTargetLostTicks = 0;
    private float smoothBackSpeedNoise = 0f;
    private int smoothBackNoiseTick = 0;
    public AttackData target = null;
    private int switchTick = 0;
    private boolean hitRegistered = false;
    boolean blockingState = false;
    private boolean isBlocking = false;
    private boolean fakeBlockState = false;
    private boolean blinkReset = false;
    private long attackDelayMS = 0L;
    private int blockTick = 0;
    public final ModeValue mode;
    public final ModeValue sort;
    public final ModeValue autoBlock;
    public final BooleanValue autoBlockRequirePress;
    public final IntValue autoBlockCPS;
    public final FloatValue autoBlockRange;
    public final FloatValue swingRange;
    public final FloatValue attackRange;
    public final IntValue fov;
    public final IntValue minCPS;
    public final IntValue maxCPS;
    public final IntValue switchDelay;
    public final ModeValue rotations;
    public final ModeValue yaw;
    public final ModeValue pitch;
    public final ModeValue moveFix;
    public final BooleanValue smoothBack;
    public final BooleanValue throughWalls;
    public final BooleanValue requirePress;
    public final BooleanValue allowMining;
    public final BooleanValue weaponsOnly;
    public final BooleanValue allowTools;
    public final BooleanValue inventoryCheck;
    public final BooleanValue botCheck;
    public final BooleanValue players;
    public final BooleanValue bosses;
    public final BooleanValue mobs;
    public final BooleanValue animals;
    public final BooleanValue silverfish;
    public final BooleanValue golems;
    public final BooleanValue teams;
    private boolean swapped;

    private long getAttackDelay() {
        return this.isBlocking ? (long) (1000.0F / this.autoBlockCPS.getValue()) : 1000L / RandomUtil.nextLong(this.minCPS.getValue(), this.maxCPS.getValue());
    }

    private Rotation clampStep(Rotation base, Rotation desired) {
        if (base == null || desired == null) {
            return desired;
        }
        float yawDiff = RotationUtils.getAngleDiff(base.yaw, desired.yaw);
        float pitchDiff = RotationUtils.getAngleDiff(base.pitch, desired.pitch);

        float yawChange = RotationUtils.clamp(RotationUtils.abs(yawDiff), 0f, Aura.SIM_MAX_YAW_STEP);
        float pitchChange = RotationUtils.clamp(RotationUtils.abs(pitchDiff), 0f, Aura.SIM_MAX_PITCH_STEP);

        float nextYaw = base.yaw;
        float nextPitch = base.pitch;

        if (yawDiff > 0) {
            nextYaw += yawChange;
        } else if (yawDiff < 0) {
            nextYaw -= yawChange;
        }

        if (pitchDiff > 0) {
            nextPitch += pitchChange;
        } else if (pitchDiff < 0) {
            nextPitch -= pitchChange;
        }

        nextPitch = RotationUtils.clamp(nextPitch, -90f, 90f);
        return new Rotation(nextYaw, nextPitch);
    }

    private Rotation smoothBackStep(Rotation base, Rotation targetRot) {
        if (base == null || targetRot == null) {
            return base;
        }

        float yawDiff = RotationUtils.getAngleDiff(base.yaw, targetRot.yaw);
        float pitchDiff = RotationUtils.getAngleDiff(base.pitch, targetRot.pitch);

        float absYaw = RotationUtils.abs(yawDiff);
        float absPitch = RotationUtils.abs(pitchDiff);

        float diffMag = Math.max(absYaw, absPitch);
        if (diffMag < 1.0E-4f) {
            return targetRot;
        }

        float t = RotationUtils.clamp(diffMag / 180f, 0f, 1f);
        float eased = (float) Math.sin(t * (Math.PI / 2.0));
        float stepBase = SMOOTH_BACK_MIN_SPEED + (SMOOTH_BACK_MAX_SPEED - SMOOTH_BACK_MIN_SPEED) * eased;
        if (++smoothBackNoiseTick >= 2) {
            smoothBackNoiseTick = 0;
            float n = SMOOTH_BACK_NOISE;
            smoothBackSpeedNoise = RotationUtils.randomFloat(-n, n);
        }

        stepBase *= (1.0f + smoothBackSpeedNoise);
        stepBase = RotationUtils.clamp(stepBase, 0.1f, 180.0f);

        float yawStep = RotationUtils.clamp(stepBase * (absYaw / diffMag), 0f, absYaw);
        float pitchStep = RotationUtils.clamp(stepBase * (absPitch / diffMag), 0f, absPitch);

        float nextYaw = base.yaw;
        float nextPitch = base.pitch;

        if (yawDiff > 0) {
            nextYaw += yawStep;
        } else if (yawDiff < 0) {
            nextYaw -= yawStep;
        }

        if (pitchDiff > 0) {
            nextPitch += pitchStep;
        } else if (pitchDiff < 0) {
            nextPitch -= pitchStep;
        }

        nextPitch = RotationUtils.clamp(nextPitch, -90f, 90f);
        return new Rotation(nextYaw, nextPitch);
    }

    private boolean performAttack(float yaw, float pitch) {
        if (attackBlocked) {
            return false;
        }
        if (!Epilogue.playerStateManager.digging && !Epilogue.playerStateManager.placing) {
            if (this.isPlayerBlocking() && this.autoBlock.getValue() != 1) {
                return false;
            } else if (this.attackDelayMS > 0L) {
                return false;
            } else {
                this.attackDelayMS = this.attackDelayMS + this.getAttackDelay();
                if (!swingBlocked) {
                    mc.thePlayer.swingItem();
                }
                if ((this.rotations.getValue() != 0 || this.isBoxInAttackRange(this.target.getBox()))
                        && RotationUtil.rayTrace(this.target.getBox(), yaw, pitch, this.attackRange.getValue()) == null) {
                    return false;
                } else {
                    ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                    PacketUtil.sendPacket(new C02PacketUseEntity(this.target.getEntity(), Action.ATTACK));
                    if (mc.playerController.getCurrentGameType() != GameType.SPECTATOR) {
                        PlayerUtil.attackEntity(this.target.getEntity());
                    }
                    this.hitRegistered = true;
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    private void sendUseItem() {
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        this.startBlock(mc.thePlayer.getHeldItem());
    }

    private void startBlock(ItemStack itemStack) {
        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(itemStack));
        mc.thePlayer.setItemInUse(itemStack, itemStack.getMaxItemUseDuration());
        this.blockingState = true;
    }

    private void stopBlock() {
        PacketUtil.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        mc.thePlayer.stopUsingItem();
        this.blockingState = false;
    }

    private void interactAttack(float yaw, float pitch) {
        if (this.target != null) {
            MovingObjectPosition mop = RotationUtil.rayTrace(this.target.getBox(), yaw, pitch, 8.0);
            if (mop != null) {
                ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                PacketUtil.sendPacket(
                        new C02PacketUseEntity(
                                this.target.getEntity(),
                                new Vec3(mop.hitVec.xCoord - this.target.getX(), mop.hitVec.yCoord - this.target.getY(), mop.hitVec.zCoord - this.target.getZ())
                        )
                );
                PacketUtil.sendPacket(new C02PacketUseEntity(this.target.getEntity(), Action.INTERACT));
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                this.blockingState = true;
            }
        }
    }

    private boolean canAttack() {
        if (attackBlocked) {
            return false;
        }
        if (this.inventoryCheck.getValue() && mc.currentScreen instanceof GuiContainer) {
            return false;
        } else if (!(java.lang.Boolean) this.weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            if (((IAccessorPlayerControllerMP) mc.playerController).getIsHittingBlock()) {
                return false;
            } else if (ItemUtil.isEating() || ItemUtil.isUsingBow()) {
                return false;
            } else {
                AutoHeal autoHeal = (AutoHeal) Epilogue.moduleManager.modules.get(AutoHeal.class);
                if (autoHeal.isEnabled() && autoHeal.isSwitching()) {
                    return false;
                } else {
                    BedNuker bedNuker = (BedNuker) Epilogue.moduleManager.modules.get(BedNuker.class);
                    if (bedNuker.isEnabled() && bedNuker.isReady()) {
                        return false;
                    } else if (Epilogue.moduleManager.modules.get(Scaffold.class).isEnabled()) {
                        return false;
                    } else if (this.requirePress.getValue()) {
                        return PlayerUtil.isAttacking();
                    } else {
                        return !this.allowMining.getValue() || !mc.objectMouseOver.typeOfHit.equals(MovingObjectType.BLOCK) || !PlayerUtil.isAttacking();
                    }
                }
            }
        } else {
            return false;
        }
    }

    private boolean canAutoBlock() {
        if (!ItemUtil.isHoldingSword()) {
            return false;
        } else {
            return !this.autoBlockRequirePress.getValue() || PlayerUtil.isUsingItem();
        }
    }

    private boolean hasValidTarget() {
        return mc.theWorld
                .loadedEntityList
                .stream()
                .anyMatch(
                        entity -> entity instanceof EntityLivingBase
                                && this.isValidTarget((EntityLivingBase) entity)
                                && this.isInRange((EntityLivingBase) entity)
                );
    }

    private boolean isValidTarget(EntityLivingBase entityLivingBase) {
        if (!mc.theWorld.loadedEntityList.contains(entityLivingBase)) {
            return false;
        } else if (entityLivingBase != mc.thePlayer && entityLivingBase != mc.thePlayer.ridingEntity) {
            if (entityLivingBase == mc.getRenderViewEntity() || entityLivingBase == mc.getRenderViewEntity().ridingEntity) {
                return false;
            } else if (entityLivingBase.deathTime > 0) {
                return false;
            } else if (RotationUtil.angleToEntity(entityLivingBase) > this.fov.getValue().floatValue()) {
                return false;
            } else if (!this.throughWalls.getValue() && RotationUtil.rayTrace(entityLivingBase) != null) {
                return false;
            } else if (entityLivingBase instanceof EntityOtherPlayerMP) {
                if (!this.players.getValue()) {
                    return false;
                } else if (TeamUtil.isFriend((EntityPlayer) entityLivingBase)) {
                    return false;
                } else {
                    return (!this.teams.getValue() || !TeamUtil.isSameTeam((EntityPlayer) entityLivingBase)) && (!this.botCheck.getValue() || !TeamUtil.isBot((EntityPlayer) entityLivingBase));
                }
            } else if (entityLivingBase instanceof EntityDragon || entityLivingBase instanceof EntityWither) {
                return this.bosses.getValue();
            } else if (!(entityLivingBase instanceof EntityMob) && !(entityLivingBase instanceof EntitySlime)) {
                if (entityLivingBase instanceof EntityAnimal
                        || entityLivingBase instanceof EntityBat
                        || entityLivingBase instanceof EntitySquid
                        || entityLivingBase instanceof EntityVillager) {
                    return this.animals.getValue();
                } else if (!(entityLivingBase instanceof EntityIronGolem)) {
                    return false;
                } else {
                    return this.golems.getValue() && (!this.teams.getValue() || !TeamUtil.hasTeamColor(entityLivingBase));
                }
            } else if (!(entityLivingBase instanceof EntitySilverfish)) {
                return this.mobs.getValue();
            } else {
                return this.silverfish.getValue() && (!this.teams.getValue() || !TeamUtil.hasTeamColor(entityLivingBase));
            }
        } else {
            return false;
        }
    }

    private boolean isInRange(EntityLivingBase entityLivingBase) {
        return this.isInBlockRange(entityLivingBase) || this.isInSwingRange(entityLivingBase) || this.isInAttackRange(entityLivingBase);
    }

    private boolean isInBlockRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.autoBlockRange.getValue();
    }

    private boolean isInSwingRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.swingRange.getValue();
    }

    private boolean isBoxInSwingRange(AxisAlignedBB axisAlignedBB) {
        return RotationUtil.distanceToBox(axisAlignedBB) <= (double) this.swingRange.getValue();
    }

    private boolean isInAttackRange(EntityLivingBase entityLivingBase) {
        return RotationUtil.distanceToEntity(entityLivingBase) <= (double) this.attackRange.getValue();
    }

    private boolean isBoxInAttackRange(AxisAlignedBB axisAlignedBB) {
        return !(RotationUtil.distanceToBox(axisAlignedBB) <= (double) this.attackRange.getValue());
    }

    private boolean isPlayerTarget(EntityLivingBase entityLivingBase) {
        return entityLivingBase instanceof EntityPlayer && TeamUtil.isTarget((EntityPlayer) entityLivingBase);
    }

    public Aura() {
        super("Aura", false);
        this.mode = new ModeValue("Select Mode", 1, new String[]{"Single", "Switch"});
        this.switchDelay = new IntValue("Switch Delay", 150, 0, 1000);
        this.sort = new ModeValue("Sort Mode", 1, new String[]{"Distance", "Health", "HurtTime", "FOV"});
        this.moveFix = new ModeValue("MoveFix Mode", 1, new String[]{"NONE", "Silent", "Strict"});
        this.rotations = new ModeValue("Rotation Mode", 2, new String[]{"NONE", "Manual", "Silent", "LockView", "Simulation"});
        this.yaw = new ModeValue("Yaw Mode", 2, new String[]{"Linear", "SmoothLinear", "EIO", "PhysicalSimulation", "SkewedUnimodal", "SimpleNeuralNetwork"}, () -> this.rotations.getValue() == 4);
        this.pitch = new ModeValue("Pitch Mode", 2, new String[]{"Linear", "SmoothLinear", "EIO", "PhysicalSimulation", "SkewedUnimodal", "SimpleNeuralNetwork"}, () -> this.rotations.getValue() == 4);
        this.smoothBack = new BooleanValue("Smooth Back", true, () -> this.rotations.getValue() == 4);
        this.fov = new IntValue("FOV", 360, 30, 360);
        this.autoBlock = new ModeValue(
                "AutoBlock Mode", 4, new String[]{"NONE", "Vanilla", "Fake", "BlinkLess", "Prediction", "Safe"}
        );
        this.autoBlockRequirePress = new BooleanValue("AutoBlock Only Right Click", false, () -> this.autoBlock.getValue() != 0 && this.autoBlock.getValue() != 2);
        this.autoBlockCPS = new IntValue("AutoBlock CPS", 10, 1, 10);
        this.autoBlockRange = new FloatValue("AutoBlock Range", 6.0F, 3.0F, 8.0F);
        this.swingRange = new FloatValue("Swing Range", 3.5F, 3.0F, 6.0F);
        this.attackRange = new FloatValue("Attack Range", 3.0F, 3.0F, 6.0F);
        this.minCPS = new IntValue("Min CPS", 10, 1, 20);
        this.maxCPS = new IntValue("Max CPS", 10, 1, 20);
        this.requirePress = new BooleanValue("Attack Only Left Click", false);
        this.throughWalls = new BooleanValue("Through Walls", true);
        this.weaponsOnly = new BooleanValue("Only Weapons", true);
        this.allowTools = new BooleanValue("Allow Tools", false, this.weaponsOnly::getValue);
        this.allowMining = new BooleanValue("Break Block", true);
        this.inventoryCheck = new BooleanValue("No Inventory", true);
        this.teams = new BooleanValue("Teams", true);
        this.botCheck = new BooleanValue("Anti Bot", true);
        this.players = new BooleanValue("Player", true);
        this.mobs = new BooleanValue("Monster", false);
        this.animals = new BooleanValue("Animals", false);
        this.bosses = new BooleanValue("Bosses", false);
        this.silverfish = new BooleanValue("ClothesMoth", false);
        this.golems = new BooleanValue("Golems", false);
    }

    public EntityLivingBase getTarget() {
        return this.target != null ? this.target.getEntity() : null;
    }

    public boolean isAttackAllowed() {
        Scaffold scaffold = (Scaffold) Epilogue.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled()) {
            return false;
        } else if (!this.weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            return !this.requirePress.getValue() || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode());
        } else {
            return false;
        }
    }

    public boolean shouldAutoBlock() {
        if (this.isPlayerBlocking() && this.isBlocking) {
            return !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && (this.autoBlock.getValue() == 3
                    || this.autoBlock.getValue() == 4
                    || this.autoBlock.getValue() == 5
                    || this.autoBlock.getValue() == 6
                    || this.autoBlock.getValue() == 7);
        } else {
            return false;
        }
    }

    public boolean isBlocking() {
        return this.fakeBlockState && ItemUtil.isHoldingSword();
    }

    public boolean isPlayerBlocking() {
        return (mc.thePlayer.isUsingItem() || this.blockingState) && ItemUtil.isHoldingSword();
    }

    @EventTarget(Priority.LOW)
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            this.cameraRotation = new Rotation(event.getYaw(), event.getPitch());
        }

        if (event.getType() == EventType.POST && this.blinkReset) {
            this.blinkReset = false;
            Epilogue.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
            Epilogue.blinkManager.setBlinkState(true, BlinkModules.AUTO_BLOCK);
        }

        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.target == null) {
                if (!this.smoothBackActive) {
                    if (this.smoothBack.getValue() && this.rotations.getValue() == 4 && lastAuraRotation != null) {
                        this.smoothBackActive = true;
                        this.smoothBackTargetRotation = this.cameraRotation;
                        this.smoothBackRotation = lastAuraRotation;
                        this.smoothBackSpeedNoise = 0f;
                        this.smoothBackNoiseTick = 0;
                        this.postTargetLostTicks = 4;
                    }
                }
            }

            if (this.smoothBackActive) {
                this.target = null;
            }

            if (this.attackDelayMS > 0L) {
                this.attackDelayMS -= 50L;
            }
            boolean attack = !this.smoothBackActive && this.target != null && this.canAttack();
            boolean block = attack && this.canAutoBlock();

            if (!block) {
                Epilogue.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                this.isBlocking = false;
                this.fakeBlockState = false;
                this.blockTick = 0;
            }
            if (attack) {
                boolean swap = false;
                boolean blocked = false;
                if (block) {
                    switch (this.autoBlock.getValue()) {

                        case 0: {
                            if (PlayerUtil.isUsingItem()) {
                                this.isBlocking = true;
                                if (!this.isPlayerBlocking() && !Epilogue.playerStateManager.digging && !Epilogue.playerStateManager.placing) {
                                    swap = true;
                                }
                            } else {
                                this.isBlocking = false;
                                if (this.isPlayerBlocking() && !Epilogue.playerStateManager.digging && !Epilogue.playerStateManager.placing) {
                                    this.stopBlock();
                                }
                            }
                            Epilogue.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                            this.fakeBlockState = false;
                            break;
                        }
                        case 1: {
                            if (this.hasValidTarget()) {
                                if (!this.isPlayerBlocking() && !Epilogue.playerStateManager.digging && !Epilogue.playerStateManager.placing) {
                                    swap = true;
                                }
                                Epilogue.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                this.isBlocking = true;
                            } else {
                                Epilogue.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                this.isBlocking = false;
                            }
                            this.fakeBlockState = false;
                            break;
                        }
                        case 2: {
                            Epilogue.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                            this.isBlocking = false;
                            this.fakeBlockState = this.hasValidTarget();
                            if (PlayerUtil.isUsingItem()
                                    && !this.isPlayerBlocking()
                                    && !Epilogue.playerStateManager.digging
                                    && !Epilogue.playerStateManager.placing) {
                                swap = true;
                            }
                            break;
                        }
                        case 3: {
                            if (this.hasValidTarget()) {
                                if (!Epilogue.playerStateManager.digging && !Epilogue.playerStateManager.placing) {
                                    switch (this.blockTick) {
                                        case 0: {
                                            this.setCurrentSlot();
                                            if (!this.isPlayerBlocking()) {
                                                swap = true;
                                            }
                                            blocked = true;
                                            this.blockTick = 1;
                                            break;
                                        }
                                        case 1: {
                                            if (this.isPlayerBlocking()) {
                                                this.stopBlock();
                                            }
                                            attack = false;
                                            int emptySlot = this.findEmptySlot(Aura.mc.thePlayer.inventory.currentItem);
                                            PacketUtil.sendPacket(new C09PacketHeldItemChange(emptySlot));
                                            this.swapped = true;
                                            if (this.attackDelayMS > 50L) break;
                                            this.blockTick = 0;
                                            break;
                                        }
                                        default: {
                                            this.blockTick = 0;
                                            this.setCurrentSlot();
                                        }
                                    }
                                }
                                this.isBlocking = true;
                                this.fakeBlockState = true;
                                break;
                            }
                            if (this.blockTick == 1 && this.isPlayerBlocking()) {
                                this.stopBlock();
                                this.setCurrentSlot();
                            }
                            this.blockTick = 0;
                            Epilogue.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                            this.isBlocking = false;
                            this.fakeBlockState = false;
                            break;
                        }
                        case 4: {
                            if (this.hasValidTarget()) {
                                if (!Epilogue.playerStateManager.digging && !Epilogue.playerStateManager.placing) {
                                    switch (this.blockTick) {
                                        case 0: {
                                            this.setCurrentSlot();
                                            if (!this.isPlayerBlocking()) {
                                                swap = true;
                                            }
                                            blocked = true;
                                            this.blockTick = 1;
                                            break;
                                        }
                                        case 1: {
                                            if (this.isPlayerBlocking()) {
                                                this.stopBlock();
                                                attack = false;
                                            }
                                            if (this.attackDelayMS > 50L) break;
                                            this.setNextSlot();
                                            this.blockTick = 0;
                                            break;
                                        }
                                        default: {
                                            this.blockTick = 0;
                                            this.setCurrentSlot();
                                        }
                                    }
                                }
                                this.isBlocking = true;
                                this.fakeBlockState = true;
                                break;
                            }
                            if (this.blockTick == 1 && this.isPlayerBlocking()) {
                                this.stopBlock();
                                this.setNextSlot();
                            }
                            this.blockTick = 0;
                            this.setCurrentSlot();
                            Epilogue.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                            this.isBlocking = false;
                            this.fakeBlockState = false;
                        }
                        case 5: {
                            if (this.hasValidTarget()) {
                                if (!Epilogue.playerStateManager.digging && !Epilogue.playerStateManager.placing) {
                                    switch (this.blockTick) {
                                        case 0:
                                            if (!this.isPlayerBlocking()) {
                                                swap = true;
                                            }
                                            blocked = true;
                                            this.blockTick = 1;
                                            break;
                                        case 1:
                                            if (this.isPlayerBlocking()) {
                                                if (Epilogue.moduleManager.modules.get(NoSlow.class).isEnabled()) {
                                                    int randomSlot = new Random().nextInt(9);
                                                    while (randomSlot == mc.thePlayer.inventory.currentItem) {
                                                        randomSlot = new Random().nextInt(9);
                                                    }
                                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(randomSlot));
                                                    PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                                                }
                                                this.stopBlock();
                                                attack = false;
                                            }
                                            if (this.attackDelayMS <= 50L) {
                                                this.blockTick = 0;
                                            }
                                            break;
                                        default:
                                            this.blockTick = 0;
                                    }
                                }
                                this.isBlocking = true;
                                this.fakeBlockState = true;
                            } else {
                                Epilogue.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                                this.isBlocking = false;
                                this.fakeBlockState = false;
                            }
                            break;
                        }
                    }
                }
                boolean attacked = false;
                if (this.isBoxInSwingRange(this.target.getBox())) {
                    if (!rotationBlocked && (this.rotations.getValue() == 2 || this.rotations.getValue() == 3)) {

                        float[] rotations = RotationUtil.getRotationsToBox(
                                this.target.getBox(),
                                event.getYaw(),
                                event.getPitch(),
                                90F + RandomUtil.nextFloat(-5.0F, 5.0F),
                                0
                        );
                        event.setRotation(rotations[0], rotations[1], 1);
                        if (this.rotations.getValue() == 3) {
                            Epilogue.rotationManager.setRotation(rotations[0], rotations[1], 1, true);
                        }
                        if (this.moveFix.getValue() != 0 || this.rotations.getValue() == 3) {
                            event.setPervRotation(rotations[0], 1);
                        }
                        lastAuraRotation = new Rotation(event.getNewYaw(), event.getNewPitch());
                    } else if (!rotationBlocked && this.rotations.getValue() == 4) {

                        simRotationSystem.setYawAlgorithm(yaw.getValuePrompt());
                        simRotationSystem.setPitchAlgorithm(pitch.getValuePrompt());
                        simRotationSystem.setSimulateFriction(true);
                        simRotationSystem.setDebugTurnSpeed(false);
                        simRotationSystem.setFrictionAlgorithm("TimeIncremental");
                        Rotation base = new Rotation(event.getYaw(), event.getPitch());
                        Rotation next = simRotationSystem.compute(this.target.getEntity(), base);
                        Rotation clamped = clampStep(base, next);
                        event.setRotation(clamped.yaw, clamped.pitch, 1);
                        if (this.moveFix.getValue() != 0) {
                            event.setPervRotation(event.getNewYaw(), 1);
                        }
                        lastAuraRotation = new Rotation(event.getNewYaw(), event.getNewPitch());
                    }
                    if (attack) {
                        attacked = this.performAttack(event.getNewYaw(), event.getNewPitch());
                    }
                }

                if (swap) {
                    if (attacked) {
                        this.interactAttack(event.getNewYaw(), event.getNewPitch());
                    } else {
                        this.sendUseItem();
                    }
                }
                if (blocked) {
                    Epilogue.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    Epilogue.blinkManager.setBlinkState(true, BlinkModules.AUTO_BLOCK);
                }
            }
        }
        if (event.getType() == EventType.PRE) {
            boolean shouldSmoothBack = this.smoothBack.getValue()
                    && this.rotations.getValue() != 0
                    && (this.smoothBackRotation != null || lastAuraRotation != null)
                    && (this.smoothBackActive || !this.isEnabled());

            if (shouldSmoothBack) {
                Rotation base = this.smoothBackRotation != null ? this.smoothBackRotation : lastAuraRotation;
                Rotation targetRot = this.smoothBackTargetRotation != null
                        ? this.smoothBackTargetRotation
                        : (this.cameraRotation != null ? this.cameraRotation : new Rotation(event.getYaw(), event.getPitch()));
                Rotation next = smoothBackStep(base, targetRot);
                this.smoothBackRotation = next;
                lastAuraRotation = next;
                event.setRotation(next.yaw, next.pitch, 1);
                if (this.moveFix.getValue() != 0) {
                    event.setPervRotation(event.getNewYaw(), 1);
                }
                if (RotationUtils.abs(RotationUtils.getAngleDiff(event.getNewYaw(), targetRot.yaw)) < 0.35f &&
                        RotationUtils.abs(RotationUtils.getAngleDiff(event.getNewPitch(), targetRot.pitch)) < 0.35f) {
                    lastAuraRotation = null;
                    this.smoothBackActive = false;
                    this.smoothBackTargetRotation = null;
                    this.smoothBackRotation = null;
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    if (this.postTargetLostTicks > 0) {
                        this.postTargetLostTicks--;
                        this.target = null;
                        break;
                    }
                    if (this.smoothBackActive) {
                        break;
                    }
                    if (this.target == null
                            || !this.isValidTarget(this.target.getEntity())
                            || this.isBoxInAttackRange(this.target.getBox())
                            || !this.isBoxInSwingRange(this.target.getBox())
                            || this.timer.hasTimeElapsed(this.switchDelay.getValue().longValue())) {

                        this.timer.reset();
                        ArrayList<EntityLivingBase> targets = new ArrayList<>();
                        for (Entity entity : mc.theWorld.loadedEntityList) {
                            if (entity instanceof EntityLivingBase
                                    && this.isValidTarget((EntityLivingBase) entity)
                                    && this.isInRange((EntityLivingBase) entity)) {
                                targets.add((EntityLivingBase) entity);
                            }
                        }
                        if (targets.isEmpty()) {
                            this.target = null;
                        } else {
                            if (targets.stream().anyMatch(this::isInSwingRange)) {
                                targets.removeIf(entityLivingBase -> !this.isInSwingRange(entityLivingBase));
                            }
                            if (targets.stream().anyMatch(this::isInAttackRange)) {
                                targets.removeIf(entityLivingBase -> !this.isInAttackRange(entityLivingBase));
                            }
                            if (targets.stream().anyMatch(this::isPlayerTarget)) {
                                targets.removeIf(entityLivingBase -> !this.isPlayerTarget(entityLivingBase));
                            }
                            targets.sort(
                                    (entityLivingBase1, entityLivingBase2) -> {
                                        int sortBase = 0;
                                        switch (this.sort.getValue()) {
                                            case 1:
                                                sortBase = Float.compare(TeamUtil.getHealthScore(entityLivingBase1), TeamUtil.getHealthScore(entityLivingBase2));
                                                break;
                                            case 2:
                                                sortBase = Integer.compare(entityLivingBase1.hurtResistantTime, entityLivingBase2.hurtResistantTime);
                                                break;
                                            case 3:
                                                sortBase = Float.compare(
                                                        RotationUtil.angleToEntity(entityLivingBase1),
                                                        RotationUtil.angleToEntity(entityLivingBase2)
                                                );
                                        }
                                        return sortBase != 0
                                                ? sortBase
                                                : Double.compare(RotationUtil.distanceToEntity(entityLivingBase1), RotationUtil.distanceToEntity(entityLivingBase2));
                                    }
                            );
                            if (this.mode.getValue() == 1 && this.hitRegistered) {
                                this.hitRegistered = false;
                                this.switchTick++;
                            }
                            if (this.mode.getValue() == 0 || this.switchTick >= targets.size()) {
                                this.switchTick = 0;
                            }
                            this.target = new AttackData(targets.get(this.switchTick));
                        }
                    }
                    if (this.target != null) {
                        this.target = new AttackData(this.target.getEntity());
                    }
                    break;
                case POST:
                    if (this.isPlayerBlocking() && !mc.thePlayer.isBlocking()) {
                        mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                    }
            }
        }
    }

    private int findEmptySlot(int currentSlot) {
        int i;
        for (i = 0; i < 9; ++i) {
            if (i == currentSlot || Aura.mc.thePlayer.inventory.getStackInSlot(i) != null) continue;
            return i;
        }
        for (i = 0; i < 9; ++i) {
            ItemStack stack;
            if (i == currentSlot || (stack = Aura.mc.thePlayer.inventory.getStackInSlot(i)) == null || stack.hasDisplayName()) continue;
            return i;
        }
        return Math.floorMod(currentSlot - 1, 9);
    }

    @EventTarget(Priority.LOWEST)
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && !event.isCancelled()) {
            if (event.getPacket() instanceof C07PacketPlayerDigging) {
                C07PacketPlayerDigging packet = (C07PacketPlayerDigging) event.getPacket();
                if (packet.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                    this.blockingState = false;
                }
            }
            if (event.getPacket() instanceof C09PacketHeldItemChange) {
                this.blockingState = false;
                if (this.isBlocking) {
                    mc.thePlayer.stopUsingItem();
                }
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.moveFix.getValue() == 1
                    && this.rotations.getValue() != 3
                    && RotationState.isActived()
                    && RotationState.getPriority() == 1.0F
                    && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
            if (this.shouldAutoBlock()) {
                mc.thePlayer.movementInput.jump = false;
            }
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        } else {
            if (this.isEnabled() && this.target != null && this.canAttack()) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        } else {
            if (this.isEnabled() && this.target != null && this.canAttack()) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        } else {
            if (this.isEnabled() && this.target != null && this.canAttack()) {
                event.setCancelled(true);
            }
        }
    }

    @EventTarget
    public void onCancelUse(CancelUseEvent event) {
        if (this.isBlocking) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEnabled() {
        this.target = null;
        this.switchTick = 0;
        this.hitRegistered = false;
        this.attackDelayMS = 0L;
        this.blockTick = 0;
        this.lastAuraRotation = null;
        this.smoothBackActive = false;
        this.smoothBackTargetRotation = null;
        this.smoothBackRotation = null;
        this.postTargetLostTicks = 0;
        this.smoothBackSpeedNoise = 0f;
        this.smoothBackNoiseTick = 0;
    }

    @Override
    public void onDisabled() {
        Epilogue.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
        this.blockingState = false;
        this.isBlocking = false;
        this.fakeBlockState = false;
        if (this.smoothBack.getValue() && this.rotations.getValue() == 4 && this.lastAuraRotation != null) {
            this.smoothBackActive = true;
            this.smoothBackTargetRotation = this.cameraRotation;
            this.smoothBackRotation = this.lastAuraRotation;
            this.smoothBackSpeedNoise = 0f;
            this.smoothBackNoiseTick = 0;
        } else {
            this.smoothBackActive = false;
            this.smoothBackTargetRotation = null;
            this.smoothBackRotation = null;
        }
        this.postTargetLostTicks = 0;
    }

    @Override
    public void verifyValue(String mode) {
        if (!this.autoBlock.getName().equals(mode) && !this.autoBlockCPS.getName().equals(mode)) {
            if (this.swingRange.getName().equals(mode)) {
                if (this.swingRange.getValue() < this.attackRange.getValue()) {
                    this.attackRange.setValue(this.swingRange.getValue());
                }
            } else if (this.attackRange.getName().equals(mode)) {
                if (this.swingRange.getValue() < this.attackRange.getValue()) {
                    this.swingRange.setValue(this.attackRange.getValue());
                }
            } else if (this.minCPS.getName().equals(mode)) {
                if (this.minCPS.getValue() > this.maxCPS.getValue()) {
                    this.maxCPS.setValue(this.minCPS.getValue());
                }
            } else {
                if (this.maxCPS.getName().equals(mode) && this.minCPS.getValue() > this.maxCPS.getValue()) {
                    this.minCPS.setValue(this.maxCPS.getValue());
                }
            }
        } else {
            boolean badCps = this.autoBlock.getValue() == 2
                    || this.autoBlock.getValue() == 3
                    || this.autoBlock.getValue() == 4
                    || this.autoBlock.getValue() == 5
                    || this.autoBlock.getValue() == 6
                    || this.autoBlock.getValue() == 7;
            if (badCps && this.autoBlockCPS.getValue() > 10.0F) {
                this.autoBlockCPS.setValue(10.0F);
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString()), this.autoBlock.getModeString()};
    }

    public static class AttackData {
        @Getter
        private final EntityLivingBase entity;
        @Getter
        private final AxisAlignedBB box;
        @Getter
        private final double x;
        @Getter
        private final double y;
        @Getter
        private final double z;

        public AttackData(EntityLivingBase entityLivingBase) {
            this.entity = entityLivingBase;
            double collisionBorderSize = entityLivingBase.getCollisionBorderSize();
            this.box = entityLivingBase.getEntityBoundingBox().expand(collisionBorderSize, collisionBorderSize, collisionBorderSize);
            this.x = entityLivingBase.posX;
            this.y = entityLivingBase.posY;
            this.z = entityLivingBase.posZ;
        }

    }

    private void setNextSlot() {
        int bestSwapSlot = getNextSlot();
        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(bestSwapSlot));
        swapped = true;
    }

    private void setCurrentSlot() {
        if (!swapped) {
            return;
        }
        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
        swapped = false;
    }

    private int getNextSlot() {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        int next;
        if (currentSlot < 8) {
            next = currentSlot + 1;
        }
        else {
            next = currentSlot - 1;
        }
        return next;
    }
}