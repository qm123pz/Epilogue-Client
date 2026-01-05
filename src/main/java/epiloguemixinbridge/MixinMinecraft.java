package epiloguemixinbridge;

import epilogue.hooks.MinecraftHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin({Minecraft.class})
public abstract class MixinMinecraft {
    @Inject(
            method = {"startGame"},
            at = {@At("HEAD")}
    )
    private void startGame(CallbackInfo callbackInfo) {
        MinecraftHooks.onStartGame(callbackInfo);
    }

    @Inject(
            method = {"startGame"},
            at = {@At("RETURN")}
    )
    private void postStartGame(CallbackInfo callbackInfo) {
        MinecraftHooks.onPostStartGame(callbackInfo);
    }

    @Inject(
            method = {"runTick"},
            at = {@At("HEAD")}
    )
    private void runTick(CallbackInfo callbackInfo) {
        MinecraftHooks.onRunTick(callbackInfo);
    }

    @Inject(
            method = {"runTick"},
            at = {@At("RETURN")}
    )
    private void postRunTick(CallbackInfo callbackInfo) {
        MinecraftHooks.onPostRunTick(callbackInfo);
    }

    @Inject(
            method = {"loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V"},
            at = {@At("HEAD")}
    )
    private void loadWorld(WorldClient worldClient, String string, CallbackInfo callbackInfo) {
        MinecraftHooks.onLoadWorld(worldClient, string, callbackInfo);
    }

    @Inject(
            method = {"updateFramebufferSize"},
            at = {@At("RETURN")}
    )
    private void updateFramebufferSize(CallbackInfo callbackInfo) {
        MinecraftHooks.onUpdateFramebufferSize(callbackInfo);
    }

    @Inject(
            method = {"clickMouse"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void clickMouse(CallbackInfo callbackInfo) {
        MinecraftHooks.onClickMouse(callbackInfo);
    }

    @Inject(
            method = {"rightClickMouse"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void rightClickMouse(CallbackInfo callbackInfo) {
        MinecraftHooks.onRightClickMouse(callbackInfo);
    }

    @Inject(
            method = {"sendClickBlockToController"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void sendClickBlockToController(CallbackInfo callbackInfo) {
        MinecraftHooks.onSendClickBlockToController(callbackInfo);
    }

    @Redirect(
            method = {"runTick"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V"
            )
    )
    private void setKeyBindState(int integer, boolean boolean2) {
        MinecraftHooks.onSetKeyBindState(integer, boolean2);
    }

    @Redirect(
            method = {"runTick"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/InventoryPlayer;changeCurrentItem(I)V"
            )
    )
    private void changeCurrentItem(InventoryPlayer inventoryPlayer, int slot) {
        MinecraftHooks.onChangeCurrentItem(inventoryPlayer, slot);
    }

    @Inject(method = "displayGuiScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;", shift = At.Shift.AFTER))
    private void replaceMainMenu(GuiScreen guiScreenIn, CallbackInfo ci) {
        MinecraftHooks.onDisplayGuiScreen(guiScreenIn, ci);
    }

    @Inject(method = "displayGuiScreen", at = @At("HEAD"), cancellable = true)
    private void epilogue$replaceGuiChat(GuiScreen guiScreenIn, CallbackInfo ci) {
        MinecraftHooks.onReplaceGuiChat(guiScreenIn, ci);
    }

    @Inject(method = "createDisplay", at = @At("RETURN"))
    private void onCreateDisplay(CallbackInfo ci) {
        MinecraftHooks.onCreateDisplay(ci);
    }
}
