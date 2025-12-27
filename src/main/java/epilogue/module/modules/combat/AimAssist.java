package epilogue.module.modules.combat;

import epilogue.Epilogue;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.KeyEvent;
import epilogue.events.TickEvent;
import epilogue.module.Module;
import epilogue.util.*;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.PercentValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import epilogue.module.modules.combat.rotation.OPRotationSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AimAssist extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    public final FloatValue hSpeed = new FloatValue("Horizontal Speed", 3.0F, 0.0F, 10.0F);
    public final FloatValue vSpeed = new FloatValue("Vertical Speed", 0.0F, 0.0F, 10.0F);
    public final PercentValue smoothing = new PercentValue("Smoothing", 50);
    public final FloatValue range = new FloatValue("Range", 4.5F, 3.0F, 8.0F);
    public final IntValue fov = new IntValue("Fov", 90, 30, 360);
    public final BooleanValue weaponOnly = new BooleanValue("Weapons Only", true);
    public final BooleanValue allowTools = new BooleanValue("Allow Tools", false, this.weaponOnly::getValue);
    public final BooleanValue botChecks = new BooleanValue("Bot Check", true);
    public final BooleanValue team = new BooleanValue("Teams", true);
    
    public final ModeValue rotationMode = new ModeValue("Rotation Mode", 0, new String[]{"Normal", "OP Rotation"});
    public final ModeValue yawAlgorithm = new ModeValue("Yaw Algorithm", 0,
        new String[]{"Linear", "SmoothLinear", "EIO", "SkewedUnimodal", "PhysicalSimulation", "SimpleNeuralNetwork"
        //        , "Recorded-Features"
        },
        () -> rotationMode.getValue() == 1);
    public final ModeValue pitchAlgorithm = new ModeValue("PitchAlgorithm", 0,
        new String[]{"Linear", "SmoothLinear", "EIO", "SkewedUnimodal", "PhysicalSimulation", "SimpleNeuralNetwork"
        //        , "Recorded-Features"
        },
        () -> rotationMode.getValue() == 1);
    public final BooleanValue simulateFriction = new BooleanValue("Simulate Friction", true,
        () -> rotationMode.getValue() == 1);
    public final ModeValue frictionAlgorithm = new ModeValue("Friction Algorithm", 0,
        new String[]{"TimeIncremental", "CustomCurve", "TPAC"},
        () -> rotationMode.getValue() == 1 && simulateFriction.getValue());
    public final BooleanValue debugTurnSpeed = new BooleanValue("Debug Turn Speed", false,
        () -> rotationMode.getValue() == 1);
