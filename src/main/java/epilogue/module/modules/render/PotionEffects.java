package epilogue.module.modules.render;

import epilogue.Epilogue;
import epilogue.ui.chat.GuiChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import epilogue.font.CustomFontRenderer;
import epilogue.font.FontTransformer;
import epilogue.module.Module;
import epilogue.util.render.ColorUtil;
import epilogue.util.render.PostProcessing;
import epilogue.util.render.RenderUtil;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class PotionEffects extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private float currentHeight = 0.0f;

    private final ResourceLocation inventoryTexture = new ResourceLocation("textures/gui/container/inventory.png");

    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Epilogue", "Exhibition"});
    public final IntValue round = new IntValue("Round", 13, 0, 30, () -> this.mode.getValue() == 0);

    public PotionEffects() {
        super("PotionEffects", false);
    }

    public void renderAt(float x, float y) {
        if ("Exhibition".equalsIgnoreCase(mode.getModeString())) {
            renderAtExhibition(x, y);
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Collection<PotionEffect> active = mc.thePlayer.getActivePotionEffects();
        boolean preview = (mc.currentScreen instanceof net.minecraft.client.gui.GuiChat) || (mc.currentScreen instanceof GuiChat);

        List<PotionEffect> potions;
        if ((active == null || active.isEmpty()) && preview) {
            potions = new ArrayList<>();
            potions.add(new PotionEffect(Potion.moveSpeed.id, 20 * 120, 1));
            potions.add(new PotionEffect(Potion.damageBoost.id, 20 * 45, 0));
            potions.add(new PotionEffect(Potion.fireResistance.id, 20 * 300, 0));
        } else if (active == null || active.isEmpty()) {
            potions = new ArrayList<>();
        } else {
            potions = new ArrayList<>(active);
            potions.sort(Comparator.comparingInt(PotionEffect::getDuration).reversed());
        }

        FontTransformer transformer = FontTransformer.getInstance();
        Font font = transformer.getFont("OpenSansSemiBold", 32);
        if (font == null) {
            font = transformer.getFont("Arial", 32);
        }
        if (font == null) return;

        float padding = 5f;
        float fontHeight = CustomFontRenderer.getFontHeight(font);
        String title = "Potions";
        float titleWidth = CustomFontRenderer.getStringWidth(title, font);

        float maxWidth = titleWidth + padding * 2;
        float listHeight = 0f;

        for (PotionEffect effect : potions) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) continue;
            String potionName = I18n.format(potion.getName());
            String amp = effect.getAmplifier() > 0 ? " " + I18n.format("enchantment.level." + (effect.getAmplifier() + 1)) : "";
            String nameText = potionName + amp;
            String durationText = Potion.getDurationString(effect);

            float nameW = CustomFontRenderer.getStringWidth(nameText, font);
            float durW = CustomFontRenderer.getStringWidth(durationText, font);
            float localWidth = nameW + durW + padding * 3 + 12f;

            if (localWidth > maxWidth) maxWidth = localWidth;
            listHeight += fontHeight + padding;
        }

        float width = Math.max(maxWidth, 80f);
        float headerHeight = fontHeight + padding * 2;
        float targetHeight = headerHeight + 1.25f + 7.5f + (potions.isEmpty() ? padding : listHeight) + 4.5f;

        if (currentHeight <= 0.0f) {
            currentHeight = targetHeight;
        } else {
            float speed = 0.22f;
            currentHeight += (targetHeight - currentHeight) * speed;
        }

        float height = Math.max(headerHeight, currentHeight);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        Framebuffer bloomBuffer = PostProcessing.beginBloom();
        if (bloomBuffer != null) {
            int r = Math.max(0, round.getValue());
            RenderUtil.drawRoundedRect(x, y, width, height, r, epilogue.module.modules.render.PostProcessing.getBloomColor());
            mc.getFramebuffer().bindFramebuffer(false);
        }

        int r = Math.max(0, round.getValue());
        if (r > 0) {
            PostProcessing.drawBlur(x, y, x + width, y + height, () -> () -> RenderUtil.drawRoundedRect(x, y, width, height, r, -1));
        } else {
            PostProcessing.drawBlurRect(x, y, x + width, y + height);
        }

        Interface interfaceModule = (Interface) Epilogue.moduleManager.getModule("Interface");
        int accent = interfaceModule != null ? interfaceModule.color(0) : 0xFF80FF95;

        Color bg = new Color(0, 0, 0, 130);

        RenderUtil.drawRoundedRect(x, y, width, height, r, bg);

        float titleX = x + width / 2f - titleWidth / 2f;
        CustomFontRenderer.drawStringWithShadow(title, titleX, y + padding + 2f, 0xFFFFFFFF, font);

        float iconSize = 10f;
        float iconX = x + width - iconSize - padding;
        drawPotionHeaderIcon(iconX, y + 6f, iconSize, iconSize, accent);

        float dividerY = y + headerHeight;
        Color dividerColor = new Color(ColorUtil.darker(accent, 0.4f), true);
        RenderUtil.drawRect(x + 1.5f, dividerY + 1.5f, width - 3f, 1.25f, dividerColor.getRGB());

        float listY = dividerY + 7.5f;

        RenderUtil.scissorStart(x, y, width, height);

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        for (PotionEffect effect : potions) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) continue;

            String potionName = I18n.format(potion.getName());
            String amp = effect.getAmplifier() > 0 ? " " + I18n.format("enchantment.level." + (effect.getAmplifier() + 1)) : "";
            String nameText = potionName + amp;
            String durationText = Potion.getDurationString(effect);

            float durW = CustomFontRenderer.getStringWidth(durationText, font);

            float potionIconSize = 9f;
            float iconDrawX = x + padding;
            float iconDrawY = listY + 2f + (fontHeight - potionIconSize) / 2f - 2f;
            float textX = x + padding + potionIconSize + 4f;

            drawPotionStatusIcon(potion, iconDrawX, iconDrawY, potionIconSize);

            CustomFontRenderer.drawStringWithShadow(nameText, textX, listY + 2f, 0xFFFFFFFF, font);
            CustomFontRenderer.drawStringWithShadow(durationText, x + width - padding - durW, listY + 2f, 0xFFFFFFFF, font);

            listY += fontHeight + padding;
            if (listY > y + height - padding) break;
        }

        RenderUtil.scissorEnd();

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        PostProcessing.endBloom(bloomBuffer);
    }

    private void renderAtExhibition(float x, float y) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Collection<PotionEffect> active = mc.thePlayer.getActivePotionEffects();
        boolean preview = (mc.currentScreen instanceof net.minecraft.client.gui.GuiChat) || (mc.currentScreen instanceof GuiChat);
        if ((active == null || active.isEmpty()) && !preview) return;

        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        float widgetWidth = getCurrentWidthExhibition();
        boolean rightAligned = (x + widgetWidth) > (sr.getScaledWidth() / 2.0f);

        List<PotionEffect> potions;
        if (active == null || active.isEmpty()) {
            potions = new ArrayList<>();
            potions.add(new PotionEffect(Potion.moveSpeed.id, 20 * 120, 1));
            potions.add(new PotionEffect(Potion.damageBoost.id, 20 * 45, 0));
            potions.add(new PotionEffect(Potion.fireResistance.id, 20 * 300, 0));
        } else {
            potions = new ArrayList<>(active);
        }

        potions.sort(Comparator.comparingInt(PotionEffect::getDuration).reversed());

        float lineHeight = 9.0f;
        float widgetHeight = Math.max(20f, potions.size() * lineHeight);
        boolean bottomAligned = (y + widgetHeight) > (sr.getScaledHeight() / 2.0f);

        float yOff = 0.0f;
        for (PotionEffect potionEffect : potions) {
            Potion potionType = Potion.potionTypes[potionEffect.getPotionID()];
            if (potionType == null) continue;
            String potionName = I18n.format(potionType.getName());

            if (potionEffect.getAmplifier() == 1) {
                potionName = potionName + " II";
            } else if (potionEffect.getAmplifier() == 2) {
                potionName = potionName + " III";
            } else if (potionEffect.getAmplifier() == 3) {
                potionName = potionName + " IV";
            }

            String type = "";
            if (potionEffect.getDuration() < 600 && potionEffect.getDuration() > 300) {
                type = type + " §6" + Potion.getDurationString(potionEffect);
            } else if (potionEffect.getDuration() < 300) {
                type = type + " §c" + Potion.getDurationString(potionEffect);
            } else if (potionEffect.getDuration() > 600) {
                type = type + " §7" + Potion.getDurationString(potionEffect);
            }

            GlStateManager.pushMatrix();
            int nameColor = new Color(potionType.getLiquidColor()).getRGB();
            float drawY = bottomAligned ? (y + widgetHeight - lineHeight - yOff) : (y + yOff);
            if (rightAligned) {
                float lineWidth = mc.fontRendererObj.getStringWidth(type + potionName);
                float startX = x + (widgetWidth - lineWidth);
                mc.fontRendererObj.drawString(potionName, startX, drawY, nameColor, true);
                mc.fontRendererObj.drawString(type, startX + mc.fontRendererObj.getStringWidth(potionName), drawY, 0xFFFFFFFF, true);
            } else {
                mc.fontRendererObj.drawString(potionName, x, drawY, nameColor, true);
                mc.fontRendererObj.drawString(type, x + mc.fontRendererObj.getStringWidth(potionName), drawY, 0xFFFFFFFF, true);
            }
            GlStateManager.popMatrix();
            yOff += lineHeight;
        }
        currentHeight = widgetHeight;
    }

    private float getCurrentWidthExhibition() {
        Collection<PotionEffect> active = mc.thePlayer != null ? mc.thePlayer.getActivePotionEffects() : null;
        if (active == null || active.isEmpty()) return 80f;

        float max = 80f;
        for (PotionEffect potionEffect : active) {
            Potion potionType = Potion.potionTypes[potionEffect.getPotionID()];
            if (potionType == null) continue;
            String potionName = I18n.format(potionType.getName());
            if (potionEffect.getAmplifier() == 1) {
                potionName = potionName + " II";
            } else if (potionEffect.getAmplifier() == 2) {
                potionName = potionName + " III";
            } else if (potionEffect.getAmplifier() == 3) {
                potionName = potionName + " IV";
            }

            String type = "";
            if (potionEffect.getDuration() < 600 && potionEffect.getDuration() > 300) {
                type = type + " §6" + Potion.getDurationString(potionEffect);
            } else if (potionEffect.getDuration() < 300) {
                type = type + " §c" + Potion.getDurationString(potionEffect);
            } else if (potionEffect.getDuration() > 600) {
                type = type + " §7" + Potion.getDurationString(potionEffect);
            }

            float w = mc.fontRendererObj.getStringWidth(potionName + type);
            if (w > max) max = w;
        }
        return max;
    }

    public float getCurrentWidth() {
        if ("Exhibition".equalsIgnoreCase(mode.getModeString())) {
            return getCurrentWidthExhibition();
        }
        Collection<PotionEffect> active = mc.thePlayer != null ? mc.thePlayer.getActivePotionEffects() : null;

        FontTransformer transformer = FontTransformer.getInstance();
        Font font = transformer.getFont("OpenSansSemiBold", 32);

        if (active == null || active.isEmpty()) {
            float padding = 5f;
            String title = "Potions";
            float titleWidth = CustomFontRenderer.getStringWidth(title, font);
            return Math.max(titleWidth + padding * 2, 80f);
        }

        List<PotionEffect> potions = new ArrayList<>(active);
        potions.sort(Comparator.comparingInt(PotionEffect::getDuration).reversed());

        float max = 80f;
        for (PotionEffect potionEffect : potions) {
            Potion potionType = Potion.potionTypes[potionEffect.getPotionID()];
            if (potionType == null) continue;
            String potionName = I18n.format(potionType.getName());
            if (potionEffect.getAmplifier() > 0) {
                potionName = potionName + " " + I18n.format("enchantment.level." + (potionEffect.getAmplifier() + 1));
            }

            String durationText = Potion.getDurationString(potionEffect);

            float w = CustomFontRenderer.getStringWidth(potionName + durationText, font);
            if (w > max) max = w;
        }
        return max;
    }

    public float getCurrentHeight() {
        if ("Exhibition".equalsIgnoreCase(mode.getModeString())) {
            Collection<PotionEffect> active = mc.thePlayer != null ? mc.thePlayer.getActivePotionEffects() : null;
            int count = active == null ? 0 : active.size();
            return Math.max(20f, count * 9.0f);
        }
        return Math.max(20f, currentHeight);
    }

    private void drawPotionHeaderIcon(float x, float y, float w, float h, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();

        float cx = x + w / 2f;
        float cy = y + h / 2f;
        float r = w / 2.4f;
        Color c = new Color(color, true);
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= 20; i++) {
            double a = Math.PI * 2.0 * i / 20.0;
            GL11.glVertex2f((float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r));
        }
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawPotionStatusIcon(Potion potion, float x, float y, float size) {
        if (potion == null) return;
        if (!potion.hasStatusIcon()) return;

        int idx = potion.getStatusIconIndex();
        int u = (idx % 8) * 18;
        int v = 198 + (idx / 8) * 18;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(inventoryTexture);
        float scale = size / 18f;
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(scale, scale, 1f);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, u, v, 18, 18, 256, 256);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}