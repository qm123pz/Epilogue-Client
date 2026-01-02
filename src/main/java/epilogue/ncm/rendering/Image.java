package epilogue.ncm.rendering;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;

public class Image {

    public enum Type {
        Normal,
        NoColor
    }

    public static void draw(int textureId, double x, double y, double width, double height, Type type) {
        draw(textureId, x, y, width, height, width, height, type);
    }

    public static void draw(int textureId, double x, double y, double width, double height, double textureWidth, double textureHeight, Type type) {
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.color(1, 1, 1, 1);

        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        if (textureId != TextureUtil.missingTexture.getGlTextureId()) {
            GlStateManager.bindTexture(textureId);
            drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, textureWidth, textureHeight);
        }

        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
    }

    public static void draw(net.minecraft.util.ResourceLocation location, double x, double y, double width, double height, double textureWidth, double textureHeight) {
        ITextureObject obj = Minecraft.getMinecraft().getTextureManager().getTexture(location);
        if (obj == null) {
            return;
        }
        draw(obj.getGlTextureId(), x, y, width, height, textureWidth, textureHeight, Type.NoColor);
    }

    public static void drawModalRectWithCustomSizedTexture(double x, double y, double u, double v, double width, double height, double textureWidth, double textureHeight) {
        double f = 1.0F / textureWidth;
        double f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, (y + height), 0.0D).tex((u * f), ((v + height) * f1)).endVertex();
        worldrenderer.pos((x + width), (y + height), 0.0D).tex(((u + width) * f), ((v + height) * f1)).endVertex();
        worldrenderer.pos((x + width), y, 0.0D).tex(((u + width) * f), (v * f1)).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex((u * f), (v * f1)).endVertex();
        tessellator.draw();
    }

}
