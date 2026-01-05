package epiloguemixinbridge;

import epilogue.hooks.PlayerControllerMPHooks;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({PlayerControllerMP.class})
public abstract class MixinPlayerControllerMP {

    @Inject(
            method = "attackEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;syncCurrentPlayItem()V"))
    private void attackEntity(
            EntityPlayer entityPlayer, Entity targetEntity, CallbackInfo callbackInfo
    ) {
        PlayerControllerMPHooks.onAttackEntity(entityPlayer, targetEntity, callbackInfo);
    }
    @Inject(
            method = {"windowClick"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void windowClick(
            int windowId, int slotId, int mouseButtonClicked, int mode, EntityPlayer entityPlayer, CallbackInfoReturnable<ItemStack> callbackInfoReturnable
    ) {
        PlayerControllerMPHooks.onWindowClick(windowId, slotId, mouseButtonClicked, mode, entityPlayer, callbackInfoReturnable);
    }

    @Inject(
            method = {"onStoppedUsingItem"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void onStoppedUsingItem(CallbackInfo callbackInfo) {
        PlayerControllerMPHooks.onStoppedUsingItem(callbackInfo);
    }
}
