package epilogue.hooks;

import epilogue.module.modules.render.Animations;
import epiloguemixinbridge.IInvokerItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class ItemRendererHooks {
    private static float astolfoSpinDelay;
    private static long astolfoSpinLastUpdateTime = System.currentTimeMillis();

    private ItemRendererHooks() {
    }

    public static void onScaleFirstPersonItem(float partialTicks, CallbackInfo ci) {
        Animations animations = Animations.getInstance();
        if (animations == null || !animations.isEnabled()) {
            return;
        }

        float itemScale = animations.scale.getValue() + 1.0F;
        if (itemScale != 1.0F) {
            GlStateManager.scale(itemScale, itemScale, itemScale);
        }
    }

    public static void onRenderBlockingItem(ItemRenderer self, float partialTicks, CallbackInfo ci) {
        Animations animations = Animations.getInstance();
        if (animations == null || !animations.isEnabled()) {
            return;
        }

        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null || !player.isBlocking()) {
            return;
        }

        float swingProgress = player.getSwingProgress(partialTicks);
        String mode = animations.swordMode.getModeString();

        GL11.glTranslated(animations.blockPosX.getValue().doubleValue(),
                animations.blockPosY.getValue().doubleValue(),
                animations.blockPosZ.getValue().doubleValue());

        switch (mode) {
            case "Push":
                applyPushAnimation(swingProgress);
                break;
            case "Dash":
                applyDashAnimation(swingProgress);
                break;
            case "Slash":
                applySlashAnimation(swingProgress);
                break;
            case "Slide":
                applySlideAnimation(swingProgress);
                break;
            case "Scale":
                applyScaleAnimation(swingProgress);
                break;
            case "Swank":
                applySwankAnimation(swingProgress);
                break;
            case "Swang":
                applySwangAnimation(swingProgress);
                break;
            case "Swonk":
                applySwonkAnimation(swingProgress);
                break;
            case "Edit":
                applyEditAnimation(swingProgress);
                break;
            case "Rhys":
                applyRhysAnimation(swingProgress, partialTicks);
                break;
            case "Stab":
                applyStabAnimation(swingProgress);
                break;
            case "Float":
                applyFloatAnimation(swingProgress);
                break;
            case "Remix":
                applyRemixAnimation(swingProgress);
                break;
            case "Avatar":
                applyAvatarAnimation(self, swingProgress);
                break;
            case "Xiv":
                applyXivAnimation(swingProgress);
                break;
            case "Yamato":
                applyYamatoAnimation(swingProgress);
                break;
            case "SlideSwing":
                applySlideSwingAnimation(self, swingProgress);
                break;
            case "SmallPush":
                applySmallPushAnimation(self, swingProgress);
                break;
            case "Invent":
                applyInventAnimation(swingProgress);
                break;
            case "Leaked":
                applyLeakedAnimation(swingProgress);
                break;
            case "Aqua":
                applyAquaAnimation(swingProgress);
                break;
            case "Astro":
                applyAstroAnimation(swingProgress);
                break;
            case "Fadeaway":
                applyFadeawayAnimation(swingProgress);
                break;
            case "Astolfo":
                applyAstolfoAnimation(swingProgress);
                break;
            case "AstolfoSpin":
                applyAstolfoSpinAnimation(self);
                break;
            case "Moon":
                applyMoonAnimation(swingProgress);
                break;
            case "MoonPush":
                applyMoonPushAnimation(swingProgress);
                break;
            case "Smooth":
                applySmoothAnimation(swingProgress);
                break;
            case "Tap1":
                applyTap1Animation(swingProgress);
                break;
            case "Tap2":
                applyTap2Animation(swingProgress);
                break;
            case "Sigma1":
                applySigma3Animation(swingProgress);
                break;
            case "Sigma2":
                applySigma4Animation(self, swingProgress);
                break;
        }
    }

    private static void applyPushAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(-var9 * 40.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-var9 * 30.0F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private static void applyDashAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GL11.glRotated(-var9 * 22.0F, var9 / 2, 0.0F, 9.0F);
        GL11.glRotated(-var9 * 50.0F, 0.8F, var9 / 2, 0F);
    }

    private static void applySlashAnimation(float swingProgress) {
        GL11.glTranslated(0.08, 0.08, 0.0);
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        GlStateManager.rotate(-var * 70F, 5F, 13F, 50F);
    }

    private static void applySlideAnimation(float swingProgress) {
        GL11.glTranslated(0.08, -0.11, -0.07);
        float var91 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.translate(-0.4f, 0.28f, 0.0f);
        GlStateManager.rotate(-var91 * 35.0f, -8.0f, -0.0f, 9.0f);
        GlStateManager.rotate(-var91 * 70.0f, 1.0f, -0.4f, -0.0f);
    }

    private static void applyScaleAnimation(float swingProgress) {
        GL11.glTranslated(0.84, -0.77, -1.1);
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float var3 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float var4 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(var3 * -27.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(var4 * -27.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(var4 * -27.0F, 0.0F, 0.0F, 0.0F);
    }

    private static void applySwankAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GL11.glRotatef(var9 * 30.0F / 2.0F, -var9, -0.0F, 9.0F);
        GL11.glRotatef(var9 * 40.0F, 1.0F, -var9 / 2.0F, -0.0F);
    }

    private static void applySwangAnimation(float swingProgress) {
        GL11.glTranslated(0.0, 0.03, 0.0);
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(-var9 * 74.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-var9 * 52.0F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private static void applySwonkAnimation(float swingProgress) {
        GL11.glTranslated(0.0, 0.03, 0.0);
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GL11.glRotated(-var9 * -30.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
        GL11.glRotated(-var9 * 7.5F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private static void applyEditAnimation(float swingProgress) {
        GL11.glTranslated(-0.04, 0.06, 0.0);
        float Swang = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(Swang * 16.0F / 2.0F, -Swang, -0.0F, 2.0F);
        GlStateManager.rotate(Swang * 22.0F, 1.0F, -Swang / 3.0F, -0.0F);
    }

    private static void applyRhysAnimation(float swingProgress, float partialTicks) {
        GL11.glTranslated(0.0, 0.19, 0.0);
        GlStateManager.translate(0.41F, -0.25F, -0.5555557F);
        GlStateManager.rotate(35.0F, 0f, 1.5F, 0.0F);
        float racism = MathHelper.sin(swingProgress * swingProgress / 64 * (float) Math.PI);
        float f4 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(racism * -5.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(f4 * -12.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f4 * -65.0F, 1.0F, 0.0F, 0.0F);
    }

    private static void applyStabAnimation(float swingProgress) {
        GL11.glTranslated(-0.25, 0.45, 0.8);
        float spin = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.translate(0.6f, 0.3f, -0.6f + -spin * 0.7);
        GlStateManager.rotate(6090, 0.0f, 0.0f, 0.1f);
        GlStateManager.rotate(6085, 0.0f, 0.1f, 0.0f);
        GlStateManager.rotate(6110, 0.1f, 0.0f, 0.0f);
    }

    private static void applyFloatAnimation(float swingProgress) {
        GlStateManager.rotate(-MathHelper.sin(swingProgress * swingProgress * (float) Math.PI) * 40.0F / 2.0F,
                MathHelper.sin(swingProgress * swingProgress * (float) Math.PI) / 2.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-MathHelper.sin(swingProgress * swingProgress * (float) Math.PI) * 30.0F,
                1.0F, MathHelper.sin(swingProgress * swingProgress * (float) Math.PI) / 2.0F, -0.0F);
    }

    private static void applyRemixAnimation(float swingProgress) {
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        GlStateManager.rotate(0.0F, -2.0F, 0.0F, 10.0F);
        GlStateManager.rotate(-var * 25.0F, 0.5F, 0F, 1F);
    }

    private static void applyAvatarAnimation(ItemRenderer self, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -40.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        ((IInvokerItemRenderer) self).callDoBlockTransformations();
    }

    private static void applyXivAnimation(float swingProgress) {
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        float var16 = MathHelper.sin((float) (swingProgress * swingProgress * Math.PI));
        GlStateManager.rotate(-var16 * 20.0f, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-var * 20.0f, 0.0f, 0.0f, 1.0f);
        GlStateManager.rotate(-var * 80.0f, 1.0f, 0.0f, 0.0f);
    }

    private static void applyYamatoAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GL11.glRotatef(-var9 * 200F / 2.0F, -9.0F, 5.0F, 9.0F);
    }

    private static void applySlideSwingAnimation(ItemRenderer self, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(f * -0.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        ((IInvokerItemRenderer) self).callDoBlockTransformations();
    }

    private static void applySmallPushAnimation(ItemRenderer self, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(f * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(f1 * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(f1 * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        ((IInvokerItemRenderer) self).callDoBlockTransformations();
    }

    private static void applyInventAnimation(float swingProgress) {
        float table = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        GlStateManager.rotate(-table * 30.0F, -8.0F, -0.2F, 9.0F);
    }

    private static void applyLeakedAnimation(float swingProgress) {
        GL11.glTranslated(0.08, 0.02, 0.0);
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        GlStateManager.rotate(-var * 41F, 1.1F, 0.8F, -0.3F);
    }

    private static void applyAquaAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(-var9 * 17.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-var9 * 6.0F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private static void applyAstroAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(var9 * 50.0F / 9.0F, -var9, -0.0F, 90.0F);
        GlStateManager.rotate(var9 * 50.0F, 200.0F, -var9 / 2.0F, -0.0F);
    }

    private static void applyFadeawayAnimation(float swingProgress) {
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        float var16 = MathHelper.sin((float) (swingProgress * swingProgress * Math.PI));
        GlStateManager.rotate(-var16 * 45f, 0.0f, 0.0f, 1.0f);
        GlStateManager.rotate(-var * 0f, 0.0f, 0.0f, 1.0f);
        GlStateManager.rotate(-var * 0f, 1.5f, 0.0f, 0.0f);
    }

    private static void applyAstolfoAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(-var9 * 58.0F / 2.0F, var9 / 2.0F, 1.0F, 0.5F);
        GlStateManager.rotate(-var9 * 43.0F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private static void applyAstolfoSpinAnimation(ItemRenderer self) {
        GlStateManager.rotate(astolfoSpinDelay, 0.0F, 0.0F, -0.1F);
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - astolfoSpinLastUpdateTime;
        astolfoSpinDelay += elapsedTime * 360.0 / 850.0;
        astolfoSpinLastUpdateTime = currentTime;
        if (astolfoSpinDelay > 360.0F) {
            astolfoSpinDelay = 0.0F;
        }
        ((IInvokerItemRenderer) self).callDoBlockTransformations();
    }

    private static void applyMoonAnimation(float swingProgress) {
        GL11.glTranslated(-0.08, 0.12, 0.0);
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(-var9 * 65.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-var9 * 60.0F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private static void applyMoonPushAnimation(float swingProgress) {
        float sin = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.translate(-0.2F, 0.45F, 0.25F);
        GlStateManager.rotate(-sin * 20.0F, -5.0F, -5.0F, 9.0F);
    }

    private static void applySmoothAnimation(float swingProgress) {
        GL11.glTranslated(0.14, -0.1, -0.24);
        float var91 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.translate(-0.36f, 0.25f, -0.06f);
        GlStateManager.rotate(-var91 * 35.0f, -8.0f, -0.0f, 9.0f);
        GlStateManager.rotate(-var91 * 70.0f, 1.0f, 0.4f, -0.0f);
    }

    private static void applyTap1Animation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((swingProgress * 0.8f - (swingProgress * swingProgress) * 0.8f) * -90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.37F, 0.37F, 0.37F);
    }

    private static void applyTap2Animation(float swingProgress) {
        GL11.glTranslated(0.0, -0.1f, 0.0);
        GlStateManager.translate(0.56F, -0.42F, -0.71999997F);
        GlStateManager.rotate(30, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI) * -30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    private static void applySigma3Animation(float swingProgress) {
        GL11.glTranslated(0.02, 0.02, 0.0);
        GL11.glTranslated(0.4D, -0.06D, -0.46D);
        float Swang = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(Swang * 25.0F / 2.0F, -Swang, -0.0F, 9.0F);
        GlStateManager.rotate(Swang * 15.0F, 1.0F, -Swang / 2.0F, -0.0F);
    }

    private static void applySigma4Animation(ItemRenderer self, float swingProgress) {
        GL11.glTranslated(-0.6, 0.2, 0.11);
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        GlStateManager.rotate(-var * 55 / 2.0F, -8.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-var * 45, 1.0F, var / 2, 0.0F);
        ((IInvokerItemRenderer) self).callDoBlockTransformations();
        GL11.glTranslated(-0.08, -1.25, 1.25);
    }
}
