package epilogue.ui.clickgui.menu;

import epilogue.Epilogue;
import epilogue.module.ModuleCategory;
import epilogue.ui.clickgui.menu.panel.CategoryPanel;
import epilogue.util.render.ColorUtil;
import epilogue.util.render.RenderUtil;
import epilogue.util.render.PostProcessing;
import epilogue.util.render.animations.advanced.Animation;
import epilogue.util.render.animations.advanced.Direction;
import epilogue.util.render.animations.advanced.impl.DecelerateAnimation;
import epilogue.util.shader.BloomShader;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MenuClickGui extends GuiScreen {

    private final List<CategoryPanel> categoryPanels = new ArrayList<>();
    public final Animation openAnimation = new DecelerateAnimation(250, 1);
    private boolean firstOpen = true;
    public int x;
    public int y;
    public int w = 360;
    public final int h = 380;
    private int dragX;
    private int dragY;
    private boolean dragging;

    private boolean embeddedInDynamicIsland = false;
    private int clipX;
    private int clipY;
    private int clipW;
    private int clipH;
    private boolean useExternalClip = false;

    private long textFadeStartMs = -1L;
    private float textAlpha = 0.0f;

    public float getTextAlpha() {
        return textAlpha;
    }

    public void setEmbeddedInDynamicIsland(boolean embeddedInDynamicIsland) {
        this.embeddedInDynamicIsland = embeddedInDynamicIsland;
        this.useExternalClip = false;
    }

    public boolean isEmbeddedInDynamicIsland() {
        return embeddedInDynamicIsland;
    }

    public void setExternalClip(int x, int y, int w, int h) {
        this.clipX = x;
        this.clipY = y;
        this.clipW = w;
        this.clipH = h;
        this.useExternalClip = true;
    }

    public Color backgroundColor;
    public Color backgroundColor2;
    public Color backgroundColor3;
    public Color smallbackgroundColor;
    public Color smallbackgroundColor2;
    public Color linecolor;
    public Color versionColor;
    public Color fontcolor;

    private final Animation hoverAnimation = new DecelerateAnimation(1000, 1);

    public MenuClickGui() {

        for (ModuleCategory category : ModuleCategory.values()) {
            CategoryPanel panel = new CategoryPanel(category, this);
            if (category == ModuleCategory.COMBAT) {
                panel.setSelected(true);
            }
            categoryPanels.add(panel);
        }

        backgroundColor = new Color(10, 10, 10, 255);
        backgroundColor2 = new Color(0, 0, 0, 255);
        backgroundColor3 = new Color(0, 0, 0, 255);
        smallbackgroundColor = new Color(18, 18, 18, 255);
        smallbackgroundColor2 = new Color(28, 28, 28, 255);
        linecolor = new Color(30, 30, 30, 255);
        versionColor = new Color(255, 255, 255, 80);
        fontcolor = new Color(255, 255, 255, 255);

        x = 260;
        y = 50;
    }

    public void selectCategory(ModuleCategory category) {
        for (CategoryPanel p : categoryPanels) {
            p.setSelected(p.getCategory() == category);
        }
    }

    @Override
    public void initGui() {
        if (embeddedInDynamicIsland) {
            ScaledResolution sr = new ScaledResolution(mc);
            this.x = (sr.getScaledWidth() - this.w) / 2;
            this.y = 16;
        }

        textFadeStartMs = -1L;
        textAlpha = 0.0f;

        for (CategoryPanel panel : categoryPanels) {
            panel.initGui();
        }
        if (firstOpen) {
            firstOpen = false;
            openAnimation.setDirection(Direction.FORWARDS);
            openAnimation.reset();
        } else {
            openAnimation.setDirection(Direction.FORWARDS);
        }
    }

    @Override
    public void onGuiClosed() {
        openAnimation.setDirection(Direction.BACKWARDS);
        textFadeStartMs = -1L;
        textAlpha = 0.0f;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        if (!embeddedInDynamicIsland && dragging) {
            x = mouseX + dragX;
            y = mouseY + dragY;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        double anim = openAnimation.getOutput();
        float fade = (float) anim;

        boolean expanded = fade >= 0.999f;
        if (!expanded) {
            textFadeStartMs = -1L;
            textAlpha = 0.0f;
        } else {
            if (textFadeStartMs < 0L) {
                textFadeStartMs = System.currentTimeMillis();
                textAlpha = 0.0f;
            } else {
                float durationMs = 160.0f;
                float t = (System.currentTimeMillis() - textFadeStartMs) / durationMs;
                if (t < 0.0f) t = 0.0f;
                if (t > 1.0f) t = 1.0f;
                textAlpha = t;
            }
        }

        boolean drawText = textAlpha > 0.0f;
        float sw = sr.getScaledWidth();
        float sh = sr.getScaledHeight();
        if (!embeddedInDynamicIsland && fade > 0) {
            epilogue.module.modules.render.PostProcessing.getBlurStrength();
            PostProcessing.drawBlur(0, 0, sw, sh, () -> () -> net.minecraft.client.gui.Gui.drawRect(0, 0, (int) sw, (int) sh, -1));

            int alpha = (int) (120 * fade);
            RenderUtil.drawRect(0, 0, sw, sh, new Color(0, 0, 0, alpha).getRGB());
        }

        if (!embeddedInDynamicIsland) {
            RenderUtil.scissorStart(x, y + 35, w, h - 35);
            RenderUtil.drawRect(x, y + 35, w, h - 35, backgroundColor2.getRGB());
            RenderUtil.scissorEnd();
        }

        if (useExternalClip) {
            RenderUtil.scissorStart(clipX, clipY, clipW, clipH);
        }

        if (!embeddedInDynamicIsland) {
            String name = Epilogue.clientName == null ? "Epilogue" : Epilogue.clientName.trim();
            if (name.isEmpty()) name = "Epilogue";

            String firstLetter = name.substring(0, 1);
            String remainingText = name.substring(1);
            int firstLetterWidth = Fonts.width(Fonts.heading(), firstLetter);

            if (drawText) {
                Fonts.draw(Fonts.heading(), firstLetter, x + 10, y + 10, ColorUtil.applyOpacity(fontcolor.getRGB(), textAlpha));
                Fonts.draw(Fonts.heading(), remainingText, x + 10 + firstLetterWidth, y + 10, ColorUtil.applyOpacity(fontcolor.getRGB(), textAlpha));
            }

            String ver = Epilogue.clientVersion == null ? "" : Epilogue.clientVersion;
            if (drawText) {
                Fonts.draw(Fonts.small(), ver, x + w - 32, y + 16, ColorUtil.applyOpacity(versionColor.getRGB(), textAlpha));
            }
        }

        int fps = net.minecraft.client.Minecraft.getDebugFPS();
        int ping = 0;
        try {
            if (mc.getNetHandler() != null && mc.thePlayer != null) {
                net.minecraft.client.network.NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
                if (info != null) {
                    ping = info.getResponseTime();
                }
            }
        } catch (Exception ignored) {
        }

        String infoText = "FPS " + fps + "  Ping " + ping + "  UserName: " + mc.getSession().getUsername() + "  HP: " + mc.thePlayer.getHealth();
        float infoX = x + 10;
        float infoY = y + 44;
        if (!embeddedInDynamicIsland) {
            RenderUtil.drawRect(infoX, infoY, 182, 14, smallbackgroundColor.getRGB());
        }
        if (drawText) {
            Fonts.draw(Fonts.tiny(), infoText, infoX + 6, infoY + 5, ColorUtil.applyOpacity(fontcolor.getRGB(), textAlpha));
        }

        Color rectColor = smallbackgroundColor2;
        rectColor = ColorUtil.interpolateColorC(rectColor, ColorUtil.brighter(rectColor, 0.6f), (float) hoverAnimation.getOutput());
        boolean hovered = false;

        for (CategoryPanel categoryPanel : categoryPanels) {
            if (isHover(categoryPanel.getIconX(), categoryPanel.getIconY(), 15, 15, mouseX, mouseY)) {
                hovered = true;
                break;
            }
        }

        for (CategoryPanel categoryPanel : categoryPanels) {
            hoverAnimation.setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);
            categoryPanel.drawScreen(mouseX, mouseY, partialTicks);
            if (categoryPanel.isSelected()) {
                if (!embeddedInDynamicIsland) {
                    RenderUtil.drawRect(categoryPanel.getIconX(), categoryPanel.getIconY(), 15, 15, rectColor.brighter().getRGB());
                }
                if (drawText) {
                    Fonts.draw(Fonts.icon(), categoryPanel.getIconChar(), categoryPanel.getIconTextX(), categoryPanel.getIconTextY(), ColorUtil.applyOpacity(fontcolor.getRGB(), textAlpha));
                }
            } else {
                if (drawText) {
                    Fonts.draw(Fonts.icon(), categoryPanel.getIconChar(), categoryPanel.getIconTextX(), categoryPanel.getIconTextY(), ColorUtil.applyOpacity(versionColor.getRGB(), textAlpha));
                }
            }
        }

        if (useExternalClip) {
            RenderUtil.scissorEnd();
        }

        if (!embeddedInDynamicIsland) {
            Framebuffer bloomBuffer = PostProcessing.beginBloom();
            if (bloomBuffer != null) {
                bloomBuffer.bindFramebuffer(false);
                GlStateManager.disableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                RenderUtil.drawRect(x, y, w, h, new Color(0, 0, 0, 255).getRGB());
                GlStateManager.disableBlend();
                GlStateManager.enableTexture2D();

                mc.getFramebuffer().bindFramebuffer(false);
                BloomShader.renderBloom(bloomBuffer.framebufferTexture, 2, 1);
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!embeddedInDynamicIsland && isHover(x, y, 100, 35, mouseX, mouseY)) {
            dragging = true;
            dragX = x - mouseX;
            dragY = y - mouseY;
        }

        if (mouseButton == 0) {
            for (CategoryPanel panel : categoryPanels) {
                if (panel.handleCategoryClick(mouseX, mouseY)) {
                    break;
                }
            }
        }

        CategoryPanel selected = getSelected();
        if (selected != null) {
            selected.mouseClicked(mouseX, mouseY, mouseButton);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        CategoryPanel selected = getSelected();
        if (selected != null) {
            selected.keyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            dragging = false;
        }
        CategoryPanel selected = getSelected();
        if (selected != null) {
            selected.mouseReleased(mouseX, mouseY, state);
        }
    }

    public CategoryPanel getSelected() {
        for (CategoryPanel p : categoryPanels) {
            if (p.isSelected()) return p;
        }
        return null;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean isHover(float x, float y, float w, float h, float mx, float my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}