package epilogue.module.modules.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import epilogue.event.EventTarget;
import epilogue.events.PacketEvent;
import epilogue.events.Render2DEvent;
import epilogue.events.Render3DEvent;
import epilogue.module.Module;
import epilogue.util.render.PostProcessing;
import epilogue.util.render.RenderUtil;
import epilogue.value.values.IntValue;
import epiloguemixinbridge.IAccessorRenderManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ChestView extends Module {
    
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private final IntValue bgAlpha = new IntValue("Background Alpha", 120, 1, 255);
    
    private BlockPos currentContainerPos;
    private float[] cachedProjection;
    
    private float viewScale = 0.0f;
    private float targetScale = 0.0f;
    private long animationStartTime = 0;
    private boolean wasChestOpen = false;
    
    private final java.util.Map<Integer, ClickAnimation> clickAnimations = new java.util.HashMap<>();
    private final java.util.Map<Integer, ItemStack> lastItems = new java.util.HashMap<>();
    
    private float smoothX = 0.0f;
    private float smoothZ = 0.0f;
    
    private static final float ANIMATION_DURATION = 500f;
    private static final float CAMERA_SMOOTH_SPEED = 0.15f;
    
    public ChestView() {
        super("ChestView", false);
    }
    
    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (currentContainerPos == null) return;
        
        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor = sr.getScaleFactor();
        cachedProjection = calculate(currentContainerPos, scaleFactor);
    }
    
    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if(!this.isEnabled()) return;
        boolean chestOpen = mc.currentScreen instanceof GuiChest;
        
        if (chestOpen) {
            if (currentContainerPos == null) {
                currentContainerPos = findChestPosition();
            }
            
            if (!wasChestOpen) {
                animationStartTime = System.currentTimeMillis();
                targetScale = 1.0f;
                wasChestOpen = true;
            }
        } else if (!chestOpen && wasChestOpen) {
            animationStartTime = System.currentTimeMillis();
            targetScale = 0.0f;
            wasChestOpen = false;
        }
        
        updateAnimation();
        updateCameraSmooth();
        updateClickAnimations();

        if (viewScale < 0.01f) {
            if (!chestOpen) {
                currentContainerPos = null;
                cachedProjection = null;
            }
            return;
        }
        
        if (!chestOpen) {
            if (currentContainerPos == null || cachedProjection == null) return;
        } else {
            if (mc.thePlayer.openContainer == null) return;
            if (currentContainerPos == null || cachedProjection == null) return;
        }
        
        Container container = mc.thePlayer.openContainer;
        int slots = container.inventorySlots.size();
        
        detectItemChanges(container);
        
        if (slots > 0) {
            float[] projection = cachedProjection;
            
            float roundX = projection[0] - (164 / 2F) + smoothX;
            float roundY = projection[1] / 1.5F + smoothZ;
            
            GlStateManager.pushMatrix();
            
            float centerX = roundX + 82;
            float centerY = roundY + 30;
            GlStateManager.translate(centerX, centerY, 0);
            GlStateManager.scale(viewScale, viewScale, 1);
            GlStateManager.translate(-centerX, -centerY, 0);
            
            Framebuffer bloomBuffer = PostProcessing.beginBloom();
            if (bloomBuffer != null) {
                RenderUtil.drawRoundedRect(roundX, roundY, 164, 60, 3, epilogue.module.modules.render.PostProcessing.getBloomColor());
                mc.getFramebuffer().bindFramebuffer(false);
            }

            PostProcessing.drawBlur(roundX, roundY, roundX + 164, roundY + 60, () -> () -> RenderUtil.drawRoundedRect(roundX, roundY, 164, 60, 3, -1));
            
            RenderUtil.drawRoundedRect(roundX, roundY, 164, 60, 3, new Color(0, 0, 0, bgAlpha.getValue()));
            
            PostProcessing.endBloom(bloomBuffer);
            
            double startX = roundX + 5;
            double startY = roundY + 5;
            
            RenderItem itemRender = mc.getRenderItem();
            
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.zLevel = 200.0F;
            
            for (Slot slot : container.inventorySlots) {
                if (!slot.inventory.equals(mc.thePlayer.inventory)) {
                    int x = (int) (startX + (slot.slotNumber % 9) * 18);
                    int y = (int) (startY + (slot.slotNumber / 9) * 18);
                    
                    ClickAnimation animation = clickAnimations.get(slot.slotNumber);
                    if (animation != null) {
                        drawWaveAnimation(x, y, animation);
                    }
                    
                    itemRender.renderItemAndEffectIntoGUI(slot.getStack(), x, y);
                }
            }
            
            GlStateManager.popMatrix();
            
            itemRender.zLevel = 0.0F;
            GlStateManager.popMatrix();
            GlStateManager.disableLighting();
        }
    }
    
    public float[] calculate(BlockPos blockPos, int factor) {
        try {
            float renderX = (float) ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
            float renderY = (float) ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
            float renderZ = (float) ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
            
            float x = blockPos.getX() + 0.5f - renderX;
            float y = blockPos.getY() + 0.5f - renderY;
            float z = blockPos.getZ() + 0.5f - renderZ;
            
            float[] projectedCenter = project(x, y, z, factor);
            if (projectedCenter != null && projectedCenter[2] >= 0.0D && projectedCenter[2] < 1.0D) {
                return new float[]{projectedCenter[0], projectedCenter[1], projectedCenter[0], projectedCenter[1]};
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static float[] project(double x, double y, double z, int factor) {
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        FloatBuffer screenCoords = BufferUtils.createFloatBuffer(3);
        
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        
        boolean result = GLU.gluProject((float) x, (float) y, (float) z, modelView, projection, viewport, screenCoords);
        
        if (result) {
            float screenX = screenCoords.get(0) / factor;
            float screenY = (Display.getHeight() - screenCoords.get(1)) / factor;
            float screenZ = screenCoords.get(2);
            return new float[]{screenX, screenY, screenZ};
        }
        return null;
    }
    
    @EventTarget
    public void onPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();
        if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement wrapper = (C08PacketPlayerBlockPlacement) packet;
            if (wrapper.getPosition() != null) {
                Block block = mc.theWorld.getBlockState(wrapper.getPosition()).getBlock();
                if (block instanceof BlockContainer) {
                    currentContainerPos = wrapper.getPosition();
                }
            }
        }
    }
    
    public void addClickAnimation(int slot) {
        clickAnimations.put(slot, new ClickAnimation());
    }
    
    private void detectItemChanges(Container container) {
        if (container == null) return;
        
        for (Slot slot : container.inventorySlots) {
            if (!slot.inventory.equals(mc.thePlayer.inventory)) {
                ItemStack currentItem = slot.getStack();
                ItemStack lastItem = lastItems.get(slot.slotNumber);
                
                if (lastItem != null && currentItem == null) {
                    clickAnimations.put(slot.slotNumber, new ClickAnimation());
                }
                
                lastItems.put(slot.slotNumber, currentItem);
            }
        }
    }
    
    private void updateClickAnimations() {
        clickAnimations.entrySet().removeIf(entry -> entry.getValue().isFinished());
    }
    
    private void updateCameraSmooth() {
        if (mc.thePlayer == null) return;
        
        float targetX = (float) (Math.sin(System.currentTimeMillis() / 2000.0) * 2.0);
        float targetZ = (float) (Math.cos(System.currentTimeMillis() / 2000.0) * 2.0);
        
        smoothX += (targetX - smoothX) * CAMERA_SMOOTH_SPEED;
        smoothZ += (targetZ - smoothZ) * CAMERA_SMOOTH_SPEED;
    }
    
    private void drawWaveAnimation(float x, float y, ClickAnimation animation) {
        float alpha = animation.getAlpha();
        if (alpha <= 0) return;
        
        float centerX = x + 9;
        float centerY = y + 9;
        
        long elapsed = System.currentTimeMillis() - animation.getStartTime();
        float progress = Math.min(1.0f, elapsed / 600f);
        
        float waveSize = 18 * (1.0f + progress * 1.5f);
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        
        int waveAlpha = (int)(alpha * 180);
        Color waveColor = new Color(100, 200, 255, waveAlpha);
        
        float halfSize = waveSize / 2;
        RenderUtil.drawRoundedRect(centerX - halfSize, centerY - halfSize, waveSize, waveSize, 
                halfSize, waveColor);
        
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    
    private static class ClickAnimation {
        private final long startTime;
        private static final long DURATION = 600;
        
        public ClickAnimation() {
            this.startTime = System.currentTimeMillis();
        }
        
        public float getAlpha() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= DURATION) return 0.0f;
            
            float progress = (float) elapsed / DURATION;
            return 1.0f - progress;
        }
        
        public boolean isFinished() {
            return System.currentTimeMillis() - startTime >= DURATION;
        }
        
        public long getStartTime() {
            return startTime;
        }
    }
    
    private BlockPos findChestPosition() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return null;
        }
        
        double reach = 6.0;
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 lookVec = mc.thePlayer.getLook(1.0F);
        Vec3 reachVec = eyePos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach);
        
        MovingObjectPosition rayTrace = mc.theWorld.rayTraceBlocks(eyePos, reachVec, false, false, true);
        
        if (rayTrace != null && rayTrace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos hitPos = rayTrace.getBlockPos();
            if (mc.theWorld.getTileEntity(hitPos) instanceof TileEntityChest) {
                return hitPos;
            }
        }

        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    if (mc.theWorld.getTileEntity(checkPos) instanceof TileEntityChest) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null;
    }
    
    private void updateAnimation() {
        long elapsed = System.currentTimeMillis() - animationStartTime;
        float progress = Math.min(1.0f, elapsed / ANIMATION_DURATION);
        
        float easing = easeOutBack(progress);
        
        if (targetScale > viewScale) {
            viewScale = easing * targetScale;
        } else {
            viewScale = targetScale + (1.0f - easing) * (viewScale - targetScale);
        }
        
        if (Math.abs(viewScale - targetScale) < 0.01f) {
            viewScale = targetScale;
        }
    }
    
    private float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float)Math.pow(t - 1, 3) + c1 * (float)Math.pow(t - 1, 2);
    }
    
    private void drawShadow(float x, float y, float width, float height, float cornerRadius) {
        float shadowRadius = 5f;
        float shadowSpread = 0.03f;
        float shadowAlpha = 0.03f;
        
        if (shadowRadius <= 0 || shadowAlpha <= 0) return;
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        
        float spreadDistance = shadowRadius * shadowSpread;
        float blurDistance = shadowRadius - spreadDistance;
        
        int samples = Math.max(10, Math.min(24, (int)(shadowRadius * 1.8f)));
        
        for (int i = 0; i < samples; i++) {
            float t = (float)i / (float)(samples - 1);
            
            float currentSpread = t * spreadDistance;
            float currentBlur = t * blurDistance;
            float totalOffset = currentSpread + currentBlur;
            
            float alpha;
            if (t <= shadowSpread) {
                alpha = 0.6f * shadowAlpha;
            } else {
                float blurProgress = (t - shadowSpread) / (1.0f - shadowSpread);
                alpha = 0.6f * shadowAlpha * (1.0f - blurProgress * blurProgress);
            }
            
            float shadowX = x - totalOffset;
            float shadowY = y - totalOffset;
            float shadowWidth = width + totalOffset * 2.0f;
            float shadowHeight = height + totalOffset * 2.0f;
            
            drawGaussianBlurredRect(shadowX, shadowY, shadowWidth, shadowHeight, cornerRadius, alpha);
        }
        
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    
    private void drawGaussianBlurredRect(float x, float y, float width, float height, float cornerRadius, float alpha) {
        int blurLayers = 3;
        float blurSpread = 2.5f;
        
        for (int layer = 0; layer < blurLayers; layer++) {
            float layerProgress = (float)layer / (float)blurLayers;
            float layerAlpha = alpha * (1.0f - layerProgress * 0.35f);
            float layerExpand = layerProgress * blurSpread;
            
            float layerX = x - layerExpand;
            float layerY = y - layerExpand;
            float layerWidth = width + layerExpand * 2.0f;
            float layerHeight = height + layerExpand * 2.0f;
            
            RenderUtil.drawRoundedRect(
                    layerX, layerY, layerWidth, layerHeight,
                    cornerRadius,
                    new Color(0, 0, 0, (int)(layerAlpha * 255))
            );
        }
    }
}
