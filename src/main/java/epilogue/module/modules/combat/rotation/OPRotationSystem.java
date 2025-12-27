package epilogue.module.modules.combat.rotation;

import epilogue.module.modules.combat.rotation.algorithm.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Map;

public class OPRotationSystem {
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private Map<String, TurnSpeedAlgorithm> algorithms = new HashMap<>();
    private FrictionSimulator frictionSimulator = new FrictionSimulator();
    
    private Rotation currRotation = new Rotation(0f, 0f);
    private Rotation aimRotation = new Rotation(0f, 0f);
    private DiffRots diffRotsData = new DiffRots(0f, 0f);
    private MaxDiffRots maxDiffRotsData = new MaxDiffRots(0f, 0f);
    private float turnSpeedPublic = 0f;
    
    private String yawAlgorithm = "Linear";
    private String pitchAlgorithm = "Linear";
    private boolean simulateFriction = true;
    private boolean debugTurnSpeed = false;
    
    private Entity target;

    public OPRotationSystem() {
        initializeAlgorithms();
    }

    private void initializeAlgorithms() {
        algorithms.put("Linear", new LinearAlgorithm(10f, 30f, 8f, 25f));
        algorithms.put("SmoothLinear", new SmoothLinearAlgorithm(10f, 30f, 8f, 25f));
        algorithms.put("EIO", new EIOAlgorithm());
        algorithms.put("Physical-Simulation", new PhysicalSimulationAlgorithm());
        algorithms.put("Skewed-Unimodal", new SkewedUnimodalAlgorithm());
        algorithms.put("Simple-NeuralNetwork", new SimpleNeuralNetworkAlgorithm());
        //algorithms.put("Recorded-Features", new RecordedFeaturesAlgorithm());
    }

    public void conduct(Entity target) {
        this.target = target;
        if (target == null) return;

        currRotation = RotationUtils.getCurrentRotation();
        aimRotation = getTargetRotation(target);
        
        updateRotsData();
        
        TurnSpeed turnSpeed = calculateTurnSpeed();
        
        if (simulateFriction) {
            frictionSimulator.conduct(aimRotation);
            FrictionSimulator.Friction friction = frictionSimulator.getFriction();
            turnSpeed.yaw *= friction.yaw;
            turnSpeed.pitch *= friction.pitch;
        }
        
        turnSpeed.yaw = RotationUtils.clamp(turnSpeed.yaw, 0f, 180f);
        turnSpeed.pitch = RotationUtils.clamp(turnSpeed.pitch, 0f, 180f);
        
        if (debugTurnSpeed) {
            System.out.println("TurnSpeed: " + turnSpeed.toString());
        }
        
        turnSpeedPublic = turnSpeed.getHypot();
        
        applyRotation(turnSpeed);
    }

    private Rotation getTargetRotation(Entity target) {
        return RotationUtils.getRotationToEntity(target);
    }

    private void updateRotsData() {
        float diffYaw = RotationUtils.getAngleDiff(currRotation.yaw, aimRotation.yaw);
        float diffPitch = RotationUtils.getAngleDiff(currRotation.pitch, aimRotation.pitch);
        
        diffRotsData.diffYaw = RotationUtils.abs(diffYaw);
        diffRotsData.diffPitch = RotationUtils.abs(diffPitch);
        
        if (diffRotsData.diffYaw < 3f && diffRotsData.diffPitch < 3f) {
            maxDiffRotsData.maxDiffYaw = diffRotsData.diffYaw;
            maxDiffRotsData.maxDiffPitch = diffRotsData.diffPitch;
        }
        
        maxDiffRotsData.maxDiffYaw = Math.max(maxDiffRotsData.maxDiffYaw, diffRotsData.diffYaw);
        maxDiffRotsData.maxDiffPitch = Math.max(maxDiffRotsData.maxDiffPitch, diffRotsData.diffPitch);
    }

