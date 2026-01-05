package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.event.EventManager;
import epilogue.event.types.EventType;
import epilogue.events.RenderLivingEvent;
import epilogue.module.modules.render.ESP;
import epilogue.module.modules.render.NameTags;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class RendererLivingEntityHooks {
    private RendererLivingEntityHooks() {
    }

    public static void onDoRender(EntityLivingBase entityLivingBase, double double2, double double3, double double4, float float5, float float6, CallbackInfo callbackInfo) {
        EventManager.call(new RenderLivingEvent(EventType.PRE, entityLivingBase));
    }

    public static void onPostRender(EntityLivingBase entityLivingBase, double double2, double double3, double double4, float float5, float float6, CallbackInfo callbackInfo) {
        EventManager.call(new RenderLivingEvent(EventType.POST, entityLivingBase));
    }

    public static void onCanRenderName(EntityLivingBase entityLivingBase, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (Epilogue.moduleManager != null) {
            NameTags nameTags = (NameTags) Epilogue.moduleManager.modules.get(NameTags.class);
            if (nameTags.isEnabled() && nameTags.shouldRenderTags(entityLivingBase)) {
                callbackInfoReturnable.setReturnValue(false);
            } else {
                ESP esp = (ESP) Epilogue.moduleManager.modules.get(ESP.class);
                if (esp.isEnabled() && !esp.isOutlineEnabled()) {
                    callbackInfoReturnable.setReturnValue(false);
                }
            }
        }
    }
}
