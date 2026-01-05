package epilogue.module.modules.render;

import epilogue.Epilogue;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import epilogue.enums.ChatColors;
import epilogue.event.EventTarget;
import epilogue.events.Render2DEvent;
import epilogue.events.Render3DEvent;
import epiloguemixinbridge.IAccessorEntityRenderer;
import epiloguemixinbridge.IAccessorRenderManager;
import epilogue.module.Module;
import epilogue.font.FontRenderer;
import epilogue.util.ColorUtil;
import epilogue.util.GLUtil;
import epilogue.util.RenderUtil;
import epilogue.util.TeamUtil;
import epilogue.value.values.*;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class NameTags extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormatter = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));

    private static final ResourceLocation POSITIONING_ICON = new ResourceLocation("epilogue/texture/nametags/Positioning.png");
    public static final ResourceLocation HEART_ICON = new ResourceLocation("epilogue/texture/nametags/Heart.png");

    public final ModeValue mode = new ModeValue("Mode", 1, new String[]{"Vanilla", "Better"});
    public final IntValue amount = new IntValue("Amount", 20, 1, 200);
    public final FloatValue scale = new FloatValue("Scale", 1.0F, 0.5F, 2.0F);
    public final FloatValue height = new FloatValue("Height", 0.4F, -1.0F, 2.0F);
    public final FloatValue rounding = new FloatValue("Rounding", 3.5F, 0.0F, 10.0F);
    public final BooleanValue autoScale = new BooleanValue("Auto Scale", true, () -> isMyauMode());
    public final PercentValue backgroundOpacity = new PercentValue("Background", 25, () -> isMyauMode());
    public final BooleanValue shadow = new BooleanValue("Shadow", true, () -> isMyauMode());
    public final ModeValue distanceMode = new ModeValue("Distance", 0, new String[]{"None", "Default", "Vape"}, () -> isMyauMode());
    public final ModeValue healthMode = new ModeValue("Health", 2, new String[]{"None", "HP", "Hearts", "Tab"}, () -> isMyauMode());
    public final BooleanValue armor = new BooleanValue("Armor", true, () -> isMyauMode());
    public final BooleanValue effects = new BooleanValue("Effects", true, () -> isMyauMode());
    public final BooleanValue players = new BooleanValue("Players", true);
    public final BooleanValue friends = new BooleanValue("Friends", true);
    public final BooleanValue enemies = new BooleanValue("Enemies", true);
    public final BooleanValue bossees = new BooleanValue("Bosses", false);
    public final BooleanValue mobs = new BooleanValue("Mobs", false);
    public final BooleanValue creepers = new BooleanValue("Creepers", false);
    public final BooleanValue endermans = new BooleanValue("Endermen", false);
    public final BooleanValue blazes = new BooleanValue("Blazes", false);
    public final BooleanValue animals = new BooleanValue("Animals", false);
    public final BooleanValue self = new BooleanValue("Self", false);
    public final BooleanValue bots = new BooleanValue("Bots", false);

    public NameTags() {
        super("NameTags", false);
    }

    private boolean isMyauMode() {
        return this.mode.getValue() == 0;
    }

    private static void drawTexturedRect(float x1, float y1, float x2, float y2) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x1, y2, 0.0).tex(0.0, 1.0).endVertex();
        wr.pos(x2, y2, 0.0).tex(1.0, 1.0).endVertex();
        wr.pos(x2, y1, 0.0).tex(1.0, 0.0).endVertex();
        wr.pos(x1, y1, 0.0).tex(0.0, 0.0).endVertex();
        tessellator.draw();
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int getTagPriority(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (TeamUtil.isTarget(player)) return 0;
            if (TeamUtil.isFriend(player)) return 1;
        }
        return 2;
    }

    public boolean shouldRenderTags(EntityLivingBase entityLivingBase) {
        if (entityLivingBase.deathTime > 0) {
            return false;
        } else if (mc.getRenderViewEntity().getDistanceToEntity(entityLivingBase) > 512.0F) {
            return false;
        } else if (entityLivingBase instanceof EntityPlayer) {
            if (entityLivingBase != mc.thePlayer && entityLivingBase != mc.getRenderViewEntity()) {
                if (TeamUtil.isBot((EntityPlayer) entityLivingBase)) {
                    return this.bots.getValue();
                } else if (TeamUtil.isFriend((EntityPlayer) entityLivingBase)) {
                    return this.friends.getValue();
                } else {
                    return TeamUtil.isTarget((EntityPlayer) entityLivingBase) ? this.enemies.getValue() : this.players.getValue();
                }
            } else {
                return this.self.getValue() && mc.gameSettings.thirdPersonView != 0;
            }
        } else if (entityLivingBase instanceof EntityDragon || entityLivingBase instanceof EntityWither) {
            return !entityLivingBase.isInvisible() && this.bossees.getValue();
        } else if (!(entityLivingBase instanceof EntityMob) && !(entityLivingBase instanceof EntitySlime)) {
            return (entityLivingBase instanceof EntityAnimal
                    || entityLivingBase instanceof EntityBat
                    || entityLivingBase instanceof EntitySquid
                    || entityLivingBase instanceof EntityVillager) && this.animals.getValue();
        } else if (entityLivingBase instanceof EntityCreeper) {
            return this.creepers.getValue();
        } else if (entityLivingBase instanceof EntityEnderman) {
            return this.endermans.getValue();
        } else {
            return entityLivingBase instanceof EntityBlaze ? this.blazes.getValue() : this.mobs.getValue();
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;
        if (this.isMyauMode()) return;

        int remaining = this.amount.getValue();

        for (Entity entity : TeamUtil.getLoadedEntitiesSorted().stream()
                .filter(e -> e instanceof EntityLivingBase)
                .sorted((a, b) -> {
                    int pa = getTagPriority((EntityLivingBase) a);
                    int pb = getTagPriority((EntityLivingBase) b);
                    if (pa != pb) return Integer.compare(pa, pb);
                    double da = mc.getRenderViewEntity().getDistanceToEntity(a);
                    double db = mc.getRenderViewEntity().getDistanceToEntity(b);
                    return Double.compare(da, db);
                })
                .collect(Collectors.toList())) {
            if (remaining-- <= 0) break;
            if (!(entity instanceof EntityLivingBase)) continue;
            if (!this.shouldRenderTags((EntityLivingBase) entity)) continue;
            if (!(entity instanceof EntityPlayer)) continue;
            if (!(entity.ignoreFrustumCheck || RenderUtil.isInViewFrustum(entity.getEntityBoundingBox(), 10.0))) continue;

            String teamName = TeamUtil.stripName(entity);
            String namePlain = EnumChatFormatting.getTextWithoutFormattingCodes(teamName);
            if (StringUtils.isBlank(namePlain)) continue;

            String nameFormatted = teamName;
            if (nameFormatted.indexOf('&') >= 0) {
                nameFormatted = ChatColors.formatColor(nameFormatted);
            }

            double distance = mc.getRenderViewEntity().getDistanceToEntity(entity);
            EntityPlayer player = (EntityPlayer) entity;

            ScaledResolution sr = new ScaledResolution(mc);
            int scaleFactor = sr.getScaleFactor();

            double wx = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, event.getPartialTicks());
            double wy = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, event.getPartialTicks()) + entity.getEyeHeight() + (entity.isSneaking() ? 0.225 : this.height.getValue());
            double wz = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, event.getPartialTicks());

            ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(event.getPartialTicks(), 0);
            float[] projected = GLUtil.project2D(
                    (float) (wx - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX()),
                    (float) (wy - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()),
                    (float) (wz - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()),
                    scaleFactor
            );
            mc.entityRenderer.setupOverlayRendering();

            if (projected == null || projected[2] < 0.0F || projected[2] >= 1.0F) continue;

            float screenX = projected[0];
            float screenY = projected[1];

            int bgColor = new Color(0, 0, 0, 140).getRGB();
            float baseH = 36.0f;
            float baseW = baseH * 4.0f / 3.0f;

            String distText = String.format("%dm", (int) distance);
            float health = player.getHealth();
            float absorption = player.getAbsorptionAmount();
            float shownHealth = clamp(health + absorption, 0.0f, 999.0f);
            String hpText = String.format("%d", (int) shownHealth);

            float iconSize = 10.0f;
            float itemSize = 12.0f;
            float padX = 4.0f;
            float padY = 3.0f;

            float distW = iconSize + 2.0f + FontRenderer.getStringWidth(distText);
            float hpW = FontRenderer.getStringWidth(hpText) + 2.0f + iconSize;
            float itemsW = 5.0f * itemSize + 4.0f * 1.0f;
            float contentW = distW + 6.0f + itemsW + 6.0f + hpW;
            float nameW = mc.fontRendererObj.getStringWidth(nameFormatted);
            float bgW = Math.max(baseW, Math.max(contentW + padX * 2.0f, nameW + padX * 2.0f));
            float bgH = baseH;
            float tagScale = this.scale.getValue();
            float r = this.rounding.getValue();

            float sBgW = bgW * tagScale;
            float sBgH = bgH * tagScale;
            float sR = r * tagScale;

            float left = screenX - sBgW / 2.0f;
            float top = screenY - sBgH;
            float right = screenX + sBgW / 2.0f;
            float bottom = screenY;

            epilogue.util.render.PostProcessing.drawBlur(left, top, right, bottom, () -> () ->
                    epilogue.util.render.RenderUtil.drawRoundedRect(left, top, sBgW, sBgH, sR, -1)
            );

            Framebuffer bloomBuffer = epilogue.util.render.PostProcessing.beginBloom();
            if (bloomBuffer != null) {
                epilogue.util.render.RenderUtil.drawRoundedRect(left, top, sBgW, sBgH, sR, PostProcessing.getBloomColor());
                mc.getFramebuffer().bindFramebuffer(false);
            }

            epilogue.util.render.RenderUtil.drawRoundedRect(left, top, sBgW, sBgH, sR, bgColor);

            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(1f, 1f, 1f, 1f);

            float sIcon = iconSize * tagScale;
            float sItem = itemSize * tagScale;
            float sPadX = padX * tagScale;
            float sPadY = padY * tagScale;

            float sDistW = sIcon + 2.0f * tagScale + FontRenderer.getStringWidth(distText) * tagScale;
            float sHpW = FontRenderer.getStringWidth(hpText) * tagScale + 2.0f * tagScale + sIcon;
            float sItemsW = 5.0f * sItem + 4.0f * (1.0f * tagScale);
            float sContentW = sDistW + 6.0f * tagScale + sItemsW + 6.0f * tagScale + sHpW;
            float sNameW = mc.fontRendererObj.getStringWidth(nameFormatted) * tagScale;

            float row1Y = top + sPadY;
            float row2Y = bottom - sPadY - mc.fontRendererObj.FONT_HEIGHT * tagScale;

            float cursorX = screenX - sContentW / 2.0f;

            mc.getTextureManager().bindTexture(POSITIONING_ICON);
            drawTexturedRect(cursorX, row1Y, cursorX + sIcon, row1Y + sIcon);
            cursorX += sIcon + 2.0f * tagScale;
            GlStateManager.pushMatrix();
            GlStateManager.translate(cursorX, row1Y + 1.0f * tagScale, 0.0f);
            GlStateManager.scale(tagScale, tagScale, 1.0f);
            FontRenderer.drawString(distText, 0.0f, 0.0f, 0xFFFFFFFF);
            GlStateManager.popMatrix();
            cursorX += FontRenderer.getStringWidth(distText) * tagScale + 6.0f * tagScale;

            ArrayList<ItemStack> items = getItemStacks(player);
            for (int i = 0; i < items.size(); i++) {
                ItemStack st = items.get(i);
                if (st != null) {
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(cursorX + (i * (sItem + (1.0f * tagScale))), row1Y - 2.0f * tagScale, 0.0f);
                    float itemScale = sItem / 16.0f;
                    GlStateManager.scale(itemScale, itemScale, 1.0f);
                    RenderUtil.renderItemInGUI(st, 0, 0);
                    GlStateManager.popMatrix();
                }
            }
            cursorX += sItemsW + 6.0f * tagScale;

            GlStateManager.pushMatrix();
            GlStateManager.translate(cursorX, row1Y + 1.0f * tagScale, 0.0f);
            GlStateManager.scale(tagScale, tagScale, 1.0f);
            FontRenderer.drawString(hpText, 0.0f, 0.0f, 0xFFFFFFFF);
            GlStateManager.popMatrix();
            cursorX += FontRenderer.getStringWidth(hpText) * tagScale + 2.0f * tagScale;
            mc.getTextureManager().bindTexture(HEART_ICON);
            drawTexturedRect(cursorX, row1Y, cursorX + sIcon, row1Y + sIcon);

            GlStateManager.pushMatrix();
            GlStateManager.translate(screenX - sNameW / 2.0f, row2Y, 0.0f);
            GlStateManager.scale(tagScale, tagScale, 1.0f);
            mc.fontRendererObj.drawString(nameFormatted, 0.0f, 0.0f, 0xFFFFFFFF, false);
            GlStateManager.popMatrix();

            GlStateManager.enableDepth();
            GlStateManager.disableBlend();

            epilogue.util.render.PostProcessing.endBloom(bloomBuffer);
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (this.isEnabled() && this.isMyauMode()) {
            RenderUtil.enableRenderState();
            int remaining = this.amount.getValue();
            for (Entity entity : TeamUtil.getLoadedEntitiesSorted().stream()
                    .filter(e -> e instanceof EntityLivingBase)
                    .sorted((a, b) -> {
                        int pa = getTagPriority((EntityLivingBase) a);
                        int pb = getTagPriority((EntityLivingBase) b);
                        if (pa != pb) return Integer.compare(pa, pb);
                        double da = mc.getRenderViewEntity().getDistanceToEntity(a);
                        double db = mc.getRenderViewEntity().getDistanceToEntity(b);
                        return Double.compare(da, db);
                    })
                    .collect(Collectors.toList())) {
                if (remaining-- <= 0) break;
                if (entity instanceof EntityLivingBase
                        && this.shouldRenderTags((EntityLivingBase) entity)
                        && (entity.ignoreFrustumCheck || RenderUtil.isInViewFrustum(entity.getEntityBoundingBox(), 10.0))) {
                    String teamName = TeamUtil.stripName(entity);
                    if (!StringUtils.isBlank(EnumChatFormatting.getTextWithoutFormattingCodes(teamName))) {
                        double x = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, event.getPartialTicks())
                                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
                        double y = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, event.getPartialTicks())
                                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()
                                + (double) entity.getEyeHeight();
                        double z = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, event.getPartialTicks())
                                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
                        double distance = mc.getRenderViewEntity().getDistanceToEntity(entity);
                        GlStateManager.pushMatrix();
                        GlStateManager.disableLighting();
                        GlStateManager.enableBlend();
                        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                        GlStateManager.disableAlpha();
                        GlStateManager.translate(x, y + (entity.isSneaking() ? 0.225 : this.height.getValue()), z);
                        GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0F, 0.0F, 1.0F, 0.0F);
                        float view = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
                        GlStateManager.rotate(mc.getRenderManager().playerViewX, view, 0.0F, 0.0F);
                        double scale = Math.pow(Math.min(Math.max(this.autoScale.getValue() ? distance : 0.0, 6.0), 128.0), 0.75) * 0.0075;
                        GlStateManager.scale(-scale * (double) this.scale.getValue(), -scale * (double) this.scale.getValue(), 1.0);

                        String distanceText = "";
                        switch (this.distanceMode.getValue()) {
                            case 1:
                                distanceText = String.format("&7%dm&r ", (int) distance);
                                break;
                            case 2:
                                distanceText = String.format("&a[&f%d&a]&r ", (int) distance);
                        }
                        float health = ((EntityLivingBase) entity).getHealth();
                        float absorption = ((EntityLivingBase) entity).getAbsorptionAmount();
                        float max = ((EntityLivingBase) entity).getMaxHealth();
                        float percent = Math.min(Math.max((health + absorption) / max, 0.0F), 1.0F);
                        String healText = "";
                        switch (this.healthMode.getValue()) {
                            case 1:
                                healText = String.format(" %d%s", (int) health, absorption > 0.0F ? String.format(" &6%d&r", (int) absorption) : "&r");
                                break;
                            case 2:
                                healText = String.format(
                                        " %s%s",
                                        healthFormatter.format((double) health / 2.0),
                                        absorption > 0.0F ? String.format(" &6%s&r", healthFormatter.format((double) absorption / 2.0)) : "&r"
                                );
                                break;
                            case 3:
                                if (entity instanceof EntityPlayer) {
                                    Scoreboard scoreboard = mc.theWorld.getScoreboard();
                                    if (scoreboard != null) {
                                        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(2);
                                        if (objective != null) {
                                            Score score = scoreboard.getValueFromObjective(entity.getName(), objective);
                                            if (score != null) {
                                                healText = String.format(" &e%d&r", score.getScorePoints());
                                            }
                                        }
                                    }
                                }
                        }
                        String color = ChatColors.formatColor(String.format("%s&f%s&r%s", distanceText, teamName, healText));
                        int width = mc.fontRendererObj.getStringWidth(color);
                        if (this.backgroundOpacity.getValue() > 0) {
                            Color textColor = !entity.isSneaking() && !entity.isInvisible()
                                    ? new Color(0.0F, 0.0F, 0.0F, (float) this.backgroundOpacity.getValue() / 100.0F)
                                    : new Color(0.33F, 0.0F, 0.33F, (float) this.backgroundOpacity.getValue() / 100.0F);
                            RenderUtil.enableRenderState();
                            RenderUtil.drawRect(
                                    (float) (-width) / 2.0F - 1.0F,
                                    (float) (-mc.fontRendererObj.FONT_HEIGHT) - 1.0F,
                                    (float) width / 2.0F + (this.shadow.getValue() ? 1.0F : 0.0F),
                                    this.shadow.getValue() ? 0.0F : -1.0F,
                                    textColor.getRGB()
                            );
                            RenderUtil.disableRenderState();
                        }
                        GlStateManager.disableDepth();
                        mc.fontRendererObj
                                .drawString(
                                        color,
                                        (float) (-width) / 2.0F,
                                        (float) (-mc.fontRendererObj.FONT_HEIGHT),
                                        ColorUtil.getHealthBlend(percent).getRGB(),
                                        this.shadow.getValue()
                                );
                        GlStateManager.enableDepth();
                        if (entity instanceof EntityPlayer) {
                            int height = mc.fontRendererObj.FONT_HEIGHT + 2;
                            if (this.armor.getValue()) {
                                ArrayList<ItemStack> renderingItems = getItemStacks((EntityPlayer) entity);
                                if (!renderingItems.isEmpty()) {
                                    int offset = renderingItems.size() * -8;
                                    for (int i = 0; i < renderingItems.size(); i++) {
                                        RenderUtil.renderItemInGUI(renderingItems.get(i), offset + i * 16, -height - 16);
                                    }
                                    height += 16;
                                }
                            }
                            if (this.effects.getValue()) {
                                List<PotionEffect> effects = ((EntityPlayer) entity)
                                        .getActivePotionEffects()
                                        .stream()
                                        .filter(potionEffect -> Potion.potionTypes[potionEffect.getPotionID()].hasStatusIcon())
                                        .collect(Collectors.toList());
                                if (!effects.isEmpty()) {
                                    GlStateManager.pushMatrix();
                                    GlStateManager.scale(0.5F, 0.5F, 1.0F);
                                    int offset = effects.size() * -9;
                                    for (int i = 0; i < effects.size(); i++) {
                                        RenderUtil.renderPotionEffect(effects.get(i), offset + i * 18, -(height * 2) - 18);
                                    }
                                    GlStateManager.popMatrix();
                                }
                            }
                            if (TeamUtil.isFriend((EntityPlayer) entity)) {
                                RenderUtil.enableRenderState();
                                float x1 = (float) (-width) / 2.0F - 1.0F;
                                view = (float) (-mc.fontRendererObj.FONT_HEIGHT) - 1.0F;
                                float y1 = (float) width / 2.0F + 1.0F;
                                float offset = this.shadow.getValue() ? 0.0F : -1.0F;
                                int friendColor = Epilogue.friendManager.getColor().getRGB();
                                RenderUtil.drawOutlineRect(x1, view, y1, offset, 1.5F, 0, friendColor);
                                RenderUtil.disableRenderState();
                            } else if (TeamUtil.isTarget((EntityPlayer) entity)) {
                                RenderUtil.enableRenderState();
                                float x1 = (float) (-width) / 2.0F - 1.0F;
                                view = (float) (-mc.fontRendererObj.FONT_HEIGHT) - 1.0F;
                                float y1 = (float) width / 2.0F + 1.0F;
                                float offset = this.shadow.getValue() ? 0.0F : -1.0F;
                                int targetColor = Epilogue.targetManager.getColor().getRGB();
                                RenderUtil.drawOutlineRect(x1, view, y1, offset, 1.5F, 0, targetColor);
                                RenderUtil.disableRenderState();
                            }
                        }
                        GlStateManager.enableAlpha();
                        GlStateManager.disableBlend();
                        GlStateManager.popMatrix();
                    }
                }
            }
            RenderUtil.disableRenderState();
        }
    }

    private static @NotNull ArrayList<ItemStack> getItemStacks(EntityPlayer entity) {
        ArrayList<ItemStack> renderingItems = new ArrayList<>();
        for (int i = 4; i >= 0; i--) {
            ItemStack itemStack;
            if (i == 0) {
                itemStack = entity.getHeldItem();
            } else {
                itemStack = entity.inventory.armorInventory[i - 1];
            }
            if (itemStack != null) {
                renderingItems.add(itemStack);
            }
        }
        return renderingItems;
    }
}
