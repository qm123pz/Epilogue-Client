package epilogue.util.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import epilogue.util.RenderUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_ONE;

public class BloomShader {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ShaderUtils KAWASE_DOWN = new ShaderUtils("kawaseDownBloom");
    private static final ShaderUtils KAWASE_UP = new ShaderUtils("kawaseUpBloom");

    private static Framebuffer framebuffer = new Framebuffer(1, 1, true);
    private static Framebuffer inputFramebuffer;
    private static int currentIterations;

    private static final List<Framebuffer> framebufferList = new ArrayList<>();

    private static Framebuffer ensureInputFramebuffer() {
        if (inputFramebuffer == null || inputFramebuffer.framebufferWidth != mc.displayWidth || inputFramebuffer.framebufferHeight != mc.displayHeight) {
            inputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
        }
        return inputFramebuffer;
    }

    public static Framebuffer beginFramebuffer() {
        Framebuffer fb = ensureInputFramebuffer();
        GL11.glClearColor(0, 0, 0, 0);
        fb.framebufferClear();
        fb.bindFramebuffer(false);
        GL11.glViewport(0, 0, fb.framebufferWidth, fb.framebufferHeight);
        return fb;
    }

    private static void initFramebuffers(float iterations) {
        framebufferList.forEach(Framebuffer::deleteFramebuffer);
        framebufferList.clear();
        framebuffer = null;
        framebufferList.add(framebuffer = RenderUtil.createFrameBuffer(null, true));

        for (int i = 1; i <= iterations; i++) {
            Framebuffer currentBuffer = new Framebuffer((int) (mc.displayWidth / Math.pow(2, i)), (int) (mc.displayHeight / Math.pow(2, i)), true);
            currentBuffer.setFramebufferFilter(GL_LINEAR);

            GlStateManager.bindTexture(currentBuffer.framebufferTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL14.GL_MIRRORED_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL14.GL_MIRRORED_REPEAT);
            GlStateManager.bindTexture(0);

            framebufferList.add(currentBuffer);
        }
    }

    public static void renderBloom(int framebufferTexture, int iterations, int offset) {
        iterations = Math.max(1, iterations);
        offset = Math.max(1, offset);
        if (currentIterations != iterations || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
            initFramebuffers(iterations);
            currentIterations = iterations;
        }

        RenderUtil.setAlphaLimit(0);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_ONE, GL_ONE);

        GL11.glClearColor(0, 0, 0, 0);
        renderDownFBO(framebufferList.get(1), framebufferTexture, offset);

        for (int i = 1; i < iterations; i++) {
            renderDownFBO(framebufferList.get(i + 1), framebufferList.get(i).framebufferTexture, offset);
        }

        for (int i = iterations; i > 1; i--) {
            renderUpFBO(framebufferList.get(i - 1), framebufferList.get(i).framebufferTexture, offset);
        }

        Framebuffer lastBuffer = framebufferList.get(0);
        lastBuffer.framebufferClear();
        lastBuffer.bindFramebuffer(false);
        GL11.glViewport(0, 0, lastBuffer.framebufferWidth, lastBuffer.framebufferHeight);
        KAWASE_UP.init();
        KAWASE_UP.setUniformf("offset", offset, offset);
        KAWASE_UP.setUniformf("halfpixel", 1.0f / lastBuffer.framebufferWidth, 1.0f / lastBuffer.framebufferHeight);
        KAWASE_UP.setUniformf("iResolution", lastBuffer.framebufferWidth, lastBuffer.framebufferHeight);
        KAWASE_UP.setUniformi("inTexture", 0);
        KAWASE_UP.setUniformi("check", 1);
        KAWASE_UP.setUniformi("textureToCheck", 1);
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE1);
        RenderUtil.bindTexture(framebufferTexture);
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        RenderUtil.bindTexture(framebufferList.get(1).framebufferTexture);
        ShaderUtils.drawQuads();
        KAWASE_UP.unload();

        GlStateManager.clearColor(0, 0, 0, 0);
        mc.getFramebuffer().bindFramebuffer(false);
        GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        RenderUtil.setAlphaLimit(0);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderUtil.bindTexture(framebufferList.get(0).framebufferTexture);
        RenderUtil.drawFixedQuads();
        GlStateManager.bindTexture(0);
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
    }

    private static void renderDownFBO(Framebuffer fb, int texture, float offset) {
        fb.framebufferClear();
        fb.bindFramebuffer(false);
        GL11.glViewport(0, 0, fb.framebufferWidth, fb.framebufferHeight);
        KAWASE_DOWN.init();
        KAWASE_DOWN.setUniformf("offset", offset, offset);
        KAWASE_DOWN.setUniformf("halfpixel", 1.0f / fb.framebufferWidth, 1.0f / fb.framebufferHeight);
        KAWASE_DOWN.setUniformf("iResolution", fb.framebufferWidth, fb.framebufferHeight);
        KAWASE_DOWN.setUniformi("inTexture", 0);
        RenderUtil.bindTexture(texture);
        ShaderUtils.drawQuads();
        KAWASE_DOWN.unload();
    }

    private static void renderUpFBO(Framebuffer fb, int texture, float offset) {
        fb.framebufferClear();
        fb.bindFramebuffer(false);
        GL11.glViewport(0, 0, fb.framebufferWidth, fb.framebufferHeight);
        KAWASE_UP.init();
        KAWASE_UP.setUniformf("offset", offset, offset);
        KAWASE_UP.setUniformf("halfpixel", 1.0f / fb.framebufferWidth, 1.0f / fb.framebufferHeight);
        KAWASE_UP.setUniformf("iResolution", fb.framebufferWidth, fb.framebufferHeight);
        KAWASE_UP.setUniformi("inTexture", 0);
        KAWASE_UP.setUniformi("check", 0);
        KAWASE_UP.setUniformi("textureToCheck", 1);
        RenderUtil.bindTexture(texture);
        ShaderUtils.drawQuads();
        KAWASE_UP.unload();
    }
}