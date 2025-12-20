package epilogue.ui.clickgui.menu.render;

import epilogue.util.render.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public final class DrawUtil {
    private DrawUtil() {
    }

    public static boolean isHovering(float x, float y, float w, float h, float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public static float animate(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) <= 0.001f) return target;
        float add = diff / Math.max(1f, speed);
        if (diff > 0) {
            current += add;
            if (current > target) current = target;
        } else {
            current += add;
            if (current < target) current = target;
        }
        return current;
    }

    public static void resetColor() {
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    public static void drawCircle(float x, float y, float start, float end, float radius, float width, boolean filled, int color) {
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        GL11.glPushMatrix();
        GL11.glDisable(GL_TEXTURE_2D);
        GL11.glEnable(GL_BLEND);
        GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, a);

        if (!filled) {
            GL11.glLineWidth(width);
            GL11.glBegin(GL_LINE_STRIP);
        } else {
            GL11.glBegin(GL_TRIANGLE_FAN);
            for (float i = start; i <= end; i += 3f) {
                double angle = Math.toRadians(i);
                GL11.glVertex2d(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius);
            }
            GL11.glEnd();
            GL11.glEnable(GL_TEXTURE_2D);
            GL11.glDisable(GL_BLEND);
            GL11.glPopMatrix();
            resetColor();
            return;
        }

        for (float i = start; i <= end; i += 3f) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius);
        }
        GL11.glEnd();
        GL11.glEnable(GL_TEXTURE_2D);
        GL11.glDisable(GL_BLEND);
        GL11.glPopMatrix();
        resetColor();
    }

    public static void drawCircleCGUI(float x, float y, float radius, int color) {
        drawCircle(x, y, 0, 360, radius / 2f, 1f, true, color);
    }

    public static void drawRect(float x, float y, float w, float h, Color c) {
        RenderUtil.drawRect(x, y, w, h, c.getRGB());
    }
}
