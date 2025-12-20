package epilogue.ui.mainmenu;

import epilogue.font.CustomFontRenderer;
import epilogue.font.FontTransformer;
import epilogue.util.render.RenderUtil;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.Font;
import java.util.List;

public final class GuiStartupIntro extends GuiScreen {
    private static final ResourceLocation LOGO = new ResourceLocation("minecraft", "epilogue/logo/EpilogueLogo.png");

    private static final float DESIGN_W = 500.0f;
    private static final float DESIGN_H = 500.0f;
    private static final float START_DELAY = 0.00f;
    private static final float T_MOVE_UP = 1.80f;
    private static final float T_HOLD_TOP = 0.60f;
    private static final float T_TEXT_FADE_IN = 1.30f;
    private static final float T_HOLD_TEXT = 3.50f;
    private static final float T_WAIT_BEFORE_END = 1.25f;
    private static final float TEXT_SCALE = 1.0f;
    private static final float BODY_SIZE = 56.0f;
    private static final float EPILO_SIZE = 92.0f;
    private static final float T_TO_MENU_FADE_OUT = 0.35f;
    private static final float T_TO_MENU_FADE_IN = 0.35f;

    private boolean started;
    private float animSeconds;
    private boolean switching;
    private boolean swapped;
    private float switchSeconds;

