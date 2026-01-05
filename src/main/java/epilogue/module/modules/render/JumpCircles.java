package epilogue.module.modules.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import epilogue.Epilogue;
import org.lwjgl.opengl.GL11;
import epilogue.event.EventTarget;
import epilogue.events.LoadWorldEvent;
import epilogue.events.UpdateEvent;
import epilogue.events.Render3DEvent;
import epilogue.module.Module;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.ModeValue;
import epilogue.value.values.FloatValue;
import epilogue.util.render.ColorUtil;
import epilogue.util.RenderUtil;
import epiloguemixinbridge.IAccessorMinecraft;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static epilogue.Epilogue.rand;

public class JumpCircles extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    @SuppressWarnings("unchecked")
    public ArrayList<ResourceLocation>[] animatedGroups = new ArrayList[]{new ArrayList<>(), new ArrayList<>()};

    private final FloatValue maxTime = new FloatValue("Max Time", 3000f, 2000f, 8000f);
    private final FloatValue radius = new FloatValue("Radius", 2f, 1f, 3f);
    private final ModeValue texture = new ModeValue("Texture", 3, new String[]{"KonchalEbal", "CubicalPieces", "Leeches", "Circle"});
    private final BooleanValue deepestLight = new BooleanValue("Deepest Light", true);
    private final String staticLoc = "epilogue/texture/jumpcircle/default/", animatedLoc = "epilogue/texture/jumpcircle/animated/";
    private final ResourceLocation JUMP_CIRCLE = new ResourceLocation("minecraft", staticLoc + "circle.png");
    private final ResourceLocation JUMP_KONCHAL = new ResourceLocation("minecraft", staticLoc + "konchal.png");
    private boolean jump = false;

    private ResourceLocation jumpTexture(int index, float progress) {
        if (texture.getModeString().equals("CubicalPieces") || texture.getModeString().equals("Leeches")) {
            ArrayList<ResourceLocation> currentGroupTextures = texture.getModeString().equals("CubicalPieces") ? animatedGroups[0] : animatedGroups[1];
            if (currentGroupTextures.isEmpty()) {
                return JUMP_CIRCLE;
            }
            final boolean animateByProgress = texture.getModeString().equals("Leeches");
            if (texture.getModeString().equals("Leeches")) {
                progress += .6F;
            }
            float frameOffset01 = progress % 1F;
            if (!animateByProgress) {
                final int ms = 1500;
                frameOffset01 = ((System.currentTimeMillis() + index) % ms) / (float) ms;
            }
            int frameIndex = (int) Math.min(frameOffset01 * (currentGroupTextures.size() - .5F), currentGroupTextures.size() - 1);
            return currentGroupTextures.get(frameIndex);
        } else {
            return texture.getModeString().equals("Circle") ? JUMP_CIRCLE : JUMP_KONCHAL;
        }
    }

    public void initResources() {
        ResourceLocation loc;
        final int[] groupsFramesLength = new int[]{100, 200};
        final String[] groupsFramesFormat = new String[]{"jpeg", "png"};
        int groupIndex = groupsFramesLength.length - 1;
        if (animatedGroups[0].isEmpty() && animatedGroups[1].isEmpty()) {
            while (groupIndex >= 0) {
                int framesCounter = 0;
                while (framesCounter < groupsFramesLength[groupIndex]) {
                    framesCounter++;
                    loc = new ResourceLocation("minecraft", animatedLoc + ("animation" + (groupIndex + 1)) + ("/circleframe_" + framesCounter) + ("." + groupsFramesFormat[groupIndex]));
                    animatedGroups[groupIndex].add(loc);
                }
                --groupIndex;
            }
        }
    }

    public JumpCircles() {
        super("JumpCircles", false);
        initResources();
    }

    private void addCircleForEntity(final Entity entity) {
        Vec3 vec = getVec3dFromEntity(entity).addVector(0.D, .005D, 0.D);
        BlockPos pos = new BlockPos(vec);
        IBlockState state = mc.theWorld.getBlockState(pos);
        if (state.getBlock() == Blocks.snow) {
            vec = vec.addVector(0.D, .125D, 0.D);
        }
        circles.add(new JumpRenderer(vec, circles.size()));
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;
        
        if (!mc.thePlayer.onGround) {
            jump = true;
        }
        if (mc.thePlayer.onGround && jump) {
            addCircleForEntity(mc.thePlayer);
            jump = false;
        }
    }

    @EventTarget
    private void onRender3D(Render3DEvent event) {
        if (!this.isEnabled()) return;
        circles.removeIf((final JumpRenderer circle) -> circle.getDeltaTime() >= 1.D);

        if (circles.isEmpty()) return;

        float deepestLightAnim = deepestLight.getValue() ? 0.3f : 0, immersiveStrengh = 0;
        if (deepestLightAnim >= 1.F / 255.F) {
            switch (texture.getModeString()) {
                case "Circle":
                case "Emission":
                    immersiveStrengh = .03F;
                    break;
                case "KonchalEbal":
                case "CubicalPieces":
                case "Inusual":
                    immersiveStrengh = .025F;
                    break;
                case "Leeches":
                    immersiveStrengh = .05F;
                    break;
            }
        }
        float finalImmersiveStrengh = immersiveStrengh;
        setupDraw(() -> circles.forEach(circle -> doCircle(circle.pos, radius.getValue(), 1.F - circle.getDeltaTime(), circle.getIndex() * 30, deepestLightAnim, finalImmersiveStrengh)));
    }

    private void doCircle(final Vec3 pos, double maxRadius, float deltaTime, int index, float immersiveShift, float immersiveIntense) {
        boolean immersive = immersiveShift >= 1.F / 255.F;
        float waveDelta = valWave01(1.F - deltaTime);
        float alphaPC = (float) easeOutCirc(valWave01(1 - deltaTime));
        if (deltaTime < .5F) alphaPC *= (float) easeInOutExpo(alphaPC);
        float radius = (float)((deltaTime > 0.5f ? easeOutElastic(waveDelta * waveDelta) : easeOutBack(waveDelta)) * maxRadius);
        double rotate = easeInOutElastic(waveDelta) * 90.D / (1.D + waveDelta);
        ResourceLocation res = jumpTexture(index, deltaTime);
        try {
            mc.getTextureManager().bindTexture(res);
            mc.getTextureManager().getTexture(res).setBlurMipmap(true, true);
        } catch (Exception e) {
            mc.getTextureManager().bindTexture(JUMP_CIRCLE);
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(pos.xCoord - radius / 2.D, pos.yCoord, pos.zCoord - radius / 2.D);
        GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
        RenderUtil.customRotatedObject2D(0, 0, radius, radius, (float)rotate);
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        int color = getColor(alphaPC);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        worldRenderer.pos(0, 0, 0).tex(0, 0).color(r, g, b, a).endVertex();
        worldRenderer.pos(0, radius, 0).tex(0, 1).color(r, g, b, a).endVertex();
        worldRenderer.pos(radius, radius, 0).tex(1, 1).color(r, g, b, a).endVertex();
        worldRenderer.pos(radius, 0, 0).tex(1, 0).color(r, g, b, a).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        if (immersive) {
            int[] colors = new int[4];
            colors[0] = getColor(alphaPC);
            colors[1] = getColor(alphaPC);
            colors[2] = getColor(alphaPC);
            colors[3] = getColor(alphaPC);
            GlStateManager.pushMatrix();
            GlStateManager.translate(pos.xCoord, pos.yCoord, pos.zCoord);
            GL11.glRotated(rotate, 0.0f, 1.0f, 0.0f);
            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            float polygons = 40, extMaxY = radius / 3.5F, extMaxXZ = radius / 7.F, minAPC = immersiveIntense * immersiveShift * 0.3f;
            float aPC;
            for (int i = 1; i < (int) polygons; i++) {
                float iPC = i / polygons, extY = extMaxY * i / polygons - extMaxY / polygons;
                if ((aPC = rand.lerp(alphaPC * minAPC, 0, iPC)) * 255 < 1) continue;
                float radiusPost = radius + (float) easeOutCirc(valWave01(iPC - 1.5F / polygons)) * extMaxXZ;
                int darkerColor0 = ColorUtil.darker(colors[0], aPC);
                int darkerColor1 = ColorUtil.darker(colors[1], aPC);
                int darkerColor2 = ColorUtil.darker(colors[2], aPC);
                int darkerColor3 = ColorUtil.darker(colors[3], aPC);
                worldRenderer.pos(-radiusPost / 2.F, extY, -radiusPost / 2.F).tex(0, 0).color((darkerColor0 >> 16) & 0xFF, (darkerColor0 >> 8) & 0xFF, darkerColor0 & 0xFF, (darkerColor0 >> 24) & 0xFF).endVertex();
                worldRenderer.pos(-radiusPost / 2.F, extY, radiusPost / 2.F).tex(0, 1).color((darkerColor1 >> 16) & 0xFF, (darkerColor1 >> 8) & 0xFF, darkerColor1 & 0xFF, (darkerColor1 >> 24) & 0xFF).endVertex();
                worldRenderer.pos(radiusPost / 2.F, extY, radiusPost / 2.F).tex(1, 1).color((darkerColor2 >> 16) & 0xFF, (darkerColor2 >> 8) & 0xFF, darkerColor2 & 0xFF, (darkerColor2 >> 24) & 0xFF).endVertex();
                worldRenderer.pos(radiusPost / 2.F, extY, -radiusPost / 2.F).tex(1, 0).color((darkerColor3 >> 16) & 0xFF, (darkerColor3 >> 8) & 0xFF, darkerColor3 & 0xFF, (darkerColor3 >> 24) & 0xFF).endVertex();
            }
            tessellator.draw();
            GlStateManager.popMatrix();
        }
    }

    @EventTarget
    private void onWorld(LoadWorldEvent event) {
        reset();
    }

    @Override
    public void onEnabled() {
        reset();
        super.onEnabled();
    }

    @Override
    public void onDisabled() {
        reset();
        jump = false;
        super.onDisabled();
    }

    private void reset() {
        circles.clear();
    }

    private final static List<JumpRenderer> circles = new java.util.ArrayList<>();

    private static Vec3 getVec3dFromEntity(final Entity entityIn) {
        final float PT = ((IAccessorMinecraft) Minecraft.getMinecraft()).getTimer().renderPartialTicks;
        final double dx = entityIn.posX - entityIn.lastTickPosX, dy = entityIn.posY - entityIn.lastTickPosY, dz = entityIn.posZ - entityIn.lastTickPosZ;
        return new Vec3((entityIn.lastTickPosX + dx * PT + dx * 2.D), (entityIn.lastTickPosY + dy * PT), (entityIn.lastTickPosZ + dz * PT + dz * 2.D));
    }

    private void setupDraw(final Runnable render) {
        final boolean light = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0);
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();
        if (light) GlStateManager.disableLighting();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GlStateManager.blendFunc(770, 1);
        RenderUtil.setupOrientationMatrix(0, 0, 0);
        render.run();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.color(1.F, 1.F, 1.F);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        if (light) GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.alphaFunc(GL11.GL_GREATER, .1F);
        GlStateManager.enableAlpha();
        GlStateManager.popMatrix();
    }

    private int getColor(float alphaPC) {
        Interface interfaceModule = (Interface) Epilogue.moduleManager.getModule("Interface");
        int colorize = interfaceModule.color();
        return ColorUtil.getOverallColorFrom(colorize, new Color(255, 255, 255, (int) (255.F * alphaPC)).getRGB(), .1F);
    }

    private final Tessellator tessellator = Tessellator.getInstance();
    private final WorldRenderer worldRenderer = tessellator.getWorldRenderer();

    private final class JumpRenderer {
        private final long time = System.currentTimeMillis();
        private final Vec3 pos;
        int index;

        private JumpRenderer(Vec3 pos, int index) {
            this.pos = pos;
            this.index = index;
        }

        private float getDeltaTime() {
            return (System.currentTimeMillis() - time) / maxTime.getValue();
        }

        private int getIndex() {
            return this.index;
        }
    }

    public static double easeOutBounce(double value) {
        double n1 = 7.5625, d1 = 2.75;
        if (value < 1 / d1) {
            return n1 * value * value;
        } else if (value < 2 / d1) {
            return n1 * (value -= 1.5 / d1) * value + 0.75;
        } else if (value < 2.5 / d1) {
            return n1 * (value -= 2.25 / d1) * value + 0.9375;
        } else {
            return n1 * (value -= 2.625 / d1) * value + 0.984375;
        }
    }

    public static double easeInOutElastic(double value) {
        double c5 = (2 * Math.PI) / 4.5;
        return value < 0 ? 0 : value > 1 ? 1 : value < 0.5 ? -(Math.pow(2, 20 * value - 10) * Math.sin((20 * value - 11.125) * c5)) / 2 : (Math.pow(2, -20 * value + 10) * Math.sin((20 * value - 11.125) * c5)) / 2 + 1;
    }

    public static double easeOutElastic(double value) {
        double c4 = (2 * Math.PI) / 3;
        return value < 0 ? 0 : value > 1 ? 1 : Math.pow(2, -10 * value) * Math.sin((value * 10 - 0.75) * c4) + 1;
    }

    public static double easeOutBack(double value) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(value - 1, 3) + c1 * Math.pow(value - 1, 2);
    }

    public static double easeOutCubic(double value) {
        return 1 - Math.pow(1 - value, 3);
    }

    public static float valWave01(float value) {
        return (value > .5 ? 1 - value : value) * 2.F;
    }

    public static double easeInOutQuad(double value) {
        return value < .5D ? 2.D * value * value : 1.D - Math.pow(-2.D * value + 2.D, 2.D) / 2.D;
    }

    public static double easeInOutQuadWave(double value) {
        value = (value > .5D ? 1.D - value : value) * 2.D;
        value = value < .5D ? 2.D * value * value : 1.D - Math.pow(-2.D * value + 2.D, 2.D) / 2.D;
        value = value > 1.D ? 1.D : value < 0.D ? 0.D : value;
        return value;
    }

    public static double easeInCircle(double value) {
        return 1 - Math.sqrt(1 - Math.pow(value, 2));
    }

    public static double easeOutCirc(double value) {
        return Math.sqrt(1 - Math.pow(value - 1, 2));
    }

    public static double easeInOutExpo(double value) {
        return value < 0 ? 0 : value > 1 ? 1 : value < 0.5 ? Math.pow(2, 20 * value - 10) / 2 : (2 - Math.pow(2, -20 * value + 10)) / 2;
    }

    public static float lerp(float value, float to, float pc) {
        return value + pc * (to - value);
    }
}
