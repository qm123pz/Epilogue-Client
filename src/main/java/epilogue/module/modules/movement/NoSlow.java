package epilogue.module.modules.movement;

import epilogue.Epilogue;
import epilogue.enums.FloatModules;
import epilogue.enums.BlinkModules;
import epilogue.event.EventTarget;
import epilogue.events.LivingUpdateEvent;
import epilogue.events.PlayerUpdateEvent;
import epilogue.events.RightClickMouseEvent;
import epilogue.module.Module;
import epilogue.module.modules.combat.Aura;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import epilogue.value.values.PercentValue;
import epilogue.util.BlockUtil;
import epilogue.util.ItemUtil;
import epilogue.util.PlayerUtil;
import epilogue.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int lastSlot = -1;
    private boolean noslowSuccess = false;
    private long lastCheckTime = 0L;
    private boolean wasBlocking = false;
    private long lastBlockingTime = 0L;
    private boolean isBlinking = false;
    private int blinkTimer = 0;
    private int onGroundTicks = 0;
    private boolean isHypixelBlinkActive = false;

    private boolean usingItem = false;
    private final Queue<Packet<?>> packetQueue = new LinkedBlockingQueue();

    public final ModeValue swordMode = new ModeValue("Sword Mode", 5, new String[]{"None", "Vanilla", "Blink", "Test", "Prediction", "Blink123"});
    public final BooleanValue onlyKillAuraAutoBlock = new BooleanValue("Only KillAura AutoBlock", false, () -> this.swordMode.getValue() != 0);
    public final PercentValue swordMotion = new PercentValue("Sword Motion", 100, () -> this.swordMode.getValue() != 0);
    public final BooleanValue swordSprint = new BooleanValue("Sword Sprint", true, () -> this.swordMode.getValue() != 0);
    public final IntValue swordBlinkDelay = new IntValue("Sword Blink Delay", 1, 1, 10, () -> this.swordMode.getValue() == 2 || this.swordMode.getValue() == 4);
    public final IntValue swordBlinkDuration = new IntValue("Sword Blink Duration", 2, 1, 5, () -> this.swordMode.getValue() == 2 || this.swordMode.getValue() == 4);

    public final BooleanValue blinkSword = new BooleanValue("Blink Sword", true, () -> this.swordMode.getValue() == 5);

    public final ModeValue foodMode = new ModeValue("Food Mode", 0, new String[]{"None", "Vanilla", "Float", "Blink", "Test"});
    public final PercentValue foodMotion = new PercentValue("Food Motion", 100, () -> this.foodMode.getValue() != 0);
    public final BooleanValue foodSprint = new BooleanValue("Food Sprint", true, () -> this.foodMode.getValue() != 0);
    public final IntValue foodBlinkDelay = new IntValue("Food Blink Delay", 2, 1, 10, () -> this.foodMode.getValue() == 3);
    public final IntValue foodBlinkDuration = new IntValue("Food Blink Duration", 1, 1, 5, () -> this.foodMode.getValue() == 3);

    public final ModeValue bowMode = new ModeValue("Bow Mode", 0, new String[]{"None", "Vanilla", "Float", "Blink"});
    public final PercentValue bowMotion = new PercentValue("Bow Motion", 100, () -> this.bowMode.getValue() != 0);
    public final BooleanValue bowSprint = new BooleanValue("Bow Sprint", true, () -> this.bowMode.getValue() != 0);
    public final IntValue bowBlinkDelay = new IntValue("Bow Blink Delay", 2, 1, 10, () -> this.bowMode.getValue() == 3);
    public final IntValue bowBlinkDuration = new IntValue("Bow Blink Duration", 1, 1, 5, () -> this.bowMode.getValue() == 3);

    public final BooleanValue successDetection = new BooleanValue("Success Detection", true, () -> this.swordMode.getValue() == 1 || this.swordMode.getValue() == 2 || this.swordMode.getValue() == 4 || this.swordMode.getValue() == 5);
    public final BooleanValue successMessage = new BooleanValue("Success Message", true, () -> this.successDetection.getValue());

    public NoSlow() {
        super("NoSlow", false);
    }

    public boolean isSwordActive() {
        return this.swordMode.getValue() != 0
                && ItemUtil.isHoldingSword()
                && (!this.onlyKillAuraAutoBlock.getValue() || this.isKillAuraAutoBlocking());
    }

    public boolean isBlink123ModeActive() {
        return this.swordMode.getValue() == 5;
    }

    private boolean isKillAuraAutoBlocking() {
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        if (aura == null || !aura.isEnabled()) {
            return false;
        }
        if (!ItemUtil.isHoldingSword()) {
            return false;
        }
        int mode = aura.autoBlock.getValue();
        if (mode == 0 || mode == 8) {
            return false;
        }
        if (!(aura.isBlocking() || aura.isPlayerBlocking())) {
            return false;
        }
        return aura.shouldAutoBlock();
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isTestFoodMode() {
        return this.foodMode.getValue() == 4 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isFloatMode() {
        return this.foodMode.getValue() == 2 && ItemUtil.isEating() || this.bowMode.getValue() == 2 && ItemUtil.isUsingBow();
    }

    public boolean isBlinkMode() {
        return this.swordMode.getValue() == 2 && ItemUtil.isHoldingSword()
                || this.foodMode.getValue() == 3 && ItemUtil.isEating()
                || this.bowMode.getValue() == 3 && ItemUtil.isUsingBow()
                || this.isBlink123ModeActive();
    }

    public boolean isPredictionMode() {
        return this.swordMode.getValue() == 3 && this.isSwordActive();
    }

    public boolean isHypixelMode() {
        return this.swordMode.getValue() == 4 && this.isSwordActive();
    }

    public boolean isAnyActive() {
        return NoSlow.mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
    }

    public boolean canSprint() {
        return this.isSwordActive() && this.swordSprint.getValue() != false
                || this.isFoodActive() && this.foodSprint.getValue() != false
                || this.isBowActive() && this.bowSprint.getValue() != false;
    }

    public int getMotionMultiplier() {
        if (ItemUtil.isHoldingSword()) {
            return this.swordMotion.getValue();
        }
        if (ItemUtil.isEating()) {
            return this.foodMotion.getValue();
        }
        return ItemUtil.isUsingBow() ? this.bowMotion.getValue() : 100;
    }

    private void flushPacketQueue() {
        while (!this.packetQueue.isEmpty()) {
            Packet<?> packet = this.packetQueue.poll();
            if (packet == null || mc.getNetHandler() == null) continue;
            mc.getNetHandler().addToSendQueue(packet);
        }
    }

    private void handleBlink123Mode() {
        if (NoSlow.mc.thePlayer == null || NoSlow.mc.thePlayer.getItemInUse() == null) {
            return;
        }

        Item item = NoSlow.mc.thePlayer.getItemInUse().getItem();
        boolean shouldBlink = false;

        if (NoSlow.mc.thePlayer.isUsingItem()
                && item instanceof ItemSword
                && this.blinkSword.getValue()) {
            shouldBlink = true;

            int currentSlot = NoSlow.mc.thePlayer.inventory.currentItem;
            int switchSlot = currentSlot % 8 + 1;

            this.packetQueue.add(new C09PacketHeldItemChange(switchSlot));
            this.packetQueue.add(new C09PacketHeldItemChange(currentSlot));
        }

        if (shouldBlink) {
            this.usingItem = true;
        } else {
            this.usingItem = false;
            this.flushPacketQueue();
        }
    }

    private boolean shouldBlink() {
        if (!this.isBlinkMode()) {
            return false;
        }

        if (this.isBlink123ModeActive()) {
            return this.usingItem;
        }

        ++this.blinkTimer;
        int delay = 2;
        int duration = 1;
        if (ItemUtil.isHoldingSword()) {
            delay = this.swordBlinkDelay.getValue();
            duration = this.swordBlinkDuration.getValue();
        } else if (ItemUtil.isEating()) {
            delay = this.foodBlinkDelay.getValue();
            duration = this.foodBlinkDuration.getValue();
        } else if (ItemUtil.isUsingBow()) {
            delay = this.bowBlinkDelay.getValue();
            duration = this.bowBlinkDuration.getValue();
        }
        int totalCycle = delay + duration;
        int currentPhase = this.blinkTimer % totalCycle;
        if (currentPhase < delay) {
            this.isBlinking = false;
            return false;
        }
        this.isBlinking = true;
        return true;
    }

    private boolean checkNoSlowSuccess() {
        boolean newSuccessState;
        if (!(this.isEnabled() && this.isSwordActive() && this.successDetection.getValue())) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastCheckTime < 500L) {
            return this.noslowSuccess;
        }
        this.lastCheckTime = currentTime;
        boolean wasSprinting = NoSlow.mc.thePlayer.isSprinting();
        boolean isMoving = Math.abs(NoSlow.mc.thePlayer.movementInput.moveForward) > 0.1f || Math.abs(NoSlow.mc.thePlayer.movementInput.moveStrafe) > 0.1f;
        boolean bl = newSuccessState = wasSprinting && isMoving && PlayerUtil.isUsingItem();
        if (newSuccessState != this.noslowSuccess && this.successMessage.getValue()) {
            if (newSuccessState) {
                NoSlow.mc.thePlayer.addChatMessage(new ChatComponentText("§a[NoSlow] §fSuccess - Sword blocking without slowdown!"));
            } else {
                NoSlow.mc.thePlayer.addChatMessage(new ChatComponentText("§c[NoSlow] §fFailed - Normal sword blocking slowdown"));
            }
        }
        this.noslowSuccess = newSuccessState;
        return this.noslowSuccess;
    }

    private void sendBzymC09() {
        if (NoSlow.mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }
        int current = NoSlow.mc.thePlayer.inventory.currentItem;
        int next = (current + 1) % 9;
        mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(next));
        mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(current));
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled()) {
            this.wasBlocking = false;
            this.onGroundTicks = 0;
            this.usingItem = false;
            return;
        }

        if (mc.thePlayer != null) {
            if (mc.thePlayer.onGround) {
                this.onGroundTicks++;
            } else {
                this.onGroundTicks = 0;
            }
        }

        boolean isTestActive = this.isTestFoodMode();
        if (isTestActive) {
            boolean isMovingForward;
            if (this.onGroundTicks % 3 != 0 && NoSlow.mc.thePlayer.getItemInUseCount() >= 4) {
                boolean playerWantsToSprint = NoSlow.mc.gameSettings.keyBindSprint.isKeyDown();
                isMovingForward = NoSlow.mc.thePlayer.movementInput.moveForward > 0.1f;
                NoSlow.mc.thePlayer.setSprinting(playerWantsToSprint && isMovingForward);
            } else {
                boolean playerWantsToSprint = NoSlow.mc.gameSettings.keyBindSprint.isKeyDown();
                isMovingForward = NoSlow.mc.thePlayer.movementInput.moveForward > 0.1f;
                NoSlow.mc.thePlayer.setSprinting(playerWantsToSprint && isMovingForward);
            }
            return;
        }

        if (this.isHypixelMode()) {
            return;
        }

        boolean isCurrentlyBlocking = this.isSwordActive() && PlayerUtil.isUsingItem();

        if (this.isBlink123ModeActive() && this.usingItem) {
            isCurrentlyBlocking = true;
            this.wasBlocking = true;
            this.lastBlockingTime = System.currentTimeMillis();
        }

        if (this.isBlinkMode() && this.shouldBlink()) {
            isCurrentlyBlocking = false;
            this.wasBlocking = false;
            return;
        }

        if (isCurrentlyBlocking) {
            this.wasBlocking = true;
            this.lastBlockingTime = System.currentTimeMillis();
        }

        boolean inSprintProtection = System.currentTimeMillis() - this.lastBlockingTime < 300L;
        boolean playerWantsToSprint = NoSlow.mc.gameSettings.keyBindSprint.isKeyDown();

        if (this.isAnyActive() || inSprintProtection || (this.isBlink123ModeActive() && this.usingItem)) {
            if (this.isSwordActive() || inSprintProtection || (this.isBlink123ModeActive() && this.usingItem)) {
                this.checkNoSlowSuccess();
            }
            float multiplier = (float)this.getMotionMultiplier() / 100.0f;
            if (this.isAnyActive() || (this.isBlink123ModeActive() && this.usingItem)) {
                NoSlow.mc.thePlayer.movementInput.moveForward *= multiplier;
                NoSlow.mc.thePlayer.movementInput.moveStrafe *= multiplier;
            }
            if ((this.canSprint() || inSprintProtection) && playerWantsToSprint && NoSlow.mc.thePlayer.movementInput.moveForward > 0.1f) {
                NoSlow.mc.thePlayer.setSprinting(true);
            } else {
                NoSlow.mc.thePlayer.setSprinting(false);
            }
        } else {
            this.wasBlocking = false;
        }
    }

    @EventTarget(value=3)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            if (this.isHypixelBlinkActive) {
                Epilogue.blinkManager.setBlinkState(false, BlinkModules.NO_SLOW);
                this.isHypixelBlinkActive = false;
            }
            this.usingItem = false;
            this.flushPacketQueue();
            return;
        }

        if (this.isBlink123ModeActive()) {
            this.handleBlink123Mode();
        }

        if (this.isHypixelMode()) {
            boolean isBlocking = PlayerUtil.isUsingItem();
            if (isBlocking) {
                if (!this.isHypixelBlinkActive) {
                    Epilogue.blinkManager.setBlinkState(true, BlinkModules.NO_SLOW);
                    this.isHypixelBlinkActive = true;
                }
                sendBzymC09();
                if (mc.thePlayer.movementInput.moveForward > 0.1f && mc.gameSettings.keyBindSprint.isKeyDown()) {
                    mc.thePlayer.setSprinting(true);
                }
            } else {
                if (this.isHypixelBlinkActive) {
                    Epilogue.blinkManager.setBlinkState(false, BlinkModules.NO_SLOW);
                    this.isHypixelBlinkActive = false;
                }
            }
        } else {
            if (this.isHypixelBlinkActive) {
                Epilogue.blinkManager.setBlinkState(false, BlinkModules.NO_SLOW);
                this.isHypixelBlinkActive = false;
            }
        }

        if (this.isPredictionMode()) {
            if (PlayerUtil.isUsingItem()) {
                this.sendPredictionC09();
            }
        }

        if (this.isFloatMode()) {
            int item = NoSlow.mc.thePlayer.inventory.currentItem;
            if (this.lastSlot != item && PlayerUtil.isUsingItem()) {
                this.lastSlot = item;
                Epilogue.floatManager.setFloatState(true, FloatModules.NO_SLOW);
            }
        } else {
            this.lastSlot = -1;
            Epilogue.floatManager.setFloatState(false, FloatModules.NO_SLOW);
        }

        if (this.isSwordActive() && this.successDetection.getValue()) {
            this.checkNoSlowSuccess();
        }
    }

    private void sendPredictionC09() {
        if (NoSlow.mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }
        int current = NoSlow.mc.thePlayer.inventory.currentItem;
        int next = (current + 1) % 9;
        mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(next));
        mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(current));
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (NoSlow.mc.objectMouseOver != null) {
                switch (NoSlow.mc.objectMouseOver.typeOfHit) {
                    case BLOCK: {
                        BlockPos blockPos = NoSlow.mc.objectMouseOver.getBlockPos();
                        if (!BlockUtil.isInteractable(blockPos) || PlayerUtil.isSneaking()) break;
                        return;
                    }
                    case ENTITY: {
                        Entity entityHit = NoSlow.mc.objectMouseOver.entityHit;
                        if (entityHit instanceof EntityVillager) {
                            return;
                        }
                        if (!(entityHit instanceof EntityLivingBase) || !TeamUtil.isShop((EntityLivingBase)entityHit)) break;
                        return;
                    }
                }
            }
            if (this.isFloatMode() && !Epilogue.floatManager.isPredicted() && NoSlow.mc.thePlayer.onGround) {
                event.setCancelled(true);
                NoSlow.mc.thePlayer.motionY = 0.42f;
            }
        }
    }

    @Override
    public void onEnabled() {
        this.blinkTimer = 0;
        this.isBlinking = false;
        this.noslowSuccess = false;
        this.lastCheckTime = 0L;
        this.wasBlocking = false;
        this.lastBlockingTime = 0L;
        this.isHypixelBlinkActive = false;
        this.packetQueue.clear();
        this.usingItem = false;
    }

    @Override
    public void onDisabled() {
        this.blinkTimer = 0;
        this.isBlinking = false;
        this.noslowSuccess = false;
        this.lastCheckTime = 0L;
        this.wasBlocking = false;
        this.lastBlockingTime = 0L;
        if (this.isHypixelBlinkActive) {
            Epilogue.blinkManager.setBlinkState(false, BlinkModules.NO_SLOW);
            this.isHypixelBlinkActive = false;
        }
        this.flushPacketQueue();
        this.usingItem = false;
    }
}