package epilogue.module.modules.combat;

import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.event.types.Priority;
import epilogue.events.LeftClickMouseEvent;
import epilogue.events.TickEvent;
import epilogue.module.Module;
import epilogue.util.ItemUtil;
import epilogue.util.KeyBindUtil;
import epilogue.util.RandomUtil;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;

import java.util.Objects;

public class AutoClicker extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean clickPending = false;
    private long clickDelay = 0L;
    private boolean blockHitPending = false;
    private long blockHitDelay = 0L;
    public final IntValue minCPS = new IntValue("Min Cps", 8, 1, 20);
    public final IntValue maxCPS = new IntValue("Max Cps", 12, 1, 20);
    public final BooleanValue blockHit = new BooleanValue("Block Hit", false);
    public final FloatValue blockHitTicks = new FloatValue("Block Hit Ticks", 1.5F, 1.0F, 20.0F, this.blockHit::getValue);
    public final BooleanValue weaponsOnly = new BooleanValue("Weapons Only", true);
    public final BooleanValue allowTools = new BooleanValue("Allow Tools", false, this.weaponsOnly::getValue);
    public final BooleanValue breakBlocks = new BooleanValue("Break Blocks", true);
    private long getNextClickDelay() {
        return 1000L / RandomUtil.nextLong(this.minCPS.getValue().intValue(), this.maxCPS.getValue().intValue());
    }

    private long getBlockHitDelay() {
        return (long) (50.0F * this.blockHitTicks.getValue());
    }

    private boolean isBreakingBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    private boolean canClick() {
        if (!this.weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || this.allowTools.getValue() && ItemUtil.isHoldingTool()) {
            if (this.breakBlocks.getValue() && this.isBreakingBlock()) {
                GameType gameType12 = mc.playerController.getCurrentGameType();
                return gameType12 != GameType.SURVIVAL && gameType12 != GameType.CREATIVE;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public AutoClicker() {
        super("AutoClicker", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.clickDelay > 0L) {
                this.clickDelay -= 50L;
            }
            if (this.blockHitDelay > 0L) {
                this.blockHitDelay -= 50L;
            }
            if (mc.currentScreen != null) {
                this.clickPending = false;
                this.blockHitPending = false;
            } else {
                if (this.clickPending) {
                    this.clickPending = false;
                    KeyBindUtil.updateKeyState(mc.gameSettings.keyBindAttack.getKeyCode());
                }
                if (this.blockHitPending) {
                    this.blockHitPending = false;
                    KeyBindUtil.updateKeyState(mc.gameSettings.keyBindUseItem.getKeyCode());
                }
                if (this.isEnabled() && this.canClick() && mc.gameSettings.keyBindAttack.isKeyDown()) {
                    if (!mc.thePlayer.isUsingItem()) {
                        while (this.clickDelay <= 0L) {
                            this.clickPending = true;
                            this.clickDelay = this.clickDelay + this.getNextClickDelay();
                            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                            KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindAttack.getKeyCode());
                        }
                    }
                    if (this.blockHit.getValue()
                            && this.blockHitDelay <= 0L
                            && mc.gameSettings.keyBindUseItem.isKeyDown()
                            && ItemUtil.isHoldingSword()) {
                        this.blockHitPending = true;
                        KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                        if (!mc.thePlayer.isUsingItem()) {
                            this.blockHitDelay = this.blockHitDelay + this.getBlockHitDelay();
                            KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindUseItem.getKeyCode());
                        }
                    }
                }
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onCLick(LeftClickMouseEvent event) {
        if (this.isEnabled() && !event.isCancelled()) {
            if (!this.clickPending) {
                this.clickDelay = this.clickDelay + this.getNextClickDelay();
            }
        }
    }

    @Override
    public void onEnabled() {
        this.clickDelay = 0L;
        this.blockHitDelay = 0L;
    }

    @Override
    public void verifyValue(String string) {
        if (this.minCPS.getName().equals(string)) {
            if (this.minCPS.getValue() > this.maxCPS.getValue()) {
                this.maxCPS.setValue(this.minCPS.getValue());
            }
        } else {
            if (this.maxCPS.getName().equals(string) && this.minCPS.getValue() > this.maxCPS.getValue()) {
                this.minCPS.setValue(this.maxCPS.getValue());
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return Objects.equals(this.minCPS.getValue(), this.maxCPS.getValue())
                ? new String[]{this.minCPS.getValue().toString()}
                : new String[]{String.format("%d-%d", this.minCPS.getValue(), this.maxCPS.getValue())};
    }
}
