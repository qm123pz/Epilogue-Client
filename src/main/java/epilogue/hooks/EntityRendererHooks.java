package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.data.Box;
import epilogue.event.EventManager;
import epilogue.events.PickEvent;
import epilogue.events.RaytraceEvent;
import epilogue.events.Render3DEvent;
import epilogue.module.modules.combat.Aura;
import epilogue.module.modules.movement.ViewClip;
import epilogue.module.modules.player.GhostHand;
import epilogue.module.modules.player.NoDebuff;
import epilogue.module.modules.player.Scaffold;
import epilogue.module.modules.render.Camera;
import epilogue.module.modules.render.NoHurtCam;
import epiloguemixinbridge.IAccessorEntityLivingBase;
import epiloguemixinbridge.IAccessorEntityPlayer;
import epiloguemixinbridge.IAccessorEntityRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

public final class EntityRendererHooks {
    private static Box<Integer> cameraSlot;
    private static Box<ItemStack> cameraUsing;
    private static Box<Integer> cameraUseCount;
    private static Box<Integer> rendererSlot;

    private EntityRendererHooks() {
    }

    public static void onUpdateCameraAndRender(float float1, long long2, CallbackInfo callbackInfo) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            Scaffold scaffold = (Scaffold) Epilogue.moduleManager.modules.get(Scaffold.class);
            if (scaffold.isEnabled() && scaffold.itemSpoof.getValue()) {
                int slot = scaffold.getSlot();
                if (slot >= 0) {
                    cameraSlot = new Box<>(mc.thePlayer.inventory.currentItem);
                    mc.thePlayer.inventory.currentItem = slot;
                }
            }
            Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
            if (aura.isEnabled() && aura.isBlocking()) {
                cameraUsing = new Box<>(((IAccessorEntityPlayer) mc.thePlayer).getItemInUse());
                ((IAccessorEntityPlayer) mc.thePlayer).setItemInUse(mc.thePlayer.inventory.getCurrentItem());
                cameraUseCount = new Box<>(((IAccessorEntityPlayer) mc.thePlayer).getItemInUseCount());
                ((IAccessorEntityPlayer) mc.thePlayer).setItemInUseCount(69000);
            }
        }
    }

    public static void onPostUpdateCameraAndRender(float float1, long long2, CallbackInfo callbackInfo) {
        Minecraft mc = Minecraft.getMinecraft();
        if (cameraSlot != null && mc.thePlayer != null) {
            mc.thePlayer.inventory.currentItem = cameraSlot.value;
            cameraSlot = null;
        }
        if (cameraUsing != null && mc.thePlayer != null) {
            ((IAccessorEntityPlayer) mc.thePlayer).setItemInUse(cameraUsing.value);
            cameraUsing = null;
        }
        if (cameraUseCount != null && mc.thePlayer != null) {
            ((IAccessorEntityPlayer) mc.thePlayer).setItemInUseCount(cameraUseCount.value);
            cameraUseCount = null;
        }
    }

    public static void onUpdateRenderer(CallbackInfo callbackInfo) {
        Minecraft mc = Minecraft.getMinecraft();
        Scaffold scaffold = (Scaffold) Epilogue.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled() && scaffold.itemSpoof.getValue()) {
            int slot = scaffold.getSlot();
            if (slot >= 0) {
                rendererSlot = new Box<>(mc.thePlayer.inventory.currentItem);
                mc.thePlayer.inventory.currentItem = slot;
            }
        }
    }

    public static void onPostUpdateRenderer(CallbackInfo callbackInfo) {
        Minecraft mc = Minecraft.getMinecraft();
        if (rendererSlot != null && mc.thePlayer != null) {
            mc.thePlayer.inventory.currentItem = rendererSlot.value;
            rendererSlot = null;
        }
    }

    public static void onRenderWorldPass(int integer, float float2, long long3, CallbackInfo callbackInfo) {
        EventManager.call(new Render3DEvent(float2));
    }

    public static float onHurtCameraEffect(float float1) {
        if (Epilogue.moduleManager == null) {
            return float1;
        }
        NoHurtCam noHurtCam = (NoHurtCam) Epilogue.moduleManager.modules.get(NoHurtCam.class);
        return noHurtCam.isEnabled() ? float1 * (float) noHurtCam.multiplier.getValue().intValue() / 100.0F : float1;
    }

    public static double onGetMouseOver(double range) {
        PickEvent event = new PickEvent(range);
        EventManager.call(event);
        return event.getRange();
    }

    public static double onStoreMouseOver(double range) {
        RaytraceEvent event = new RaytraceEvent(range);
        EventManager.call(event);
        return event.getRange();
    }

    public static void onGetMouseOverList(
            float float1,
            CallbackInfo callbackInfo,
            Entity entity,
            double double4,
            double double5,
            Vec3 vec36,
            boolean boolean7,
            int integer8,
            Vec3 vec39,
            Vec3 vec310,
            Vec3 vec311,
            float float12,
            List<Entity> list,
            double double14,
            int integer15
    ) {
        if (Epilogue.moduleManager != null) {
            GhostHand event = (GhostHand) Epilogue.moduleManager.modules.get(GhostHand.class);
            if (event.isEnabled()) {
                list.removeIf(event::shouldSkip);
            }
        }
    }

    public static double onOrientCamera(EntityRenderer self, Vec3 vec31, Vec3 vec32) {
        if (Epilogue.moduleManager == null) {
            return vec31.distanceTo(vec32);
        }
        return Epilogue.moduleManager.modules.get(ViewClip.class).isEnabled()
                ? (double) ((IAccessorEntityRenderer) self).getThirdPersonDistance()
                : vec31.distanceTo(vec32);
    }

    public static Material onSetupFog(Block block) {
        if (Epilogue.moduleManager == null) {
            return block.getMaterial();
        }
        return Epilogue.moduleManager.modules.get(ViewClip.class).isEnabled() ? Material.air : block.getMaterial();
    }

    public static boolean onUpdateFogColor(EntityLivingBase entityLivingBase, Potion potion) {
        if (potion == Potion.blindness && Epilogue.moduleManager != null) {
            NoDebuff noDebuff = (NoDebuff) Epilogue.moduleManager.modules.get(NoDebuff.class);
            if (noDebuff.isEnabled() && noDebuff.blindness.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entityLivingBase).getActivePotionsMap().containsKey(potion.id);
    }

    public static boolean onSetupFogPotion(EntityLivingBase entityLivingBase, Potion potion) {
        if (potion == Potion.blindness && Epilogue.moduleManager != null) {
            NoDebuff noDebuff = (NoDebuff) Epilogue.moduleManager.modules.get(NoDebuff.class);
            if (noDebuff.isEnabled() && noDebuff.blindness.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entityLivingBase).getActivePotionsMap().containsKey(potion.id);
    }

    public static boolean onSetupCameraTransform(EntityPlayerSP entityPlayerSP, Potion potion) {
        if (potion == Potion.confusion && Epilogue.moduleManager != null) {
            NoDebuff noDebuff = (NoDebuff) Epilogue.moduleManager.modules.get(NoDebuff.class);
            if (noDebuff.isEnabled() && noDebuff.nausea.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entityPlayerSP).getActivePotionsMap().containsKey(potion.id);
    }

    public static void onSmoothCamera(float partialTicks, int pass, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (Epilogue.moduleManager != null && mc.getRenderViewEntity() != null) {
            Camera camera = (Camera) Epilogue.moduleManager.modules.get(Camera.class);
            if (camera != null && camera.isEnabled()) {
                if (camera.onlyThirdPerson.getValue() && mc.gameSettings.thirdPersonView == 0) {
                    return;
                }

                Entity entity = mc.getRenderViewEntity();
                double targetX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
                double targetY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
                double targetZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

                camera.updateSmoothedPosition(targetX, targetY, targetZ);

                double deltaX = camera.getSmoothedX() - targetX;
                double deltaY = camera.getSmoothedY() - targetY;
                double deltaZ = camera.getSmoothedZ() - targetZ;

                org.lwjgl.opengl.GL11.glTranslated(deltaX, deltaY, deltaZ);
            }
        }
    }
}
