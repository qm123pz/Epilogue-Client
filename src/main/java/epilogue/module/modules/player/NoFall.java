package epilogue.module.modules.player;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import epilogue.enums.BlinkModules;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.event.types.Priority;
import epilogue.events.MoveInputEvent;
import epilogue.events.TickEvent;
import epilogue.events.UpdateEvent;
import epiloguemixinbridge.IAccessorMinecraft;
import epiloguemixinbridge.IAccessorPlayerControllerMP;
import epilogue.management.RotationState;
import epilogue.module.Module;
import epilogue.util.*;
import epilogue.value.values.FloatValue;
import epilogue.value.values.ModeValue;
import epilogue.value.values.IntValue;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;

public class NoFall extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil packetDelayTimer = new TimerUtil();
    private final TimerUtil scoreboardResetTimer = new TimerUtil();
    private boolean slowFalling = false;
    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Packet", "Blink", "NoGround", "Spoof", "Legit"});
    public final FloatValue distance = new FloatValue("Distance", 3.0F, 0.0F, 20.0F);
    public final IntValue delay = new IntValue("Delay", 0, 0, 10000);

    private int lStage = 0;
    private int lPrevSlot = -1;
    private float lYaw = 0.0F;
    private float lPitch = 0.0F;
    private boolean lRun = false;
    private boolean lRot = false;
    private int lTick = 0;
    private int lPickTick = 0;
    private boolean lSilent = false;
    private BlockPos lPos = null;

    public NoFall() {
        super("NoFall", false);
    }

    private int findWaterBucketSlot() {
        for(int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if(stack != null && stack.getItem() == Items.water_bucket) {
                return i;
            }
        }
        return -1;
    }

    private boolean legitAllowed() {
        if(mc.thePlayer == null) return false;
        if(mc.thePlayer.capabilities.allowFlying) return false;
        return mc.thePlayer.fallDistance >= this.distance.getValue();
    }

    private void legitReset() {
        if(mc.thePlayer != null && this.lPrevSlot != -1) {
            mc.thePlayer.inventory.currentItem = this.lPrevSlot;
        }
        this.lStage = 0;
        this.lPrevSlot = -1;
        this.lRun = false;
        this.lRot = false;
        this.lTick = 0;
        this.lPickTick = 0;
        this.lSilent = false;
        this.lPos = null;
        Epilogue.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
    }

    private void handleLegit() {
        if(mc.thePlayer.onGround && this.lRun) {
            if(this.lStage == 2) {
                this.lStage = 4;
                this.lPickTick = 0;
            } else if(this.lStage != 4) {
                this.legitReset();
                this.packetDelayTimer.reset();
                mc.thePlayer.fallDistance = 0.0F;
                return;
            }
        }
        if(!this.legitAllowed()) {
            if(this.lRun) {
                this.legitReset();
            }
            return;
        }

        if(!this.lRun) {
            int slot = this.findWaterBucketSlot();
            if(slot == -1) return;
            BlockPos p = new BlockPos(
                    Math.floor(mc.thePlayer.posX),
                    Math.floor(mc.thePlayer.posY),
                    Math.floor(mc.thePlayer.posZ)
            );
            BlockPos t = p.down();
            if(BlockUtil.isReplaceable(t)) {
                t = t.down();
            }
            if(BlockUtil.isReplaceable(t)) return;
            this.lPos = t;
            this.lPrevSlot = mc.thePlayer.inventory.currentItem;
            mc.thePlayer.inventory.currentItem = slot;
            this.lStage = 1;
            this.lRun = true;
            this.lTick = 0;
            this.lPickTick = 0;
            this.lSilent = true;
        }

        Epilogue.blinkManager.setBlinkState(true, BlinkModules.NO_FALL);

        if(this.lStage == 1) {
            this.lStage = 2;
            this.lTick = 0;
        }

        if(this.lStage == 2 || this.lStage == 4) {
            float cur = this.lRot ? this.lPitch : mc.thePlayer.rotationPitch;
            float d = 90.0F - cur;
            float step = 180.0F;
            if(d > step) {
                d = step;
            } else if(d < -step) {
                d = -step;
            }
            this.lYaw = mc.thePlayer.rotationYaw;
            this.lPitch = cur + d;
            this.lRot = true;

            if(this.lPos != null) {
                ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                mc.thePlayer.swingItem();
            }

            if(this.lStage == 2) {
                if(PlayerUtil.checkInWater(mc.thePlayer.getEntityBoundingBox())) {
                    this.lStage = 4;
                    this.lPickTick = 0;
                    return;
                }
                this.lTick++;
                if(this.lTick >= 20) {
                    this.lStage = 3;
                    this.lRot = false;
                    this.lSilent = false;
                }
                return;
            }

            this.lPickTick++;
            if(this.lPickTick >= 6) {
                mc.thePlayer.fallDistance = 0.0F;
                this.packetDelayTimer.reset();
                this.legitReset();
            }
            return;
        }

        if(this.lStage == 3) {
            if(mc.thePlayer.onGround || PlayerUtil.checkInWater(mc.thePlayer.getEntityBoundingBox())) {
                mc.thePlayer.fallDistance = 0.0F;
                this.packetDelayTimer.reset();
                this.legitReset();
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if(this.isEnabled()) {
            if((this.mode.getValue() == 4 && this.lRun || RotationState.isActived())
                    && RotationState.getPriority() == 3.0F
                    && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget(Priority.HIGH)
    public void onUpdate(UpdateEvent event) {
        if(this.isEnabled() && event.getType() == EventType.PRE) {
            if(this.mode.getValue() == 4 && this.lSilent) {
                event.setRotation(this.lYaw, this.lPitch, 3);
                event.setPervRotation(this.lYaw, 3);
            }
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onTick(TickEvent event) {
        if(this.isEnabled() && event.getType() == EventType.PRE) {
            if(ServerUtil.hasPlayerCountInfo()) {
                this.scoreboardResetTimer.reset();
            }
            if(this.mode.getValue() == 0 && this.slowFalling) {
                PacketUtil.sendPacketNoEvent(new C03PacketPlayer(true));
                mc.thePlayer.fallDistance = 0.0F;
            }

            if(this.mode.getValue() == 4) {
                this.handleLegit();
            }
        }
    }

    @Override
    public void onDisabled() {
        this.legitReset();
        Epilogue.blinkManager.setBlinkState(false, BlinkModules.NO_FALL);
        if(this.slowFalling) {
            this.slowFalling = false;
            ((IAccessorMinecraft) mc).getTimer().timerSpeed = 1.0F;
        }
    }

    @Override
    public void verifyValue(String mode) {
        if(this.isEnabled()) {
            this.onDisabled();
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}