package epilogue.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import epilogue.util.RenderUtil;
import epilogue.util.shader.BloomShader;
import epilogue.util.shader.BlurShader;

import java.util.function.Supplier;

public class PostProcessing {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static boolean internalBloomSuppressed;

    public static boolean isInternalBloomSuppressed() {
        return internalBloomSuppressed;
    }

    public static void setInternalBloomSuppressed(boolean suppressed) {
        internalBloomSuppressed = suppressed;
    }

    public static void applyTestGlow() {
        if (!OpenGlHelper.isFramebufferEnabled()) return;
        if (!epilogue.module.modules.render.PostProcessing.isTestGlowEnabled()) return;
        BloomShader.renderBloom(mc.getFramebuffer().framebufferTexture,
                epilogue.module.modules.render.PostProcessing.getBloomIterations(),
                Math.max(1, epilogue.module.modules.render.PostProcessing.getBloomOffset()));
    }

    public static void drawBlur(float x, float y, float x2, float y2, Supplier<Runnable> maskDrawer) {
        if (!OpenGlHelper.isFramebufferEnabled()) return;
        if (!epilogue.module.modules.render.PostProcessing.isBlurEnabled()) {
            return;
        }

        float left = x;
        float top = y;
        float right = x2;
        float bottom = y2;

        if (left > right) {
            float t = left;
            left = right;
            right = t;
        }

        if (top > bottom) {
            float t = top;
            top = bottom;
            bottom = t;
        }

        ScaledResolution sc = new ScaledResolution(mc);
        int width = sc.getScaledWidth();
        int height = sc.getScaledHeight();

        StencilUtil.write(false);
        Runnable r = maskDrawer.get();
        if (r != null) r.run();

        StencilUtil.erase(true);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.pushMatrix();
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();

        float strength = epilogue.module.modules.render.PostProcessing.getBlurStrength();
        int blurredTex = BlurShader.render(mc.getFramebuffer().framebufferTexture, strength * 1.5f, left, top, right - left, bottom - top, width, height);
        RenderUtil.bindTexture(blurredTex);
        RenderUtil.drawQuads();
        GlStateManager.bindTexture(0);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.popMatrix();

        StencilUtil.dispose();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawBlurRect(float x, float y, float x2, float y2) {
        drawBlur(x, y, x2, y2, () -> () -> {
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            epilogue.util.render.RenderUtil.setup2DRendering(() -> {
                net.minecraft.client.gui.Gui.drawRect((int)x, (int)y, (int)x2, (int)y2, -1);
            });
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
        });
    }

    public static Framebuffer beginBloom() {
        if (internalBloomSuppressed) return null;
        if (!epilogue.module.modules.render.PostProcessing.isBloomEnabled()
                && !epilogue.module.modules.render.PostProcessing.isTestGlowEnabled()) {
            return null;
        }
        return BloomShader.beginFramebuffer();
    }

    public static void endBloom(Framebuffer bloomBuffer) {
        endBloom(bloomBuffer, epilogue.module.modules.render.PostProcessing.getBloomOffset());
    }

    public static void endBloom(Framebuffer bloomBuffer, int bloomOffset) {
        if (bloomBuffer == null) return;
        mc.getFramebuffer().bindFramebuffer(false);
        BloomShader.renderBloom(bloomBuffer.framebufferTexture,
                epilogue.module.modules.render.PostProcessing.getBloomIterations(),
                Math.max(1, bloomOffset));
    }
}
