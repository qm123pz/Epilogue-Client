package epilogue.util.render;

import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

public class RoundedUtil {

    public static void drawRound(float x, float y, float width, float height, float radius, Color color) {
        if (width <= 0.0f || height <= 0.0f) return;
        float r = Math.max(0.0f, Math.min(radius, Math.min(width, height) / 2.0f));

        boolean tex = glIsEnabled(GL_TEXTURE_2D);
        boolean blend = glIsEnabled(GL_BLEND);
        boolean depth = glIsEnabled(GL_DEPTH_TEST);
        boolean cull = glIsEnabled(GL_CULL_FACE);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.disableDepth();
        glDisable(GL_CULL_FACE);
        glDisable(GL_TEXTURE_2D);
        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        float x2 = x + width;
        float y2 = y + height;

        glBegin(GL_QUADS);
        glVertex2f(x + r, y);
        glVertex2f(x2 - r, y);
        glVertex2f(x2 - r, y2);
        glVertex2f(x + r, y2);

        glVertex2f(x, y + r);
        glVertex2f(x + r, y + r);
        glVertex2f(x + r, y2 - r);
        glVertex2f(x, y2 - r);

        glVertex2f(x2 - r, y + r);
        glVertex2f(x2, y + r);
        glVertex2f(x2, y2 - r);
        glVertex2f(x2 - r, y2 - r);
        glEnd();

        int segments = 24;
        drawQuarterFan(x + r, y + r, r, 180.0, 270.0, segments);
        drawQuarterFan(x + r, y2 - r, r, 90.0, 180.0, segments);
        drawQuarterFan(x2 - r, y2 - r, r, 0.0, 90.0, segments);
        drawQuarterFan(x2 - r, y + r, r, 270.0, 360.0, segments);

        if (tex) glEnable(GL_TEXTURE_2D);
        if (depth) GlStateManager.enableDepth();
        if (cull) glEnable(GL_CULL_FACE);
        if (!blend) GlStateManager.disableBlend();
    }

    private static void drawQuarterFan(float cx, float cy, float r, double startDeg, double endDeg, int segments) {
        if (r <= 0.0f) return;
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(cx, cy);
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double ang = Math.toRadians(startDeg + (endDeg - startDeg) * t);
            glVertex2d(cx + Math.cos(ang) * r, cy + Math.sin(ang) * r);
        }
        glEnd();
    }

    public static void drawRoundOutline(float x, float y, float width, float height, float radius, float thickness, Color fill, Color outline) {
        drawRound(x, y, width, height, radius, fill);
        if (width <= 0.0f || height <= 0.0f) return;
        float r = Math.max(0.0f, Math.min(radius, Math.min(width, height) / 2.0f));

        boolean tex = glIsEnabled(GL_TEXTURE_2D);
        boolean blend = glIsEnabled(GL_BLEND);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(Math.max(1.0f, thickness));
        glColor4f(outline.getRed() / 255f, outline.getGreen() / 255f, outline.getBlue() / 255f, outline.getAlpha() / 255f);

        float x2 = x + width;
        float y2 = y + height;

        int segments = 32;
        glBegin(GL_LINE_STRIP);
        arcToLine(x + r, y + r, r, 180.0, 270.0, segments);
        arcToLine(x2 - r, y + r, r, 270.0, 360.0, segments);
        arcToLine(x2 - r, y2 - r, r, 0.0, 90.0, segments);
        arcToLine(x + r, y2 - r, r, 90.0, 180.0, segments);
        glVertex2d(x + r + Math.cos(Math.toRadians(180.0)) * r, y + r + Math.sin(Math.toRadians(180.0)) * r);
        glEnd();

        glDisable(GL_LINE_SMOOTH);
        if (tex) glEnable(GL_TEXTURE_2D);
        if (!blend) GlStateManager.disableBlend();
    }

    private static void arcToLine(float cx, float cy, float r, double startDeg, double endDeg, int segments) {
        if (r <= 0.0f) {
            glVertex2f(cx, cy);
            return;
        }
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double ang = Math.toRadians(startDeg + (endDeg - startDeg) * t);
            glVertex2d(cx + Math.cos(ang) * r, cy + Math.sin(ang) * r);
        }
    }

    public static void drawCircle(float x, float y, float radius, float progress, float thickness, Color color, int unused) {
        if (radius <= 0.0f) return;
        float p = Math.max(0.0f, Math.min(1.0f, progress));

        boolean tex = glIsEnabled(GL_TEXTURE_2D);
        boolean blend = glIsEnabled(GL_BLEND);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(Math.max(1.0f, thickness));
        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        double start = -90.0;
        double end = start + 360.0 * (1.0 - p);
        int points = 90;

        glBegin(GL_LINE_STRIP);
        for (int i = 0; i <= points; i++) {
            double t = i / (double) points;
            double ang = Math.toRadians(start + (end - start) * t);
            glVertex2d(x + Math.cos(ang) * radius, y + Math.sin(ang) * radius);
        }
        glEnd();

        glDisable(GL_LINE_SMOOTH);
        if (tex) glEnable(GL_TEXTURE_2D);
        if (!blend) GlStateManager.disableBlend();
    }

    public static void drawGradientHorizontal(float x, float y, float width, float height, float radius, Color left, Color right) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glDisable(GL_TEXTURE_2D);
        glShadeModel(GL_SMOOTH);
        glBegin(GL_QUADS);
        
        glColor4f(left.getRed() / 255f, left.getGreen() / 255f, left.getBlue() / 255f, left.getAlpha() / 255f);
        glVertex2f(x, y);
        glVertex2f(x, y + height);
        
        glColor4f(right.getRed() / 255f, right.getGreen() / 255f, right.getBlue() / 255f, right.getAlpha() / 255f);
        glVertex2f(x + width, y + height);
        glVertex2f(x + width, y);
        
        glEnd();
        glShadeModel(GL_FLAT);
        glEnable(GL_TEXTURE_2D);
        GlStateManager.disableBlend();
    }

    public static void drawGradientVertical(float x, float y, float width, float height, float radius, Color top, Color bottom) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        glDisable(GL_TEXTURE_2D);
        glShadeModel(GL_SMOOTH);
        glBegin(GL_QUADS);
        
        glColor4f(top.getRed() / 255f, top.getGreen() / 255f, top.getBlue() / 255f, top.getAlpha() / 255f);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        
        glColor4f(bottom.getRed() / 255f, bottom.getGreen() / 255f, bottom.getBlue() / 255f, bottom.getAlpha() / 255f);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        
        glEnd();
        glShadeModel(GL_FLAT);
        glEnable(GL_TEXTURE_2D);
        GlStateManager.disableBlend();
    }
}