//    public final BooleanValue recordMode = new BooleanValue("RecordMode", false,
//        () -> rotationMode.getValue() == 1 &&
//              (yawAlgorithm.getValue() == 6 ||
//               pitchAlgorithm.getValue() == 6));

    public final BooleanValue stopOnTarget = new BooleanValue("Stop On Target", false);
    public final IntValue delayTick = new IntValue("Delay Tick", 0, 0, 5);
    
    private final OPRotationSystem opRotationSystem = new OPRotationSystem();
    private int tickCounter = 0;

    private boolean isValidTarget(EntityPlayer entityPlayer) {
        if (entityPlayer != mc.thePlayer && entityPlayer != mc.thePlayer.ridingEntity) {
            if (entityPlayer == mc.getRenderViewEntity() || entityPlayer == mc.getRenderViewEntity().ridingEntity) {
                return false;
            } else if (entityPlayer.deathTime > 0) {
                return false;
            } else if (RotationUtil.distanceToEntity(entityPlayer) > (double) this.range.getValue()) {
                return false;
            } else if (RotationUtil.angleToEntity(entityPlayer) > (float) this.fov.getValue()) {
                return false;
            } else if (RotationUtil.rayTrace(entityPlayer) != null) {
                return false;
            } else if (TeamUtil.isFriend(entityPlayer)) {
                return false;
            } else {
                return (!this.team.getValue() || !TeamUtil.isSameTeam(entityPlayer)) && (!this.botChecks.getValue() || !TeamUtil.isBot(entityPlayer));
            }
        } else {
            return false;
        }
    }

    private boolean isInReach(EntityPlayer entityPlayer) {
        Range range = (Range) Epilogue.moduleManager.modules.get(Range.class);
        double distance = range.isEnabled() ? (double) range.range.getValue() : 3.0;
        return RotationUtil.distanceToEntity(entityPlayer) <= distance;
    }

    private boolean isLookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }
    
    private boolean isLookingAtPlayer(EntityPlayer player) {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.ENTITY) {
            if (mc.objectMouseOver.entityHit == player) {
                float randomChance = RandomUtil.nextFloat(0.7f, 0.95f);
                return Math.random() < randomChance;
            }
        }
        return false;
    }

    public AimAssist() {
        super("AimAssist", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST && mc.currentScreen == null) {
            
            if (delayTick.getValue() > 0) {
                tickCounter++;
                if (tickCounter < delayTick.getValue()) {
                    return;
                }
                tickCounter = 0;
            }
            if (!(java.lang.Boolean) this.weaponOnly.getValue()
                    || ItemUtil.hasRawUnbreakingEnchant()
                    || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
                boolean attacking = PlayerUtil.isAttacking();
                if (!attacking || !this.isLookingAtBlock()) {
                    if (attacking || !this.timer.hasTimeElapsed(350L)) {
                        List<EntityPlayer> inRange = mc.theWorld
                                .loadedEntityList
                                .stream()
                                .filter(entity -> entity instanceof EntityPlayer)
                                .map(entity -> (EntityPlayer) entity)
                                .filter(this::isValidTarget)
                                .sorted(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                                .collect(Collectors.toList());
                        if (!inRange.isEmpty()) {
                            if (inRange.stream().anyMatch(this::isInReach)) {
                                inRange.removeIf(entityPlayer -> !this.isInReach(entityPlayer));
                            }
                            EntityPlayer player = inRange.get(0);
                            if (!(RotationUtil.distanceToEntity(player) <= 0.0)) {
                                
                                if (stopOnTarget.getValue() && isLookingAtPlayer(player)) {
                                    return;
                                }
                                
                                if (rotationMode.getValue() == 1) {
                                    updateOPRotationSettings();
                                    opRotationSystem.conduct(player);
                                    
                                    if (debugTurnSpeed.getValue()) {
                                        System.out.println("转头速度: " + opRotationSystem.getTurnSpeedPublic());
                                        System.out.println("角度差异: Yaw=" + opRotationSystem.getDiffRotsData().diffYaw + 
                                                         ", Pitch=" + opRotationSystem.getDiffRotsData().diffPitch);
                                        System.out.println("最大角度差异: Yaw=" + opRotationSystem.getMaxDiffRotsData().maxDiffYaw + 
                                                         ", Pitch=" + opRotationSystem.getMaxDiffRotsData().maxDiffPitch);
                                    }
                                } else {
                                    AxisAlignedBB axisAlignedBB = player.getEntityBoundingBox();
                                    double collisionBorderSize = player.getCollisionBorderSize();
                                    float[] rotation = RotationUtil.getRotationsToBox(
                                            axisAlignedBB.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize),
                                            mc.thePlayer.rotationYaw,
                                            mc.thePlayer.rotationPitch,
                                            180.0F,
                                            (float) this.smoothing.getValue() / 100.0F
                                    );
                                    float yaw = Math.min(Math.abs(this.hSpeed.getValue()), 10.0F);
                                    float pitch = Math.min(Math.abs(this.vSpeed.getValue()), 10.0F);
                                    Epilogue.rotationManager
                                            .setRotation(
                                                    mc.thePlayer.rotationYaw + (rotation[0] - mc.thePlayer.rotationYaw) * 0.1F * yaw,
                                                    mc.thePlayer.rotationPitch + (rotation[1] - mc.thePlayer.rotationPitch) * 0.1F * pitch,
                                                    0,
                                                    false
                                            );
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void updateOPRotationSettings() {
        opRotationSystem.setYawAlgorithm(yawAlgorithm.getModeString());
        opRotationSystem.setPitchAlgorithm(pitchAlgorithm.getModeString());
        opRotationSystem.setSimulateFriction(simulateFriction.getValue());
        opRotationSystem.setDebugTurnSpeed(debugTurnSpeed.getValue());
        opRotationSystem.setFrictionAlgorithm(frictionAlgorithm.getModeString());
        //opRotationSystem.setRecordMode(recordMode.getValue());
    }

    @EventTarget
    public void onPress(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindAttack.getKeyCode() && !Epilogue.moduleManager.modules.get(AutoClicker.class).isEnabled()) {
            this.timer.reset();
        }
    }
}

