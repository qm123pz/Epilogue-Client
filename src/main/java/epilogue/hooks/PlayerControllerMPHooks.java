package epilogue.hooks;

import epilogue.event.EventManager;
import epilogue.events.AttackEvent;
import epilogue.events.CancelUseEvent;
import epilogue.events.WindowClickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class PlayerControllerMPHooks {
    private PlayerControllerMPHooks() {
    }

    public static void onAttackEntity(EntityPlayer entityPlayer, Entity targetEntity, CallbackInfo callbackInfo) {
        AttackEvent event = new AttackEvent(targetEntity);
        EventManager.call(event);
    }

    public static void onWindowClick(int windowId, int slotId, int mouseButtonClicked, int mode, EntityPlayer entityPlayer, CallbackInfoReturnable<ItemStack> callbackInfoReturnable) {
        WindowClickEvent event = new WindowClickEvent(windowId, slotId, mouseButtonClicked, mode);
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfoReturnable.cancel();
        }
    }

    public static void onStoppedUsingItem(CallbackInfo callbackInfo) {
        CancelUseEvent event = new CancelUseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
