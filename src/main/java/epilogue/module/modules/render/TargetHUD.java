package epilogue.module.modules.render;

import epilogue.Epilogue;
import epilogue.enums.ChatColors;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.PacketEvent;
import epilogue.events.Render2DEvent;
import epilogue.module.Module;
import epilogue.module.modules.combat.Aura;
import epilogue.ui.clickgui.menu.Fonts;
import epilogue.value.values.BooleanValue;
import epilogue.util.ColorUtil;
import epilogue.util.RenderUtil;
import epilogue.util.TeamUtil;
import epilogue.util.TimerUtil;
import epilogue.value.values.*;
import net.minecraft.client.Minecraft;
import epilogue.font.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

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
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import epilogue.util.render.RoundedUtil;
import epilogue.util.render.PostProcessing;
import epilogue.util.render.StencilUtil;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Supplier;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat diffFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));

    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private EntityLivingBase lastTarget = null;
    private EntityLivingBase target = null;
    private ResourceLocation headTexture = null;

    private float oldHealth = 0.0F;
    private float newHealth = 0.0F;
    private float maxHealth = 0.0F;
    private long lastHurtTime = 0L;
    private static final long HURT_DURATION = 500L;
    private int lastTargetEntityId = -1;
    private float lastTargetHealth = -1.0F;
    private int lastTargetHurtTime = 0;
    private long lastParticleSpawnTime = 0L;
    private final List<HitParticle> hitParticles = new ArrayList<>();

    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Exhibition", "Myau", "Astolfo", "Adjust", "Moon", "Augustus", "Rise", "NeverLose", "Akrien"});
    public final ModeValue color = new ModeValue("Color", 0, new String[]{"Default", "Hud"}, () -> this.mode.getValue() == 1);

    private static final float SCALE = 1.0F;
    private static final float EXHIBITION_Y_OFFSET = 0.0F;
    private static final float MYAU_Y_OFFSET = 0.0F;
    private static final float ADJUST_Y_OFFSET = 2.0F;
    private static final float MOON_Y_OFFSET = 0.75F;
    private static final float AUGUSTUS_Y_OFFSET = 0.6F;
    private static final float RISE_Y_OFFSET = 0.25F;
    private static final float ASTOLFO_Y_OFFSET = 0.75F;
    private static final float NEVERLOSE_Y_OFFSET = 2.0F;
    private static final float AKRIEN_Y_OFFSET = 2.0F;
    public final PercentValue background = new PercentValue("Back Ground", 25, () -> this.mode.getValue() == 1);
    public final BooleanValue head = new BooleanValue("Head", true);
    public final BooleanValue indicator = new BooleanValue("Indicator", true, () -> this.mode.getValue() == 1);

    public final BooleanValue outline = new BooleanValue("Outline", false, () -> this.mode.getValue() == 1);
    public final BooleanValue animations = new BooleanValue("Animations", true, () -> this.mode.getValue() == 1);
    public final BooleanValue shadow = new BooleanValue("Text Shadow", true, () -> this.mode.getValue() == 1);
    public final BooleanValue kaOnly = new BooleanValue("Only KillAura", true);
    public final BooleanValue chatPreview = new BooleanValue("Chat Preview", false);

    private float anchorX = 0.0f;
    private float anchorY = 0.0f;

    private float lastWidth = 0.0f;
    private float lastHeight = 0.0f;

    public void renderAt(float x, float y) {
        this.anchorX = x;
        this.anchorY = y;
        render(new Render2DEvent(0.0f));
    }

    public float getLastWidth() {
        return lastWidth <= 0 ? 180f : lastWidth;
    }

    public float getLastHeight() {
        return lastHeight <= 0 ? 80f : lastHeight;
    }

    private void setLastSize(float w, float h) {
        this.lastWidth = w;
        this.lastHeight = h;
    }

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

    private ResourceLocation getSkin(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(entityLivingBase.getName());
            if (playerInfo != null) {
                return playerInfo.getLocationSkin();
            }
        }
        return null;
    }

    private Color getTargetColor(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer) entityLivingBase)) {
                return Epilogue.friendManager.getColor();
            }
            if (TeamUtil.isTarget((EntityPlayer) entityLivingBase)) {
                return Epilogue.targetManager.getColor();
            }
        }
        switch (this.color.getValue()) {
            case 0:
                if (!(entityLivingBase instanceof EntityPlayer)) {
                    return new Color(-1);
                }
                return TeamUtil.getTeamColor((EntityPlayer) entityLivingBase, 1.0F);
            case 1:
                return Color.CYAN;
            default:
                return new Color(-1);
        }
    }

    public TargetHUD() {
        super("TargetHUD", false, true);
    }

    public void render(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        EntityLivingBase currentTarget = this.resolveTarget();
        if (currentTarget == null && mc.currentScreen instanceof GuiChat) {
            currentTarget = mc.thePlayer;
        }

        if (currentTarget == null) {
            this.target = null;
            return;
        }

        if (this.target != currentTarget) {
            this.target = currentTarget;
            this.headTexture = null;
            this.animTimer.setTime();
            float heal = this.target.getHealth() / 2.0F + this.target.getAbsorptionAmount() / 2.0F;
            this.oldHealth = heal;
            this.newHealth = heal;

            this.lastTargetEntityId = currentTarget.getEntityId();
            this.lastTargetHealth = currentTarget.getHealth();
            this.lastTargetHurtTime = currentTarget.hurtTime;
        } else {
            updateHurtTrigger(currentTarget);
        }

        switch (this.mode.getValue()) {
            case 0:
                renderExhibitionMode();
                break;
            case 1:
                renderMyauMode();
                break;
            case 2:
                renderAstolfoMode();
                break;
            case 3:
                renderAdjustMode();
                break;
            case 4:
                renderMoonMode();
                break;
            case 5:
                renderAugustusMode();
                break;
            case 6:
                renderRiseMode();
                break;
            case 7:
                renderNeverLoseMode();
                break;
            case 8:
                renderAkrienMode();
                break;
        }
    }

    private void updateHurtTrigger(EntityLivingBase currentTarget) {
        if (currentTarget == null) return;
        if (currentTarget.getEntityId() != lastTargetEntityId) {
            lastTargetEntityId = currentTarget.getEntityId();
            lastTargetHealth = currentTarget.getHealth();
            lastTargetHurtTime = currentTarget.hurtTime;
            return;
        }

        boolean hurtTimeRisingEdge = (lastTargetHurtTime <= 0 && currentTarget.hurtTime > 0);
        boolean healthDecreased = (lastTargetHealth >= 0.0F && currentTarget.getHealth() < lastTargetHealth);

        if (hurtTimeRisingEdge || healthDecreased) {
            lastHurtTime = System.currentTimeMillis();
        }

        lastTargetHealth = currentTarget.getHealth();
        lastTargetHurtTime = currentTarget.hurtTime;
    }

    private float getHurtAlpha() {
        long timeSinceHurt = System.currentTimeMillis() - lastHurtTime;
        if (timeSinceHurt > HURT_DURATION) return 0.0f;
        float progress = (float) timeSinceHurt / HURT_DURATION;

        if (progress < 0.5f) {
            return progress * 2.0f;
        } else {
            return 2.0f - (progress * 2.0f);
        }
    }

    private Interface getInterfaceModule() {
        Module m = Epilogue.moduleManager.getModule("Interface");
        return m instanceof Interface ? (Interface) m : null;
    }

    private int themeColor(int counter) {
        Interface ui = getInterfaceModule();
        return ui != null ? ui.color(counter) : new Color(128, 255, 149).getRGB();
    }

    private int themeBgColor() {
        return new Color(0, 0, 0, 150).getRGB();
    }

    private float[] computePos(float width, float height) {
        return new float[]{anchorX / SCALE, anchorY / SCALE};
    }

    private void applyTargetHudTransform(float x, float y, float width, float height) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + width / 2.0F, y + height / 2.0F, -450.0F);
        float finalScale = SCALE;
        GlStateManager.scale(finalScale, finalScale, 1.0F);
        GlStateManager.translate(-width / 2.0F, -height / 2.0F, 0.0F);
    }

    private float animateLinear(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) <= 0.001f) return target;
        float add = diff / Math.max(1f, speed);
        if (diff > 0) {
            current += add;
            if (current > target) current = target;
        } else {
            current += add;
            if (current < target) current = target;
        }
        return current;
    }

    private float adjustHealthAnim = 0.0F;
    private float moonHealthAnim = 0.0F;
    private float augustusHealthAnim = 0.0F;
    private float riseHealthAnim = 0.0F;
    private float astolfoHealthAnim = 0.0F;
    private float akrienHealthAnim = 0.0F;

    private void drawHudPost(float x, float y, float width, float height, Supplier<Runnable> maskDrawer, Runnable bloomDrawer) {
        StencilUtil.checkSetupFBO(mc.getFramebuffer());
//        ScaledResolution sr = new ScaledResolution(mc);
//        float fixedY = y + (mc.displayHeight - sr.getScaledHeight()) - height;
//        PostProcessing.drawBlur(x, fixedY, x + width, fixedY + height, maskDrawer);
        PostProcessing.drawBlur(x, y, x + width, y + height, maskDrawer);
        Framebuffer bloomBuffer = PostProcessing.beginBloom();
        if (bloomBuffer != null) {
            if (bloomDrawer != null) {
                bloomDrawer.run();
            }
            PostProcessing.endBloom(bloomBuffer, 1);
        }
    }

    private static final class HitParticle {
        private final float x;
        private final float y;
        private final float vx;
        private final float vy;
        private final float size;
        private final long startTime;

        private HitParticle(float x, float y, float vx, float vy, float size, long startTime) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.startTime = startTime;
        }
    }

    private void spawnHitParticles(float headX, float headY, float headW, float headH) {
        int count = 5 + (int) (Math.random() * 4.0);
        long now = System.currentTimeMillis();

        float cx = headX + headW / 2.0F;
        float cy = headY + headH / 2.0F;

        for (int i = 0; i < count; i++) {
            float angle = (float) (Math.random() * Math.PI * 2.0);
            float speed = 45.0F + (float) Math.random() * 85.0F;
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed;

            float spawnJitter = 2.0F + (float) Math.random() * 4.0F;
            float px = cx + (float) ((Math.random() - 0.5) * spawnJitter);
            float py = cy + (float) ((Math.random() - 0.5) * spawnJitter);

            float size = 1.5F + (float) Math.random() * 2.0F;
            hitParticles.add(new HitParticle(px, py, vx, vy, size, now));
        }
    }

    private void renderHitParticles() {
        if (hitParticles.isEmpty()) return;

        long now = System.currentTimeMillis();

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();

        for (int i = hitParticles.size() - 1; i >= 0; i--) {
            HitParticle p = hitParticles.get(i);
            float age = (now - p.startTime) / 1000.0F;
            if (age >= 1.0F) {
                hitParticles.remove(i);
                continue;
            }

            float alpha = 1.0F - age;

            float damping = 0.78F;
            float t = age;
            float dampFactor = (float) Math.pow(damping, t * 10.0F);
            float px = p.x + p.vx * t * dampFactor;
            float py = p.y + p.vy * t * dampFactor + (22.0F * t * t);

            int color = new Color(255, 255, 255, (int) (255.0F * alpha)).getRGB();
            RoundedUtil.drawRound(px, py, p.size, p.size, p.size / 2.0F, new Color(color, true));
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void renderAkrienMode() {
        if (!(this.target instanceof EntityPlayer)) return;
        EntityPlayer target = (EntityPlayer) this.target;

        float width = 170.0F;
        float height = 43.0F;
        setLastSize(width, height + AKRIEN_Y_OFFSET);
        float[] pos = computePos(width, height);
        float x = pos[0];
        float y = pos[1] + AKRIEN_Y_OFFSET;

        float healthPercentage = target.getHealth() / target.getMaxHealth();
        float space = width - 2.0F;
        akrienHealthAnim = animateLinear(akrienHealthAnim, space * MathHelper.clamp_float(healthPercentage, 0.0F, 1.0F), 30.0F);

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        applyTargetHudTransform(x, y, width, height);

        RenderUtil.enableRenderState();
        RenderUtil.drawRect(0, 0, width, height, themeBgColor());
        RenderUtil.disableRenderState();
        epilogue.util.render.RenderUtil.drawBorderedRect(1.0F, 34.5F, space, 2.5F, 0.74F, new Color(0, 0, 0, 100).getRGB(), new Color(0, 0, 0, 100).getRGB());

        epilogue.util.render.RenderUtil.drawHorizontalGradientSideways(1.0F, 34.5F, akrienHealthAnim, 2.5F,
                new Color(40, 145, 90).getRGB(),
                new Color(170, 255, 220).getRGB());
        float armorRatio = MathHelper.clamp_float(target.getTotalArmorValue() / 20.0F, 0.0F, 1.0F);
        float armorAnim = space * armorRatio;
        epilogue.util.render.RenderUtil.drawHorizontalGradientSideways(
                1.0F,
                37.5F,
                armorAnim,
                2.0F,
                new Color(80, 190, 255).getRGB(),
                new Color(170, 230, 255).getRGB()
        );
        String text = String.format("%.1f", target.getHealth());
        String text2 = String.format("%.1f", mc.thePlayer.getDistanceToEntity(target));
        FontRenderer.drawStringWithShadow("Health: " + text, 32.5F, 18.0F, -1);
        FontRenderer.drawStringWithShadow("Distance: " + text2 + "m", 32.5F, 26.5F, -1);
        FontRenderer.drawStringWithShadow(target.getName(), 32.5F, 5.0F, -1);
        epilogue.util.render.RenderUtil.renderPlayer2D(target, 1.0F, 3.0F, 30.0F, 0, -1);

        GlStateManager.popMatrix();
    }

    private void renderMyauMode() {
        float health = (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / 2.0F;
        float abs = this.target.getAbsorptionAmount() / 2.0F;
        float heal = this.target.getHealth() / 2.0F + abs;

        if (!this.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
            this.oldHealth = this.newHealth;
            this.newHealth = heal;
            this.maxHealth = this.target.getMaxHealth() / 2.0F;
            if (this.oldHealth != this.newHealth) {
                this.animTimer.reset();
            }
        }

        ResourceLocation resourceLocation = this.getSkin(this.target);
        if (resourceLocation != null) {
            this.headTexture = resourceLocation;
        }

        float elapsedTime = (float) Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
        float healthRatio = Math.min(Math.max(RenderUtil.lerpFloat(this.newHealth, this.oldHealth, elapsedTime / 150.0F) / this.maxHealth, 0.0F), 1.0F);
        Color targetColor = this.getTargetColor(this.target);
        Color healthBarColor = this.color.getValue() == 0 ? ColorUtil.getHealthBlend(healthRatio) : targetColor;
        float healthDeltaRatio = Math.min(Math.max((health - heal + 1.0F) / 2.0F, 0.0F), 1.0F);
        Color healthDeltaColor = ColorUtil.getHealthBlend(healthDeltaRatio);

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(this.target)));
        int targetNameWidth = mc.fontRendererObj.getStringWidth(targetNameText);
        String healthText = ChatColors.formatColor(
                String.format("&r&f%s%s❤&r", healthFormat.format(heal), abs > 0.0F ? "&6" : "&c")
        );
        int healthTextWidth = mc.fontRendererObj.getStringWidth(healthText);
        String statusText = ChatColors.formatColor(String.format("&r&l%s&r", heal == health ? "D" : (heal < health ? "W" : "L")));
        int statusTextWidth = mc.fontRendererObj.getStringWidth(statusText);
        String healthDiffText = ChatColors.formatColor(
                String.format("&r%s&r", heal == health ? "0.0" : diffFormat.format(health - heal))
        );
        int healthDiffWidth = mc.fontRendererObj.getStringWidth(healthDiffText);

        float barContentWidth = Math.max(
                (float) targetNameWidth + (this.indicator.getValue() ? 2.0F + (float) statusTextWidth + 2.0F : 0.0F),
                (float) healthTextWidth + (this.indicator.getValue() ? 2.0F + (float) healthDiffWidth + 2.0F : 0.0F)
        );
        float headIconOffset = this.head.getValue() && this.headTexture != null ? 25.0F : 0.0F;
        float barTotalWidth = Math.max(headIconOffset + 70.0F, headIconOffset + 2.0F + barContentWidth + 2.0F);

        setLastSize(barTotalWidth, 27.0F + MYAU_Y_OFFSET);

        float[] pos = computePos(barTotalWidth, 27.0F);
        float posX = pos[0];
        float posY = pos[1] + MYAU_Y_OFFSET;

        float finalPosX = posX;
        float finalPosY = posY;
        float finalPosX1 = posX;
        float finalPosX2 = posX;
        float finalPosY1 = posY;
        float finalPosX3 = posX;
        float finalPosY2 = posY;
        float finalPosY3 = posY;
        drawHudPost(posX, posY - MYAU_Y_OFFSET, barTotalWidth, 27.0F,
                () -> () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RenderUtil.drawRect(finalPosX, finalPosY, finalPosX1 + barTotalWidth, finalPosY3 + 27.0F, -1);
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                },
                () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RenderUtil.drawRect(finalPosX2, finalPosY1, finalPosX3 + barTotalWidth, finalPosY2 + 27.0F, epilogue.module.modules.render.PostProcessing.getBloomColor(0));
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                });

        GlStateManager.pushMatrix();
        GlStateManager.translate(posX + barTotalWidth / 2.0F, posY + 13.5F, -450.0F);
        float finalScale = SCALE;
        GlStateManager.scale(finalScale, finalScale, 1.0F);
        GlStateManager.translate(-barTotalWidth / 2.0F, -13.5F, 0.0F);
        RenderUtil.enableRenderState();

        int backgroundColor = new Color(0.0F, 0.0F, 0.0F, (float) this.background.getValue() / 100.0F).getRGB();
        int outlineColor = this.outline.getValue() ? targetColor.getRGB() : new Color(0, 0, 0, 0).getRGB();
        RenderUtil.drawOutlineRect(0.0F, 0.0F, barTotalWidth, 27.0F, 1.5F, backgroundColor, outlineColor);
        RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, barTotalWidth - 2.0F, 25.0F, ColorUtil.darker(healthBarColor, 0.2F).getRGB());
        RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, headIconOffset + 2.0F + healthRatio * (barTotalWidth - 2.0F - headIconOffset - 2.0F), 25.0F, healthBarColor.getRGB());
        RenderUtil.disableRenderState();

        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.fontRendererObj.drawString(targetNameText, headIconOffset + 2.0F, 2.0F, -1, this.shadow.getValue());
        mc.fontRendererObj.drawString(healthText, headIconOffset + 2.0F, 12.0F, -1, this.shadow.getValue());

        if (this.indicator.getValue()) {
            mc.fontRendererObj.drawString(statusText, barTotalWidth - 2.0F - (float) statusTextWidth, 2.0F, healthDeltaColor.getRGB(), this.shadow.getValue());
            mc.fontRendererObj.drawString(healthDiffText, barTotalWidth - 2.0F - (float) healthDiffWidth, 12.0F, ColorUtil.darker(healthDeltaColor, 0.8F).getRGB(), this.shadow.getValue());
        }

        if (this.head.getValue() && this.headTexture != null) {
            GlStateManager.color(1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(this.headTexture);
            Gui.drawScaledCustomSizeModalRect(2, 2, 8.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
            Gui.drawScaledCustomSizeModalRect(2, 2, 40.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F);
        }

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private void renderAdjustMode() {
        if (!(this.target instanceof EntityPlayer)) return;
        EntityPlayer target = (EntityPlayer) this.target;

        float width = 160.0F;
        float height = 45.0F;
        setLastSize(width, height + ADJUST_Y_OFFSET);
        float[] pos = computePos(width, height);
        float x = pos[0];
        float y = pos[1] + ADJUST_Y_OFFSET;
        float padding = 2.0F;
        float healthPercentage = target.getHealth() / target.getMaxHealth();
        float healthWidth = (width - padding * 2.0F) * MathHelper.clamp_float(healthPercentage, 0.0F, 1.0F);
        adjustHealthAnim = animateLinear(adjustHealthAnim, healthWidth, 25.0F);

        String sheesh = healthFormat.format(target.getHealth());
        String healthDiff = mc.thePlayer.getHealth() < target.getHealth() ? "-" + sheesh : "+" + sheesh;

        int accent = themeColor(0);
        int barBg = epilogue.util.render.ColorUtil.darker(accent, 0.3f);

        applyTargetHudTransform(x, y, width, height);
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(0, 0, width, height, new Color(0, 0, 0, 150).getRGB());
        RenderUtil.drawRect(padding, height - 5.0F, width - padding * 2.0F, 4.0F, barBg);
        RenderUtil.drawRect(padding, height - 5.0F, adjustHealthAnim, 4.0F, accent);
        RenderUtil.disableRenderState();
        epilogue.util.render.RenderUtil.renderPlayer2D(target, padding, padding, 28 - padding, 0, -1);

        FontRenderer.drawStringWithShadow(target.getName(), padding + 30.0F, 3.0F + padding, -1);
        int diffW = mc.fontRendererObj.getStringWidth(healthDiff);
        FontRenderer.drawStringWithShadow(healthDiff, width - padding - diffW, height - 10.0F - padding, -1);

        List<ItemStack> items = new ArrayList<>();
        if (target.getHeldItem() != null) items.add(target.getHeldItem());
        for (int index = 3; index >= 0; index--) {
            ItemStack stack = target.inventory.armorInventory[index];
            if (stack != null) items.add(stack);
        }

        float ix = 30.0F + padding;
        for (ItemStack stack : items) {
            epilogue.util.render.RenderUtil.renderItemStack(stack, ix, 10.0F + padding, 1.0F, true, 0.5f);
            ix += 16.0F;
        }

        GlStateManager.popMatrix();
    }

    private void renderMoonMode() {
        if (!(this.target instanceof EntityPlayer)) return;
        EntityPlayer target = (EntityPlayer) this.target;

        float width = 150.0F;
        float height = 40.5F;
        setLastSize(width, height + MOON_Y_OFFSET);
        float[] pos = computePos(width, height);
        float x = pos[0];
        float y = pos[1] + MOON_Y_OFFSET;
        float healthPercentage = target.getHealth() / target.getMaxHealth();
        float space = (width - 48.0F) / 100.0F;
        float animTo = (100.0F * space) * MathHelper.clamp_float(healthPercentage, 0.0F, 1.0F);
        moonHealthAnim = animateLinear(moonHealthAnim, animTo, 30.0F);

        int accent = themeColor(0);
        int bg = themeBgColor();

        drawHudPost(x, y, width, height,
                () -> () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RoundedUtil.drawRound(x, y, width, height, 8, new Color(255, 255, 255, 255));
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                },
                () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RoundedUtil.drawRound(x, y, width, height, 8, new Color(epilogue.module.modules.render.PostProcessing.getBloomColor(0), true));
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                });

        applyTargetHudTransform(x, y, width, height);
        RoundedUtil.drawRound(0, 0, width, height, 8, new Color(bg, true));
        RoundedUtil.drawRound(42.0F, 26.5F, (100.0F * space), 8.0F, 4, new Color(0, 0, 0, 150));
        RoundedUtil.drawRound(42.0F, 26.5F, moonHealthAnim, 8.5F, 4, new Color(accent, true));
        float hurtAlpha = getHurtAlpha();
        float headX = 2.5F;
        float headY = 2.5F;
        float headS = 35.0F;
        float shrink = (target.hurtTime == 0 ? 0 : target.hurtTime) * 0.5F;
        float renderSize = headS - shrink;
        float centeredX = headX + (headS - renderSize) / 2.0F;
        float centeredY = headY + (headS - renderSize) / 2.0F;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        epilogue.util.render.RenderUtil.renderPlayer2D(target, centeredX, centeredY, renderSize, 10.0F, -1);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (hurtAlpha > 0.0f) {
            int red = new Color(255, 0, 0, (int) (185 * hurtAlpha)).getRGB();
            epilogue.util.render.RenderUtil.renderPlayer2D(target, centeredX, centeredY, renderSize, 10.0F, red);
        }
        if (lastHurtTime != 0L && lastParticleSpawnTime != lastHurtTime) {
            spawnHitParticles(centeredX, centeredY, renderSize, renderSize);
            lastParticleSpawnTime = lastHurtTime;
        }
        renderHitParticles();

        String text = String.format("%.1f", target.getHealth());
        FontRenderer.drawStringWithShadow(text + "HP", 40.0F, 17.0F, -1);
        FontRenderer.drawStringWithShadow(target.getName(), 40.0F, 6.0F, -1);

        GlStateManager.popMatrix();
    }

    private void renderAugustusMode() {
        if (!(this.target instanceof EntityPlayer)) return;
        EntityPlayer target = (EntityPlayer) this.target;

        float width = 160.0F;
        float height = 40.5F;
        setLastSize(width, height + AUGUSTUS_Y_OFFSET);
        float[] pos = computePos(width, height);
        float x = pos[0];
        float y = pos[1] + AUGUSTUS_Y_OFFSET;
        float hurtTime = (target.hurtTime == 0 ? 0 : target.hurtTime) * 0.5F;

        float healthPercentage = target.getHealth() / target.getMaxHealth();
        float space = (width - 51.0F) / 100.0F;
        float animTo = (100.0F * space) * MathHelper.clamp_float(healthPercentage, 0.0F, 1.0F);
        augustusHealthAnim = animateLinear(augustusHealthAnim, animTo, 30.0F);

        int accent = themeColor(0);

        drawHudPost(x, y, width, height,
                () -> () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RoundedUtil.drawRound(x, y, width, height, 8, new Color(255, 255, 255, 255));
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                },
                () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RoundedUtil.drawRound(x, y, width, height, 8, new Color(epilogue.module.modules.render.PostProcessing.getBloomColor(0), true));
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                });

        applyTargetHudTransform(x, y, width, height);
        RoundedUtil.drawRound(0, 0, width, height, 8, new Color(0, 0, 0, 100));
        RoundedUtil.drawRound(45.0F, 23.0F, (100.0F * space), 10.0F, 5, new Color(0, 0, 0, 255));
        RoundedUtil.drawRound(45.0F, 23.0F, augustusHealthAnim, 10.0F, 4, new Color(accent, true));
        float hurtAlpha = getHurtAlpha();
        float headX = 2.5F;
        float headY = 2.5F;
        float headS = 35.0F;
        float renderSize = headS - hurtTime;
        float centeredX = headX + (headS - renderSize) / 2.0F;
        float centeredY = headY + (headS - renderSize) / 2.0F;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        epilogue.util.render.RenderUtil.renderPlayer2D(target, centeredX, centeredY, renderSize, 15.0F, -1);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (hurtAlpha > 0.0f) {
            int red = new Color(255, 0, 0, (int) (185 * hurtAlpha)).getRGB();
            epilogue.util.render.RenderUtil.renderPlayer2D(target, centeredX, centeredY, renderSize, 15.0F, red);
        }
        if (lastHurtTime != 0L && lastParticleSpawnTime != lastHurtTime) {
            spawnHitParticles(centeredX, centeredY, renderSize, renderSize);
            lastParticleSpawnTime = lastHurtTime;
        }
        renderHitParticles();

        FontRenderer.drawStringWithShadow(target.getName(), 52.5F, 10.5F, -1);

        GlStateManager.popMatrix();
    }

    private void renderRiseMode() {
        if (!(this.target instanceof EntityPlayer)) return;
        EntityPlayer target = (EntityPlayer) this.target;

        float width = 160.0F;
        float height = 40.5F;
        setLastSize(width, height + RISE_Y_OFFSET);
        float[] pos = computePos(width, height);
        float x = pos[0];
        float y = pos[1] + RISE_Y_OFFSET;
        float healthPercentage = target.getHealth() / target.getMaxHealth();
        float space = (width - 48.0F) / 100.0F;
        float animTo = (100.0F * space) * MathHelper.clamp_float(healthPercentage, 0.0F, 1.0F);
        riseHealthAnim = animateLinear(riseHealthAnim, animTo, 30.0F);

        int accent = themeColor(0);
        int bg = themeBgColor();

        drawHudPost(x, y, width, height,
                () -> () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RoundedUtil.drawRound(x, y, width, height, 8, new Color(255, 255, 255, 255));
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                },
                () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RoundedUtil.drawRound(x, y, width, height, 8, new Color(epilogue.module.modules.render.PostProcessing.getBloomColor(0), true));
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                });

        applyTargetHudTransform(x, y, width, height);
        RoundedUtil.drawRound(0, 0, width, height, 8, new Color(bg, true));
        RoundedUtil.drawRound(42.0F, 22.0F, (100.0F * space), 6.0F, 3, new Color(0, 0, 0, 120));
        RoundedUtil.drawRound(42.0F, 22.0F, riseHealthAnim, 6.0F, 3, new Color(accent, true));
        RoundedUtil.drawRoundOutline(0, 0, width, height, 8, 1.0f, new Color(0, 0, 0, 0), new Color(accent, true));
        float hurtAlpha = getHurtAlpha();
        float headX = 4.0F;
        float headY = 3.3F;
        float headS = 33.0F;
        float shrink = (target.hurtTime == 0 ? 0 : target.hurtTime) * 0.5F;
        float renderSize = headS - shrink;
        float centeredX = headX + (headS - renderSize) / 2.0F;
        float centeredY = headY + (headS - renderSize) / 2.0F;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        epilogue.util.render.RenderUtil.renderPlayer2D(target, centeredX, centeredY, renderSize, 12.0F, -1);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (hurtAlpha > 0.0f) {
            int red = new Color(255, 0, 0, (int) (185 * hurtAlpha)).getRGB();
            epilogue.util.render.RenderUtil.renderPlayer2D(target, centeredX, centeredY, renderSize, 12.0F, red);
        }
        if (lastHurtTime != 0L && lastParticleSpawnTime != lastHurtTime) {
            spawnHitParticles(centeredX, centeredY, renderSize, renderSize);
            lastParticleSpawnTime = lastHurtTime;
        }
        renderHitParticles();

        String text = String.format("%.1f", target.getHealth());
        FontRenderer.drawStringWithShadow(target.getName(), 42.0F, 6.0F, -1);
        int hpW = mc.fontRendererObj.getStringWidth(text);
        FontRenderer.drawStringWithShadow(text, width - 6.0F - hpW, 6.0F, accent);
        GlStateManager.popMatrix();
    }

    private void renderNeverLoseMode() {
        if (!(this.target instanceof EntityPlayer)) return;
        EntityPlayer target = (EntityPlayer) this.target;

        float baseWidth = 140.0F;
        float height = 32.0F;
        float nameWidth = mc.fontRendererObj.getStringWidth(target.getName());
        float dynamicWidth = Math.max(baseWidth, nameWidth + 42.0F);

        setLastSize(dynamicWidth, height + NEVERLOSE_Y_OFFSET);

        float[] pos = computePos(dynamicWidth, height);
        float x = pos[0];
        float y = pos[1] + NEVERLOSE_Y_OFFSET;

        float xoffset = 1.0F;
        float yoffset = -1.0F;
        float hurtTime = (target.hurtTime == 0 ? 0 : target.hurtTime) * 0.5F;

        float ringRadius = 9.5F;
        float ringMargin = 4.0F;
        float circleX = xoffset + dynamicWidth - ringMargin - ringRadius;
        float circleY = yoffset + height / 2.0F;
        String text = String.format("%.0f", target.getHealth());
        float textWidth = Fonts.width(Fonts.tiny(), text);
        float textX = circleX - (textWidth / 2.0F) + 0.5F;
        float textY = circleY - (Fonts.height(Fonts.tiny()) / 2.0F) + 1.0F;

        int accent = themeColor(0);
        Color bg = new Color(0, 0, 0, 140);

        drawHudPost(x + xoffset, y + yoffset, dynamicWidth, height,
                () -> () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RoundedUtil.drawRound(x + xoffset, y + yoffset, dynamicWidth, height, 4, new Color(255, 255, 255, 255));
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                },
                () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RoundedUtil.drawRound(x + xoffset, y + yoffset, dynamicWidth, height, 4, new Color(epilogue.module.modules.render.PostProcessing.getBloomColor(0), true));
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                });

        applyTargetHudTransform(x, y, dynamicWidth, height);
        RoundedUtil.drawRound(xoffset, yoffset, dynamicWidth, height, 4, bg);

        float hurtAlpha = getHurtAlpha();
        float headX = xoffset + 3.0F;
        float headY = yoffset + 3.0F;
        float headS = 26.0F;
        float renderSize = headS - hurtTime;
        float centeredX = headX + (headS - renderSize) / 2.0F;
        float centeredY = headY + (headS - renderSize) / 2.0F;
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        epilogue.util.render.RenderUtil.renderPlayer2D(target, centeredX, centeredY, renderSize, 24.0F, -1);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (hurtAlpha > 0.0f) {
            int red = new Color(255, 0, 0, (int) (185 * hurtAlpha)).getRGB();
            epilogue.util.render.RenderUtil.renderPlayer2D(target, centeredX, centeredY, renderSize, 24.0F, red);
        }
        if (lastHurtTime != 0L && lastParticleSpawnTime != lastHurtTime) {
            spawnHitParticles(centeredX, centeredY, renderSize, renderSize);
            lastParticleSpawnTime = lastHurtTime;
        }
        renderHitParticles();

        RoundedUtil.drawCircle(circleX, circleY, ringRadius, -2.0F, 1.0F, new Color(0, 0, 0, 120), 1);
        RoundedUtil.drawCircle(circleX, circleY, ringRadius, 1.0F - target.getHealth() / target.getMaxHealth(), 1.0F, new Color(accent, true), 1);

        Fonts.draw(Fonts.small(), target.getName(), xoffset + 34.0F, yoffset + 8.0F, -1);
        Fonts.draw(
                Fonts.small(),
                "Distance: " + String.format("%.1f", target.getDistanceToEntity(mc.thePlayer)) + "m",
                xoffset + 34.0F,
                yoffset + 20.0F,
                accent
        );
        Fonts.draw(Fonts.tiny(), text, textX, textY, new Color(255, 255, 255, 200).getRGB());

        GlStateManager.popMatrix();
    }

    private void renderAstolfoMode() {
        if (!(this.target instanceof EntityPlayer)) return;
        EntityPlayer target = (EntityPlayer) this.target;

        float width = 160.0F;
        float height = 55.0F;
        setLastSize(width, height + ASTOLFO_Y_OFFSET);
        float[] pos = computePos(width, height);
        float x = pos[0];
        float y = pos[1] + ASTOLFO_Y_OFFSET;

        int accent = themeColor(1);

        drawHudPost(x, y, width, height,
                () -> () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RoundedUtil.drawRound(x, y, width, height, 0, new Color(255, 255, 255, 255));
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                },
                () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.disableTexture2D();
                    RoundedUtil.drawRound(x, y, width, height, 0, new Color(epilogue.module.modules.render.PostProcessing.getBloomColor(1), true));
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                });

        applyTargetHudTransform(x, y, width, height);

        RoundedUtil.drawRound(0, 0, width, height, 0, epilogue.util.render.ColorUtil.applyOpacity(new Color(0, 0, 0), 0.4F));

        GlStateManager.pushMatrix();
        GlStateManager.translate(22.0F, 38.0F, 0.0F);

        float largestSize = Math.max(target.height, target.width);
        float relativeScale = Math.max(largestSize / 1.8F, 1);

        GlStateManager.scale((float) -16 / relativeScale, (float) 16 / relativeScale, (float) 16 / relativeScale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-((float) Math.atan((float) 17 / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        RenderManager renderManager = mc.getRenderManager();
        renderManager.setPlayerViewY(180.0F);
        renderManager.setRenderShadow(false);
        renderManager.renderEntityWithPosYaw(target, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        renderManager.setRenderShadow(true);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

        mc.fontRendererObj.drawStringWithShadow(target.getName(), 50.0F, 6.0F, -1);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.5F, 1.5F, 1.5F);
        mc.fontRendererObj.drawStringWithShadow(
                String.format("%.1f", target.getHealth()) + " ❤",
                50.0F / 1.5F,
                22.0F / 1.5F,
                accent
        );
        GlStateManager.popMatrix();

        float healthWidth = (width - 54.0F);
        float clamped = MathHelper.clamp_float(target.getHealth() / target.getMaxHealth(), 0.0F, 1.0F);
        astolfoHealthAnim = animateLinear(astolfoHealthAnim, healthWidth * clamped, 30.0F);
        RoundedUtil.drawRound(48.0F, 42.0F, width - 54.0F, 7.0F, 0, epilogue.util.render.ColorUtil.applyOpacity(new Color(new Color(accent).darker().darker().darker().getRGB()), 1.0F));
        RoundedUtil.drawRound(48.0F, 42.0F, astolfoHealthAnim, 7.0F, 0, epilogue.util.render.ColorUtil.applyOpacity(new Color(accent), 1.0F));

        GlStateManager.popMatrix();
    }

    private void renderExhibitionMode() {
        if (!(this.target instanceof EntityPlayer)) return;
        ScaledResolution resolution = new ScaledResolution(mc);

        double boxWidth = 40 + mc.fontRendererObj.getStringWidth(target.getName());
        double renderWidth = Math.max(boxWidth, 120);

        setLastSize((float) renderWidth, 40.0F + EXHIBITION_Y_OFFSET);

        float[] pos = computePos((float) renderWidth, 40.0F);
        float posX = pos[0];
        float posY = pos[1] + EXHIBITION_Y_OFFSET;

        GlStateManager.pushMatrix();
        GlStateManager.translate(posX + (float) renderWidth / 2.0F, posY + 20.0F, 0);
        float finalScale = SCALE;
        GlStateManager.scale(finalScale, finalScale, 1.0F);
        GlStateManager.translate(-(float) renderWidth / 2.0F, -20.0F, 0);

        drawExhibitionBorderedRect(-2.5F, -2.5F, (float) renderWidth + 2.5F, 40 + 2.5F, 0.5F, getExhibitionColor(60), getExhibitionColor(10));
        drawExhibitionBorderedRect(-1.5F, -1.5F, (float) renderWidth + 1.5F, 40 + 1.5F, 1.5F, getExhibitionColor(60), getExhibitionColor(40));
        drawExhibitionBorderedRect(0, 0, (float) renderWidth, 40, 0.5F, getExhibitionColor(22), getExhibitionColor(60));
        drawExhibitionBorderedRect(2, 2, 38, 38, 0.5F, getExhibitionColor(0, 0), getExhibitionColor(10));
        drawExhibitionBorderedRect(2.5F, 2.5F, 38 - 0.5F, 38 - 0.5F, 0.5F, getExhibitionColor(17), getExhibitionColor(48));

        GL11.glScissor((int) ((posX + 3) * resolution.getScaleFactor()), (int) ((resolution.getScaledHeight() - (posY + 37)) * resolution.getScaleFactor()), (int) ((37 - 3) * resolution.getScaleFactor()), (int) ((37 - 3) * resolution.getScaleFactor()));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        drawEntityOnScreen(target);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
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
        double width = Math.max(mc.fontRendererObj.getStringWidth(target.getName()), 60);

        width = getIncremental(width, 10);
        if (width < 60) {
            width = 60;
        }
        double healthLocation = width * progress;

        drawExhibitionBorderedRect(37, 12, 39 + (float) width, 16, 0.5F, getExhibitionColor(0, 0), getExhibitionColor(0));
        drawExhibitionRect(38 + (float) healthLocation + 0.5F, 12.5F, 38 + (float) width + 0.5F, 15.5F, getExhibitionColorOpacity(customColor.getRGB(), 35));
        drawExhibitionRect(37.5F, 12.5F, 38 + (float) healthLocation + 0.5F, 15.5F, customColor.getRGB());

        if (absorption > 0) {
            double absorptionDifferent = width * (absorption / (target.getMaxHealth() + absorption));
            drawExhibitionRect(38 + (float) healthLocation + 0.5F, 12.5F, 38 + (float) healthLocation + 0.5F + (float) absorptionDifferent, 15.5F, 0x80FFAA00);
        }

        for (int i = 1; i < 10; i++) {
            double dThing = (width / 10) * i;
            drawExhibitionRect(38 + (float) dThing, 12, 38 + (float) dThing + 0.5F, 16, getExhibitionColor(0));
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
                    }
                } else if ((itemStack.getItem() instanceof ItemArmor)) {
                    int pLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, itemStack);
                    if (pLevel > 0) {
                        drawEnchantTag("P" + getProtectionColor(pLevel) + pLevel, split, renderY);
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
        GlStateManager.rotate(-((float) Math.atan((float) 17 / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
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

    private void drawEntityOnScreen(int posX, int posY, int scale, float rotationYaw, float rotationPitch, EntityLivingBase ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posX, (float) posY, 50.0F);
        GlStateManager.scale((float) (-scale), (float) scale, (float) scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        float prevRenderYawOffset = ent.renderYawOffset;
        float prevRotationYaw = ent.rotationYaw;
        float prevRotationPitch = ent.rotationPitch;
        float prevRotationYawHead = ent.rotationYawHead;
        float prevPrevRotationYawHead = ent.prevRotationYawHead;
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-rotationPitch, 1.0F, 0.0F, 0.0F);
        ent.renderYawOffset = rotationYaw;
        ent.rotationYaw = rotationYaw;
        ent.rotationPitch = rotationPitch;
        ent.rotationYawHead = rotationYaw;
        ent.prevRotationYawHead = rotationYaw;
        RenderManager renderManager = mc.getRenderManager();
        renderManager.setPlayerViewY(180.0F);
        renderManager.setRenderShadow(false);
        renderManager.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        renderManager.setRenderShadow(true);
        ent.renderYawOffset = prevRenderYawOffset;
        ent.rotationYaw = prevRotationYaw;
        ent.rotationPitch = prevRotationPitch;
        ent.prevRotationYawHead = prevPrevRotationYawHead;
        ent.rotationYawHead = prevRotationYawHead;
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
            return blend(colorRange[0], colorRange[1], 1.0F - weight);
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
            case 1:
                return "§a";
            case 2:
                return "§9";
            case 3:
                return "§e";
            case 4:
                return "§c";
            default:
                return "§f";
        }
    }

    private String getSharpnessColor(int level) {
        switch (level) {
            case 1:
                return "§a";
            case 2:
                return "§9";
            case 3:
                return "§e";
            case 4:
                return "§6";
            case 5:
                return "§c";
            default:
                return "§f";
        }
    }

    private String getFireAspectColor(int level) {
        switch (level) {
            case 1:
                return "§6";
            case 2:
                return "§c";
            default:
                return "§f";
        }
    }

    private int getExhibitionColor(int brightness) {
        return getExhibitionColor(brightness, brightness, brightness, 255);
    }

    private int getExhibitionColor(int brightness, int alpha) {
        return getExhibitionColor(brightness, brightness, brightness, alpha);
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