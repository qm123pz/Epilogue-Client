package epilogue.module.modules.render;

import epilogue.Epilogue;
import epilogue.enums.ChatColors;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.PacketEvent;
import epilogue.events.Render2DEvent;
import epilogue.module.Module;
import epilogue.module.modules.combat.Aura;
import epilogue.value.values.BooleanValue;
import epilogue.util.ColorUtil;
import epilogue.util.RenderUtil;
import epilogue.util.TeamUtil;
import epilogue.util.TimerUtil;
import epilogue.value.values.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat diffFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private final TimerUtil scaleAnimTimer = new TimerUtil();
    private final TimerUtil targetLostTimer = new TimerUtil();
    private EntityLivingBase lastTarget = null;
    private EntityLivingBase target = null;
    private ResourceLocation headTexture = null;
    private float oldHealth = 0.0F;
    private float newHealth = 0.0F;
    private float maxHealth = 0.0F;
    private float scaleAnimation = 0.0F;
    private boolean isAnimatingOut = false;
    private boolean targetLost = false;
    private EntityLivingBase lastRenderTarget = null;
    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Exhibition"});
    public final ModeValue color = new ModeValue("Color", 0, new String[]{"Default", "Hud"});
    public final ModeValue posX = new ModeValue("PositionX", 1, new String[]{"Left", "Middle", "Right"});
    public final ModeValue posY = new ModeValue("PositionY", 1, new String[]{"Top", "Middle", "Bottom"});
    public final FloatValue scale = new FloatValue("Scale", 1.0F, 0.5F, 1.5F);
    public final IntValue offX = new IntValue("OffsetX", 0, -255, 255);
    public final IntValue offY = new IntValue("OffsetY", 40, -255, 255);
//    public final PercentValue background = new PercentValue("Background", 25);
    public final BooleanValue head = new BooleanValue("Head", true);
//    public final BooleanValue indicator = new BooleanValue("Indicator", true);
//    public final BooleanValue outline = new BooleanValue("Outline", false);
    public final BooleanValue animations = new BooleanValue("Animations", true);
    public final BooleanValue shadow = new BooleanValue("Shadow", true);
    public final BooleanValue kaOnly = new BooleanValue("KaOnly", true);
    public final BooleanValue chatPreview = new BooleanValue("ChatPreview", false);

    private EntityLivingBase resolveTarget() {
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        if (aura.isEnabled() && aura.isAttackAllowed() && TeamUtil.isEntityLoaded(aura.getTarget())) {
            return aura.getTarget();
        } else if (!(java.lang.Boolean) this.kaOnly.getValue()
                && !this.lastAttackTimer.hasTimeElapsed(1500L)
                && TeamUtil.isEntityLoaded(this.lastTarget)) {
            return this.lastTarget;
        } else {
            return this.chatPreview.getValue() && mc.currentScreen instanceof GuiChat ? mc.thePlayer : null;
        }
    }

