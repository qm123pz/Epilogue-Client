package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.event.EventManager;
import epilogue.events.Render2DEvent;
import epilogue.module.modules.misc.Nick;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class GuiIngameForgeHooks {
    private GuiIngameForgeHooks() {
    }

    public static void onRenderGameOverlay(float float1, CallbackInfo callbackInfo) {
        EventManager.call(new Render2DEvent(float1));
    }

    public static float onRenderExperience(EntityPlayerSP entityPlayerSP) {
        if (Epilogue.moduleManager == null) {
            return entityPlayerSP.experience;
        }
        Nick event = (Nick) Epilogue.moduleManager.modules.get(Nick.class);
        return event.isEnabled() && event.level.getValue() ? 0.0F : entityPlayerSP.experience;
    }

    public static int onRenderExperienceLevel(EntityPlayerSP entityPlayerSP) {
        if (Epilogue.moduleManager == null) {
            return entityPlayerSP.experienceLevel;
        }
        Nick event = (Nick) Epilogue.moduleManager.modules.get(Nick.class);
        return event.isEnabled() && event.level.getValue() ? 0 : entityPlayerSP.experienceLevel;
    }
}
