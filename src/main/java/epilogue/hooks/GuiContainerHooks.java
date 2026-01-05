package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.player.ChestStealer;
import epilogue.module.modules.render.ChestView;
import epilogue.module.modules.render.dynamicisland.DynamicIsland;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class GuiContainerHooks {
    private GuiContainerHooks() {
    }

    public static void onDrawScreen(GuiContainer self, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (self instanceof GuiChest) {
            ChestView chestView = (ChestView) Epilogue.moduleManager.getModule("ChestView");
            ChestStealer chestStealer = (ChestStealer) Epilogue.moduleManager.getModule("ChestStealer");
            DynamicIsland dynamicIsland = (DynamicIsland) Epilogue.moduleManager.getModule("DynamicIsland");
            if ((chestView != null && chestView.isEnabled()
                    && chestStealer != null && chestStealer.isEnabled()) || (dynamicIsland.isEnabled() && chestStealer.isEnabled())) {
                Minecraft.getMinecraft().setIngameFocus();
                Minecraft.getMinecraft().currentScreen = (GuiChest) self;
                ci.cancel();
            }
        }
    }
}
