package epilogue.rendering.rendersystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class RenderSystem {
    public static final float DIVIDE_BY_255 = 0.003921568627451F;
    public static Minecraft mc = Minecraft.getMinecraft();

    private static double frameDeltaTime = 0;

    public static double getWidth() {
        return new ScaledResolution(mc).getScaledWidth_double();
    }

    public static double getHeight() {
        return new ScaledResolution(mc).getScaledHeight_double();
    }

    public static void linearFilter() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    public static void nearestFilter() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    public static void color(int color) {
        float f = (color >> 24 & 255) * DIVIDE_BY_255;
        float f1 = (color >> 16 & 255) * DIVIDE_BY_255;
        float f2 = (color >> 8 & 255) * DIVIDE_BY_255;
        float f3 = (color & 255) * DIVIDE_BY_255;
        GlStateManager.color(f1, f2, f3, f);
    }

    public static void drawRect(double left, double top, double right, double bottom, int color) {
        if (left > right) {
            double i = left;
            left = right;
            right = i;
        }

        if (top > bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }

        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();

        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        color(color);

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);

        GL11.glVertex2d(left, bottom);
        GL11.glVertex2d(right, bottom);
        GL11.glVertex2d(left, top);
        GL11.glVertex2d(right, top);

        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
    }

    public static int reAlpha(int rgb, double alpha) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int a = (int) Math.max(0, Math.min(255, alpha * 255));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int hexColor(float r, float g, float b, float a) {
        int ri = (int) (r * 255);
        int gi = (int) (g * 255);
        int bi = (int) (b * 255);
        int ai = (int) (a * 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    public static void translateAndScale(double x, double y, double scale) {
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1);
        GlStateManager.translate(-x, -y, 0);
    }

    public static double getFrameDeltaTime() {
        return frameDeltaTime;
    }

    public static void setFrameDeltaTime(double dt) {
        frameDeltaTime = dt;
    }
}