    @Override
    public void initGui() {
        super.initGui();
        if (!started) {
            started = true;
            animSeconds = 0.0f;
            switchSeconds = 0.0f;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (!switching) {
            animSeconds += 1.0f / 20.0f;
        } else {
            switchSeconds += 1.0f / 20.0f;
        }
        if (!switching && elapsed() > totalDuration()) {
            switching = true;
            swapped = false;
            switchSeconds = 0.0f;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
        GL11.glClearColor(0f, 0f, 0f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        ScaledResolution sr = new ScaledResolution(this.mc);
        float w = sr.getScaledWidth();
        float h = sr.getScaledHeight();
        float uiScale = computeUiScale(w, h);
        beginUiScale(w, h, uiScale);
        float elapsed = elapsedRender(partialTicks);
        if (!switching && elapsed < START_DELAY) {
            endUiScale();
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }
        float t = Math.max(0.0f, elapsed - START_DELAY);
        float p0 = 0.0f;
        float p1 = p0 + T_MOVE_UP;
        float p2 = p1 + T_HOLD_TOP;
        float p3 = p2 + T_TEXT_FADE_IN;
        float p4 = p3 + T_HOLD_TEXT;
        float moveT = easeOut(seg(t, p0, p1));
        float textAlpha = (t >= p3) ? 1.0f : easeInOut(seg(t, p2, p3));
        float moveAlphaEnd = p0 + (T_MOVE_UP * 0.65f);
        float moveAlpha = (t >= moveAlphaEnd) ? 1.0f : easeInOut(seg(t, p0, moveAlphaEnd));

        float centerX = w / 2.0f;
        float centerY = h / 2.0f;
        float base = Math.min(w, h);
        float logoBase = Math.min(256.0f, base * 0.24f);
        float logoSize = logoBase * uiScale;
        float logoW = logoSize;
        float logoH = logoSize;
        if (logoH > base * 0.26f * uiScale) {
            logoH = base * 0.26f * uiScale;
            logoW = logoH;
        }
        float startGroupY = h * 0.64f;
        float endGroupY = h * 0.14f;
        float groupY = lerp(startGroupY, endGroupY, moveT);
        float logoX = centerX - logoW / 2.0f;
        float logoY = groupY - 6.0f * uiScale;
        float epiloY = logoY + logoH + 16.0f * uiScale;
        float alphaMain = clamp01(moveAlpha);
        if (alphaMain > 0.001f) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0f, 1.0f, 1.0f, alphaMain);
            this.mc.getTextureManager().bindTexture(LOGO);
            applyHighQualityTextureSampling();
            RenderUtil.drawTexturedRect(logoX, logoY, logoW, logoH);
        }
        if (alphaMain > 0.001f) {
            String mainText = "Epilogue";
            int color = rgba(255, 255, 255, (int) (255.0f * alphaMain));
            float epiloSize = EPILO_SIZE * uiScale;
            int textW = getStringWidthFontScaled(mainText, epiloSize, TEXT_SCALE);
            float textX = centerX - textW / 2.0f;
            drawStringFontScaled(mainText, textX, epiloY, color, epiloSize, TEXT_SCALE);
        }
        if (textAlpha > 0.001f) {
            int bodyColor = rgba(235, 235, 235, 255);
            String en = "The story completes only when closed, the road reveals its form only at its end—we build forward, yet seldom admit, from the start, that inevitable backward gaze.";
            String zh = "故事在合上书页后才真正完整，道路在抵达边界后方显露出全部形状。我们习惯向前构建，却鲜少在开始的时刻，就承认那个必然到来的、回望的视角。";
            float bodySize = BODY_SIZE * uiScale;
            int maxWidth = (int) (w * 0.82f);
            List<String> enLines = wrapTextScaled(en, maxWidth, bodySize, TEXT_SCALE);
            List<String> zhLines = wrapTextScaled(zh, maxWidth, bodySize, TEXT_SCALE);
            int lineHeight = getFontHeightFontScaled(bodySize, TEXT_SCALE);
            int totalLines = enLines.size() + 1 + zhLines.size();
            int totalH = totalLines * (lineHeight + 3);
            float blockY = h * 0.72f;
            int y = (int) (blockY - totalH / 2.0f + 10.0f * uiScale);
            for (String s : enLines) {
                int sw = getStringWidthFontScaled(s, bodySize, TEXT_SCALE);
                drawStringFontScaled(s, (centerX - sw / 2.0f), y, bodyColor, bodySize, TEXT_SCALE);
                y += lineHeight + 3;
            }
            y += lineHeight;
            for (String s : zhLines) {
                int sw = getStringWidthFontScaled(s, bodySize, TEXT_SCALE);
                drawStringFontScaled(s, (centerX - sw / 2.0f), y, bodyColor, bodySize, TEXT_SCALE);
                y += lineHeight + 3;
            }
            float maskAlpha = 1.0f - clamp01(textAlpha);
            if (maskAlpha > 0.001f) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                endUiScale();
                int scissorY = (int) (blockY - totalH / 2.0f + 10.0f * uiScale) - 8;
                int scissorH = totalH + 16;
                applyScissorRect(0, scissorY, (int) w, scissorH);
                beginUiScale(w, h, uiScale);
                drawFullscreenBlack(maskAlpha);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }
        if (switching) {
            float a = switchAlpha(partialTicks);
            drawFullscreenBlack(a);
            if (!swapped && switchSeconds >= T_TO_MENU_FADE_OUT) {
                swapped = true;
                this.mc.displayGuiScreen(new GuiMainMenu());
            }
        }
        endUiScale();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private float elapsed() {
        return animSeconds;
    }

    private float elapsedRender(float partialTicks) {
        return animSeconds + clamp01(partialTicks) / 20.0f;
    }

    private float totalDuration() {
        return START_DELAY + T_MOVE_UP + T_HOLD_TOP + T_TEXT_FADE_IN + T_HOLD_TEXT + T_WAIT_BEFORE_END;
    }

    private float switchAlpha(float partialTicks) {
        float t = switchSeconds + clamp01(partialTicks) / 20.0f;
        if (t <= 0.0f) return 0.0f;
        if (t < T_TO_MENU_FADE_OUT) {
            return easeInOut(t / T_TO_MENU_FADE_OUT);
        }
        float t2 = t - T_TO_MENU_FADE_OUT;
        if (t2 < T_TO_MENU_FADE_IN) {
            return 1.0f - easeInOut(t2 / T_TO_MENU_FADE_IN);
        }
        return 0.0f;
    }

    private void drawFullscreenBlack(float alpha) {
        if (alpha <= 0.001f) return;
        ScaledResolution sr = new ScaledResolution(this.mc);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(0.0f, 0.0f, 0.0f, clamp01(alpha));
        RenderUtil.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), rgba(0, 0, 0, (int) (255.0f * clamp01(alpha))));
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static float computeUiScale(float w, float h) {
        float s = Math.min(w / DESIGN_W, h / DESIGN_H);
        if (s < 0.75f) s = 0.75f;
        if (s > 2.0f) s = 2.0f;
        return s;
    }

    private static void beginUiScale(float w, float h, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(w / 2.0f, h / 2.0f, 0.0f);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate(-w / 2.0f, -h / 2.0f, 0.0f);
    }

    private static void endUiScale() {
        GlStateManager.popMatrix();
    }

    private void applyScissorRect(int x, int y, int width, int height) {
        int scale = new ScaledResolution(this.mc).getScaleFactor();
        int sx = x * scale;
        int sy = (this.mc.displayHeight - (y + height) * scale);
        int sw = width * scale;
        int sh = height * scale;
        if (sw < 0) sw = 0;
        if (sh < 0) sh = 0;
        GL11.glScissor(sx, sy, sw, sh);
    }

    private void drawStringFontScaled(String text, float x, float y, int color, float baseSize, float scale) {
        Font font = getYaHeiFont(baseSize);
        if (font != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0.0f);
            GlStateManager.scale(scale, scale, 1.0f);
            float yFix = 0.0f;
            CustomFontRenderer.drawString(text, 0.0f, yFix, color, font);
            GlStateManager.popMatrix();
        } else {
            this.fontRendererObj.drawString(text, (int) x, (int) y, color, false);
        }
    }

