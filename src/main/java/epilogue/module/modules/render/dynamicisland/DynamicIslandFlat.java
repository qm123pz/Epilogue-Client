package epilogue.module.modules.render.dynamicisland;

import epilogue.Epilogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import epilogue.event.EventTarget;
import epilogue.events.Render2DEvent;
import epilogue.font.CustomFontRenderer;
import epilogue.font.FontTransformer;
import epilogue.module.modules.player.Scaffold;
import epilogue.module.modules.render.dynamicisland.notification.Notification;
import epilogue.module.modules.render.dynamicisland.notification.NotificationManager;
import epilogue.module.modules.render.dynamicisland.notification.ScaffoldData;
import epilogue.ui.clickgui.menu.MenuClickGui;
import epilogue.util.render.PostProcessing;
import epilogue.util.render.RoundedUtil;
import epilogue.util.render.RenderUtil;
import epilogue.util.shader.BloomShader;

import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DynamicIslandFlat {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private boolean midInited = false;
    private long lastFrameMs = 0L;
    private float midW = 0.0f;
    private float midH = 0.0f;
    private float midMix = 0.0f;

    private boolean latchedToggleActive = false;
    private long latchedToggleStart = 0L;
    private String latchedModuleName = "";
    private boolean latchedEnabled = false;

    private long lastLatchSwitchMs = 0L;

    private float scaffoldMix = 0.0f;
    private float scaffoldProgressAnim = 0.0f;

    private boolean mergeInited = false;
    private float mergeLeftEdge = 0.0f;
    private float mergeRightEdge = 0.0f;

    private float splitMix = 1.0f;

    private boolean clickGuiInited = false;
    private float clickGuiMix = 0.0f;

    public DynamicIslandFlat() {
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (mc.currentScreen == null && mc.gameSettings != null && mc.gameSettings.keyBindPlayerList != null && mc.gameSettings.keyBindPlayerList.isKeyDown()) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);

        boolean embeddedClickGui = mc.currentScreen instanceof MenuClickGui && ((MenuClickGui) mc.currentScreen).isEmbeddedInDynamicIsland();
        float targetClickGui = 0.0f;
        if (embeddedClickGui) {
            try {
                targetClickGui = (float) ((MenuClickGui) mc.currentScreen).openAnimation.getOutput();
            } catch (Exception ignored) {
                targetClickGui = 1.0f;
            }
        }

        FontTransformer transformer = FontTransformer.getInstance();
        Font font = transformer.getFont("MicrosoftYaHei", 38);
        if (font == null) {
            font = transformer.getFont("Arial", 32);
        }
        if (font == null) return;

        String timeText = new SimpleDateFormat("HH:mm").format(new Date());
        String fpsText = "FPS " + Minecraft.getDebugFPS();

        float paddingX = 10f;
        float paddingY = 6f;
        float radius = 24f;

        float sideSpacingFromCenter = 110f;

        float h = CustomFontRenderer.getFontHeight(font) + paddingY * 2f;

        float w1 = CustomFontRenderer.getStringWidth(timeText, font) + paddingX * 2f;
        float w3 = CustomFontRenderer.getStringWidth(fpsText, font) + paddingX * 2f;

        Notification current = NotificationManager.getInstance().getCurrentNotification();
        boolean currentIsToggle = current != null && (current.getType() == Notification.NotificationType.MODULE_ENABLED || current.getType() == Notification.NotificationType.MODULE_DISABLED);

        Scaffold scaffoldModule = (Scaffold) Epilogue.moduleManager.getModule("Scaffold");
        boolean scaffoldActive = scaffoldModule != null && scaffoldModule.isEnabled();

        long nowMs = System.currentTimeMillis();
        float dt;
        if (lastFrameMs == 0L) {
            dt = 0.016f;
        } else {
            dt = (nowMs - lastFrameMs) / 1000.0f;
            if (dt < 0.0f) dt = 0.0f;
            if (dt > 0.05f) dt = 0.05f;
        }
        lastFrameMs = nowMs;

        float kClick = 18.0f;
        float aClick = 1.0f - (float) Math.exp(-kClick * dt);
        if (!clickGuiInited) {
            clickGuiInited = true;
            clickGuiMix = targetClickGui;
        }
        clickGuiMix += (targetClickGui - clickGuiMix) * aClick;
        if (clickGuiMix < 0.001f) clickGuiMix = 0.0f;
        if (clickGuiMix > 0.999f) clickGuiMix = 1.0f;

        if (clickGuiMix <= 0.0f && !latchedToggleActive) {
            if (currentIsToggle) {
                latchedToggleActive = true;
                latchedToggleStart = current.getTimestamp();
                latchedModuleName = current.getTitle();
                latchedEnabled = current.getType() == Notification.NotificationType.MODULE_ENABLED;
                lastLatchSwitchMs = nowMs;
            }
        } else if (clickGuiMix <= 0.0f) {
            if (nowMs - latchedToggleStart > 1700L) {
                latchedToggleActive = false;
            } else if (currentIsToggle && current.getTimestamp() != latchedToggleStart) {
                if (nowMs - lastLatchSwitchMs > 45L) {
                    latchedToggleStart = current.getTimestamp();
                    latchedModuleName = current.getTitle();
                    latchedEnabled = current.getType() == Notification.NotificationType.MODULE_ENABLED;
                    lastLatchSwitchMs = nowMs;
                }
            }
        }

        boolean showToggle = clickGuiMix <= 0.0f && !scaffoldActive && latchedToggleActive;

        String middleTextClient = Epilogue.clientName + Epilogue.clientVersion;

        String iconText = showToggle ? (latchedEnabled ? "√" : "×") : "";
        String moduleWord = showToggle ? " Module " : "";
        String moduleName = showToggle ? latchedModuleName : "";
        String statusText = showToggle ? (latchedEnabled ? " Enabled" : " Disabled") : "";

        float targetW;
        if (showToggle) {
            targetW = CustomFontRenderer.getStringWidth(iconText + moduleWord + moduleName + statusText, font) + paddingX * 2f;
        } else {
            targetW = CustomFontRenderer.getStringWidth(middleTextClient, font) + paddingX * 2f;
        }
        float targetMix = showToggle ? 1.0f : 0.0f;

        if (!midInited) {
            midInited = true;
            midW = targetW;
            midH = h;
            midMix = targetMix;
        }

        float kW = 14.0f;
        float kH = 14.0f;
        float kMix = 18.0f;
        float aW = 1.0f - (float) Math.exp(-kW * dt);
        float aH = 1.0f - (float) Math.exp(-kH * dt);
        float aMix = 1.0f - (float) Math.exp(-kMix * dt);
        midW += (targetW - midW) * aW;
        midH += (h - midH) * aH;
        midMix += (targetMix - midMix) * aMix;
        if (midMix < 0.001f) midMix = 0.0f;
        if (midMix > 0.999f) midMix = 1.0f;

        float centerX = sr.getScaledWidth() / 2f;
        float y = 8f;

        float x2 = centerX - midW / 2f;
        float x1 = centerX - sideSpacingFromCenter - w1;
        float x3 = centerX + sideSpacingFromCenter;

        float targetScaffold = (clickGuiMix > 0.0f) ? 0.0f : (scaffoldActive ? 1.0f : 0.0f);
        float kScaffold = 10.0f;
        float aScaffold = 1.0f - (float) Math.exp(-kScaffold * dt);
        scaffoldMix += (targetScaffold - scaffoldMix) * aScaffold;
        if (scaffoldMix < 0.001f) scaffoldMix = 0.0f;
        if (scaffoldMix > 0.999f) scaffoldMix = 1.0f;

        float targetSplit = scaffoldMix > 0.0f ? 0.0f : 1.0f;
        float kSplit = 14.0f;
        float aSplit = 1.0f - (float) Math.exp(-kSplit * dt);
        splitMix += (targetSplit - splitMix) * aSplit;
        if (splitMix < 0.001f) splitMix = 0.0f;
        if (splitMix > 0.999f) splitMix = 1.0f;

        float bgScale = 1.0f + 0.08f * midMix;
        float bgExtra = (midW * bgScale - midW) / 2f;
        float bgX2 = x2 - bgExtra;
        float bgW2 = midW + bgExtra * 2f;
        int midBgAlpha = (int) (40 + (26 * midMix));
        if (midBgAlpha > 90) midBgAlpha = 90;
        Color bgMid = new Color(0, 0, 0, midBgAlpha);

        int bloomColor = 0xFF000000;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        

        float mergedFullW = (x3 + w3 - x1);

        float targetLeftEdge;
        float targetRightEdge;
        if (scaffoldMix > 0.0f) {
            targetLeftEdge = x1 + (bgX2 - x1) * splitMix;
            targetRightEdge = (x1 + mergedFullW) + ((bgX2 + bgW2) - (x1 + mergedFullW)) * splitMix;
        } else {
            targetLeftEdge = bgX2;
            targetRightEdge = bgX2 + bgW2;
        }

        MenuClickGui clickGui = embeddedClickGui ? (MenuClickGui) mc.currentScreen : null;
        float clickGuiW = clickGui != null ? clickGui.w : 0.0f;
        float clickGuiH = clickGui != null ? clickGui.h : 0.0f;
        float clickPadX = 18.0f;
        float clickPadY = 18.0f;
        float clickTargetW = clickGuiW + clickPadX * 2f;
        float clickTargetH = clickGuiH + clickPadY * 2f;
        float mergeExtraX = 34.0f;

        float clickLeft = centerX - clickTargetW / 2f;
        float clickRight = centerX + clickTargetW / 2f;

        float mergeEnd = 0.55f;
        float mergePhase = Math.min(1.0f, clickGuiMix / mergeEnd);
        float expandPhase = clickGuiMix <= mergeEnd ? 0.0f : Math.min(1.0f, (clickGuiMix - mergeEnd) / (1.0f - mergeEnd));

        float mergeL = x1 + (centerX - x1) * mergePhase;
        float mergeR = (x3 + w3) + (centerX - (x3 + w3)) * mergePhase;
        float finalL = (mergeL) + ((clickLeft - mergeExtraX) - mergeL) * expandPhase;
        float finalR = (mergeR) + ((clickRight + mergeExtraX) - mergeR) * expandPhase;

        if (clickGuiMix > 0.0f) {
            targetLeftEdge = targetLeftEdge + (finalL - targetLeftEdge) * clickGuiMix;
            targetRightEdge = targetRightEdge + (finalR - targetRightEdge) * clickGuiMix;
        }
        if (!mergeInited) {
            mergeInited = true;
            mergeLeftEdge = targetLeftEdge;
            mergeRightEdge = targetRightEdge;
        }
        float kMerge = 16.0f;
        float aMerge = 1.0f - (float) Math.exp(-kMerge * dt);
        mergeLeftEdge += (targetLeftEdge - mergeLeftEdge) * aMerge;
        mergeRightEdge += (targetRightEdge - mergeRightEdge) * aMerge;

        float mergeX = mergeLeftEdge;
        float mergeW = Math.max(1f, mergeRightEdge - mergeLeftEdge);
        float mergeRadius = radius + scaffoldMix;

        float clickDown = clickTargetH - h;
        if (clickDown < 0.0f) clickDown = 0.0f;
        float downH = h + clickDown * expandPhase;
        float downRadius = mergeRadius;

        Framebuffer bloomBuffer = PostProcessing.beginBloom();
        if (bloomBuffer != null) {
            if (splitMix < 0.999f || clickGuiMix > 0.001f) {
                RenderUtil.drawRoundedRect(mergeX, y, mergeW, downH, downRadius, bloomColor);
            }
            if (splitMix > 0.001f) {
                RenderUtil.drawRoundedRect(x1, y, w1, h, radius, bloomColor);
                RenderUtil.drawRoundedRect(bgX2, y, bgW2, h, radius, bloomColor);
                RenderUtil.drawRoundedRect(x3, y, w3, h, radius, bloomColor);
            }
            mc.getFramebuffer().bindFramebuffer(false);
        }

        if (splitMix < 0.999f || clickGuiMix > 0.001f) {
            int a = (int) (40 * (1.0f - splitMix));
            if (a < 0) a = 0;
            if (a > 120) a = 120;
            Color bgA = new Color(0, 0, 0, a);
            PostProcessing.drawBlur(mergeX, y, mergeX + mergeW, y + downH, () -> () -> RenderUtil.drawRoundedRect(mergeX, y, mergeW, downH, downRadius, -1));
            RenderUtil.drawRoundedRect(mergeX, y, mergeW, downH, downRadius, bgA);
        }

        if (splitMix > 0.001f) {
            int a = (int) (40 * splitMix);
            Color bgA = new Color(0, 0, 0, a);
            Color bgMidA = new Color(0, 0, 0, (int) (bgMid.getAlpha() * splitMix));

            PostProcessing.drawBlur(x1, y, x1 + w1, y + h, () -> () -> RenderUtil.drawRoundedRect(x1, y, w1, h, radius, -1));
            RenderUtil.drawRoundedRect(x1, y, w1, h, radius, bgA);

            PostProcessing.drawBlur(bgX2, y, bgX2 + bgW2, y + h, () -> () -> RenderUtil.drawRoundedRect(bgX2, y, bgW2, h, radius, -1));
            RenderUtil.drawRoundedRect(bgX2, y, bgW2, h, radius, bgMidA);

            PostProcessing.drawBlur(x3, y, x3 + w3, y + h, () -> () -> RenderUtil.drawRoundedRect(x3, y, w3, h, radius, -1));
            RenderUtil.drawRoundedRect(x3, y, w3, h, radius, bgA);
        }

        float textY = y + (h - CustomFontRenderer.getFontHeight(font)) / 2f + 3f;
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        float normalTextAlpha = (1.0f - scaffoldMix);
        int whiteNormal = new Color(255, 255, 255, (int) (255 * normalTextAlpha)).getRGB();
        float timeTextX = x1 + paddingX;
        float fpsTextX = x3 + paddingX;
        if (normalTextAlpha > 0.01f) {
            CustomFontRenderer.drawString(timeText, timeTextX, textY, whiteNormal, font);
        }

        float midX = x2 + paddingX;

        GlStateManager.pushMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        float notifAlphaNow;

        if (showToggle) {
            long ts = latchedToggleStart;
            float timeAlpha;
            long elapsed = nowMs - ts;
            long duration = 1700L;
            long fadeIn = 180L;
            long fadeOut = 260L;
            if (elapsed < 0) elapsed = 0;
            if (elapsed < fadeIn) {
                timeAlpha = elapsed / (float) fadeIn;
            } else if (elapsed > duration - fadeOut) {
                timeAlpha = Math.max(0.0f, 1.0f - (elapsed - (duration - fadeOut)) / (float) fadeOut);
            } else {
                timeAlpha = 1.0f;
            }
            notifAlphaNow = midMix * timeAlpha;

            int okColor = new Color(128, 255, 149, (int) (255 * notifAlphaNow)).getRGB();
            int badColor = new Color(255, 120, 120, (int) (255 * notifAlphaNow)).getRGB();
            int whiteA = new Color(255, 255, 255, (int) (255 * notifAlphaNow)).getRGB();
            int iconColor = latchedEnabled ? okColor : badColor;

            float cx = midX;
            CustomFontRenderer.drawString(iconText, cx, textY, iconColor, font);
            cx += CustomFontRenderer.getStringWidth(iconText, font);

            CustomFontRenderer.drawString(moduleWord, cx, textY, whiteA, font);
            cx += CustomFontRenderer.getStringWidth(moduleWord, font);

            CustomFontRenderer.drawString(moduleName, cx, textY, whiteA, font);
            cx += CustomFontRenderer.getStringWidth(moduleName, font);

            CustomFontRenderer.drawString(statusText, cx, textY, iconColor, font);
        }

        if (!showToggle && normalTextAlpha > 0.01f) {
            int w = new Color(255, 255, 255, (int) (255 * normalTextAlpha)).getRGB();
            int g = new Color(128, 255, 149, (int) (255 * normalTextAlpha)).getRGB();
            CustomFontRenderer.drawString(Epilogue.clientName, midX, textY, w, font);
            float namePartW = CustomFontRenderer.getStringWidth(Epilogue.clientName, font);
            CustomFontRenderer.drawString(Epilogue.clientVersion, midX + namePartW, textY, g, font);
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();

        if (normalTextAlpha > 0.01f) {
            CustomFontRenderer.drawString(fpsText, fpsTextX, textY, whiteNormal, font);
        }
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        PostProcessing.endBloom(bloomBuffer);

        if (scaffoldMix > 0.0f) {
            int totalBlocks = 0;
            int stackBlocks;
            for (int i = 0; i < 9; i++) {
                ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
                if (s != null && s.stackSize > 0 && s.getItem() instanceof ItemBlock) {
                    totalBlocks += s.stackSize;
                }
            }
            stackBlocks = Math.max(1, Math.min(64, totalBlocks));
            float progress = stackBlocks / 64.0f;
            float kProg = 10.0f;
            float aProg = 1.0f - (float) Math.exp(-kProg * dt);
            scaffoldProgressAnim += (progress - scaffoldProgressAnim) * aProg;
            if (scaffoldProgressAnim < 0.0f) scaffoldProgressAnim = 0.0f;
            if (scaffoldProgressAnim > 1.0f) scaffoldProgressAnim = 1.0f;

            float uiAlpha = scaffoldMix;
            float leftPad = 10f;

            float bps = ScaffoldData.getInstance().getBlocksPerSecond();
            if (bps < 0.0f) bps = 0.0f;
            if (bps > 9.9f) bps = 9.9f;
            String bpsText = String.format("%.1f", bps);
            int bpsColor = new Color(255, 255, 255, (int) (255 * uiAlpha)).getRGB();
            float bpsX = mergeX + leftPad;
            if (uiAlpha > 0.01f) {
                CustomFontRenderer.drawString(bpsText, bpsX, textY, bpsColor, font);
            }

            String rightText = totalBlocks + " Blocks";
            float rightTextW = CustomFontRenderer.getStringWidth(rightText, font);
            float rightTextX = mergeX + mergeW - 8f - rightTextW;
            if (uiAlpha > 0.01f) {
                int rightColor = new Color(255, 255, 255, (int) (255 * uiAlpha)).getRGB();
                CustomFontRenderer.drawString(rightText, rightTextX, textY, rightColor, font);
            }

            float barLeft = bpsX + CustomFontRenderer.getStringWidth(bpsText, font) + 10f;
            float barRight = rightTextX - 10f;
            float barW = Math.max(0f, barRight - barLeft);
            float barH = 6f;
            float barY = y + (h - barH) / 2f;
            float fillW = barW * scaffoldProgressAnim;
            if (uiAlpha > 0.01f && fillW > 0.5f) {
                epilogue.module.modules.render.Interface ui = (epilogue.module.modules.render.Interface) Epilogue.moduleManager.getModule("Interface");
                int leftC = ui != null ? ui.getMainColor().getRGB() : 0xFF80FF95;
                int rightC = ui != null ? ui.getSecondColor().getRGB() : 0xFF80FFFF;
                int alpha255 = (int) (255 * uiAlpha);
                Color lc = new Color((leftC >> 16) & 255, (leftC >> 8) & 255, leftC & 255, alpha255);
                Color rc = new Color((rightC >> 16) & 255, (rightC >> 8) & 255, rightC & 255, alpha255);

                if (epilogue.module.modules.render.PostProcessing.isNewDynamicIslandBloomFromItSelf()) {
                    Framebuffer pb = BloomShader.beginFramebuffer();
                    RenderUtil.drawRoundedRect(barLeft, barY, fillW, barH, 5f, lc.getRGB());
                    mc.getFramebuffer().bindFramebuffer(false);
                    BloomShader.renderBloom(pb.framebufferTexture, 2, 1);
                } else {
                    Framebuffer pb = BloomShader.beginFramebuffer();
                    RenderUtil.drawRoundedRect(barLeft, barY, fillW, barH, 5f, epilogue.module.modules.render.PostProcessing.getBloomColor());
                    mc.getFramebuffer().bindFramebuffer(false);
                    BloomShader.renderBloom(pb.framebufferTexture, 2, 1);
                }

                RenderUtil.scissorStart(barLeft, barY, fillW, barH);
                RoundedUtil.drawGradientHorizontal(barLeft, barY, barW, barH, 0f, lc, rc);
                RenderUtil.scissorEnd();
            }
        }

        if (embeddedClickGui) {
            float clipX = mergeX;
            float clipY = y;
            float clipW = mergeW;
            float clipH = downH;
            ((MenuClickGui) mc.currentScreen).setExternalClip((int) clipX, (int) clipY, (int) clipW, (int) clipH);
        }
    }
}