//    private ResourceLocation getSkin(EntityLivingBase entityLivingBase) {
//        if (entityLivingBase instanceof EntityPlayer) {
//            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(entityLivingBase.getName());
//            if (playerInfo != null) {
//                return playerInfo.getLocationSkin();
//            }
//        }
//        return null;
//    }
//
//    private Color getTargetColor(EntityLivingBase entityLivingBase) {
//        if (entityLivingBase instanceof EntityPlayer) {
//            if (TeamUtil.isFriend((EntityPlayer) entityLivingBase)) {
//                return Epilogue.friendManager.getColor();
//            }
//            if (TeamUtil.isTarget((EntityPlayer) entityLivingBase)) {
//                return Epilogue.targetManager.getColor();
//            }
//        }
//        switch (this.color.getValue()) {
//            case 0:
//                if (!(entityLivingBase instanceof EntityPlayer)) {
//                    return new Color(-1);
//                }
//                return TeamUtil.getTeamColor((EntityPlayer) entityLivingBase, 1.0F);
//            case 1:
//                return Color.CYAN;
//            default:
//                return new Color(-1);
//        }
//    }

    public TargetHUD() {
        super("TargetHUD", false, true);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && mc.thePlayer != null) {
            EntityLivingBase currentTarget = this.resolveTarget();
            
            if (currentTarget != null) {
                targetLost = false;
                if (!isAnimatingOut) {
                    if (this.target != currentTarget) {
                        if (this.target == null) {
                            scaleAnimTimer.reset();
                        }
                        this.target = currentTarget;
                        this.lastRenderTarget = currentTarget;
                        this.headTexture = null;
                        this.animTimer.setTime();
                        float heal = this.target.getHealth() / 2.0F + this.target.getAbsorptionAmount() / 2.0F;
                        this.oldHealth = heal;
                        this.newHealth = heal;
                    }
                    updateScaleAnimation();
                    
                    switch (this.mode.getValue()) {
                        case 0:
                            renderExhibitionMode();
                            break;
//                        case 1:
//                            renderMyauMode();
//                            break;
                    }
                }
            } else if (this.target != null) {
                if (!targetLost) {
                    targetLost = true;
                    targetLostTimer.reset();
                }
                
                if (targetLostTimer.hasTimeElapsed(50L)) {
                    isAnimatingOut = true;
                    scaleAnimTimer.reset();
                    this.target = null;
                } else {
                    switch (this.mode.getValue()) {
                        case 0:
                            renderExhibitionMode();
                            break;
//                        case 1:
//                            renderMyauMode();
//                            break;
                    }
                }
            }
            
            if (isAnimatingOut) {
                updateScaleAnimation();
                if (scaleAnimation > 0.0F && lastRenderTarget != null) {
                    this.target = lastRenderTarget;
                    switch (this.mode.getValue()) {
                        case 0:
                            renderExhibitionMode();
                            break;
//                        case 1:
//                            renderMyauMode();
//                            break;
                    }
                    this.target = null;
                } else if (scaleAnimation <= 0.0F) {
                    isAnimatingOut = false;
                    targetLost = false;
                    lastRenderTarget = null;
                }
            }
        }
    }

    private void updateScaleAnimation() {
        long elapsedTime = scaleAnimTimer.getElapsedTime();
        float animationDuration = 200.0F;
        
        if (!isAnimatingOut) {
            scaleAnimation = Math.min(elapsedTime / animationDuration, 1.0F);
        } else {
            scaleAnimation = Math.max(1.0F - (elapsedTime / animationDuration), 0.0F);
        }
        
        scaleAnimation = easeOutQuart(scaleAnimation);
    }

    private float easeOutQuart(float t) {
        return 1.0F - (float)Math.pow(1.0F - t, 4.0F);
    }