    private int getStringWidthFontScaled(String text, float baseSize, float scale) {
        Font font = getYaHeiFont(baseSize);
        if (font != null) {
            return (int) Math.ceil(CustomFontRenderer.getStringWidth(text, font) * scale);
        }
        return this.fontRendererObj.getStringWidth(text);
    }

    private int getFontHeightFontScaled(float baseSize, float scale) {
        Font font = getYaHeiFont(baseSize);
        if (font != null) {
            return (int) Math.ceil(CustomFontRenderer.getFontHeight(font) * scale);
        }
        return this.fontRendererObj.FONT_HEIGHT;
    }

    private List<String> wrapTextScaled(String text, int maxWidth, float baseSize, float scale) {
        Font font = getYaHeiFont(baseSize);
        if (font == null) {
            return this.fontRendererObj.listFormattedStringToWidth(text, maxWidth);
        }
        int scaledMaxWidth = (int) Math.max(1, Math.floor(maxWidth / scale));
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        int lineW = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                out.add(line.toString());
                line.setLength(0);
                lineW = 0;
                continue;
            }
            String s = String.valueOf(c);
            int cw = CustomFontRenderer.getStringWidth(s, font);
            if (lineW + cw > scaledMaxWidth && line.length() > 0) {
                out.add(line.toString());
                line.setLength(0);
                lineW = 0;
            }
            line.append(c);
            lineW += cw;
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out;
    }

    private static Font getYaHeiFont(float size) {
        try {
            FontTransformer ft = FontTransformer.getInstance();
            Font f = ft.getFont("MicrosoftYaHei", size);
            if (f == null) {
                f = ft.getFont("MicrosoftYaHei Bold", size);
            }
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    private static float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        if (v > 1.0f) return 1.0f;
        return v;
    }

    private static float easeInOut(float t) {
        t = clamp01(t);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float easeOut(float t) {
        t = clamp01(t);
        float inv = 1.0f - t;
        return 1.0f - inv * inv;
    }

    private static float seg(float t, float a, float b) {
        if (b <= a) return 0.0f;
        return clamp01((t - a) / (b - a));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp01(t);
    }

    private static int rgba(int r, int g, int b, int a) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

    private static void applyHighQualityTextureSampling() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }
}
