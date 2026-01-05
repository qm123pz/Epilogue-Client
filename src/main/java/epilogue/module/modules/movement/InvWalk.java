package epilogue.module.modules.movement;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.event.types.Priority;
import epilogue.events.PacketEvent;
import epilogue.events.TickEvent;
import epilogue.events.UpdateEvent;
import epiloguemixinbridge.IAccessorC0DPacketCloseWindow;
import epilogue.module.Module;
import epilogue.util.KeyBindUtil;
import epilogue.util.PacketUtil;
import epilogue.value.values.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InvWalk extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Queue<C0EPacketClickWindow> clickQueue = new ConcurrentLinkedQueue<C0EPacketClickWindow>();
    private boolean keysPressed = false;
    private C16PacketClientStatus pendingStatus = null;
    private int delayTicks = 0;
    public final ModeValue mode = new ModeValue("Mode", 1, new String[]{"Vanilla", "Legit", "StopOnClick"});

    public InvWalk() {
        super("InvWalk", false);
    }

    public void pressMovementKeys() {
        KeyBinding[] movementKeys = new KeyBinding[]{
                mc.gameSettings.keyBindForward,
                mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft,
                mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump,
                mc.gameSettings.keyBindSprint
        };
        for (KeyBinding keyBinding : movementKeys) {
            KeyBindUtil.updateKeyState(keyBinding.getKeyCode());
        }
        if (Epilogue.moduleManager.modules.get(Sprint.class).isEnabled()) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }
        this.keysPressed = true;
    }

    public boolean canInvWalk() {
        if (!(mc.currentScreen instanceof GuiContainer)) {
            return false;
        } else if (mc.currentScreen instanceof GuiContainerCreative) {
            return false;
        } else {
            switch (this.mode.getValue()) {
                case 1:
                    if (!(mc.currentScreen instanceof GuiInventory)) {
                        return false;
                    }
                    return this.pendingStatus != null && this.clickQueue.isEmpty();
                case 2:
                    return this.clickQueue.isEmpty();
                default:
                    return true;
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            while (!this.clickQueue.isEmpty()) {
                PacketUtil.sendPacketNoEvent(this.clickQueue.poll());
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.canInvWalk() && this.delayTicks == 0) {
                this.pressMovementKeys();
            } else {
                if (this.keysPressed) {
                    if (mc.currentScreen != null) {
                        KeyBinding.unPressAllKeys();
                    }
                    this.keysPressed = false;
                }
                if (this.pendingStatus != null) {
                    PacketUtil.sendPacketNoEvent(this.pendingStatus);
                    this.pendingStatus = null;
                }
                if (this.delayTicks > 0) {
                    this.delayTicks--;
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.SEND) {
            if (event.getPacket() instanceof C16PacketClientStatus) {
                if (this.mode.getValue() == 1) {
                    C16PacketClientStatus packet = (C16PacketClientStatus) event.getPacket();
                    if (packet.getStatus() == EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                        event.setCancelled(true);
                        this.pendingStatus = packet;
                    }
                }
            } else if (!(event.getPacket() instanceof C0EPacketClickWindow)) {
                if (event.getPacket() instanceof C0DPacketCloseWindow) {
                    C0DPacketCloseWindow packet = (C0DPacketCloseWindow) event.getPacket();
                    if (this.pendingStatus != null && ((IAccessorC0DPacketCloseWindow) packet).getWindowId() == 0) {
                        this.pendingStatus = null;
                        event.setCancelled(true);
                    }
                }
            } else {
                C0EPacketClickWindow packet = (C0EPacketClickWindow) event.getPacket();
                switch (this.mode.getValue()) {
                    case 1:
                        if (packet.getWindowId() == 0) {
                            if ((packet.getMode() == 3 || packet.getMode() == 4) && packet.getSlotId() == -999) {
                                event.setCancelled(true);
                                return;
                            }
                            if (this.pendingStatus != null) {
                                KeyBinding.unPressAllKeys();
                                event.setCancelled(true);
                                this.clickQueue.offer(packet);
                            }
                        }
                        break;
                    case 2:
                        if ((packet.getMode() == 3 || packet.getMode() == 4) && packet.getSlotId() == -999) {
                            event.setCancelled(true);
                        } else {
                            KeyBinding.unPressAllKeys();
                            event.setCancelled(true);
                            this.clickQueue.offer(packet);
                            this.delayTicks = 8;
                        }
                }
                if (this.pendingStatus != null) {
                    PacketUtil.sendPacketNoEvent(this.pendingStatus);
                    this.pendingStatus = null;
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        if (this.keysPressed) {
            if (mc.currentScreen != null) {
                KeyBinding.unPressAllKeys();
            }
            this.keysPressed = false;
        }
        if (this.pendingStatus != null) {
            PacketUtil.sendPacketNoEvent(this.pendingStatus);
            this.pendingStatus = null;
        }
        this.delayTicks = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