//    private void renderMyauMode() {
//        float health = (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / 2.0F;
//        float abs = this.target.getAbsorptionAmount() / 2.0F;
//        float heal = this.target.getHealth() / 2.0F + abs;
//
//        if (!this.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
//            this.oldHealth = this.newHealth;
//            this.newHealth = heal;
//            this.maxHealth = this.target.getMaxHealth() / 2.0F;
//            if (this.oldHealth != this.newHealth) {
//                this.animTimer.reset();
//            }
//        }
//
//        ResourceLocation resourceLocation = this.getSkin(this.target);
//        if (resourceLocation != null) {
//            this.headTexture = resourceLocation;
//        }
//
//        float elapsedTime = (float) Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
//        float healthRatio = Math.min(Math.max(RenderUtil.lerpFloat(this.newHealth, this.oldHealth, elapsedTime / 150.0F) / this.maxHealth, 0.0F), 1.0F);
//        Color targetColor = this.getTargetColor(this.target);
//        Color healthBarColor = this.color.getValue() == 0 ? ColorUtil.getHealthBlend(healthRatio) : targetColor;
//        float healthDeltaRatio = Math.min(Math.max((health - heal + 1.0F) / 2.0F, 0.0F), 1.0F);
//        Color healthDeltaColor = ColorUtil.getHealthBlend(healthDeltaRatio);
//
//        ScaledResolution scaledResolution = new ScaledResolution(mc);
//        String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(this.target)));
//        int targetNameWidth = mc.fontRendererObj.getStringWidth(targetNameText);
//        String healthText = ChatColors.formatColor(
//                String.format("&r&f%s%s❤&r", healthFormat.format(heal), abs > 0.0F ? "&6" : "&c")
//        );
//        int healthTextWidth = mc.fontRendererObj.getStringWidth(healthText);
//        String statusText = ChatColors.formatColor(String.format("&r&l%s&r", heal == health ? "D" : (heal < health ? "W" : "L")));
//        int statusTextWidth = mc.fontRendererObj.getStringWidth(statusText);
//        String healthDiffText = ChatColors.formatColor(
//                String.format("&r%s&r", heal == health ? "0.0" : diffFormat.format(health - heal))
//        );
//        int healthDiffWidth = mc.fontRendererObj.getStringWidth(healthDiffText);
//
//        float barContentWidth = Math.max(
//                (float) targetNameWidth + (this.indicator.getValue() ? 2.0F + (float) statusTextWidth + 2.0F : 0.0F),
//                (float) healthTextWidth + (this.indicator.getValue() ? 2.0F + (float) healthDiffWidth + 2.0F : 0.0F)
//        );
//        float headIconOffset = this.head.getValue() && this.headTexture != null ? 25.0F : 0.0F;
//        float barTotalWidth = Math.max(headIconOffset + 70.0F, headIconOffset + 2.0F + barContentWidth + 2.0F);
//
//        float posX = this.offX.getValue().floatValue() / this.scale.getValue();
//        switch (this.posX.getValue()) {
//            case 1:
//                posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() / 2.0F - barTotalWidth / 2.0F;
//                break;
//            case 2:
//                posX *= -1.0F;
//                posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() - barTotalWidth;
//        }
//
//        float posY = this.offY.getValue().floatValue() / this.scale.getValue();
//        switch (this.posY.getValue()) {
//            case 1:
//                posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() / 2.0F - 13.5F;
//                break;
//            case 2:
//                posY *= -1.0F;
//                posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() - 27.0F;
//        }
//
//        GlStateManager.pushMatrix();
//        GlStateManager.translate(posX + barTotalWidth / 2.0F, posY + 13.5F, -450.0F);
//        float finalScale = this.scale.getValue() * scaleAnimation;
//        GlStateManager.scale(finalScale, finalScale, 0.0F);
//        GlStateManager.translate(-barTotalWidth / 2.0F, -13.5F, 0.0F);
//        RenderUtil.enableRenderState();
//
//        int backgroundColor = new Color(0.0F, 0.0F, 0.0F, (float) this.background.getValue() / 100.0F).getRGB();
//        int outlineColor = this.outline.getValue() ? targetColor.getRGB() : new Color(0, 0, 0, 0).getRGB();
//        RenderUtil.drawOutlineRect(0.0F, 0.0F, barTotalWidth, 27.0F, 1.5F, backgroundColor, outlineColor);
//        RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, barTotalWidth - 2.0F, 25.0F, ColorUtil.darker(healthBarColor, 0.2F).getRGB());
//        RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, headIconOffset + 2.0F + healthRatio * (barTotalWidth - 2.0F - headIconOffset - 2.0F), 25.0F, healthBarColor.getRGB());
//        RenderUtil.disableRenderState();
//
//        GlStateManager.disableDepth();
//        GlStateManager.enableBlend();
//        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//        mc.fontRendererObj.drawString(targetNameText, headIconOffset + 2.0F, 2.0F, -1, this.shadow.getValue());
//        mc.fontRendererObj.drawString(healthText, headIconOffset + 2.0F, 12.0F, -1, this.shadow.getValue());
//
//        if (this.indicator.getValue()) {
//            mc.fontRendererObj.drawString(statusText, barTotalWidth - 2.0F - (float) statusTextWidth, 2.0F, healthDeltaColor.getRGB(), this.shadow.getValue());
//            mc.fontRendererObj.drawString(healthDiffText, barTotalWidth - 2.0F - (float) healthDiffWidth, 12.0F, ColorUtil.darker(healthDeltaColor, 0.8F).getRGB(), this.shadow.getValue());
//        }
//
//        if (this.head.getValue() && this.headTexture != null) {
//            GlStateManager.color(1.0F, 1.0F, 1.0F);
//            mc.getTextureManager().bindTexture(this.headTexture);
//            Gui.drawScaledCustomSizeModalRect(2, 2, 8.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
//            Gui.drawScaledCustomSizeModalRect(2, 2, 40.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
//            GlStateManager.color(1.0F, 1.0F, 1.0F);
//        }
//
//        GlStateManager.disableBlend();
//        GlStateManager.enableDepth();
//        GlStateManager.popMatrix();
//    }

    private void renderExhibitionMode() {
        ScaledResolution resolution = new ScaledResolution(mc);
        double boxWidth = 40 + mc.fontRendererObj.getStringWidth(target.getName());
        double renderWidth = Math.max(boxWidth, 120);
        
        float posX = this.offX.getValue().floatValue() / this.scale.getValue();
        switch (this.posX.getValue()) {
            case 1:
                posX += (float) resolution.getScaledWidth() / this.scale.getValue() / 2.0F - (float)renderWidth / 2.0F;
                break;
            case 2:
                posX *= -1.0F;
                posX += (float) resolution.getScaledWidth() / this.scale.getValue() - (float)renderWidth;
        }
        
        float posY = this.offY.getValue().floatValue() / this.scale.getValue();
        switch (this.posY.getValue()) {
            case 1:
                posY += (float) resolution.getScaledHeight() / this.scale.getValue() / 2.0F - 20.0F;
                break;
            case 2:
                posY *= -1.0F;
                posY += (float) resolution.getScaledHeight() / this.scale.getValue() - 40.0F;
        }
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(posX + (float)renderWidth / 2.0F, posY + 20.0F, 0);
        float finalScale = this.scale.getValue() * scaleAnimation;
        GlStateManager.scale(finalScale, finalScale, 0.0F);
        GlStateManager.translate(-(float)renderWidth / 2.0F, -20.0F, 0);

        drawExhibitionBorderedRect(-2.5F, -2.5F, (float)renderWidth + 2.5F, 40 + 2.5F, 0.5F, getExhibitionColor(60), getExhibitionColor(10));
        drawExhibitionBorderedRect(-1.5F, -1.5F, (float)renderWidth + 1.5F, 40 + 1.5F, 1.5F, getExhibitionColor(60), getExhibitionColor(40));
        
        drawExhibitionBorderedRect(0, 0, (float)renderWidth, 40, 0.5F, getExhibitionColor(22), getExhibitionColor(60));
        
        drawExhibitionBorderedRect(2, 2, 38, 38, 0.5F, getExhibitionColor(0, 0), getExhibitionColor(10));
        drawExhibitionBorderedRect(2.5F, 2.5F, 38 - 0.5F, 38 - 0.5F, 0.5F, getExhibitionColor(17), getExhibitionColor(48));

        GlStateManager.pushMatrix();
        int factor = resolution.getScaleFactor();
        
        GL11.glScissor((int) ((posX + 3) * factor), (int) ((resolution.getScaledHeight() - (posY + 37)) * factor), (int) ((37 - 3) * factor), (int) ((37 - 3) * factor));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        
        drawEntityOnScreen(target);
        
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.popMatrix();

        GlStateManager.translate(2, 0, 0);
        
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.8F, 0.8F, 0.8F);
        mc.fontRendererObj.drawString(target.getName(), 46, 4, -1, this.shadow.getValue());
        GlStateManager.popMatrix();

        float health = target.getHealth();
        float absorption = target.getAbsorptionAmount();
        float progress = health / (target.getMaxHealth() + absorption);
        float realHealthProgress = (health / target.getMaxHealth());
        
        Color customColor = health >= 0 ? blendColors(new float[]{0f, 0.5f, 1f}, new Color[]{Color.RED, Color.YELLOW, Color.GREEN}, realHealthProgress).brighter() : Color.RED;
        double width = Math.min(mc.fontRendererObj.getStringWidth(target.getName()), 60);
        
        width = getIncremental(width, 10);
        if (width < 60) {
            width = 60;
        }
        double healthLocation = width * progress;

        drawExhibitionBorderedRect(37, 12, 39 + (float)width, 16, 0.5F, getExhibitionColor(0, 0), getExhibitionColor(0));
        drawExhibitionRect(38 + (float)healthLocation + 0.5F, 12.5F, 38 + (float)width + 0.5F, 15.5F, getExhibitionColorOpacity(customColor.getRGB(), 35));
        drawExhibitionRect(37.5F, 12.5F, 38 + (float)healthLocation + 0.5F, 15.5F, customColor.getRGB());

        if (absorption > 0) {
            double absorptionDifferent = width * (absorption / (target.getMaxHealth() + absorption));
            drawExhibitionRect(38 + (float)healthLocation + 0.5F, 12.5F, 38 + (float)healthLocation + 0.5F + (float)absorptionDifferent, 15.5F, 0x80FFAA00);
        }

        for (int i = 1; i < 10; i++) {
            double dThing = (width / 10) * i;
            drawExhibitionRect(38 + (float)dThing, 12, 38 + (float)dThing + 0.5F, 16, getExhibitionColor(0));
        }
        
        String str = "HP: " + (int) health + " | Dist: " + (int) mc.thePlayer.getDistanceToEntity(target);
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.7F, 0.7F, 0.7F);
        mc.fontRendererObj.drawString(str, 53, 26, -1, this.shadow.getValue());
        GlStateManager.popMatrix();

        if (target instanceof EntityPlayer) {
            EntityPlayer targetPlayer = (EntityPlayer) target;
            GL11.glPushMatrix();
            final List<ItemStack> items = new ArrayList<ItemStack>();
            int split = 20;
            
            for (int index = 3; index >= 0; --index) {
                final ItemStack armor = targetPlayer.inventory.armorInventory[index];
                if (armor != null) {
                    items.add(armor);
                }
            }
            int yOffset = 23;
            if (targetPlayer.getCurrentEquippedItem() != null) {
                items.add(targetPlayer.getCurrentEquippedItem());
            }
            
            RenderHelper.enableGUIStandardItemLighting();
            for (final ItemStack itemStack : items) {
                if (mc.theWorld != null) {
                    split += 16;
                }
                GlStateManager.pushMatrix();
                GlStateManager.disableAlpha();
                GlStateManager.clear(256);
                mc.getRenderItem().zLevel = -150.0f;
                mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, split, yOffset);
                mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, itemStack, split, yOffset);
                mc.getRenderItem().zLevel = 0.0f;

                int renderY = yOffset;
                if (itemStack.getItem() instanceof ItemSword) {
                    int sLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack);
                    int fLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack);
                    if (sLevel > 0) {
                        drawEnchantTag("S" + getSharpnessColor(sLevel) + sLevel, split, renderY);
                        renderY += 4.5F;
                    }
                    if (fLevel > 0) {
                        drawEnchantTag("F" + getFireAspectColor(fLevel) + fLevel, split, renderY);
                        renderY += 4.5F;
                    }
                } else if ((itemStack.getItem() instanceof ItemArmor)) {
                    int pLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, itemStack);
                    if (pLevel > 0) {
                        drawEnchantTag("P" + getProtectionColor(pLevel) + pLevel, split, renderY);
                        renderY += 4.5F;
                    }
                }

                GlStateManager.disableBlend();
                GlStateManager.disableLighting();
                GlStateManager.enableAlpha();
                GlStateManager.popMatrix();
            }
            RenderHelper.disableStandardItemLighting();
            GL11.glPopMatrix();
        }

        GlStateManager.popMatrix();
    }

    private double getIncremental(double value, double increment) {
        return Math.ceil(value / increment) * increment;
    }

    private void drawEntityOnScreen(EntityLivingBase ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(20.0F, 36.0F, 50.0F);

        float largestSize = Math.max(ent.height, ent.width);
        float relativeScale = Math.max(largestSize / 1.8F, 1);

        GlStateManager.scale((float) -16 / relativeScale, (float) 16 / relativeScale, (float) 16 / relativeScale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-((float) Math.atan((double) ((float) 17 / 40.0F))) * 20.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        RenderManager renderManager = mc.getRenderManager();
        renderManager.setPlayerViewY(180.0F);
        renderManager.setRenderShadow(false);
        renderManager.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        renderManager.setRenderShadow(true);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private Color blendColors(float[] fractions, Color[] colors, float progress) {
        if (fractions.length == colors.length) {
            int[] indicies = getFractionIndicies(fractions, progress);
            float[] range = new float[]{fractions[indicies[0]], fractions[indicies[1]]};
            Color[] colorRange = new Color[]{colors[indicies[0]], colors[indicies[1]]};
            float max = range[1] - range[0];
            float value = progress - range[0];
            float weight = value / max;
            return blend(colorRange[0], colorRange[1], (double) (1.0F - weight));
        } else {
            return colors[0];
        }
    }

    private int[] getFractionIndicies(float[] fractions, float progress) {
        int[] range = new int[2];
        int startPoint = 0;
        while (startPoint < fractions.length && fractions[startPoint] <= progress) {
            ++startPoint;
        }
        if (startPoint >= fractions.length) {
            startPoint = fractions.length - 1;
        }
        range[0] = startPoint - 1;
        range[1] = startPoint;
        return range;
    }

    private Color blend(Color color1, Color color2, double ratio) {
        float r = (float) ratio;
        float ir = 1.0F - r;
        float[] rgb1 = color1.getColorComponents(new float[3]);
        float[] rgb2 = color2.getColorComponents(new float[3]);
        return new Color(rgb1[0] * r + rgb2[0] * ir, rgb1[1] * r + rgb2[1] * ir, rgb1[2] * r + rgb2[2] * ir);
    }

    private void drawEnchantTag(String text, int x, float y) {
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        mc.fontRendererObj.drawString(text, x * 2, y * 2, -1, true);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private String getProtectionColor(int level) {
        switch (level) {
            case 1: return "§a";
            case 2: return "§9";
            case 3: return "§e";
            case 4: return "§c";
            default: return "§f";
        }
    }

    private String getSharpnessColor(int level) {
        switch (level) {
            case 1: return "§a";
            case 2: return "§9";
            case 3: return "§e";
            case 4: return "§6";
            case 5: return "§c";
            default: return "§f";
        }
    }

    private String getFireAspectColor(int level) {
        switch (level) {
            case 1: return "§6";
            case 2: return "§c";
            default: return "§f";
        }
    }

    private int getExhibitionColor(int brightness) {
        return getExhibitionColor(brightness, brightness, brightness, 255);
    }

    private int getExhibitionColor(int brightness, int alpha) {
        return getExhibitionColor(brightness, brightness, brightness, alpha);
    }

    private int getExhibitionColor(int red, int green, int blue) {
        return getExhibitionColor(red, green, blue, 255);
    }

    private int getExhibitionColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= Math.max(0, Math.min(255, alpha)) << 24;
        color |= Math.max(0, Math.min(255, red)) << 16;
        color |= Math.max(0, Math.min(255, green)) << 8;
        color |= Math.max(0, Math.min(255, blue));
        return color;
    }

    private int getExhibitionColorOpacity(int color, int alpha) {
        int red = (color >> 16 & 0xFF);
        int green = (color >> 8 & 0xFF);
        int blue = (color & 0xFF);
        return getExhibitionColor(red, green, blue, Math.max(0, Math.min(255, alpha)));
    }

    private void drawExhibitionRect(float x1, float y1, float x2, float y2, int color) {
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(x1, y1, x2, y2, color);
        RenderUtil.disableRenderState();
    }

    private void drawExhibitionBorderedRect(float x1, float y1, float x2, float y2, float borderWidth, int fillColor, int borderColor) {
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(x1, y1, x2, y2, borderColor);
        RenderUtil.drawRect(x1 + borderWidth, y1 + borderWidth, x2 - borderWidth, y2 - borderWidth, fillColor);
        RenderUtil.disableRenderState();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() != Action.ATTACK) {
                return;
            }
            Entity entity = packet.getEntityFromWorld(mc.theWorld);
            if (entity instanceof EntityLivingBase) {
                if (entity instanceof EntityArmorStand) {
                    return;
                }
                this.lastAttackTimer.reset();
                this.lastTarget = (EntityLivingBase) entity;
            }
        }
    }
}