package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.player.Scaffold;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public final class GuiIngameHooks {
    private GuiIngameHooks() {
    }

    public static ItemStack onUpdateTick(InventoryPlayer inventoryPlayer) {
        Scaffold scaffold = (Scaffold) Epilogue.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled() && scaffold.itemSpoof.getValue()) {
            int slot = scaffold.getSlot();
            if (slot >= 0) {
                return inventoryPlayer.getStackInSlot(slot);
            }
        }
        return inventoryPlayer.getCurrentItem();
    }
}
