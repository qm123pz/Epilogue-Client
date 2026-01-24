package epilogue.ui.clickgui.exhibition;

import epilogue.ui.clickgui.exhibition.bridge.Client;
import epilogue.ui.clickgui.exhibition.bridge.Module;
import epilogue.ui.clickgui.exhibition.bridge.ModuleData;
import epilogue.ui.clickgui.exhibition.components.*;
import epilogue.ui.clickgui.exhibition.management.ColorManager;
import epilogue.ui.clickgui.exhibition.management.ColorObject;
import epilogue.ui.clickgui.exhibition.management.GlobalValues;
import epilogue.ui.clickgui.exhibition.util.Opacity;
import epilogue.ui.clickgui.exhibition.util.Translate;
import epilogue.ui.clickgui.menu.Fonts;
import epilogue.ui.clickgui.exhibition.util.RenderingUtil;
import epilogue.ui.clickgui.exhibition.util.render.Colors;
import epilogue.ui.clickgui.exhibition.util.render.Depth;
import epilogue.util.KeyBindUtil;
import epilogue.config.Config;
import epilogue.Epilogue;
import epilogue.ui.clickgui.exhibition.values.MultiBool;
import epilogue.ui.clickgui.exhibition.values.Options;
import epilogue.ui.clickgui.exhibition.values.Setting;
import epilogue.value.Value;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ColorValue;
import epilogue.value.values.ModeValue;
import epilogue.value.values.PercentValue;
import epilogue.value.values.TextValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.File;
import java.awt.Desktop;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SkeetMenu extends UI {

    private final boolean allowMinigames = Boolean.parseBoolean(System.getProperty("NEoBuMASs"));

    public static Opacity opacity = new Opacity(0);
    private final Minecraft mc = Minecraft.getMinecraft();

    private void applyScrollDelta(CategoryPanel categoryPanel, float delta) {
        if (categoryPanel == null || delta == 0) return;
        for (GroupBox g : categoryPanel.groupBoxes) g.y += delta;
        for (Button b : categoryPanel.buttons) b.y += delta;
        for (Checkbox c : categoryPanel.checkboxes) c.y += delta;
        for (Slider s : categoryPanel.sliders) s.y += delta;
        for (DropdownBox d : categoryPanel.dropdownBoxes) {
            d.y += delta;
            if (d.buttons != null) {
                for (DropdownButton btn : d.buttons) {
                    btn.y += delta;
                }
            }
        }
        for (MultiDropdownBox d : categoryPanel.multiDropdownBoxes) {
            d.y += delta;
            if (d.buttons != null) {
                for (MultiDropdownButton btn : d.buttons) {
                    btn.y += delta;
                }
            }
        }
        for (ColorPreview c : categoryPanel.colorPreviews) {
            c.y += delta;
            if (c.sliders != null) {
                for (HSVColorPicker s : c.sliders) {
                    s.y += delta;
                }
            }
        }
        for (TextBox t : categoryPanel.textBoxes) t.y += delta;
        if (categoryPanel.configTextBox != null) categoryPanel.configTextBox.y += delta;
        if (categoryPanel.configList != null) categoryPanel.configList.y += delta;

        if (categoryPanel.contentMaxY != 0) categoryPanel.contentMaxY += delta;
    }

    private void updateScrollBounds(CategoryPanel categoryPanel) {
        if (categoryPanel == null) return;
        float clipH = 340 - 18;
        float content = categoryPanel.contentMaxY;
        if (content <= 0) {
            categoryPanel.minScroll = 0;
            categoryPanel.maxScroll = 0;
            return;
        }
        float max = Math.max(0, content - clipH);
        categoryPanel.minScroll = -max;
        categoryPanel.maxScroll = 0;
        if (categoryPanel.scroll < categoryPanel.minScroll) {
            float delta = categoryPanel.minScroll - categoryPanel.scroll;
            categoryPanel.scroll = categoryPanel.minScroll;
            applyScrollDelta(categoryPanel, delta);
        }
        if (categoryPanel.scroll > categoryPanel.maxScroll) {
            float delta = categoryPanel.maxScroll - categoryPanel.scroll;
            categoryPanel.scroll = categoryPanel.maxScroll;
            applyScrollDelta(categoryPanel, delta);
        }
    }

    @Override
    public void mainConstructor(ClickGui p0) {
    }

    private ResourceLocation tex = new ResourceLocation("epilogue/texture/skeetui/tex.png");
    private ResourceLocation texture = new ResourceLocation("epilogue/texture/skeetui/skeetchainmail.png");
    private ResourceLocation cursor = new ResourceLocation("epilogue/texture/skeetui/cursor.png");

    private Translate bar = new Translate(0, 0);

    private final Map<ColorPreview, ColorValue> colorValueByPreview = new IdentityHashMap<>();
    private final Map<ColorPreview, Class<?>> colorModuleClassByPreview = new IdentityHashMap<>();
    private final Map<ColorPreview, String> colorValueNameByPreview = new IdentityHashMap<>();

    private int lastColorsCount;

    @Override
    public void mainPanelDraw(epilogue.ui.clickgui.exhibition.components.MainPanel panel, int p0, int p1) {
        mc.mcProfiler.startSection("background");

        opacity.interp(panel.isOpen ? 255 : 0, 25);

        RenderingUtil.rectangleBordered(panel.x + panel.dragX - 0.3, panel.y + panel.dragY - 0.3, panel.x + 340 + panel.dragX + 0.5, panel.y + 340 + panel.dragY + 0.3, 0.5, Colors.getColor(0, 0), Colors.getColor(10, (int) opacity.getOpacity()));
        RenderingUtil.rectangleBordered(panel.x + panel.dragX, panel.y + panel.dragY, panel.x + 340 + panel.dragX, panel.y + 340 + panel.dragY, 0.5, Colors.getColor(0, 0), Colors.getColor(60, (int) opacity.getOpacity()));
        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2, panel.y + panel.dragY + 2, panel.x + 340 + panel.dragX - 2, panel.y + 340 + panel.dragY - 2, 0.5, Colors.getColor(0, 0), Colors.getColor(60, (int) opacity.getOpacity()));
        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 0.6, panel.y + panel.dragY + 0.6, panel.x + 340 + panel.dragX - 0.5, panel.y + 340 + panel.dragY - 0.6, 1.3, Colors.getColor(0, 0), Colors.getColor(40, (int) opacity.getOpacity()));

        Depth.pre();
        Depth.mask();

        mc.mcProfiler.startSection("categories");
        float y = 15;
        for (int i = 0; i <= panel.typeButton.size(); i++) {
            if (i <= panel.typeButton.size() - 1 && panel.typeButton.get(i).categoryPanel.visible && i > 0) {
                y = 15 + ((i) * 40);
            }
        }
        mc.mcProfiler.endSection();
        bar.interpolate(0, y, 0.6F);
        y = bar.getY();

        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + 4F, panel.x + panel.dragX + 40, panel.y + panel.dragY + y + 1, -1);
        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + y + 40, panel.x + panel.dragX + 40, panel.y + panel.dragY + 307.5 + 30, -1);

        Depth.render(GL11.GL_LESS);

        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2.5, panel.y + panel.dragY + 2.5, panel.x + 340 + panel.dragX - 2.5, panel.y + 340 + panel.dragY - 2.5, 0.5, Colors.getColor(22, (int) opacity.getOpacity()), Colors.getColor(22, (int) opacity.getOpacity()));

        Depth.post();

        RenderingUtil.drawGradientSideways(panel.x + panel.dragX + 3, panel.y + panel.dragY + 3, panel.x + 178 + panel.dragX - 3, panel.dragY + panel.y + 4, Colors.getColor(55, 177, 218, (int) opacity.getOpacity()), Colors.getColor(204, 77, 198, (int) opacity.getOpacity()));
        RenderingUtil.drawGradientSideways(panel.x + panel.dragX + 175, panel.y + panel.dragY + 3, panel.x + 340 + panel.dragX - 3, panel.dragY + panel.y + 4, Colors.getColor(204, 77, 198, (int) opacity.getOpacity()), Colors.getColor(204, 227, 53, (int) opacity.getOpacity()));

        int i11 = (int) opacity.getOpacity() - 145;
        if (i11 < 0) {
            i11 = 0;
        }
        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + 3.3, panel.x + 340 + panel.dragX - 3, panel.dragY + panel.y + 4, Colors.getColor(0, i11));

        GlStateManager.pushMatrix();
        Depth.pre();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        Depth.mask();

        mc.mcProfiler.startSection("chainmail");
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.translate(panel.x + panel.dragX + 40, panel.dragY + panel.y + 3f, 0);
        drawIcon(0, 0, 0, .5F, 340 - 3 - 40, 310 - 6 + 30, 812 / 2F, 688 / 2F);
        drawIcon(-40 + 2.5, y - 3, .5F, .5F + y, 40 - 2.5F, 40, 812 / 2F, 688 / 2F);

        Depth.render(GL11.GL_EQUAL);

        mc.getTextureManager().bindTexture(tex);
        drawIcon(-40, 1, 0, 0, 340, 310 - 7 + 30, 812 / 2F, 688 / 2F);
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();

        Depth.post();
        GlStateManager.popMatrix();
        mc.mcProfiler.endSection();

        GlStateManager.pushMatrix();
        Depth.pre();
        Depth.mask();
        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + 4F, panel.x + panel.dragX + 40, panel.y + panel.dragY + y + 1, -1);
        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + y + 40, panel.x + panel.dragX + 40, panel.y + panel.dragY + 307.5 + 30, -1);

        Depth.render();
        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2, panel.y + panel.dragY + 3, panel.x + panel.dragX + 39.5, panel.y + panel.dragY + y - 0.5, 0.5, Colors.getColor(0, 0), Colors.getColor(0, (int) opacity.getOpacity()));
        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2, panel.y + panel.dragY + 3, panel.x + panel.dragX + 40, panel.y + panel.dragY + y, 0.5, Colors.getColor(0, 0), Colors.getColor(48, (int) opacity.getOpacity()));

        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + 4, panel.x + panel.dragX + 39, panel.y + panel.dragY + y - 1, Colors.getColor(12, (int) opacity.getOpacity()));

        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2, panel.y + panel.dragY + y + 40.5, panel.x + panel.dragX + 39.5, panel.y + panel.dragY + 308 + 30, 0.5, Colors.getColor(0, 0), Colors.getColor(0, (int) opacity.getOpacity()));
        RenderingUtil.rectangleBordered(panel.x + panel.dragX + 2, panel.y + panel.dragY + y + 40, panel.x + panel.dragX + 40, panel.y + panel.dragY + 308 + 30, 0.5, Colors.getColor(0, 0), Colors.getColor(48, (int) opacity.getOpacity()));

        RenderingUtil.rectangle(panel.x + panel.dragX + 3, panel.y + panel.dragY + y + 41, panel.x + panel.dragX + 39, panel.y + panel.dragY + 307.5 + 30, Colors.getColor(12, (int) opacity.getOpacity()));
        Depth.post();
        GlStateManager.popMatrix();

        if (opacity.getOpacity() != 0) {
            mc.mcProfiler.startSection("SLButton");
            for (SLButton button : panel.slButtons) {
                button.draw(p0, p1);
            }
            mc.mcProfiler.endStartSection("CategoryButton");
            for (CategoryButton button : panel.typeButton) {
                button.draw(p0, p1);
            }
            mc.mcProfiler.endSection();
            ScaledResolution rs = new ScaledResolution(mc);
            double twoDscale = (rs.getScaleFactor() / Math.pow(rs.getScaleFactor(), 2.0D)) * 2;
            if (panel.dragging) {
                panel.dragX = p0 - panel.lastDragX;
                panel.dragY = p1 - panel.lastDragY;
            }
            double xBorder = (rs.getScaledWidth() / twoDscale - 392);
            if (panel.dragX > xBorder) {
                panel.dragX = (float) xBorder;
            }
            if (panel.dragX < 2 - 50) {
                panel.dragX = 2 - 50;
            }
            double yBorder = (rs.getScaledHeight() / twoDscale - 392);

            if (panel.dragY > yBorder) {
                panel.dragY = (float) yBorder;
            }
            if (panel.dragY < 2 - 50) {
                panel.dragY = 2 - 50;
            }
        }

        if (panel.isOpen && !GlobalValues.showCursor.getValue() && Mouse.isGrabbed()) {
            GlStateManager.pushMatrix();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            mc.getTextureManager().bindTexture(cursor);
            GlStateManager.translate(p0, p1, 0);
            ColorObject c = ColorManager.hudColor;
            RenderingUtil.glColor(Colors.getColor(c.getRed(), c.getGreen(), c.getBlue(), (int) (255 * opacity.getScale())));
            GlStateManager.scale(0.5, 0.5, 0.5);
            drawIcon(0, 0, 0, 0, 12, 19, 12, 19);
            GL11.glColor4d(1, 1, 1, 1);
            GlStateManager.disableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.popMatrix();
        }
        mc.mcProfiler.endSection();
    }

    private void drawIcon(double x, double y, float u, float v, double width, double height, float textureWidth, float textureHeight) {
        float f = 1.0F / textureWidth;
        float f1 = 1.0F / textureHeight;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos((double) x, (double) (y + height), 0.0D).tex((double) (u * f), (double) ((v + (float) height) * f1)).endVertex();
        worldrenderer.pos((double) (x + width), (double) (y + height), 0.0D).tex((double) ((u + (float) width) * f), (double) ((v + (float) height) * f1)).endVertex();
        worldrenderer.pos((double) (x + width), (double) y, 0.0D).tex((double) ((u + (float) width) * f), (double) (v * f1)).endVertex();
        worldrenderer.pos((double) x, (double) y, 0.0D).tex((double) (u * f), (double) (v * f1)).endVertex();
        tessellator.draw();
    }

    @Override
    public void mainPanelKeyPress(epilogue.ui.clickgui.exhibition.components.MainPanel panel, int key) {
        boolean bad = false;
        for (CategoryButton button : Client.getClickGui().mainPanel.typeButton) {
            for (TextBox textBox : button.categoryPanel.textBoxes) {
                if (textBox.isTyping || textBox.isFocused)
                    bad = true;
            }
            if (button.categoryPanel.configTextBox != null) {
                ConfigTextBox textBox = button.categoryPanel.configTextBox;
                if (button.categoryPanel.visible) {
                    if (textBox.isTyping || textBox.isFocused)
                        bad = true;
                }
            }
        }
        if (opacity.getOpacity() < 10)
            return;
        if (key == 1) {
            for (CategoryButton buttonb : panel.typeButton) {
                for (Button button : buttonb.categoryPanel.buttons) {
                    if (button.isBinding) {
                        bad = true;
                    }
                }
            }
        }

        if (!bad && ((key == Keyboard.KEY_ESCAPE) || key == Keyboard.KEY_INSERT || key == Keyboard.KEY_DELETE || key == Keyboard.KEY_RSHIFT)) {
            try {
                panel.typeButton.forEach(o -> o.categoryPanel.multiDropdownBoxes.forEach(b -> b.active = false));
                panel.typeButton.forEach(o -> o.categoryPanel.dropdownBoxes.forEach(b -> b.active = false));
                panel.typeButton.forEach(o -> o.categoryPanel.buttons.forEach(b -> b.isBinding = false));
                panel.typeButton.forEach(o -> o.categoryPanel.textBoxes.forEach(b -> b.isTyping = false));
                panel.typeButton.forEach(o -> o.categoryPanel.textBoxes.forEach(b -> b.isFocused = false));

                if (mc.currentScreen instanceof ClickGui)
                    mc.displayGuiScreen(null);
            } catch (Exception e) {

            }
        }
        panel.typeButton.forEach(o -> o.categoryPanel.buttons.forEach(b -> b.keyPressed(key)));
        panel.typeButton.forEach(o -> o.categoryPanel.textBoxes.forEach(t -> t.keyPressed(key)));
        for (CategoryButton button : Client.getClickGui().mainPanel.typeButton) {
            if (button.categoryPanel.configTextBox != null) {
                ConfigTextBox textBox = button.categoryPanel.configTextBox;
                if (button.categoryPanel.visible) {
                    textBox.keyPressed(key);
                }
            }
        }
    }

    @Override
    public void panelConstructor(epilogue.ui.clickgui.exhibition.components.MainPanel mainPanel, float x, float y) {
        int y1 = 15;
        for (ModuleData.Type types : ModuleData.Type.values()) {
            mainPanel.typeButton.add(new CategoryButton(mainPanel, types.name(), x + 3, y + y1));
            y += 40;
        }
        if (allowMinigames) {
            mainPanel.typeButton.add(new CategoryButton(mainPanel, ModuleData.Type.Minigames.name(), x + 3, y + y1));
            y += 40;
        }
        mainPanel.typeButton.add(new CategoryButton(mainPanel, "Colors", x + 3, y + y1));
        y += 40;
        mainPanel.typeButton.add(new CategoryButton(mainPanel, "Settings", x + 3, y + y1));
        mainPanel.typeButton.get(0).enabled = true;
        mainPanel.typeButton.get(0).categoryPanel.visible = true;
    }

    @Override
    public void panelMouseClicked(epilogue.ui.clickgui.exhibition.components.MainPanel mainPanel, int x, int y, int z) {
        if (opacity.getOpacity() < 220)
            return;
        if (x >= mainPanel.x + mainPanel.dragX && y >= mainPanel.dragY + mainPanel.y && x <= mainPanel.dragX + mainPanel.x + 400 && y <= mainPanel.dragY + mainPanel.y + 12.0f && z == 0) {
            mainPanel.dragging = true;
            mainPanel.lastDragX = x - mainPanel.dragX;
            mainPanel.lastDragY = y - mainPanel.dragY;
        }
        mainPanel.typeButton.forEach(c -> {
            c.mouseClicked(x, y, z);
            c.categoryPanel.mouseClicked(x, y, z);
        });
        mainPanel.slButtons.forEach(slButton -> slButton.mouseClicked(x, y, z));
    }

    @Override
    public void panelMouseMovedOrUp(epilogue.ui.clickgui.exhibition.components.MainPanel mainPanel, int x, int y, int z) {
        if (opacity.getOpacity() < 220)
            return;
        if (z == 0) {
            mainPanel.dragging = false;
        }
        for (CategoryButton button : mainPanel.typeButton) {
            button.mouseReleased(x, y, z);
        }

        mainPanel.typeButton.forEach(c -> {
            if (c.categoryPanel != null) {
                c.categoryPanel.mouseReleased(x, y, z);
            }
        });
    }

    @Override
    public void categoryButtonConstructor(CategoryButton p0, epilogue.ui.clickgui.exhibition.components.MainPanel p1) {
        p0.categoryPanel = new CategoryPanel(p0.name, p0, 0, 0);
    }

    @Override
    public void categoryButtonMouseClicked(CategoryButton p0, epilogue.ui.clickgui.exhibition.components.MainPanel p1, int p2, int p3, int p4) {
        if (p2 >= p0.x + p1.dragX && p3 >= p1.dragY + p0.y && p2 <= p1.dragX + p0.x + 40 && p3 <= p1.dragY + p0.y + 40 && p4 == 0) {
            for (CategoryButton button : p1.typeButton) {
                if (button == p0) {
                    p0.enabled = true;
                    p0.categoryPanel.visible = true;
                } else {
                    button.enabled = false;
                    button.categoryPanel.visible = false;
                }
            }
        }
    }

    @Override
    public void categoryButtonDraw(CategoryButton p0, float p2, float p3) {
        int brightness = p0.enabled ? 210 : 91;
        boolean hovering = p2 >= p0.x + p0.panel.dragX && p3 >= p0.panel.dragY + p0.y && p2 <= p0.panel.dragX + p0.x + 40 && p3 < p0.panel.dragY + p0.y + 40;
        if (hovering && !p0.enabled) {
            brightness = 165;
        }

        if (hovering) {
            Fonts.drawWithShadow(Fonts.tiny(), p0.name, (p0.panel.x + 2 + p0.panel.dragX) + 55, (p0.panel.y + 9 + p0.panel.dragY), Colors.getColor(205, (int) opacity.getOpacity()));
        }

        p0.fade.interp(brightness, 10);
        int color = Colors.getColor((int) p0.fade.getOpacity(), (int) opacity.getOpacity());

        if (p0.enabled) {
            RenderingUtil.rectangle(p0.x + 3 + p0.panel.dragX, p0.y + p0.panel.dragY + 1, p0.x + 4 + p0.panel.dragX, p0.y + 39 + p0.panel.dragY, Colors.getColor(255, (int) opacity.getOpacity()));
        }

        switch (p0.name) {
            case "Other":
                Fonts.draw(Fonts.skeetIcon(), "I", (p0.x + 19 + p0.panel.dragX) - (Fonts.width(Fonts.skeetIcon(), "I") / 2f), (p0.y + 21 + p0.panel.dragY) - (Fonts.height(Fonts.skeetIcon()) / 2f), color);
                break;
            case "Combat":
                Fonts.draw(Fonts.skeetIcon(), "E", (p0.x + 19 + p0.panel.dragX) - (Fonts.width(Fonts.skeetIcon(), "E") / 2f), (p0.y + 21 + p0.panel.dragY) - (Fonts.height(Fonts.skeetIcon()) / 2f), color);
                break;
            case "Player":
                Fonts.draw(Fonts.skeetIcon(), "F", (p0.x + 18 + p0.panel.dragX) - (Fonts.width(Fonts.skeetIcon(), "F") / 2f), (p0.y + 21 + p0.panel.dragY) - (Fonts.height(Fonts.skeetIcon()) / 2f), color);
                break;
            case "Movement":
                Fonts.draw(Fonts.skeetIcon(), "J", (p0.x + 19 + p0.panel.dragX) - (Fonts.width(Fonts.skeetIcon(), "J") / 2f), (p0.y + 23 + p0.panel.dragY) - (Fonts.height(Fonts.skeetIcon()) / 2f), color);
                break;
            case "Visuals":
                Fonts.draw(Fonts.skeetIcon(), "C", (p0.x + 18 + p0.panel.dragX) - (Fonts.width(Fonts.skeetIcon(), "C") / 2f), (p0.y + 21 + p0.panel.dragY) - (Fonts.height(Fonts.skeetIcon()) / 2f), color);
                break;
            case "Colors":
                Fonts.draw(Fonts.skeetIcon(), "H", (p0.x + 18.5F + p0.panel.dragX) - (Fonts.width(Fonts.skeetIcon(), "H") / 2f), (p0.y + 21 + p0.panel.dragY) - (Fonts.height(Fonts.skeetIcon()) / 2f), color);
                break;
            case "Minigames":
                Fonts.draw(Fonts.skeetIcon(), "A", (p0.x + 20F + p0.panel.dragX) - (Fonts.width(Fonts.skeetIcon(), "A") / 2f), (p0.y + 21 + p0.panel.dragY) - (Fonts.height(Fonts.skeetIcon()) / 2f), color);
                break;
            case "Settings":
                Fonts.draw(Fonts.tiny(), "[+]", (p0.x + 18.5F + p0.panel.dragX) - (Fonts.width(Fonts.tiny(), "[+]") / 2f), (p0.y + 21 + p0.panel.dragY) - (Fonts.height(Fonts.tiny()) / 2f), color);
                break;
            default:
                Fonts.drawWithShadow(Fonts.tiny(), p0.name.substring(0, 1), (p0.x + 12 + p0.panel.dragX), (p0.y + 13 + p0.panel.dragY), color);
                break;
        }

        if (p0.enabled) {
            p0.categoryPanel.draw(p2, p3);
        }
    }

    private List<Setting> getSettings(Module mod) {
        List<Setting> out = new ArrayList<>();
        if (mod == null) return out;
        for (Value<?> v : mod.getSettings().values()) {
            if (v == null || !v.isVisible()) continue;

            if (v instanceof BooleanValue) {
                out.add(new Setting<>(v.getName(), ((BooleanValue) v).getValue(), v));
            } else if (v instanceof TextValue) {
                out.add(new Setting<>(v.getName(), ((TextValue) v).getValue(), v));
            } else if (v instanceof IntValue) {
                out.add(new Setting<>(v.getName(), ((IntValue) v).getValue(), v));
            } else if (v instanceof FloatValue) {
                out.add(new Setting<>(v.getName(), ((FloatValue) v).getValue(), v));
            } else if (v instanceof PercentValue) {
                out.add(new Setting<>(v.getName(), ((PercentValue) v).getValue(), v));
            } else if (v instanceof ModeValue) {
                String[] modes = ((ModeValue) v).getModes();
                Options opt = modes == null ? new Options() : new Options(modes);
                String selected = ((ModeValue) v).getModeString();
                if (selected != null) opt.setSelected(selected);
                out.add(new Setting<>(v.getName(), opt, v));
            }
        }
        if (out.isEmpty()) return null;
        return out;
    }

    @Override
    public void categoryPanelConstructor(CategoryPanel categoryPanel, CategoryButton categoryButton, float x, float y) {
        float xOff = 50 + categoryButton.panel.x;
        float yOff = 15 + categoryButton.panel.y;

        if (categoryButton.name.equalsIgnoreCase("Settings")) {
            categoryPanel.configTextBox = new ConfigTextBox(xOff + 0.5f, yOff + 20, categoryPanel);
            categoryPanel.configList = new ConfigList(xOff + 0.5f, yOff + 35, categoryPanel);
            refreshConfigs(categoryPanel.configList);
            categoryPanel.contentMaxY = yOff + 110;
            return;
        }

        if (categoryButton.name.equalsIgnoreCase("Colors")) {
            categoryPanel.colorPreviews.clear();
            colorValueByPreview.clear();
            colorModuleClassByPreview.clear();
            colorValueNameByPreview.clear();

            float clipX = categoryButton.panel.x + 40;
            float clipW = 340 - 40 - 4;
            float leftPad = 6f;
            float gridStartX = clipX + leftPad;
            float colGap = 8f;
            float cardW = (clipW - leftPad * 2 - colGap * 2) / 3f;

            float baseY = 32;
            float col0X = (gridStartX) + 85.5f;
            float col1X = (gridStartX + cardW + colGap) + 85.5f;
            float col2X = (gridStartX + (cardW + colGap) * 2) + 85.5f;

            java.util.List<epilogue.module.Module> backingModules = new LinkedList<>();
            if (Epilogue.moduleManager != null && Epilogue.moduleManager.modules != null) {
                backingModules.addAll(Epilogue.moduleManager.modules.values());
            }
            backingModules.removeIf(m -> m == null);
            backingModules.sort(java.util.Comparator.comparing(epilogue.module.Module::getName, String.CASE_INSENSITIVE_ORDER));

            int idx = 0;
            for (epilogue.module.Module backing : backingModules) {
                java.util.List<Value<?>> values = null;
                if (Epilogue.valueHandler != null && Epilogue.valueHandler.properties != null) {
                    java.util.List<Value<?>> direct = Epilogue.valueHandler.properties.get(backing.getClass());
                    if (direct != null) values = new LinkedList<>(direct);
                }
                if (values == null) continue;
                values.sort(java.util.Comparator.comparing(Value::getName, String.CASE_INSENSITIVE_ORDER));

                for (Value<?> v : values) {
                    if (!(v instanceof ColorValue)) continue;
                    ColorValue cv = (ColorValue) v;

                    float px;
                    float py;
                    int col = idx % 3;
                    int row = idx / 3;
                    px = col == 0 ? col0X : (col == 1 ? col1X : col2X);
                    py = baseY + (row * 57);

                    int rgb = cv.getValue();
                    int a = (rgb >> 24) & 0xFF;
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    ColorObject obj = new ColorObject(r, g, b, a);

                    String label = backing.getName() + ": " + cv.getName();
                    ColorPreview preview = new ColorPreview(obj, label, px, py, categoryButton);
                    categoryPanel.colorPreviews.add(preview);
                    colorValueByPreview.put(preview, cv);
                    colorModuleClassByPreview.put(preview, backing.getClass());
                    colorValueNameByPreview.put(preview, cv.getName());
                    idx++;
                }
            }

            lastColorsCount = idx;

            int rows = (int) Math.ceil(idx / 3f);
            float lastRowBottom = baseY + ((Math.max(0, rows - 1)) * 57) + 46;
            categoryPanel.contentMaxY = lastRowBottom + 120;
            updateScrollBounds(categoryPanel);
            return;
        }

        float col0Y = yOff;
        float col1Y = yOff;
        float col2Y = yOff;
        int col = 0;

        if (categoryButton.name.equalsIgnoreCase("Minigames")) {
            for (Module module : Client.getModuleManager().getArray()) {
                if (module.getType() == ModuleData.Type.Minigames) {
                    y = 20;
                    List<Setting> list = getSettings(module);
                    if (getSettings(module) != null) {
                        if (col0Y <= col1Y && col0Y <= col2Y) {
                            col = 0;
                            xOff = 50 + categoryButton.panel.x;
                            yOff = col0Y;
                        } else if (col1Y <= col0Y && col1Y <= col2Y) {
                            col = 1;
                            xOff = 50 + categoryButton.panel.x + 95;
                            yOff = col1Y;
                        } else {
                            col = 2;
                            xOff = 50 + categoryButton.panel.x + 190;
                            yOff = col2Y;
                        }

                        categoryPanel.buttons.add(new Button(categoryPanel, module.getName(), xOff + 0.5f, yOff + 10, module.backing()));
                        float baseY = yOff;
                        float x1 = 0.5f;
                        float rowY = 20;

                        for (Setting setting : list) {
                            if (setting.getValue() instanceof Boolean) {
                                categoryPanel.checkboxes.add(new Checkbox(categoryPanel, setting.getName(), xOff + x1, yOff + rowY, setting));
                                rowY += 11;
                            }
                        }

                        List<Setting> sliders = new ArrayList<>();
                        list.forEach(setting -> {
                            if (setting.getValue() instanceof Number) {
                                sliders.add(setting);
                            }
                        });
                        sliders.sort(Comparator.comparing(Setting::getName));
                        for (Setting setting : sliders) {
                            categoryPanel.sliders.add(new Slider(categoryPanel, xOff + x1 + 1, yOff + rowY + 4, setting));
                            rowY += 14;
                        }

                        for (Setting setting : getSettings(module)) {
                            if (setting.getValue() instanceof Options) {
                                categoryPanel.dropdownBoxes.add(new DropdownBox(setting, xOff + x1, yOff + rowY + 4, categoryPanel));
                                rowY += 19;
                            }
                            if (setting.getValue() instanceof MultiBool) {
                                categoryPanel.multiDropdownBoxes.add(new MultiDropdownBox((MultiBool) setting.getValue(), setting, xOff + x1, yOff + rowY + 4, categoryPanel));
                                rowY += 19;
                            }
                        }

                        for (Setting setting : getSettings(module)) {
                            if (setting.getValue().getClass().equals(String.class)) {
                                categoryPanel.textBoxes.add(new TextBox(setting, xOff + x1, yOff + rowY + 4, categoryPanel));
                                rowY += 18;
                            }
                        }

                        float maxY = baseY + 12;
                        for (Button b : categoryPanel.buttons) {
                            if (b.y >= baseY && b.x >= xOff && b.x <= xOff + 90) maxY = Math.max(maxY, b.y + 9);
                        }
                        for (Checkbox c : categoryPanel.checkboxes) {
                            if (c.y >= baseY && c.x >= xOff && c.x <= xOff + 90) maxY = Math.max(maxY, c.y + 9);
                        }
                        for (Slider s : categoryPanel.sliders) {
                            if (s.y >= baseY && s.x >= xOff && s.x <= xOff + 90) maxY = Math.max(maxY, s.y + 9);
                        }
                        for (DropdownBox d : categoryPanel.dropdownBoxes) {
                            if (d.y >= baseY && d.x >= xOff && d.x <= xOff + 90) maxY = Math.max(maxY, d.y + 9);
                        }
                        for (MultiDropdownBox d : categoryPanel.multiDropdownBoxes) {
                            if (d.y >= baseY && d.x >= xOff && d.x <= xOff + 90) maxY = Math.max(maxY, d.y + 9);
                        }
                        for (TextBox t : categoryPanel.textBoxes) {
                            if (t.y >= baseY && t.x >= xOff && t.x <= xOff + 90) maxY = Math.max(maxY, t.y + 9);
                        }
                        float boxH = Math.max(40, (maxY - baseY) + 2);
                        categoryPanel.groupBoxes.add(new GroupBox(module.getName(), categoryPanel, xOff, baseY, boxH));

                        if (col == 0) col0Y = baseY + boxH;
                        if (col == 1) col1Y = baseY + boxH;
                        if (col == 2) col2Y = baseY + boxH;
                    } else {
                        if (col0Y <= col1Y && col0Y <= col2Y) {
                            col = 0;
                            xOff = 50 + categoryButton.panel.x;
                            yOff = col0Y;
                        } else if (col1Y <= col0Y && col1Y <= col2Y) {
                            col = 1;
                            xOff = 50 + categoryButton.panel.x + 95;
                            yOff = col1Y;
                        } else {
                            col = 2;
                            xOff = 50 + categoryButton.panel.x + 190;
                            yOff = col2Y;
                        }

                        float baseY = yOff;
                        categoryPanel.buttons.add(new Button(categoryPanel, module.getName(), xOff + 0.5f, yOff + 10, module.backing()));
                        float boxH = 12;
                        categoryPanel.groupBoxes.add(new GroupBox(module.getName(), categoryPanel, xOff, baseY, boxH));
                        if (col == 0) col0Y = baseY + boxH;
                        if (col == 1) col1Y = baseY + boxH;
                        if (col == 2) col2Y = baseY + boxH;
                    }
                }
            }
        }
        ModuleData.Type type = null;
        try {
            type = ModuleData.Type.valueOf(categoryButton.name);
        } catch (Exception e) {
        }

        if (type != null && type != ModuleData.Type.Minigames) {
            for (Module module : Client.getModuleManager().getArray()) {
                if (module.getType() == type) {
                    y = 20;
                    List<Setting> list = getSettings(module);
                    if (getSettings(module) != null) {
                        if (col0Y <= col1Y && col0Y <= col2Y) {
                            col = 0;
                            xOff = 50 + categoryButton.panel.x;
                            yOff = col0Y;
                        } else if (col1Y <= col0Y && col1Y <= col2Y) {
                            col = 1;
                            xOff = 50 + categoryButton.panel.x + 95;
                            yOff = col1Y;
                        } else {
                            col = 2;
                            xOff = 50 + categoryButton.panel.x + 190;
                            yOff = col2Y;
                        }

                        categoryPanel.buttons.add(new Button(categoryPanel, module.getName(), xOff + 0.5f, yOff + 10, module.backing()));
                        float baseY = yOff;
                        float x1 = 0.5f;
                        float rowY = 20;

                        for (Setting setting : list) {
                            if (setting.getValue() instanceof Boolean) {
                                categoryPanel.checkboxes.add(new Checkbox(categoryPanel, setting.getName(), xOff + x1, yOff + rowY, setting));
                                rowY += 11;
                            }
                        }

                        List<Setting> sliders = new ArrayList<>();
                        list.forEach(setting -> {
                            if (setting.getValue() instanceof Number) {
                                sliders.add(setting);
                            }
                        });
                        sliders.sort(Comparator.comparing(Setting::getName));
                        for (Setting setting : sliders) {
                            categoryPanel.sliders.add(new Slider(categoryPanel, xOff + x1 + 1, yOff + rowY + 4, setting));
                            rowY += 14;
                        }

                        for (Setting setting : getSettings(module)) {
                            if (setting.getValue() instanceof Options) {
                                categoryPanel.dropdownBoxes.add(new DropdownBox(setting, xOff + x1, yOff + rowY + 4, categoryPanel));
                                rowY += 19;
                            }
                            if (setting.getValue() instanceof MultiBool) {
                                categoryPanel.multiDropdownBoxes.add(new MultiDropdownBox((MultiBool) setting.getValue(), setting, xOff + x1, yOff + rowY + 4, categoryPanel));
                                rowY += 19;
                            }
                        }

                        for (Setting setting : getSettings(module)) {
                            if (setting.getValue().getClass().equals(String.class)) {
                                categoryPanel.textBoxes.add(new TextBox(setting, xOff + x1, yOff + rowY + 4, categoryPanel));
                                rowY += 18;
                            }
                        }

                        float maxY = baseY + 12;
                        for (Button b : categoryPanel.buttons) {
                            if (b.y >= baseY && b.x >= xOff && b.x <= xOff + 90) maxY = Math.max(maxY, b.y + 9);
                        }
                        for (Checkbox c : categoryPanel.checkboxes) {
                            if (c.y >= baseY && c.x >= xOff && c.x <= xOff + 90) maxY = Math.max(maxY, c.y + 9);
                        }
                        for (Slider s : categoryPanel.sliders) {
                            if (s.y >= baseY && s.x >= xOff && s.x <= xOff + 90) maxY = Math.max(maxY, s.y + 9);
                        }
                        for (DropdownBox d : categoryPanel.dropdownBoxes) {
                            if (d.y >= baseY && d.x >= xOff && d.x <= xOff + 90) maxY = Math.max(maxY, d.y + 9);
                        }
                        for (MultiDropdownBox d : categoryPanel.multiDropdownBoxes) {
                            if (d.y >= baseY && d.x >= xOff && d.x <= xOff + 90) maxY = Math.max(maxY, d.y + 9);
                        }
                        for (TextBox t : categoryPanel.textBoxes) {
                            if (t.y >= baseY && t.x >= xOff && t.x <= xOff + 90) maxY = Math.max(maxY, t.y + 9);
                        }
                        float boxH = Math.max(40, (maxY - baseY) + 2);
                        categoryPanel.groupBoxes.add(new GroupBox(module.getName(), categoryPanel, xOff, baseY, boxH));

                        if (col == 0) col0Y = baseY + boxH;
                        if (col == 1) col1Y = baseY + boxH;
                        if (col == 2) col2Y = baseY + boxH;
                    } else {
                        if (col0Y <= col1Y && col0Y <= col2Y) {
                            col = 0;
                            xOff = 50 + categoryButton.panel.x;
                            yOff = col0Y;
                        } else if (col1Y <= col0Y && col1Y <= col2Y) {
                            col = 1;
                            xOff = 50 + categoryButton.panel.x + 95;
                            yOff = col1Y;
                        } else {
                            col = 2;
                            xOff = 50 + categoryButton.panel.x + 190;
                            yOff = col2Y;
                        }

                        float baseY = yOff;
                        categoryPanel.buttons.add(new Button(categoryPanel, module.getName(), xOff + 0.5f, yOff + 10, module.backing()));
                        float boxH = 12;
                        categoryPanel.groupBoxes.add(new GroupBox(module.getName(), categoryPanel, xOff, baseY, boxH));
                        if (col == 0) col0Y = baseY + boxH;
                        if (col == 1) col1Y = baseY + boxH;
                        if (col == 2) col2Y = baseY + boxH;
                    }
                }
            }
        }

        categoryPanel.contentMaxY = Math.max(col0Y, Math.max(col1Y, col2Y));

        updateScrollBounds(categoryPanel);
    }

    @Override
    public void categoryPanelMouseClicked(CategoryPanel categoryPanel, int p1, int p2, int p3) {
        if (!categoryPanel.visible) return;
        categoryPanel.buttons.forEach(b -> b.mouseClicked(p1, p2, p3));
        categoryPanel.checkboxes.forEach(c -> c.mouseClicked(p1, p2, p3));
        categoryPanel.sliders.forEach(s -> s.mouseClicked(p1, p2, p3));
        categoryPanel.dropdownBoxes.forEach(d -> d.mouseClicked(p1, p2, p3));
        categoryPanel.multiDropdownBoxes.forEach(d -> d.mouseClicked(p1, p2, p3));
        categoryPanel.groupBoxes.forEach(g -> g.mouseClicked(p1, p2, p3));
        categoryPanel.textBoxes.forEach(t -> t.mouseClicked(p1, p2, p3));
        categoryPanel.colorPreviews.forEach(c -> {
            if (c != null && c.sliders != null) {
                for (HSVColorPicker s : c.sliders) {
                    if (s != null) s.mouseClicked(p1, p2, p3);
                }
            }
        });
        if (categoryPanel.configTextBox != null) {
            categoryPanel.configTextBox.mouseClicked(p1, p2, p3);
        }
        if (categoryPanel.configList != null) {
            categoryPanel.configList.mouseClicked(p1, p2, p3);
        }
    }

    @Override
    public void categoryPanelDraw(CategoryPanel categoryPanel, float x, float y) {
        if (!categoryPanel.visible) return;
        float xOff = categoryPanel.categoryButton.panel.dragX;
        float yOff = categoryPanel.categoryButton.panel.dragY;

        float clipX = categoryPanel.categoryButton.panel.x + xOff + 40;
        float clipY = categoryPanel.categoryButton.panel.y + yOff + 12;
        float clipW = 340 - 40 - 2;
        float clipH = 340 - 18;

        categoryPanel.clipX = clipX;
        categoryPanel.clipY = clipY;
        categoryPanel.clipW = clipW;
        categoryPanel.clipH = clipH;

        glEnable(GL_SCISSOR_TEST);
        epilogue.util.render.RenderUtil.scissorStart(clipX, clipY, clipW, clipH);

        if ("Colors".equalsIgnoreCase(categoryPanel.headerString)) {
            Fonts.draw(Fonts.tiny(), "Count: " + lastColorsCount, (categoryPanel.categoryButton.panel.x + xOff + 50), (categoryPanel.categoryButton.panel.y + yOff + 14), Colors.getColor(255, (int) opacity.getOpacity()));
        }

        categoryPanel.groupBoxes.forEach(g -> g.draw(x, y));
        categoryPanel.buttons.forEach(b -> b.draw(x, y));
        categoryPanel.checkboxes.forEach(c -> c.draw(x, y));
        categoryPanel.sliders.forEach(s -> s.draw(x, y));
        categoryPanel.dropdownBoxes.forEach(d -> d.draw(x, y));
        categoryPanel.multiDropdownBoxes.forEach(d -> d.draw(x, y));
        categoryPanel.colorPreviews.forEach(c -> c.draw(x, y));
        categoryPanel.textBoxes.forEach(t -> t.draw(x, y));
        if (categoryPanel.configTextBox != null) {
            categoryPanel.configTextBox.draw(x, y);
        }
        if (categoryPanel.configList != null) {
            categoryPanel.configList.draw(x, y);
        }

        epilogue.util.render.RenderUtil.scissorEnd();
        glDisable(GL_SCISSOR_TEST);
    }

    @Override
    public void handleMouseInput(epilogue.ui.clickgui.exhibition.components.MainPanel panel) {
        if (panel == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int mx = Mouse.getEventX() * sr.getScaledWidth() / mc.displayWidth;
        int my = sr.getScaledHeight() - Mouse.getEventY() * sr.getScaledHeight() / mc.displayHeight - 1;

        if (Mouse.getEventButtonState()) {
            int btn = Mouse.getEventButton();
            if (btn >= 0) {
                boolean allowMouseBind = btn == 2 || btn == 3 || btn == 4;
                int key = allowMouseBind ? -(btn + 100) : 0;
                for (CategoryButton cb : panel.typeButton) {
                    if (cb == null || cb.categoryPanel == null || !cb.categoryPanel.visible) continue;
                    for (Button b : cb.categoryPanel.buttons) {
                        if (b != null && b.isBinding) {
                            b.module.setKey(key);
                            b.isBinding = false;
                        }
                    }
                }
            }
        }

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            for (CategoryButton button : panel.typeButton) {
                if (button.categoryPanel == null || !button.categoryPanel.visible) continue;

                float xOff = panel.dragX;
                float yOff = panel.dragY;
                float clipX = panel.x + xOff + 40;
                float clipY = panel.y + yOff + 12;
                float clipW = 340 - 40 - 4;
                float clipH = 340 - 18;
                boolean hovered = mx >= clipX && mx <= clipX + clipW && my >= clipY && my <= clipY + clipH;
                if (!hovered) continue;

                float old = button.categoryPanel.scroll;
                float target = old + (wheel > 0 ? 15f : -15f);
                float delta = target - old;
                if (delta != 0) {
                    button.categoryPanel.scroll = target;
                    button.categoryPanel.sliders.forEach(s -> s.dragging = false);
                    applyScrollDelta(button.categoryPanel, delta);
                }
                break;
            }
        }

        if (panel.typeButton != null) {
            panel.typeButton.forEach(o -> {
                if (o.categoryPanel != null && o.categoryPanel.configList != null) {
                    o.categoryPanel.configList.handleMouseInput();
                }
            });
        }
    }

    @Override
    public void categoryPanelMouseMovedOrUp(CategoryPanel categoryPanel, int x, int y, int button) {
        if (!categoryPanel.visible) return;
        categoryPanel.sliders.forEach(s -> s.mouseReleased(x, y, button));
        categoryPanel.colorPreviews.forEach(c -> c.sliders.forEach(p -> p.mouseReleased(x, y, button)));
    }

    @Override
    public void groupBoxConstructor(GroupBox groupBox, float x, float y) {
    }

    @Override
    public void groupBoxMouseClicked(GroupBox groupBox, int p1, int p2, int p3) {
    }

    @Override
    public void groupBoxDraw(GroupBox groupBox, float x, float y) {
        if (groupBox == null || groupBox.categoryPanel == null || !groupBox.categoryPanel.visible) return;
        float xOff = groupBox.x + groupBox.categoryPanel.categoryButton.panel.dragX - 2.5F;
        float yOff = groupBox.y + groupBox.categoryPanel.categoryButton.panel.dragY + 10;

        float labelW = Fonts.width(Fonts.tiny(), groupBox.label);

        Depth.pre();
        Depth.mask();
        RenderingUtil.rectangle(xOff + 4.5, yOff - 6, xOff + labelW + 6.5, yOff - 5.5, -1);
        RenderingUtil.rectangle(xOff + 5, yOff - 5.5, xOff + labelW + 6, yOff - 5, -1);
        Depth.render(GL11.GL_LESS);
        RenderingUtil.rectangleBordered(xOff, yOff - 6, xOff + groupBox.width, yOff + groupBox.height, 0.5, Colors.getColor(0, 0), Colors.getColor(10, (int) opacity.getOpacity()));
        Depth.post();

        Depth.pre();
        Depth.mask();
        RenderingUtil.rectangle(xOff + 4.5, yOff - 6, xOff + labelW + 6.5, yOff - 5.5, -1);
        RenderingUtil.rectangle(xOff + 5, yOff - 5.5, xOff + labelW + 6, yOff - 5, -1);
        Depth.render(GL11.GL_LESS);
        RenderingUtil.rectangleBordered(xOff + 0.5, yOff - 5.5, xOff + groupBox.width - 0.5, yOff + groupBox.height - 0.5, 0.5, Colors.getColor(17, (int) opacity.getOpacity()), Colors.getColor(48, (int) opacity.getOpacity()));
        Depth.post();

        if (groupBox.renderLabel) {
            Fonts.draw(Fonts.tiny(), groupBox.label, xOff + 6, yOff - 6.5F, Colors.getColor(220, (int) opacity.getOpacity()));
        }
    }

    @Override
    public void groupBoxMouseMovedOrUp(GroupBox groupBox, int x, int y, int button) {
    }

    @Override
    public void buttonContructor(Button p0, CategoryPanel panel) {
    }

    @Override
    public void buttonMouseClicked(Button p0, int p2, int p3, int p4, CategoryPanel panel) {
        if (!panel.visible) return;
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        boolean enableHover = p2 >= p0.x + xOff && p3 >= p0.y + yOff && p2 <= p0.x + 35 + xOff && p3 <= p0.y + 6 + yOff;
        String keyText = p0.module.getKey() != 0 ? "[" + KeyBindUtil.getKeyName(p0.module.getKey()) + "]" : "[-]";
        float keyW = Fonts.width(Fonts.tiny(), keyText);
        float keyX = (p0.x + xOff + 42) - keyW;
        boolean bindHover = p2 >= keyX && p3 >= (p0.y + yOff) && p2 <= (p0.x + xOff + 42) && p3 <= (p0.y + yOff + 6);

        if (p4 == 0) {
            if (bindHover) {
                p0.isBinding = !p0.isBinding;
                return;
            }
            if (enableHover) {
                if (!p0.isBinding) {
                    p0.module.toggle();
                    p0.enabled = p0.module.isEnabled();
                }
                return;
            }
        }

        if (p4 == 1) {
            if (bindHover) {
                p0.isBinding = !p0.isBinding;
                return;
            }
            if (p0.isBinding) {
                p0.module.setKey(0);
                p0.isBinding = false;
                return;
            }
        }

        if (p0.isBinding && !enableHover && !bindHover) {
            p0.isBinding = false;
        }
    }

    @Override
    public void buttonDraw(Button p0, float p2, float p3, CategoryPanel panel) {
        if (!panel.visible) return;
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        GlStateManager.pushMatrix();
        RenderingUtil.rectangleBordered(p0.x + xOff + 0.6, p0.y + yOff + 0.6, p0.x + 6 + xOff + -0.6, p0.y + 6 + yOff + -0.6, 0.5, Colors.getColor(0, 0), Colors.getColor(10, (int) opacity.getOpacity()));
        p0.enabled = p0.module.isEnabled();
        if (p0.enabled) {
            RenderingUtil.drawGradient(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + xOff + 5, p0.y + yOff + 5,
                    Colors.getColor(ColorManager.hudColor.getRed(), ColorManager.hudColor.getGreen(), ColorManager.hudColor.getBlue(), (int) opacity.getOpacity()),
                    Colors.getColor(ColorManager.hudColor.getRed(), ColorManager.hudColor.getGreen(), ColorManager.hudColor.getBlue(), (int) (120 * (opacity.getOpacity() / 255F))));
        } else {
            RenderingUtil.drawGradient(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + 6 + xOff + -1, p0.y + 6 + yOff + -1, Colors.getColor(76, (int) opacity.getOpacity()), Colors.getColor(51, (int) opacity.getOpacity()));
        }
        Fonts.draw(Fonts.miniBold(), p0.module.getName(), (p0.x + xOff + 3), (p0.y + 0.5f + yOff - 7), Colors.getColor(220, (int) opacity.getOpacity()));
        Fonts.draw(Fonts.tiny(), "Enable", (p0.x + 7.6f + xOff), (p0.y + 1 + yOff), Colors.getColor(185, (int) opacity.getOpacity()));
        String keyText = p0.module.getKey() != 0 ? "[" + KeyBindUtil.getKeyName(p0.module.getKey()) + "]" : "[-]";
        float keyW = Fonts.width(Fonts.tiny(), keyText);
        float keyX = (p0.x + xOff + 42) - keyW;
        Fonts.draw(Fonts.tiny(), keyText, keyX, (p0.y + 1 + yOff), p0.isBinding ? Colors.getColor(216, 56, 56, (int) opacity.getOpacity()) : Colors.getColor(75, (int) opacity.getOpacity()));
        GlStateManager.popMatrix();
    }

    @Override
    public void buttonKeyPressed(Button button, int key) {
        if (button == null || button.panel == null || !button.panel.visible) return;
        if (!button.isBinding) return;

        if (key == Keyboard.KEY_ESCAPE) {
            button.module.setKey(0);
            button.isBinding = false;
            return;
        }
        if (key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK) {
            button.module.setKey(0);
            button.isBinding = false;
            return;
        }
        if (key == Keyboard.KEY_LSHIFT || key == Keyboard.KEY_RSHIFT || key == Keyboard.KEY_LCONTROL || key == Keyboard.KEY_RCONTROL || key == Keyboard.KEY_LMENU || key == Keyboard.KEY_RMENU) {
            return;
        }
        if (key == 0) return;

        button.module.setKey(key);
        button.isBinding = false;
    }

    @Override
    public void checkBoxMouseClicked(Checkbox p0, int p2, int p3, int p4, CategoryPanel panel) {
        if (!panel.visible) return;
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        boolean hovering = (p2 >= xOff + p0.x) && (p3 >= yOff + p0.y) && (p2 <= xOff + p0.x + 9) && (p3 <= yOff + p0.y + 9);
        if (hovering && p4 == 0) {
            p0.enabled = !p0.enabled;
            p0.setting.setValue(p0.enabled);
            if (p0.setting.getBacking() instanceof BooleanValue) {
                ((BooleanValue) p0.setting.getBacking()).setValue(p0.enabled);
            }
        }
    }

    @Override
    public void checkBoxDraw(Checkbox p0, float p2, float p3, CategoryPanel panel) {
        if (!panel.visible) return;
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        GlStateManager.pushMatrix();
        String name = p0.setting.getName().charAt(0) + p0.setting.getName().toLowerCase().substring(1);
        Fonts.draw(Fonts.tiny(), name, (p0.x + 7.5f + xOff), (p0.y + 1 + yOff), Colors.getColor(185, (int) opacity.getOpacity()));
        RenderingUtil.rectangleBordered(p0.x + xOff + 0.6, p0.y + yOff + 0.6, p0.x + 6 + xOff + -0.6, p0.y + 6 + yOff + -0.6, 0.5, Colors.getColor(0, 0), Colors.getColor(10, (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + 6 + xOff + -1, p0.y + 6 + yOff + -1, Colors.getColor(76, (int) opacity.getOpacity()), Colors.getColor(51, (int) opacity.getOpacity()));
        p0.enabled = ((Boolean) p0.setting.getValue());
        boolean hovering = p2 >= p0.x + xOff && p3 >= p0.y + yOff && p2 <= p0.x + 35 + xOff && p3 <= p0.y + 6 + yOff;
        if (p0.enabled) {
            RenderingUtil.drawGradient(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + xOff + 5, p0.y + yOff + 5,
                    Colors.getColor(ColorManager.hudColor.getRed(), ColorManager.hudColor.getGreen(), ColorManager.hudColor.getBlue(), (int) opacity.getOpacity()),
                    Colors.getColor(ColorManager.hudColor.getRed(), ColorManager.hudColor.getGreen(), ColorManager.hudColor.getBlue(), (int) (120 * (opacity.getOpacity() / 255F))));
        }
        if (hovering && !p0.enabled) {
            RenderingUtil.rectangle(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + xOff + 5, p0.y + yOff + 5, Colors.getColor(255, 40));
        }
        GlStateManager.popMatrix();
    }

    @Override
    public void dropDownContructor(DropdownBox p0, float x, float u, CategoryPanel panel) {
        p0.buttons.clear();
        Options opt = (Options) p0.setting.getValue();
        if (opt == null || opt.getOptions() == null) return;
        int yOff = 10;
        for (String value : opt.getOptions()) {
            p0.buttons.add(new DropdownButton(value, x, u + yOff, p0));
            yOff += 9;
        }
    }

    @Override
    public void dropDownMouseClicked(DropdownBox p0, int x, int u, int mouse, CategoryPanel panel) {
        if (!panel.visible) return;
        for (DropdownButton db : p0.buttons) {
            if (p0.active && p0.panel.visible) {
                db.mouseClicked(x, u, mouse);
            }
        }
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        if ((x >= xOff + p0.x) && (u >= yOff + p0.y) && (x <= xOff + p0.x + 40) && (u <= yOff + p0.y + 8) && (mouse == 0) && p0.panel.visible) {
            p0.active = !p0.active;
        } else {
            p0.active = false;
        }
    }

    @Override
    public void dropDownDraw(DropdownBox p0, float x, float y, CategoryPanel panel) {
        if (!panel.visible) return;
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        boolean hovering = (x >= xOff + p0.x) && (y >= yOff + p0.y) && (x <= xOff + p0.x + 40) && (y <= yOff + p0.y + 9);
        RenderingUtil.rectangle(p0.x + xOff - 0.3, p0.y + yOff - 0.3, p0.x + xOff + 40 + 0.3, p0.y + yOff + 9 + 0.3, Colors.getColor(10, (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(p0.x + xOff, p0.y + yOff, p0.x + xOff + 40, p0.y + yOff + 9, Colors.getColor(31, (int) opacity.getOpacity()), Colors.getColor(36, (int) opacity.getOpacity()));
        if (hovering) {
            RenderingUtil.rectangleBordered(p0.x + xOff, p0.y + yOff, p0.x + xOff + 40, p0.y + yOff + 9, 0.3, Colors.getColor(0, 0), Colors.getColor(90, (int) opacity.getOpacity()));
        }
        Fonts.draw(Fonts.micro(), p0.setting.getName(), (p0.x + xOff + 1), (p0.y - 5.5f + yOff), Colors.getColor(185, (int) opacity.getOpacity()));
        GlStateManager.pushMatrix();
        GlStateManager.translate((p0.x + xOff + 38 - (p0.active ? 2.5f : 0)), (p0.y + 4.5f + yOff), 0);
        GlStateManager.rotate(p0.active ? 270 : 90, 0, 0, 90);
        RenderingUtil.rectangle(-1, 0, -0.5, 2.5, Colors.getColor(0, (int) opacity.getOpacity()));
        RenderingUtil.rectangle(-0.5, 0, -1, 2.5, Colors.getColor(151, (int) opacity.getOpacity()));
        RenderingUtil.rectangle(0, 0.5, 0.5, 2, Colors.getColor(151, (int) opacity.getOpacity()));
        RenderingUtil.rectangle(0.5, 1, 1, 1.5, Colors.getColor(151, (int) opacity.getOpacity()));
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        Depth.pre();
        Depth.mask();
        RenderingUtil.rectangle(p0.x + xOff, p0.y + yOff, p0.x + xOff + 30, p0.y + yOff + 9, Colors.getColor(31, (int) opacity.getOpacity()));
        RenderingUtil.drawGradientSideways(p0.x + xOff + 30, p0.y + yOff, p0.x + xOff + 35, p0.y + yOff + 9, Colors.getColor(31, (int) opacity.getOpacity()), Colors.getColor(31, 0));
        Depth.render();
        RenderingUtil.rectangle(p0.x + xOff + 1, p0.y + yOff + 1, p0.x + xOff + 29, p0.y + yOff + 8, Colors.getColor(25, (int) opacity.getOpacity()));
        Fonts.draw(Fonts.tiny(), p0.option.getSelected(), (p0.x + 4 + xOff) - 1, (p0.y + 3f + yOff), Colors.getColor(151, (int) opacity.getOpacity()));
        Depth.post();
        GlStateManager.popMatrix();
        if (p0.active) {
            int i = p0.buttons.size();
            RenderingUtil.rectangle(p0.x + xOff - 0.3, p0.y + 10 + yOff - 0.3, p0.x + xOff + 40 + 0.3, p0.y + yOff + 9 + (9 * i) + 0.3, Colors.getColor(10, (int) opacity.getOpacity()));
            RenderingUtil.drawGradient(p0.x + xOff, p0.y + yOff + 10, p0.x + xOff + 40, p0.y + yOff + 9 + (9 * i), Colors.getColor(31, (int) opacity.getOpacity()), Colors.getColor(36, (int) opacity.getOpacity()));
            for (DropdownButton b : p0.buttons) {
                b.draw(x, y);
            }
        }
    }

    @Override
    public void dropDownButtonMouseClicked(DropdownButton p0, DropdownBox p1, int x, int y, int mouse) {
        if (!p1.panel.visible) return;
        float xOff = p1.panel.categoryButton.panel.dragX;
        float yOff = p1.panel.categoryButton.panel.dragY;
        boolean hovering = (x >= xOff + p0.x) && (y >= yOff + p0.y) && (x <= xOff + p0.x + 40) && (y <= yOff + p0.y + 9);
        if (hovering && mouse == 0) {
            Options opt = (Options) p1.setting.getValue();
            if (opt != null) {
                opt.setSelected(p0.name);
                p1.setting.setValue(opt);
                if (p1.setting.getBacking() instanceof ModeValue) {
                    ((ModeValue) p1.setting.getBacking()).parseString(p0.name);
                }
            }
            p1.active = false;
        }
    }

    @Override
    public void dropDownButtonDraw(DropdownButton p0, DropdownBox p1, float x, float y) {
        if (!p1.panel.visible) return;
        float xOff = p1.panel.categoryButton.panel.dragX;
        float yOff = p1.panel.categoryButton.panel.dragY;
        boolean hovering = (x >= xOff + p0.x) && (y >= yOff + p0.y) && (x <= xOff + p0.x + 40) && (y <= yOff + p0.y + 8.5);
        boolean active = p0.name.equalsIgnoreCase(p1.option.getSelected());
        int color = active && !hovering
                ? Colors.getColor(ColorManager.hudColor.getRed(), ColorManager.hudColor.getGreen(), ColorManager.hudColor.getBlue(), (int) opacity.getOpacity())
                : Colors.getColor(255, (int) opacity.getOpacity());
        Fonts.draw(active ? Fonts.tinyBold() : Fonts.tiny(), p0.name, (p0.x + 3 + xOff), (p0.y + 2f + yOff), color);
    }

    @Override
    public void multiDropDownContructor(MultiDropdownBox p0, float x, float u, CategoryPanel panel) {
        p0.buttons.clear();
        int y = 10;
        if (p0.multiBool == null) return;
        p0.buttons.add(new MultiDropdownButton(p0.name, x, u + y, p0, p0.setting));
    }

    @Override
    public void multiDropDownMouseClicked(MultiDropdownBox p0, int x, int u, int mouse, CategoryPanel panel) {
        for (MultiDropdownButton db : p0.buttons) {
            if (p0.active && p0.panel.visible) {
                db.mouseClicked(x, u, mouse);
            }
        }
        if (mouse == 0) {
            if ((x >= panel.categoryButton.panel.dragX + p0.x) && (u >= panel.categoryButton.panel.dragY + p0.y) && (x <= panel.categoryButton.panel.dragX + p0.x + 40) && (u <= panel.categoryButton.panel.dragY + p0.y + 8) &&
                    p0.panel.visible) {
                p0.active = (!p0.active);
            } else if (!((x >= panel.categoryButton.panel.dragX + p0.x) && (u >= panel.categoryButton.panel.dragY + p0.y + 8) && (x <= panel.categoryButton.panel.dragX + p0.x + 40) && (u <= panel.categoryButton.panel.dragY + p0.y + 8 + p0.buttons.size() * 9))) {
                p0.active = false;
            }
        }
    }

    @Override
    public void multiDropDownDraw(MultiDropdownBox p0, float x, float y, CategoryPanel panel) {
        if (!panel.visible) return;
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        boolean hovering = (x >= panel.categoryButton.panel.dragX + p0.x) && (y >= panel.categoryButton.panel.dragY + p0.y) && (x <= panel.categoryButton.panel.dragX + p0.x + 40) && (y <= panel.categoryButton.panel.dragY + p0.y + 9);

        RenderingUtil.rectangle(p0.x + xOff - 0.3, p0.y + yOff - 0.3, p0.x + xOff + 40 + 0.3, p0.y + yOff + 9 + 0.3, Colors.getColor(10, (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(p0.x + xOff, p0.y + yOff, p0.x + xOff + 40, p0.y + yOff + 9, Colors.getColor(31, (int) opacity.getOpacity()), Colors.getColor(36, (int) opacity.getOpacity()));
        if (hovering) {
            RenderingUtil.rectangleBordered(p0.x + xOff, p0.y + yOff, p0.x + xOff + 40, p0.y + yOff + 9, 0.3, Colors.getColor(0, 0), Colors.getColor(90, (int) opacity.getOpacity()));
        }
        Fonts.draw(Fonts.tiny(), p0.name, (p0.x + xOff + 1), (p0.y - 6 + yOff), Colors.getColor(185, (int) opacity.getOpacity()));

        GlStateManager.pushMatrix();
        GlStateManager.translate((p0.x + xOff + 38 - (p0.active ? 2.5f : 0)), (p0.y + 4.5f + yOff), 0);
        GlStateManager.rotate(p0.active ? 270 : 90, 0, 0, 90);
        RenderingUtil.rectangle(-1, 0, -0.5, 2.5, Colors.getColor(0, (int) opacity.getOpacity()));
        RenderingUtil.rectangle(-0.5, 0, -1, 2.5, Colors.getColor(151, (int) opacity.getOpacity()));
        RenderingUtil.rectangle(0, 0.5, 0.5, 2, Colors.getColor(151, (int) opacity.getOpacity()));
        RenderingUtil.rectangle(0.5, 1, 1, 1.5, Colors.getColor(151, (int) opacity.getOpacity()));
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        Depth.pre();
        Depth.mask();
        Fonts.draw(Fonts.tiny(), "None", (p0.x + 4 + xOff) - 1, (p0.y + 3f + yOff), -1);
        Depth.render();
        RenderingUtil.rectangle(p0.x + xOff, p0.y + yOff, p0.x + xOff + 30, p0.y + yOff + 9, Colors.getColor(151, (int) opacity.getOpacity()));
        RenderingUtil.drawGradientSideways(p0.x + xOff + 30, p0.y + yOff, p0.x + xOff + 35, p0.y + yOff + 9, Colors.getColor(151, (int) opacity.getOpacity()), Colors.getColor(151, 0));
        Depth.post();
        GlStateManager.popMatrix();

        if (p0.active) {
            int i = p0.buttons.size();
            RenderingUtil.rectangle(p0.x + xOff - 0.3, p0.y + 10 + yOff - 0.3, p0.x + xOff + 40 + 0.3, p0.y + yOff + 9 + (9 * i) + 0.3, Colors.getColor(10, (int) opacity.getOpacity()));
            RenderingUtil.drawGradient(p0.x + xOff, p0.y + yOff + 10, p0.x + xOff + 40, p0.y + yOff + 9 + (9 * i), Colors.getColor(31, (int) opacity.getOpacity()), Colors.getColor(36, (int) opacity.getOpacity()));
        }
    }

    @Override
    public void multiDropDownButtonMouseClicked(MultiDropdownButton p0, MultiDropdownBox p1, int x, int y, int mouse) {
        if ((x >= p1.panel.categoryButton.panel.dragX + p0.x) && (y >= p1.panel.categoryButton.panel.dragY + p0.y) && (x <= p1.panel.categoryButton.panel.dragX + p0.x + 40) && (y <= p1.panel.categoryButton.panel.dragY + p0.y + 8.5) && (mouse == 0)) {
            p0.setting.setValue(!(boolean) p0.setting.getValue());
        }
    }

    @Override
    public void multiDropDownButtonDraw(MultiDropdownButton p0, MultiDropdownBox p1, float x, float y) {
        if (p1 == null || p1.panel == null || !p1.panel.visible) return;
        float xOff = p1.panel.categoryButton.panel.dragX;
        float yOff = p1.panel.categoryButton.panel.dragY;
        boolean hovering = (x >= xOff + p0.x) && (y >= yOff + p0.y) && (x <= xOff + p0.x + 40) && (y <= yOff + p0.y + 8.5);
        boolean active = (boolean) p0.setting.getValue();
        int color = active && !hovering
                ? Colors.getColor(ColorManager.hudColor.getRed(), ColorManager.hudColor.getGreen(), ColorManager.hudColor.getBlue(), (int) opacity.getOpacity())
                : Colors.getColor(255, (int) opacity.getOpacity());
        String name = p0.setting.getName().charAt(0) + p0.setting.getName().toLowerCase().substring(1);
        Fonts.draw(active ? Fonts.tinyBold() : Fonts.tiny(), name, (p0.x + 3 + xOff), (p0.y + 2f + yOff), color);
    }

    @Override
    public void SliderContructor(Slider p0, CategoryPanel panel) {
        Object v = p0.setting.getValue();
        if (!(v instanceof Number)) return;
        Value<?> backing = p0.setting.getBacking();
        double min = 0;
        double max = 1;
        if (backing instanceof IntValue) {
            min = ((IntValue) backing).getMinimum();
            max = ((IntValue) backing).getMaximum();
        } else if (backing instanceof FloatValue) {
            min = ((FloatValue) backing).getMinimum();
            max = ((FloatValue) backing).getMaximum();
        } else if (backing instanceof PercentValue) {
            min = ((PercentValue) backing).getMinimum();
            max = ((PercentValue) backing).getMaximum();
        }
        double value = ((Number) v).doubleValue();
        if (value < min) value = min;
        if (value > max) value = max;
        double percent = (value - min) / (max - min);
        p0.dragX = 70 * percent;
    }

    @Override
    public void SliderMouseClicked(Slider p0, int p2, int p3, int p4, CategoryPanel panel) {
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        if (panel.visible && p2 >= panel.x + xOff + p0.x && p3 >= yOff + panel.y + p0.y - 6 && p2 <= xOff + panel.x + p0.x + 70.0f && p3 <= yOff + panel.y + p0.y + 3.5F && p4 == 0) {
            p0.dragging = true;
            p0.lastDragX = p2;
            p0.dragX = (p2 - (p0.x + xOff));
        }
    }

    @Override
    public void SliderMouseMovedOrUp(Slider p0, int p2, int p3, int p4, CategoryPanel panel) {
        if (p4 == 0) {
            if (p0.dragging) {
                double value = ((Number) p0.setting.getValue()).doubleValue();
                Value<?> backing = p0.setting.getBacking();
                if (backing instanceof IntValue) {
                    ((IntValue) backing).setValue((int) Math.round(value));
                } else if (backing instanceof FloatValue) {
                    ((FloatValue) backing).setValue((float) value);
                } else if (backing instanceof PercentValue) {
                    ((PercentValue) backing).setValue((int) Math.round(value));
                }
            }
            p0.dragging = false;
        }
    }

    @Override
    public void SliderDraw(Slider p0, float p2, float p3, CategoryPanel panel) {
        if (!panel.visible) return;
        GlStateManager.pushMatrix();
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        Object v = p0.setting.getValue();
        if (!(v instanceof Number)) {
            GlStateManager.popMatrix();
            return;
        }

        Value<?> backing = p0.setting.getBacking();
        double min = 0;
        double max = 1;
        if (backing instanceof IntValue) {
            min = ((IntValue) backing).getMinimum();
            max = ((IntValue) backing).getMaximum();
        } else if (backing instanceof FloatValue) {
            min = ((FloatValue) backing).getMinimum();
            max = ((FloatValue) backing).getMaximum();
        } else if (backing instanceof PercentValue) {
            min = ((PercentValue) backing).getMinimum();
            max = ((PercentValue) backing).getMaximum();
        }

        double settingValue = ((Number) v).doubleValue();
        if (settingValue < min) settingValue = min;
        if (settingValue > max) settingValue = max;

        float trackW = 70f;
        float sliderX = (float) (((settingValue - min) / (max - min)) * trackW);
        RenderingUtil.rectangle(p0.x + xOff - 0.3, p0.y + yOff - 0.3, p0.x + xOff + trackW + 0.3, p0.y + yOff + 2.5 + 0.3, Colors.getColor(10, (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(p0.x + xOff, p0.y + yOff, p0.x + xOff + trackW, p0.y + yOff + 2.5, Colors.getColor(46, (int) opacity.getOpacity()), Colors.getColor(27, (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(p0.x + xOff, p0.y + yOff, p0.x + xOff + sliderX, p0.y + yOff + 2.5,
                Colors.getColor(ColorManager.hudColor.getRed(), ColorManager.hudColor.getGreen(), ColorManager.hudColor.getBlue(), (int) opacity.getOpacity()),
                Colors.getColor(ColorManager.hudColor.getRed(), ColorManager.hudColor.getGreen(), ColorManager.hudColor.getBlue(), (int) (120 * (opacity.getOpacity() / 255F))));

        String name = p0.setting.getName().charAt(0) + p0.setting.getName().toLowerCase().substring(1);
        String valueStr = String.valueOf(p0.setting.getValue());
        float valueW = Fonts.width(Fonts.tiny(), valueStr);
        float valueX = (p0.x + xOff + trackW + 10) - valueW;
        float nameMaxW = (valueX - 2) - (p0.x + xOff);
        GlStateManager.pushMatrix();
        Depth.pre();
        Depth.mask();
        RenderingUtil.rectangle(p0.x + xOff, p0.y - 7 + yOff, p0.x + xOff + Math.max(0, nameMaxW), p0.y + 5 + yOff, Colors.getColor(0, 0));
        Depth.render();
        Fonts.draw(Fonts.tiny(), name, (p0.x + xOff), (p0.y - 6 + yOff), Colors.getColor(185, (int) opacity.getOpacity()));
        Depth.post();
        GlStateManager.popMatrix();

        if (p0.dragging) {
            double percent = Math.max(0, Math.min(1, p0.dragX / trackW));
            double newVal = min + (max - min) * percent;
            if (v instanceof Integer) {
                int intVal = (int) Math.round(newVal);
                p0.setting.setValue(intVal);
            } else if (v instanceof Float) {
                p0.setting.setValue((float) newVal);
            } else {
                p0.setting.setValue(newVal);
            }
            double mouseDiff = (p2 - p0.lastDragX);
            p0.dragX = p0.dragX + mouseDiff;
            p0.lastDragX = p2;
        }

        Fonts.draw(Fonts.tiny(), valueStr, valueX, (p0.y - 6 + yOff), Colors.getColor(220, (int) opacity.getOpacity()));
        GlStateManager.popMatrix();
    }

    @Override
    public void categoryButtonMouseReleased(CategoryButton categoryButton, int x, int y, int button) {
        if (categoryButton == null || categoryButton.categoryPanel == null || !categoryButton.categoryPanel.visible) return;
        categoryButton.categoryPanel.mouseReleased(x, y, button);
    }

    @Override
    public void slButtonDraw(SLButton slButton, float x, float y, epilogue.ui.clickgui.exhibition.components.MainPanel panel) {
    }

    @Override
    public void slButtonMouseClicked(SLButton slButton, float x, float y, int button, epilogue.ui.clickgui.exhibition.components.MainPanel panel) {
    }

    @Override
    public void colorConstructor(ColorPreview colorPreview, float x, float y) {
        if (colorPreview == null || colorPreview.colorObject == null) return;
        if (colorPreview.sliders == null) return;
        colorPreview.sliders.clear();
        colorPreview.sliders.add(new HSVColorPicker(x + 10, y, colorPreview, colorPreview.colorObject));
    }

    @Override
    public void colorPrewviewDraw(ColorPreview colorPreview, float x, float y) {
        if (colorPreview == null || colorPreview.categoryPanel == null) return;
        float xOff = colorPreview.x + colorPreview.categoryPanel.panel.dragX;
        float yOff = colorPreview.y + colorPreview.categoryPanel.panel.dragY + 75;

        RenderingUtil.rectangleBordered(xOff - 80, yOff - 6, xOff + 1, yOff + 46, 0.3, Colors.getColor(48, (int) opacity.getOpacity()), Colors.getColor(10, (int) opacity.getOpacity()));
        RenderingUtil.rectangle(xOff - 79, yOff - 5, xOff, yOff + 45, Colors.getColor(17, (int) opacity.getOpacity()));

        float labelW = Fonts.width(Fonts.tiny(), colorPreview.colorName);
        RenderingUtil.rectangle(xOff - 74, yOff - 6, xOff - 73 + labelW + 1, yOff - 4, Colors.getColor(17, (int) opacity.getOpacity()));
        Fonts.drawWithShadow(Fonts.tiny(), colorPreview.colorName, xOff - 73, yOff - 8, Colors.getColor(255, (int) opacity.getOpacity()));

        ColorValue cv = resolveLiveColorValue(colorPreview);
        if (cv != null) {
            int rgb = cv.getValue();
            int a = (rgb >> 24) & 0xFF;
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            colorPreview.colorObject.setRed(r);
            colorPreview.colorObject.setGreen(g);
            colorPreview.colorObject.setBlue(b);
            colorPreview.colorObject.setAlpha(a);
        }

        if (colorPreview.sliders != null && !colorPreview.sliders.isEmpty()) {
            colorPreview.sliders.get(0).draw(x, y);
        }
    }

    private ColorValue resolveLiveColorValue(ColorPreview preview) {
        if (preview == null) return null;
        Class<?> moduleClass = colorModuleClassByPreview.get(preview);
        String valueName = colorValueNameByPreview.get(preview);
        if (moduleClass == null || valueName == null) return colorValueByPreview.get(preview);
        if (Epilogue.valueHandler == null || Epilogue.valueHandler.properties == null) return colorValueByPreview.get(preview);
        java.util.List<Value<?>> values = Epilogue.valueHandler.properties.get(moduleClass);
        if (values == null) return colorValueByPreview.get(preview);
        for (Value<?> v : values) {
            if (v instanceof ColorValue && Objects.equals(v.getName(), valueName)) {
                return (ColorValue) v;
            }
        }
        return colorValueByPreview.get(preview);
    }

    @Override
    public void colorPickerConstructor(HSVColorPicker slider, float x, float y) {
        if (slider == null) return;
        ColorObject co = slider.colorPreview.colorObject;
        int argb = Colors.getColor(co.getRed(), co.getGreen(), co.getBlue(), co.getAlpha());
        Color color = new Color(argb, true);
        slider.opacity = (float) co.getAlpha() / 255f;
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        slider.hue = hsb[0];
        slider.saturation = hsb[1];
        slider.brightness = hsb[2];
    }

    @Override
    public void colorPickerDraw(HSVColorPicker slider, float x, float y) {
        if (slider == null || slider.colorPreview == null || slider.colorPreview.categoryPanel == null) return;
        float xOff = slider.x + slider.colorPreview.categoryPanel.panel.dragX - 85.5f;
        float yOff = slider.y + slider.colorPreview.categoryPanel.panel.dragY + 74;

        if (slider.selectingOpacity) {
            float tempY = y;
            if (tempY > yOff + 42) tempY = yOff + 42;
            else if (tempY < yOff) tempY = yOff;
            tempY -= yOff;
            slider.opacity = tempY / 42;
        }
        if (slider.selectingHue) {
            float tempY = y;
            if (tempY > yOff + 42) tempY = yOff + 42;
            else if (tempY < yOff) tempY = yOff;
            tempY -= yOff;
            slider.hue = tempY / 42;
        }
        if (slider.selectingColor) {
            float tempY = y;
            float tempX = x;
            if (tempY > yOff + 43) tempY = yOff + 43;
            else if (tempY < yOff) tempY = yOff;
            tempY -= yOff;
            if (tempX > xOff + 43) tempX = xOff + 43;
            else if (tempX < xOff) tempX = xOff;
            tempX -= xOff;
            slider.brightness = 1 - tempY / 43;
            slider.saturation = tempX / 43;
        }

        RenderingUtil.rectangle(xOff, yOff, xOff + 43, yOff + 43, Colors.getColor(32, (int) opacity.getOpacity()));
        Depth.pre();
        Depth.mask();
        RenderingUtil.rectangle(xOff + 0.5, yOff + 0.5, xOff + 42.5, yOff + 42.5, -1);
        Depth.render();
        RenderingUtil.drawGradientSideways(xOff + 0.5, yOff + 0.5, xOff + 46.5f, yOff + 42.5, Colors.getColor(255, (int) opacity.getOpacity()), Colors.getColorOpacity(MathHelper.hsvToRGB(slider.hue, 1, 1), (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(xOff + 0.5, yOff - 4, xOff + 42.5, yOff + 42.5, Colors.getColor(0, 0), Colors.getColor(0, (int) opacity.getOpacity()));
        Depth.post();
        RenderingUtil.rectangleBordered(xOff + (42.5 * slider.saturation) - 1, yOff + 42.5 - (42.5 * slider.brightness) - 1, xOff + (42.5 * slider.saturation) + 1, yOff + 42.5 - (42.5 * slider.brightness) + 1, 0.5, Colors.getColorOpacity(MathHelper.hsvToRGB(slider.hue, slider.saturation, slider.brightness), (int) opacity.getOpacity()), Colors.getColor(0, (int) opacity.getOpacity()));

        RenderingUtil.rectangle(xOff + 45, yOff, xOff + 48, yOff + 43, Colors.getColor(32, (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(xOff + 45.5f, yOff + 0.5f, xOff + 47.5f, yOff + 8, Colors.getColorOpacity(MathHelper.hsvToRGB(0, 1, 1), (int) opacity.getOpacity()), Colors.getColorOpacity(MathHelper.hsvToRGB(0.2f, 1, 1), (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(xOff + 45.5f, yOff + 8, xOff + 47.5f, yOff + 13, Colors.getColorOpacity(MathHelper.hsvToRGB(0.2f, 1, 1), (int) opacity.getOpacity()), Colors.getColorOpacity(MathHelper.hsvToRGB(0.3f, 1, 1), (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(xOff + 45.5f, yOff + 13, xOff + 47.5f, yOff + 17, Colors.getColorOpacity(MathHelper.hsvToRGB(0.3f, 1, 1), (int) opacity.getOpacity()), Colors.getColorOpacity(MathHelper.hsvToRGB(0.4f, 1, 1), (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(xOff + 45.5f, yOff + 17, xOff + 47.5f, yOff + 22, Colors.getColorOpacity(MathHelper.hsvToRGB(0.4f, 1, 1), (int) opacity.getOpacity()), Colors.getColorOpacity(MathHelper.hsvToRGB(0.5f, 1, 1), (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(xOff + 45.5f, yOff + 22, xOff + 47.5f, yOff + 26, Colors.getColorOpacity(MathHelper.hsvToRGB(0.5f, 1, 1), (int) opacity.getOpacity()), Colors.getColorOpacity(MathHelper.hsvToRGB(0.6f, 1, 1), (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(xOff + 45.5f, yOff + 26, xOff + 47.5f, yOff + 30, Colors.getColorOpacity(MathHelper.hsvToRGB(0.6f, 1, 1), (int) opacity.getOpacity()), Colors.getColorOpacity(MathHelper.hsvToRGB(0.7f, 1, 1), (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(xOff + 45.5f, yOff + 30, xOff + 47.5f, yOff + 34, Colors.getColorOpacity(MathHelper.hsvToRGB(0.7f, 1, 1), (int) opacity.getOpacity()), Colors.getColorOpacity(MathHelper.hsvToRGB(0.8f, 1, 1), (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(xOff + 45.5f, yOff + 34, xOff + 47.5f, yOff + 42.5, Colors.getColorOpacity(MathHelper.hsvToRGB(0.8f, 1, 1), (int) opacity.getOpacity()), Colors.getColorOpacity(MathHelper.hsvToRGB(1f, 1, 1), (int) opacity.getOpacity()));
        RenderingUtil.rectangleBordered(xOff + 45, yOff + (42.5 * slider.hue) - 1.5f, xOff + 48, yOff + (42.5 * slider.hue) + 1.5f, 0.5f, Colors.getColor(0, (int) opacity.getOpacity()), Colors.getColor(slider.selectingHue ? 255 : 200, (int) opacity.getOpacity()));

        ColorObject co = slider.colorPreview.colorObject;
        int curr = Colors.getColor(co.getRed(), co.getGreen(), co.getBlue(), co.getAlpha());
        int cr = (curr >> 16) & 0xFF;
        int cg = (curr >> 8) & 0xFF;
        int cb = curr & 0xFF;
        RenderingUtil.rectangleBordered(xOff + 50, yOff, xOff + 53, yOff + 43, 0.5f, Colors.getColor(cr, cg, cb, (int) opacity.getOpacity()), Colors.getColor(32, (int) opacity.getOpacity()));
        RenderingUtil.rectangleBordered(xOff + 50, yOff + (42.5 * slider.opacity) - 1.5f, xOff + 53, yOff + (42.5 * slider.opacity) + 1.5f, 0.5f, Colors.getColor(0, (int) opacity.getOpacity()), Colors.getColor(slider.selectingOpacity ? 255 : 200, (int) opacity.getOpacity()));

        boolean shouldUpdate = slider.selectingHue || slider.selectingColor || slider.selectingOpacity;
        if (shouldUpdate) {
            Color tcolor = Color.getHSBColor(slider.hue, slider.saturation, slider.brightness);
            int a = (int) (255 * slider.opacity);
            ColorValue cv = resolveLiveColorValue(slider.colorPreview);
            if (cv != null) {
                cv.setHue(slider.hue);
                cv.setSaturation(slider.saturation);
                cv.setBrightness(slider.brightness);
            }
            slider.colorPreview.colorObject.setRed(tcolor.getRed());
            slider.colorPreview.colorObject.setGreen(tcolor.getGreen());
            slider.colorPreview.colorObject.setBlue(tcolor.getBlue());
            slider.colorPreview.colorObject.setAlpha(a);
        } else {
            ColorValue cv = resolveLiveColorValue(slider.colorPreview);
            int rgb = cv != null ? cv.getValue() : Colors.getColor(slider.colorPreview.colorObject.getRed(), slider.colorPreview.colorObject.getGreen(), slider.colorPreview.colorObject.getBlue(), slider.colorPreview.colorObject.getAlpha());
            int a = (rgb >> 24) & 0xFF;
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            slider.opacity = a / 255f;
            slider.hue = hsb[0];
            slider.saturation = hsb[1];
            slider.brightness = hsb[2];
        }

        if (x > xOff + 57 && y > yOff && x < xOff + 72 && y < yOff + 30) {
            String hex = String.format("#%02X%02X%02X%02X", slider.colorPreview.colorObject.getAlpha(), slider.colorPreview.colorObject.getRed(), slider.colorPreview.colorObject.getGreen(), slider.colorPreview.colorObject.getBlue());
            Fonts.drawWithShadow(Fonts.tiny(), hex + String.format(" rgba(%d, %d, %d, %d)", slider.colorPreview.colorObject.getRed(), slider.colorPreview.colorObject.getGreen(), slider.colorPreview.colorObject.getBlue(), slider.colorPreview.colorObject.getAlpha()),
                    (slider.colorPreview.categoryPanel.panel.x + 2 + slider.colorPreview.categoryPanel.panel.dragX) + 55,
                    (slider.colorPreview.categoryPanel.panel.y + 9 + slider.colorPreview.categoryPanel.panel.dragY),
                    Colors.getColor(255, (int) opacity.getOpacity()));
        }

        RenderingUtil.rectangle(xOff + 57, yOff, xOff + 72, yOff + 30, Colors.getColor(255, (int) opacity.getOpacity()));
        boolean offset = false;
        for (int yThing = 0; yThing < 30; yThing += 1) {
            for (int i = offset ? 0 : 1; i < 15; i += 2) {
                RenderingUtil.rectangle(xOff + 57 + i, yOff + yThing, xOff + 57 + i + 1, yOff + yThing + 1, Colors.getColor(190, (int) opacity.getOpacity()));
            }
            offset = !offset;
        }

        float scale = (float) (opacity.getOpacity() / 255f);
        int colorXD = Colors.getColor(slider.colorPreview.colorObject.getRed(), slider.colorPreview.colorObject.getGreen(), slider.colorPreview.colorObject.getBlue(), (int) (slider.colorPreview.colorObject.getAlpha() * scale));
        RenderingUtil.rectangleBordered(xOff + 59, yOff + 2, xOff + 70, yOff + 28, 0.5f, colorXD, Colors.getColor(0, (int) opacity.getOpacity()));
        GlStateManager.pushMatrix();
        GlStateManager.translate(xOff + 65, yOff + 33, 0);
        GlStateManager.scale(0.5, 0.5, 0.5);
        GlStateManager.enableBlend();
        mc.fontRendererObj.drawStringWithShadow("Copy", 0 - mc.fontRendererObj.getStringWidth("Copy") / 2F, 0, Colors.getColor(255, (int) opacity.getOpacity()));
        mc.fontRendererObj.drawStringWithShadow("Paste", 0 - mc.fontRendererObj.getStringWidth("Paste") / 2F, 12, Colors.getColor(255, (int) opacity.getOpacity()));
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    public void colorPickerClick(HSVColorPicker slider, float x, float y, int mouse) {
        if (slider == null || slider.colorPreview == null || slider.colorPreview.categoryPanel == null) return;
        if (!slider.colorPreview.categoryPanel.enabled) return;
        float xOff = slider.x + slider.colorPreview.categoryPanel.panel.dragX - 85.5f;
        float yOff = slider.y + slider.colorPreview.categoryPanel.panel.dragY + 74;

        if (mouse == 0) {
            try {
                if (hovering(x, y, xOff + 59, yOff + 33, 12, 4)) {
                    String hex = String.format("#%02X%02X%02X%02X", slider.colorPreview.colorObject.getAlpha(), slider.colorPreview.colorObject.getRed(), slider.colorPreview.colorObject.getGreen(), slider.colorPreview.colorObject.getBlue());
                    paste(hex);
                }
                if (hovering(x, y, xOff + 59, yOff + 39, 12, 4)) {
                    String hex = copy().trim();
                    if (!hex.isEmpty()) {
                        String s = hex.replace("#", "").replace("0x", "").trim();
                        if (s.length() >= 6) {
                            String digits = "0123456789ABCDEF";
                            s = s.toUpperCase();
                            int hexValue = 0;
                            for (int i = 0; i < s.length(); i++) {
                                char c = s.charAt(i);
                                int d = digits.indexOf(c);
                                hexValue = 16 * hexValue + d;
                            }

                            int alpha = (hexValue >> 24) & 0xFF;
                            if (s.length() < 8) alpha = 255;
                            int red = (hexValue >> 16) & 0xFF;
                            int green = (hexValue >> 8) & 0xFF;
                            int blue = (hexValue & 0xFF);
                            slider.opacity = alpha / 255f;
                            float[] hsb = Color.RGBtoHSB(red, green, blue, null);
                            slider.hue = hsb[0];
                            slider.saturation = hsb[1];
                            slider.brightness = hsb[2];

                            ColorValue cv = colorValueByPreview.get(slider.colorPreview);
                            if (cv != null) {
                                cv.setHue(slider.hue);
                                cv.setSaturation(slider.saturation);
                                cv.setBrightness(slider.brightness);
                                cv.setValue((alpha << 24) | (cv.getValue() & 0xFFFFFF));
                            }
                            slider.colorPreview.colorObject.setRed(red);
                            slider.colorPreview.colorObject.setGreen(green);
                            slider.colorPreview.colorObject.setBlue(blue);
                            slider.colorPreview.colorObject.setAlpha(alpha);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (!slider.selectingHue && !slider.selectingColor && !slider.selectingOpacity && mouse == 0) {
            if (hovering(x, y, xOff + 50, yOff, 3, 43)) slider.selectingOpacity = true;
            if (hovering(x, y, xOff + 45, yOff, 3, 43)) slider.selectingHue = true;
            if (hovering(x, y, xOff, yOff, 43, 43)) slider.selectingColor = true;
        }

        if (mouse == 0 && (slider.selectingHue || slider.selectingColor || slider.selectingOpacity)) {
            colorPickerDraw(slider, x, y);
        }
    }

    private static String copy() {
        String ret = "";
        Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable clipTf = sysClip.getContents(null);
        if (clipTf != null && clipTf.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                ret = (String) clipTf.getTransferData(DataFlavor.stringFlavor);
            } catch (Exception ignored) {
            }
        }
        return ret;
    }

    private static void paste(String writeMe) {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable tText = new StringSelection(writeMe);
        clip.setContents(tText, null);
    }

    @Override
    public void colorPickerMovedOrUp(HSVColorPicker slider, float x, float y, int mouse) {
        if (mouse == 0 && slider != null && (slider.selectingHue || slider.selectingColor || slider.selectingOpacity)) {
            slider.selectingOpacity = false;
            slider.selectingColor = false;
            slider.selectingHue = false;
        }
    }

    private boolean hovering(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
    }

    @Override
    public void configButtonDraw(ConfigButton configButton, float x, float y) {
        if (configButton == null || configButton.configList == null || configButton.configList.categoryPanel == null) return;
        CategoryPanel panel = configButton.configList.categoryPanel;
        if (!panel.visible) return;

        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        boolean hovering = x >= xOff + configButton.x && y >= yOff + configButton.y && x <= xOff + configButton.x + 60 && y <= yOff + configButton.y + 9;

        RenderingUtil.rectangle(configButton.x + xOff - 0.3, configButton.y + yOff - 0.3, configButton.x + xOff + 60 + 0.3, configButton.y + yOff + 9 + 0.3, Colors.getColor(10, 255));
        RenderingUtil.drawGradient(configButton.x + xOff, configButton.y + yOff, configButton.x + xOff + 60, configButton.y + yOff + 9, Colors.getColor(31, 255), Colors.getColor(36, 255));
        if (hovering) {
            RenderingUtil.rectangleBordered(configButton.x + xOff, configButton.y + yOff, configButton.x + xOff + 60, configButton.y + yOff + 9, 0.3, Colors.getColor(0, 0), Colors.getColor(90, 255));
        }
        Fonts.draw(Fonts.tiny(), configButton.buttonType.name(), configButton.x + xOff + 3, configButton.y + yOff + 2, Colors.getColor(185, 255));
    }

    @Override
    public void configButtonMouseClicked(ConfigButton configButton, float x, float y, int button) {
        if (configButton == null || configButton.configList == null || configButton.configList.categoryPanel == null) return;
        CategoryPanel panel = configButton.configList.categoryPanel;
        if (!panel.visible) return;

        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        boolean hovering = x >= xOff + configButton.x && y >= yOff + configButton.y && x <= xOff + configButton.x + 60 && y <= yOff + configButton.y + 9;
        if (!hovering || button != 0) return;

        ConfigList list = configButton.configList;
        String selected = (list.selectedConfigID >= 0 && list.configs != null && list.selectedConfigID < list.configs.length)
                ? list.configs[list.selectedConfigID]
                : null;

        if (configButton.buttonType == ConfigButton.ButtonType.LOAD) {
            if (selected != null) new Config(selected).load();
        } else if (configButton.buttonType == ConfigButton.ButtonType.SAVE) {
            if (selected != null) new Config(selected).save();
        } else if (configButton.buttonType == ConfigButton.ButtonType.DELETE) {
            if (selected != null) {
                File f = new File("./Epilogue/", selected + ".json");
                if (f.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
                refreshConfigs(list);
            }
        } else if (configButton.buttonType == ConfigButton.ButtonType.CREATE) {
            if (list.configTextBox != null) {
                String name = list.configTextBox.textString;
                if (name != null) {
                    name = name.trim();
                }
                if (name != null && !name.isEmpty()) {
                    new Config(name, true);
                    refreshConfigs(list);
                }
            }
        } else if (configButton.buttonType == ConfigButton.ButtonType.OPEN_FOLDER) {
            try {
                File dir = new File("./Epilogue/");
                if (!dir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    dir.mkdirs();
                }
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(dir);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void configListDraw(ConfigList configList, float x, float y) {
        if (configList == null || configList.categoryPanel == null) return;
        CategoryPanel panel = configList.categoryPanel;
        if (!panel.visible) return;

        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;

        if (panel.configTextBox != null) {
            panel.configTextBox.draw(x, y);
        }

        float listX = configList.x + xOff;
        float listY = configList.y + yOff;
        float listW = 84;
        float listH = 50;

        RenderingUtil.rectangle(listX - 0.3f, listY - 0.3f, listX + listW + 0.3f, listY + listH + 0.3f, Colors.getColor(10, 255));
        RenderingUtil.drawGradient(listX, listY, listX + listW, listY + listH, Colors.getColor(31, 255), Colors.getColor(36, 255));

        Depth.pre();
        Depth.mask();
        RenderingUtil.rectangle(listX + 1, listY + 1, listX + listW - 1, listY + listH - 1, Colors.getColor(0, 0));
        Depth.render();

        int startY = 2;
        int idx = 0;
        if (configList.configs != null) {
            for (String cfg : configList.configs) {
                float yy = listY + startY + (idx * 9) - configList.amountScrolled;
                if (yy > listY + listH || yy < listY - 9) {
                    idx++;
                    continue;
                }
                boolean selected = idx == configList.selectedConfigID;
                if (selected) {
                    RenderingUtil.rectangle(listX + 1, yy, listX + listW - 1, yy + 9, Colors.getColor(255, 20));
                }
                Fonts.draw(Fonts.tiny(), cfg, listX + 3, yy + 1, Colors.getColor(185, 255));
                idx++;
            }
        }

        Depth.post();
    }

    @Override
    public void configListMouseClicked(ConfigList configList, float x, float y, int button) {
        if (configList == null || configList.categoryPanel == null) return;
        CategoryPanel panel = configList.categoryPanel;
        if (!panel.visible) return;

        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;

        if (panel.configTextBox != null) {
            panel.configTextBox.mouseClicked((int) x, (int) y, button);
        }

        float listX = configList.x + xOff;
        float listY = configList.y + yOff;
        float listW = 84;
        float listH = 50;
        boolean hovering = x >= listX && y >= listY && x <= listX + listW && y <= listY + listH;
        if (hovering && button == 0) {
            int relY = (int) (y - listY + configList.amountScrolled);
            int idx = relY / 9;
            if (configList.configs != null && idx >= 0 && idx < configList.configs.length) {
                configList.selectedConfigID = idx;
            }
        }
    }

    @Override
    public void textBoxDraw(TextBox textBox, float x, float y) {
        CategoryPanel panel = textBox.panel;
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        if (textBox.cursorPos > textBox.textString.length()) {
            textBox.cursorPos = textBox.textString.length();
        } else if (textBox.cursorPos < 0) {
            textBox.cursorPos = 0;
        }
        if (!textBox.isFocused && !textBox.isTyping && !textBox.textString.equals(String.valueOf(textBox.setting.getValue()))) {
            textBox.textString = String.valueOf(textBox.setting.getValue());
        }
        int selectedChar = textBox.cursorPos;
        boolean hovering = (x >= xOff + textBox.x) && (y >= yOff + textBox.y)
                && (x <= xOff + textBox.x + 84) && (y <= yOff + textBox.y + 9);

        RenderingUtil.rectangle(textBox.x + xOff - 0.3, textBox.y + yOff - 0.3, textBox.x + xOff + 84 + 0.3, textBox.y + yOff + 7.5F + 0.3, Colors.getColor(10, (int) opacity.getOpacity()));
        RenderingUtil.drawGradient(textBox.x + xOff, textBox.y + yOff, textBox.x + xOff + 84, textBox.y + yOff + 7.5F, Colors.getColor(31, (int) opacity.getOpacity()), Colors.getColor(36, (int) opacity.getOpacity()));
        if (hovering || textBox.isFocused) {
            RenderingUtil.rectangleBordered(textBox.x + xOff, textBox.y + yOff, textBox.x + xOff + 84, textBox.y + yOff + 7.5F, 0.3, Colors.getColor(0, 0), textBox.isFocused ? Colors.getColor(130, (int) opacity.getOpacity()) : Colors.getColor(90, (int) opacity.getOpacity()));
        }
        String label = textBox.setting.getName().charAt(0) + textBox.setting.getName().toLowerCase().substring(1);
        Fonts.draw(Fonts.tiny(), label, (textBox.x + xOff + 1), (textBox.y - 6 + yOff), Colors.getColor(185, (int) opacity.getOpacity()));

        Depth.pre();
        Depth.mask();
        RenderingUtil.rectangle(textBox.x + xOff + 2, textBox.y + yOff, textBox.x + xOff + 82, textBox.y + yOff + 7.5F, Colors.getColor(90, (int) opacity.getOpacity()));
        Depth.render();
        Fonts.draw(Fonts.tiny(), textBox.textString, (textBox.x + 1.5F + xOff) - textBox.offset, (textBox.y + 2 + yOff), Colors.getColor(151, (int) opacity.getOpacity()));
        Depth.post();

        textBox.opacity.interp(textBox.backwards ? 40 : 270, 7);
        if (textBox.opacity.getOpacity() >= 270) {
            textBox.backwards = true;
        } else if (textBox.opacity.getOpacity() <= 40) {
            textBox.backwards = false;
        }

        if (textBox.isFocused) {
            float width = Fonts.width(Fonts.tiny(), textBox.textString.substring(0, Math.min(selectedChar, textBox.textString.length())));
            float posX = textBox.x + xOff + 1.5f + width - textBox.offset;
            RenderingUtil.rectangle(posX - 0.5, textBox.y + yOff + 1.5, posX, textBox.y + yOff + 6, Colors.getColor(220, (int) textBox.opacity.getOpacity()));
        } else {
            textBox.opacity.setOpacity(255);
        }
    }

    @Override
    public void textBoxMouseClicked(TextBox textBox, int x, int y, int mouseID) {
        CategoryPanel panel = textBox.panel;
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        boolean hovering = (x >= xOff + textBox.x) && (y >= yOff + textBox.y) && (x <= xOff + textBox.x + 84) && (y <= yOff + textBox.y + 9);

        if (hovering && mouseID == 0 && !textBox.isFocused) {
            float width = Fonts.width(Fonts.tiny(), textBox.textString.substring(0, Math.min(textBox.cursorPos, textBox.textString.length())));
            float barOffset = (width - textBox.offset);
            if (barOffset < 0) {
                textBox.offset += barOffset;
            }
            if (barOffset > 82) {
                textBox.offset += (barOffset - 82);
            }

            textBox.isFocused = true;
            Keyboard.enableRepeatEvents(true);

            String currentString = textBox.textString;
            float mouseOffsetAdjusted = x - (xOff + textBox.x) - 2;
            textBox.cursorPos = currentString.length();
            int len = currentString.length();
            for (int i = 0; i <= len; i++) {
                if (Fonts.width(Fonts.tiny(), currentString.substring(0, i)) >= textBox.offset + mouseOffsetAdjusted) {
                    textBox.cursorPos = i;
                    break;
                }
            }
        } else {
            if (!hovering) {
                textBox.isFocused = false;
                textBox.isTyping = false;
            } else if (mouseID == 0) {
                String currentString = textBox.textString;
                float mouseOffsetAdjusted = x - (xOff + textBox.x) - 2.5F;
                textBox.cursorPos = currentString.length();
                int len = currentString.length();
                for (int i = 0; i <= len; i++) {
                    if (Fonts.width(Fonts.tiny(), currentString.substring(0, i)) >= textBox.offset + mouseOffsetAdjusted) {
                        textBox.cursorPos = i;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void textBoxKeyPressed(TextBox textBox, int key) {
        char letter = Keyboard.getEventCharacter();
        if (letter == '\r') {
            textBox.isFocused = false;
            textBox.isTyping = false;
            textBox.setting.setValue(textBox.textString);
            if (textBox.setting.getBacking() instanceof TextValue) {
                ((TextValue) textBox.setting.getBacking()).setValue(textBox.textString);
            }
            return;
        }

        if (textBox.isFocused) {
            switch (key) {
                case Keyboard.KEY_LEFT:
                    if (textBox.cursorPos > 0) textBox.cursorPos--;
                    break;
                case Keyboard.KEY_RIGHT:
                    if (textBox.cursorPos < textBox.textString.length()) textBox.cursorPos++;
                    break;
                case Keyboard.KEY_BACK:
                    if (textBox.textString.length() > 0 && textBox.cursorPos > 0) {
                        textBox.textString = textBox.textString.substring(0, textBox.cursorPos - 1) + textBox.textString.substring(textBox.cursorPos);
                        textBox.cursorPos--;
                    }
                    break;
                case Keyboard.KEY_ESCAPE:
                    textBox.isFocused = false;
                    textBox.isTyping = false;
                    Keyboard.enableRepeatEvents(false);
                    return;
            }
        }

        if (textBox.isFocused && letter >= 32 && letter != 127) {
            if (!Keyboard.areRepeatEventsEnabled()) Keyboard.enableRepeatEvents(true);
            if (!textBox.isTyping) textBox.isTyping = true;
            String old = textBox.textString;
            StringBuilder sb = new StringBuilder(old);
            sb.insert(textBox.cursorPos, letter);
            textBox.textString = sb.toString();
            textBox.cursorPos++;
            textBox.setting.setValue(textBox.textString);
        }
    }

    @Override
    public void configHandleMouseInput(ConfigList configList) {
        if (configList == null || configList.categoryPanel == null) return;
        CategoryPanel panel = configList.categoryPanel;
        if (!panel.visible) return;

        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        float delta = wheel > 0 ? -9f : 9f;
        configList.amountScrolled = Math.max(0, configList.amountScrolled + delta);
    }

    private void refreshConfigs(ConfigList list) {
        if (list == null) return;
        File dir = new File("./Epilogue/");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        File[] files = dir.listFiles((d, name) -> name != null && name.toLowerCase().endsWith(".json"));
        if (files == null) {
            list.configs = new String[]{};
            list.selectedConfigID = -1;
            return;
        }
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            String n = files[i].getName();
            if (n.toLowerCase().endsWith(".json")) {
                n = n.substring(0, n.length() - 5);
            }
            names[i] = n;
        }
        Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
        list.configs = names;
        if (list.selectedConfigID >= list.configs.length) list.selectedConfigID = list.configs.length - 1;
        if (list.selectedConfigID < 0 && list.configs.length > 0) list.selectedConfigID = 0;
    }
}