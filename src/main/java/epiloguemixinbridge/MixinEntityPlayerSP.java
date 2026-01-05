package epiloguemixinbridge;

import epilogue.hooks.EntityPlayerSPHooks;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin({EntityPlayerSP.class})
public abstract class MixinEntityPlayerSP extends MixinEntityPlayer {
    @Inject(
            method = {"onUpdate"},
            at = {@At("HEAD")}
    )
    private void onUpdate(CallbackInfo callbackInfo) {
        EntityPlayerSPHooks.onUpdate((EntityPlayerSP) (Object) this, callbackInfo);
    }

    @Inject(
            method = {"onUpdate"},
            at = {@At("RETURN")}
    )
    private void postUpdate(CallbackInfo callbackInfo) {
        EntityPlayerSPHooks.onPostUpdate((EntityPlayerSP) (Object) this, callbackInfo);
    }

    @Redirect(
            method = {"onUpdate"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;isRiding()Z"
            )
    )
    private boolean onRidding(EntityPlayerSP entityPlayerSP) {
        return EntityPlayerSPHooks.onRiding(entityPlayerSP);
    }

    @Inject(
            method = {"onUpdate"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V"
            )}
    )
    private void onMotionUpdate(CallbackInfo callbackInfo) {
        EntityPlayerSPHooks.onMotionUpdate(callbackInfo);
    }

    @Inject(
            method = {"onLivingUpdate"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onLivingUpdate()V"
            )}
    )
    private void onLivingUpdate(CallbackInfo callbackInfo) {
        EntityPlayerSPHooks.onLivingUpdate(callbackInfo);
    }

    @Inject(
            method = {"onLivingUpdate"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V",
                    shift = At.Shift.AFTER
            )}
    )
    private void updateMove(CallbackInfo callbackInfo) {
        EntityPlayerSPHooks.onUpdateMove(callbackInfo);
    }

    @Redirect(
            method = {"onLivingUpdate"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;isUsingItem()Z"
            )
    )
    private boolean isUsing(EntityPlayerSP entityPlayerSP) {
        return EntityPlayerSPHooks.onIsUsing(entityPlayerSP);
    }

    @Redirect(
            method = {"onLivingUpdate"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
            )
    )
    private boolean checkPotion(EntityPlayerSP entityPlayerSP, Potion potion) {
        return EntityPlayerSPHooks.onCheckPotion(entityPlayerSP, potion);
    }
}