    private TurnSpeed calculateTurnSpeed() {
        TurnSpeedAlgorithm yawAlg = algorithms.get(yawAlgorithm);
        TurnSpeedAlgorithm pitchAlg = algorithms.get(pitchAlgorithm);
        
        if (yawAlg == null) yawAlg = algorithms.get("Linear");
        if (pitchAlg == null) pitchAlg = algorithms.get("Linear");
        
        if (yawAlg instanceof PhysicalSimulationAlgorithm) {
            ((PhysicalSimulationAlgorithm) yawAlg).setTarget(target);
        }
        if (yawAlg instanceof SkewedUnimodalAlgorithm) {
            ((SkewedUnimodalAlgorithm) yawAlg).setTarget(target);
        }
        if (yawAlg instanceof SimpleNeuralNetworkAlgorithm) {
            ((SimpleNeuralNetworkAlgorithm) yawAlg).setTarget(target);
            ((SimpleNeuralNetworkAlgorithm) yawAlg).setHittable(isHittable());
        }
        if (pitchAlg instanceof PhysicalSimulationAlgorithm) {
            ((PhysicalSimulationAlgorithm) pitchAlg).setTarget(target);
        }
        if (pitchAlg instanceof SkewedUnimodalAlgorithm) {
            ((SkewedUnimodalAlgorithm) pitchAlg).setTarget(target);
        }
        if (pitchAlg instanceof SimpleNeuralNetworkAlgorithm) {
            ((SimpleNeuralNetworkAlgorithm) pitchAlg).setTarget(target);
            ((SimpleNeuralNetworkAlgorithm) pitchAlg).setHittable(isHittable());
        }
        
        TurnSpeed yawSpeed = yawAlg.calculateTurnSpeed(
            diffRotsData.diffYaw, diffRotsData.diffPitch, 
            maxDiffRotsData.maxDiffYaw, maxDiffRotsData.maxDiffPitch
        );
        
        TurnSpeed pitchSpeed = pitchAlg.calculateTurnSpeed(
            diffRotsData.diffYaw, diffRotsData.diffPitch, 
            maxDiffRotsData.maxDiffYaw, maxDiffRotsData.maxDiffPitch
        );
        
        return new TurnSpeed(yawSpeed.yaw, pitchSpeed.pitch);
    }

    private void applyRotation(TurnSpeed turnSpeed) {
        Rotation diffRotation = RotationUtils.getDiffRotation(currRotation, aimRotation);
        
        float yawChange = Math.min(RotationUtils.abs(diffRotation.yaw), turnSpeed.yaw);
        float pitchChange = Math.min(RotationUtils.abs(diffRotation.pitch), turnSpeed.pitch);
        
        if (diffRotation.yaw > 0) {
            mc.thePlayer.rotationYaw += yawChange;
        } else if (diffRotation.yaw < 0) {
            mc.thePlayer.rotationYaw -= yawChange;
        }
        
        if (diffRotation.pitch > 0) {
            mc.thePlayer.rotationPitch += pitchChange;
        } else if (diffRotation.pitch < 0) {
            mc.thePlayer.rotationPitch -= pitchChange;
        }
        
        mc.thePlayer.rotationPitch = RotationUtils.clamp(mc.thePlayer.rotationPitch, -90f, 90f);
    }

    public void setYawAlgorithm(String algorithm) {
        this.yawAlgorithm = algorithm;
    }

    public void setPitchAlgorithm(String algorithm) {
        this.pitchAlgorithm = algorithm;
    }

    public void setSimulateFriction(boolean simulate) {
        this.simulateFriction = simulate;
    }

    public void setDebugTurnSpeed(boolean debug) {
        this.debugTurnSpeed = debug;
    }

    public void setFrictionAlgorithm(String algorithm) {
        frictionSimulator.setFrictionAlgorithm(algorithm);
    }

    public float getTurnSpeedPublic() {
        return turnSpeedPublic;
    }

    public DiffRots getDiffRotsData() {
        return diffRotsData;
    }

    public MaxDiffRots getMaxDiffRotsData() {
        return maxDiffRotsData;
    }
    
    private boolean isHittable() {
        if (target == null) return false;
        double distance = RotationUtils.getDistanceToEntity(target);
        return distance <= 4.0;
    }
    
//    public void setRecordMode(boolean record) {
//        TurnSpeedAlgorithm recordedAlg = algorithms.get("Recorded-Features");
//        if (recordedAlg instanceof RecordedFeaturesAlgorithm) {
//            ((RecordedFeaturesAlgorithm) recordedAlg).setRecordMode(record);
//        }
//    }

    public static class DiffRots {
        public float diffYaw;
        public float diffPitch;

        public DiffRots(float diffYaw, float diffPitch) {
            this.diffYaw = diffYaw;
            this.diffPitch = diffPitch;
        }
    }

    public static class MaxDiffRots {
        public float maxDiffYaw;
        public float maxDiffPitch;

        public MaxDiffRots(float maxDiffYaw, float maxDiffPitch) {
            this.maxDiffYaw = maxDiffYaw;
            this.maxDiffPitch = maxDiffPitch;
        }
    }
}
