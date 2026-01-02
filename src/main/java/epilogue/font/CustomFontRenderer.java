package epilogue.font;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CustomFontRenderer {
    private static final FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
    private static final Map<Font, CustomFontRenderer> fontRenderers = new HashMap<>();
    
    private final int[] charWidth = new int[65536];
    private final int[] textures = new int[4096];
    private Font font;
    private int fontHeight;
    private int textureWidth = 1024;
    private int textureHeight = 1024;

    private CustomFontRenderer(Font font) {
        this.font = font;
        Arrays.fill(textures, -1);
        Rectangle2D maxBounds = font.getMaxCharBounds(frc);
        this.fontHeight = (int) Math.ceil(maxBounds.getHeight());

        for (int i = 0; i < 65536; i++) {
            charWidth[i] = (int) Math.ceil(font.getStringBounds(String.valueOf((char) i), frc).getWidth());
        }
    }
    
    public static CustomFontRenderer getRenderer(Font font) {
        return fontRenderers.computeIfAbsent(font, CustomFontRenderer::new);
    }
    
    public static void drawString(String text, float x, double y, int color, Font font) {
        if (text == null || text.isEmpty() || font == null) {
            return;
        }
        
        CustomFontRenderer renderer = getRenderer(font);
        renderer.drawStringInternal(text, x, y, color);
    }
    
    private void drawStringInternal(String text, float x, double y, int color) {
        y = y - 2;
        x *= 2;
        y *= 2;
        y -= 2;
        
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = (color >> 24 & 0xFF) / 255f;
        if (a == 0) a = 1;
        
        GlStateManager.color(r, g, b, a);
        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);
        
        int offset = 0;
        char[] chars = text.toCharArray();
        for (char chr : chars) {
            offset += drawChar(chr, x + offset, y);
        }
        
        GL11.glPopMatrix();
    }
    
    private int drawChar(char chr, float x, double y) {
        int page = (chr >> 8) & 0xFF;
        int id = chr & 0xFF;
        int xTexCoord = (id & 0xF) * 64;
        int yTexCoord = ((id >> 4) & 0xF) * 64;
        int width = charWidth[chr];

        GlStateManager.bindTexture(getOrGenerateCharTexture(page));
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d((double) xTexCoord / textureWidth, (double) yTexCoord / textureHeight);
        GL11.glVertex2f(x, (float) y);
        GL11.glTexCoord2d((double) xTexCoord / textureWidth, (double) (yTexCoord + 64) / textureHeight);
        GL11.glVertex2f(x, (float) (y + 64));
        GL11.glTexCoord2d((double) (xTexCoord + width) / textureWidth, (double) (yTexCoord + 64) / textureHeight);
        GL11.glVertex2f(x + width, (float) (y + 64));
        GL11.glTexCoord2d((double) (xTexCoord + width) / textureWidth, (double) yTexCoord / textureHeight);
        GL11.glVertex2f(x + width, (float) y);
        GL11.glEnd();
        
        return width;
    }
    
    private int getOrGenerateCharTexture(int page) {
        if (page < 0 || page >= textures.length) {
            return 0;
        }
        if (textures[page] == -1)
            return textures[page] = generateCharTexture(page);
        return textures[page];
    }
    
    private int generateCharTexture(int page) {
        int textureId = GL11.glGenTextures();
        int offset = page << 8;

        BufferedImage img = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();

        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        
        g.setFont(font);
        g.setColor(Color.WHITE);

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                char ch = (char) ((y << 4 | x) | offset);
                String chr = String.valueOf(ch);
                int charX = x * 64;
                int charY = y * 64;

                GlyphVector gv = font.createGlyphVector(frc, chr);
                g.drawGlyphVector(gv, charX, charY + g.getFontMetrics().getAscent());
            }
        }
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, textureWidth, textureHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, imageToBuffer(img));
        
        g.dispose();
        return textureId;
    }
    
    private static ByteBuffer imageToBuffer(BufferedImage img) {
        int[] arr = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        ByteBuffer buf = ByteBuffer.allocateDirect(4 * arr.length);
        for (int i : arr) {
            buf.putInt(i << 8 | i >> 24 & 0xFF);
        }
        buf.flip();
        return buf;
    }
    
    public static void drawStringWithShadow(String text, float x, float y, int color, Font font) {
        if (text == null || text.isEmpty() || font == null) {
            return;
        }
        
        int shadowColor = (color & 0xFF000000) != 0 ? 
            (((color >> 16) & 0xFF) / 4 << 16) | (((color >> 8) & 0xFF) / 4 << 8) | ((color & 0xFF) / 4) | (color & 0xFF000000) : 
            0x404040;
        
        drawString(text, x + 0.5f, y + 0.5f, shadowColor, font);
        drawString(text, x, y, color, font);
    }
    
    public static int getStringWidth(String text, Font font) {
        if (text == null || text.isEmpty() || font == null) {
            return 0;
        }
        
        CustomFontRenderer renderer = getRenderer(font);
        int width = 0;
        char[] chars = text.toCharArray();
        for (char chr : chars) {
            width += renderer.charWidth[chr];
        }
        return width / 2;
    }
    
    public static int getFontHeight(Font font) {
        if (font == null) {
            return 9;
        }
        CustomFontRenderer renderer = getRenderer(font);
        return renderer.fontHeight / 2;
    }
}