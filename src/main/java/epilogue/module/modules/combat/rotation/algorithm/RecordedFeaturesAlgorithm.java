package epilogue.module.modules.combat.rotation.algorithm;

import epilogue.module.modules.combat.rotation.TurnSpeed;
import epilogue.module.modules.combat.rotation.RotationUtils;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RecordedFeaturesAlgorithm implements TurnSpeedAlgorithm {
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private TurnSpeed turnSpeed = new TurnSpeed(0f, 0f);
    private boolean recordTurnSpeed = false;
    private float maxDiffYaw = 0f;
    private float maxDiffPitch = 0f;
    
    private File dataDir;
    private File yawDir;
    private File pitchDir;
    private File ySpeedFile;
    private File yMaxDiffFile;
    private File pSpeedFile;
    private File pMaxDiffFile;

    public RecordedFeaturesAlgorithm() {
        initializeFiles();
    }

    private void initializeFiles() {
        try {
            String gameDir = mc.mcDataDir.getAbsolutePath();
            dataDir = new File(gameDir, "Epilogue/rotation/recorded_features");
            yawDir = new File(dataDir, "yaw");
            pitchDir = new File(dataDir, "pitch");
            
            yawDir.mkdirs();
            pitchDir.mkdirs();
            
            ySpeedFile = new File(yawDir, "speed.txt");
            yMaxDiffFile = new File(yawDir, "maxd.txt");
            pSpeedFile = new File(pitchDir, "speed.txt");
            pMaxDiffFile = new File(pitchDir, "maxd.txt");
            
            if (!ySpeedFile.exists()) ySpeedFile.createNewFile();
            if (!yMaxDiffFile.exists()) yMaxDiffFile.createNewFile();
            if (!pSpeedFile.exists()) pSpeedFile.createNewFile();
            if (!pMaxDiffFile.exists()) pMaxDiffFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRecordMode(boolean record) {
        this.recordTurnSpeed = record;
    }

    @Override
    public TurnSpeed calculateTurnSpeed(float diffYaw, float diffPitch, float maxDiffYaw, float maxDiffPitch) {
        this.maxDiffYaw = maxDiffYaw;
        this.maxDiffPitch = maxDiffPitch;
        
        if (recordTurnSpeed) {
            recordMouseData(diffYaw, diffPitch, maxDiffYaw, maxDiffPitch);
        } else {
            calculateTurnSpeedFromRecords();
        }
        
        return turnSpeed;
    }

    private void recordMouseData(float diffYaw, float diffPitch, float maxDiffYaw, float maxDiffPitch) {
        try {
            int deltaYaw = getMouseDeltaX();
            int deltaPitch = getMouseDeltaY();
            
            if (deltaYaw > 0 && maxDiffYaw > 0f) {
                String lastMaxDiff = getLastLine(yMaxDiffFile);
                if (lastMaxDiff == null || !lastMaxDiff.equals(String.valueOf(maxDiffYaw))) {
                    appendToFile(ySpeedFile, String.valueOf(deltaYaw));
                    appendToFile(yMaxDiffFile, String.valueOf(maxDiffYaw));
                }
            }
            
            if (deltaPitch > 0 && maxDiffPitch > 0f) {
                String lastMaxDiff = getLastLine(pMaxDiffFile);
                if (lastMaxDiff == null || !lastMaxDiff.equals(String.valueOf(maxDiffPitch))) {
                    appendToFile(pSpeedFile, String.valueOf(deltaPitch));
                    appendToFile(pMaxDiffFile, String.valueOf(maxDiffPitch));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calculateTurnSpeedFromRecords() {
        try {
            float yawSpeed = calculateSpeedFromFile(ySpeedFile, yMaxDiffFile, maxDiffYaw);
            float pitchSpeed = calculateSpeedFromFile(pSpeedFile, pMaxDiffFile, maxDiffPitch);
            
            turnSpeed = new TurnSpeed(yawSpeed, pitchSpeed);
        } catch (Exception e) {
            e.printStackTrace();
            turnSpeed = new TurnSpeed(0f, 0f);
        }
    }

    private float calculateSpeedFromFile(File speedFile, File maxDiffFile, float targetMaxDiff) {
        try {
            List<String> speedLines = readAllLines(speedFile);
            List<String> maxDiffLines = readAllLines(maxDiffFile);
            
            if (speedLines.isEmpty() || maxDiffLines.isEmpty()) {
                return 0f;
            }
            
            List<Integer> matchingIndices = new ArrayList<>();
            for (int i = 0; i < maxDiffLines.size(); i++) {
                float maxDiff = Float.parseFloat(maxDiffLines.get(i));
                if (maxDiff >= targetMaxDiff - 2.5f && maxDiff <= targetMaxDiff + 2.5f) {
                    matchingIndices.add(i);
                }
            }
            
            if (matchingIndices.isEmpty()) {
                return 0f;
            }
            
            List<Integer> mouseInputs = new ArrayList<>();
            for (int index : matchingIndices) {
                if (index < speedLines.size()) {
                    mouseInputs.add(Integer.parseInt(speedLines.get(index)));
                }
            }
            
            if (mouseInputs.isEmpty()) {
                return 0f;
            }
            
            double average = mouseInputs.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            
            List<Integer> filteredInputs = new ArrayList<>();
            float minRange = (float) (average * RotationUtils.randomFloat(0.1f, 0.6f));
            float maxRange = (float) (average * RotationUtils.randomFloat(2.5f, 4.0f));
            
            for (int input : mouseInputs) {
                if (input >= minRange && input <= maxRange) {
                    filteredInputs.add(input);
                }
            }
            
            List<Integer> finalInputs = new ArrayList<>();
            for (int input : filteredInputs) {
                float angle = getAngleFromMouseInput(input);
                if (angle > targetMaxDiff / RotationUtils.randomFloat(2f, 4f)) {
                    finalInputs.add(input);
                }
            }
            
            List<Integer> selectedInputs = finalInputs.size() >= 5 ? finalInputs : filteredInputs;
            
            if (selectedInputs.isEmpty()) {
                return 0f;
            }
            
            int selectedInput = selectedInputs.get(RotationUtils.randomInt(0, selectedInputs.size() - 1));
            return getAngleFromMouseInput(selectedInput);
            
        } catch (Exception e) {
            e.printStackTrace();
            return 0f;
        }
    }

    private int getMouseDeltaX() {
        return Math.abs(RotationUtils.randomInt(-5, 5));
    }

    private int getMouseDeltaY() {
        return Math.abs(RotationUtils.randomInt(-3, 3));
    }

    private float getAngleFromMouseInput(int mouseInput) {
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        float f1 = sensitivity * sensitivity * sensitivity * 8.0f;
        return mouseInput * f1 * 0.15f;
    }

    private int getMouseInputFromAngle(float angle) {
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        float f1 = sensitivity * sensitivity * sensitivity * 8.0f;
        return Math.round(angle / (f1 * 0.15f));
    }

    private void appendToFile(File file, String content) {
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(content + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLastLine(File file) {
        try {
            List<String> lines = readAllLines(file);
            return lines.isEmpty() ? null : lines.get(lines.size() - 1);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> readAllLines(File file) {
        try {
            return Files.readAllLines(Paths.get(file.getAbsolutePath()));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
