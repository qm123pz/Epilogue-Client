package epilogue.module.modules.misc;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.MoveInputEvent;
import epilogue.events.Render2DEvent;
import epilogue.events.SwapItemEvent;
import epilogue.events.TickEvent;
import epilogue.events.UpdateEvent;
import epilogue.management.RotationState;
import epilogue.module.Module;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import epilogue.util.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import org.lwjgl.opengl.GL11;

public class AutoBlockIn extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final Map<String, Integer> BLOCK_SCORE = new HashMap<String, Integer>();
    private long lastPlaceTime = 0L;
    public final FloatValue range = new FloatValue("Range", Float.valueOf(4.5f), Float.valueOf(3.0f), Float.valueOf(6.0f));
    public final IntValue speed = new IntValue("Speed", 20, 5, 100);
    public final IntValue placeDelay = new IntValue("Place Delay", 50, 0, 200);
    public final IntValue rotationTolerance = new IntValue("Rotation Tolerance", 25, 5, 100);
    public final BooleanValue showProgress = new BooleanValue("Show Progress", true);
    public final BooleanValue autoJump = new BooleanValue("AutoJump", false);
    public final ModeValue moveFix = new ModeValue("Move Fix", 1, new String[]{"None", "Silent", "Strict"});
    private float serverYaw;
    private float serverPitch;
    private float progress;
    private float aimYaw;
    private float aimPitch;
    private BlockPos targetBlock;
    private EnumFacing targetFacing;
    private Vec3 targetHitVec;
    private int lastSlot = -1;
    private static final int[][] DIRS = new int[][]{{1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 0, -1}};
    private static final double INSET = 0.05;
    private static final double STEP = 0.2;
    private static final double JIT = 0.020000000000000004;

    private boolean autoJumpWasBlocked = false;

    public AutoBlockIn() {
        super("AutoBlockIn", false);
        this.BLOCK_SCORE.put("obsidian", 0);
        this.BLOCK_SCORE.put("end_stone", 1);
        this.BLOCK_SCORE.put("planks", 2);
        this.BLOCK_SCORE.put("log", 2);
        this.BLOCK_SCORE.put("glass", 3);
        this.BLOCK_SCORE.put("stained_glass", 3);
        this.BLOCK_SCORE.put("hardened_clay", 4);
        this.BLOCK_SCORE.put("stained_hardened_clay", 4);
        this.BLOCK_SCORE.put("cloth", 5);
    }

    @Override
    public void onEnabled() {
        if (AutoBlockIn.mc.thePlayer != null) {
            this.serverYaw = AutoBlockIn.mc.thePlayer.rotationYaw;
            this.serverPitch = AutoBlockIn.mc.thePlayer.rotationPitch;
            this.aimYaw = this.serverYaw;
            this.aimPitch = this.serverPitch;
            this.progress = 0.0f;
            this.lastSlot = AutoBlockIn.mc.thePlayer.inventory.currentItem;
            this.targetBlock = null;
            this.targetFacing = null;
            this.targetHitVec = null;
            this.lastPlaceTime = 0L;
        }
    }

    @Override
    public void onDisabled() {
        if (this.lastSlot != -1 && AutoBlockIn.mc.thePlayer != null && AutoBlockIn.mc.thePlayer.inventory.currentItem != this.lastSlot) {
            AutoBlockIn.mc.thePlayer.inventory.currentItem = this.lastSlot;
        }
        this.progress = 0.0f;
        this.targetBlock = null;
        this.targetFacing = null;
        this.targetHitVec = null;
    }

    @EventTarget(value=1)
    public void onUpdate(UpdateEvent event) {
        ItemStack currentHeld;
        boolean holdingBlock;
        if (!this.isEnabled()) {
            return;
        }
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (AutoBlockIn.mc.thePlayer == null || AutoBlockIn.mc.theWorld == null) {
            return;
        }
        if (AutoBlockIn.mc.currentScreen != null) {
            return;
        }

        this.handleAutoJump();

        this.serverYaw = event.getYaw();
        this.serverPitch = event.getPitch();
        this.updateProgress();
        int blockSlot = this.findBestBlockSlot();
        if (blockSlot != -1 && AutoBlockIn.mc.thePlayer.inventory.currentItem != blockSlot) {
            AutoBlockIn.mc.thePlayer.inventory.currentItem = blockSlot;
        }
        boolean bl = holdingBlock = (currentHeld = AutoBlockIn.mc.thePlayer.inventory.getCurrentItem()) != null && currentHeld.getItem() instanceof ItemBlock;
        if (!holdingBlock) {
            this.targetBlock = null;
            this.targetFacing = null;
            this.targetHitVec = null;
            return;
        }
        this.findBestPlacement();
        if (this.targetBlock != null && this.targetFacing != null && this.targetHitVec != null) {
            Vec3 eyes = AutoBlockIn.mc.thePlayer.getPositionEyes(1.0f);
            double dx = this.targetHitVec.xCoord - eyes.xCoord;
            double dy = this.targetHitVec.yCoord - eyes.yCoord;
            double dz = this.targetHitVec.zCoord - eyes.zCoord;
            double dist = Math.sqrt(dx * dx + dz * dz);
            float targetYaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
            float targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
            targetYaw = MathHelper.wrapAngleTo180_float((float)targetYaw);
            float yawDiff = MathHelper.wrapAngleTo180_float((float)(targetYaw - this.serverYaw));
            float pitchDiff = targetPitch - this.serverPitch;
            float maxTurn = ((Integer)this.speed.getValue()).floatValue();
            float yawStep = MathHelper.clamp_float((float)yawDiff, (float)(-maxTurn), (float)maxTurn);
            float pitchStep = MathHelper.clamp_float((float)pitchDiff, (float)(-maxTurn), (float)maxTurn);
            this.aimYaw = this.serverYaw + yawStep;
            this.aimPitch = MathHelper.clamp_float((float)(this.serverPitch + pitchStep), (float)-90.0f, (float)90.0f);
            event.setRotation(this.aimYaw, this.aimPitch, 6);
            event.setPervRotation((Integer)this.moveFix.getValue() != 0 ? this.aimYaw : AutoBlockIn.mc.thePlayer.rotationYaw, 6);
        }
    }

    private void handleAutoJump() {
        if (!((Boolean)this.autoJump.getValue()).booleanValue()) {
            this.autoJumpWasBlocked = false;
            return;
        }
        if (AutoBlockIn.mc.thePlayer == null || AutoBlockIn.mc.theWorld == null) {
            this.autoJumpWasBlocked = false;
            return;
        }
        if (!AutoBlockIn.mc.thePlayer.onGround) {
            this.autoJumpWasBlocked = false;
            return;
        }
        if (AutoBlockIn.mc.gameSettings.keyBindJump.isKeyDown()) {
            this.autoJumpWasBlocked = true;
            return;
        }

        Vec3 playerPos = AutoBlockIn.mc.thePlayer.getPositionVector();
        BlockPos feetPos = new BlockPos(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord);
        boolean blocked = true;
        for (int[] d : DIRS) {
            if (this.isAir(feetPos.add(d[0], 0, d[2])) || this.isAir(feetPos.add(d[0], 1, d[2]))) {
                blocked = false;
                break;
            }
        }

        if (blocked && !this.autoJumpWasBlocked) {
            AutoBlockIn.mc.thePlayer.jump();
        }
        this.autoJumpWasBlocked = blocked;
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled() && (Integer)this.moveFix.getValue() == 1 && RotationState.isActived() && RotationState.getPriority() == 6.0f && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    @EventTarget(value=1)
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (AutoBlockIn.mc.thePlayer == null || AutoBlockIn.mc.theWorld == null) {
            return;
        }
        if (AutoBlockIn.mc.currentScreen != null) {
            return;
        }
        if (this.targetBlock != null && this.targetFacing != null && this.targetHitVec != null) {
            if (!this.withinRotationTolerance(this.aimYaw, this.aimPitch)) {
                return;
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastPlaceTime >= (long)((Integer)this.placeDelay.getValue()).intValue()) {
                ItemStack heldStack;
                this.lastPlaceTime = currentTime;
                MovingObjectPosition mop = this.rayTraceBlock(this.aimYaw, this.aimPitch, ((Float)this.range.getValue()).floatValue());
                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mop.getBlockPos().equals((Object)this.targetBlock) && mop.sideHit == this.targetFacing && (heldStack = AutoBlockIn.mc.thePlayer.inventory.getCurrentItem()) != null && heldStack.getItem() instanceof ItemBlock) {
                    AutoBlockIn.mc.playerController.onPlayerRightClick(AutoBlockIn.mc.thePlayer, AutoBlockIn.mc.theWorld, heldStack, this.targetBlock, this.targetFacing, mop.hitVec);
                    AutoBlockIn.mc.thePlayer.swingItem();
                    this.targetBlock = null;
                    this.targetFacing = null;
                    this.targetHitVec = null;
                }
            }
        }
    }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (this.isEnabled()) {
            this.lastSlot = event.setSlot(this.lastSlot);
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || AutoBlockIn.mc.currentScreen != null) {
            return;
        }
        if (!((Boolean)this.showProgress.getValue()).booleanValue()) {
            return;
        }
        if (AutoBlockIn.mc.fontRendererObj == null) {
            return;
        }
        float scale = 1.0f;
        String text = String.format("Blocking: %.0f%%", Float.valueOf(this.progress * 100.0f));
        GL11.glPushMatrix();
        GL11.glScaled((double)scale, (double)scale, (double)0.0);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc((int)770, (int)771);
        ScaledResolution sr = new ScaledResolution(mc);
        int width = AutoBlockIn.mc.fontRendererObj.getStringWidth(text);
        Color color = this.getProgressColor();
        AutoBlockIn.mc.fontRendererObj.drawString(text, (float)sr.getScaledWidth() / 2.0f / scale - (float)width / 2.0f, (float)sr.getScaledHeight() / 5.0f * 2.0f / scale, color.getRGB() & 0xFFFFFF | 0xBF000000, true);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GL11.glPopMatrix();
    }

    private int findBestBlockSlot() {
        int bestSlot = -1;
        int bestScore = Integer.MAX_VALUE;
        for (int slot = 0; slot <= 8; ++slot) {
            net.minecraft.block.Block block;
            String blockName;
            Integer score;
            ItemStack stack = AutoBlockIn.mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack == null || stack.stackSize == 0 || !(stack.getItem() instanceof ItemBlock) || (score = this.BLOCK_SCORE.get(blockName = (block = ((ItemBlock)stack.getItem()).getBlock()).getUnlocalizedName().replace("tile.", ""))) == null || score >= bestScore) continue;
            bestScore = score;
            bestSlot = slot;
            if (score == 0) break;
        }
        return bestSlot;
    }

    private void findBestPlacement() {
        Vec3 playerPos = AutoBlockIn.mc.thePlayer.getPositionVector();
        BlockPos feetPos = new BlockPos(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord);
        Vec3 eye = AutoBlockIn.mc.thePlayer.getPositionEyes(1.0f);
        double reach = ((Float)this.range.getValue()).doubleValue();
        double reachSq = reach * reach;
        double rp12 = (reach + 1.0) * (reach + 1.0);
        BlockPos roofTarget = feetPos.up(2);
        if (!this.isAir(roofTarget)) {
            this.sidesAim(eye, reach, feetPos);
            return;
        }
        ArrayList<BlockData> supports = new ArrayList<BlockData>();
        int minX = (int)Math.floor(eye.xCoord - reach);
        int maxX = (int)Math.floor(eye.xCoord + reach);
        int minY = (int)Math.floor(eye.yCoord - 1.0);
        int maxY = (int)Math.floor(eye.yCoord + reach);
        int minZ = (int)Math.floor(eye.zCoord - reach);
        int maxZ = (int)Math.floor(eye.zCoord + reach);
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    Vec3 mid;
                    MovingObjectPosition mop;
                    double d2;
                    double dz;
                    double dy;
                    double dx;
                    BlockPos p = new BlockPos(x, y, z);
                    if (this.isAir(p) || (dx = (double)x + 0.5 - eye.xCoord) * dx + (dy = (double)y + 0.5 - eye.yCoord) * dy + (dz = (double)z + 0.5 - eye.zCoord) * dz > rp12 || (d2 = this.dist2PointAABB(eye, x, y, z)) > reachSq || (mop = AutoBlockIn.mc.theWorld.rayTraceBlocks(eye, mid = new Vec3((double)x + 0.5, (double)y + 0.5, (double)z + 0.5), false, false, false)) == null || !mop.getBlockPos().equals((Object)p)) continue;
                    supports.add(new BlockData(p, d2));
                }
            }
        }
        if (supports.isEmpty()) {
            this.sidesAim(eye, reach, feetPos);
            return;
        }
        supports.sort(Comparator.comparingDouble(a -> a.distance));
        for (BlockData bd : supports) {
            if (!this.tryPlaceOnBlock(bd.pos, eye, reach, roofTarget)) continue;
            return;
        }
        LinkedList<BlockPos> q = new LinkedList<BlockPos>();
        HashMap<BlockPos, BlockPos> parent = new HashMap<BlockPos, BlockPos>();
        HashSet<BlockPos> visited = new HashSet<BlockPos>();
        for (BlockData bd : supports) {
            BlockPos sup = bd.pos;
            for (EnumFacing f : EnumFacing.values()) {
                BlockPos node = sup.offset(f);
                if (!this.isAir(node) || visited.contains(node)) continue;
                visited.add(node);
                parent.put(node, null);
                q.add(node);
            }
        }
        BlockPos endNode = null;
        int nodesSeen = 0;
        while (!q.isEmpty() && nodesSeen < 8964) {
            BlockPos cur = (BlockPos)q.poll();
            ++nodesSeen;
            if (cur.distanceSq((Vec3i)roofTarget) <= 1.5) {
                endNode = cur;
                break;
            }
            for (EnumFacing f : EnumFacing.values()) {
                BlockPos nxt = cur.offset(f);
                if (visited.contains(nxt) || !this.isAir(nxt)) continue;
                visited.add(nxt);
                parent.put(nxt, cur);
                q.add(nxt);
            }
        }
        if (endNode == null) {
            this.sidesAim(eye, reach, feetPos);
            return;
        }
        ArrayList<BlockPos> path = new ArrayList<BlockPos>();
        BlockPos cur = endNode;
        while (cur != null) {
            path.add(cur);
            cur = (BlockPos)parent.get(cur);
        }
        Collections.reverse(path);
        for (BlockPos place : path) {
            if (!this.isAir(place)) continue;
            boolean placedThis = false;
            for (BlockData bd : supports) {
                BlockPos sup = bd.pos;
                if (!this.isAdjacent(sup, place) || !this.tryPlaceOnBlock(sup, eye, reach, place)) continue;
                return;
            }
            for (EnumFacing f : EnumFacing.values()) {
                BlockPos sup = place.offset(f);
                if (this.isAir(sup) || !this.tryPlaceOnBlock(sup, eye, reach, place)) continue;
                return;
            }
            if (!placedThis) continue;
            break;
        }
        this.sidesAim(eye, reach, feetPos);
    }

    private boolean isAdjacent(BlockPos a, BlockPos b) {
        int dz;
        int dy;
        int dx = Math.abs(a.getX() - b.getX());
        return dx + (dy = Math.abs(a.getY() - b.getY())) + (dz = Math.abs(a.getZ() - b.getZ())) == 1;
    }

    private boolean tryPlaceOnBlock(BlockPos supportBlock, Vec3 eye, double reach, BlockPos targetPos) {
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos placementPos = supportBlock.offset(facing);
            if (!placementPos.equals((Object)targetPos)) continue;
            int n = (int)Math.round(5.0);
            for (int r = 0; r <= n; ++r) {
                double v = (double)r * 0.2 + (Math.random() * 0.020000000000000004 * 2.0 - 0.020000000000000004);
                if (v < 0.0) {
                    v = 0.0;
                } else if (v > 1.0) {
                    v = 1.0;
                }
                for (int c = 0; c <= n; ++c) {
                    double u = (double)c * 0.2 + (Math.random() * 0.020000000000000004 * 2.0 - 0.020000000000000004);
                    if (u < 0.0) {
                        u = 0.0;
                    } else if (u > 1.0) {
                        u = 1.0;
                    }
                    Vec3 hitPos = this.getHitPosOnFace(supportBlock, facing, u, v);
                    float[] rot = this.getRotationsWrapped(eye, hitPos.xCoord, hitPos.yCoord, hitPos.zCoord);
                    MovingObjectPosition mop = this.rayTraceBlock(rot[0], rot[1], reach);
                    if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !mop.getBlockPos().equals((Object)supportBlock) || mop.sideHit != facing) continue;
                    this.targetBlock = supportBlock;
                    this.targetFacing = facing;
                    this.targetHitVec = mop.hitVec;
                    this.aimYaw = rot[0];
                    this.aimPitch = rot[1];
                    return true;
                }
            }
        }
        return false;
    }

    private void sidesAim(Vec3 eye, double reach, BlockPos feetPos) {
        ArrayList<BlockPos> goals = new ArrayList<BlockPos>();
        for (int[] d : DIRS) {
            BlockPos headPos = feetPos.add(d[0], 1, d[2]);
            if (!this.isAir(headPos)) continue;
            goals.add(headPos);
        }
        for (int[] d : DIRS) {
            BlockPos feetGoal = feetPos.add(d[0], 0, d[2]);
            if (!this.isAir(feetGoal)) continue;
            goals.add(feetGoal);
        }
        this.findBestForGoals(goals, eye, reach);
    }

    private void findBestForGoals(List<BlockPos> goals, Vec3 eye, double reach) {
        for (BlockPos goal : goals) {
            for (EnumFacing facing : EnumFacing.values()) {
                Vec3 center;
                BlockPos support = goal.offset(facing);
                if (this.isAir(support) || eye.distanceTo(center = new Vec3((double)support.getX() + 0.5, (double)support.getY() + 0.5, (double)support.getZ() + 0.5)) > reach) continue;
                int n = (int)Math.round(5.0);
                for (int r = 0; r <= n; ++r) {
                    double v = (double)r * 0.2 + (Math.random() * 0.020000000000000004 * 2.0 - 0.020000000000000004);
                    if (v < 0.0) {
                        v = 0.0;
                    } else if (v > 1.0) {
                        v = 1.0;
                    }
                    for (int c = 0; c <= n; ++c) {
                        double u = (double)c * 0.2 + (Math.random() * 0.020000000000000004 * 2.0 - 0.020000000000000004);
                        if (u < 0.0) {
                            u = 0.0;
                        } else if (u > 1.0) {
                            u = 1.0;
                        }
                        Vec3 hitPos = this.getHitPosOnFace(support, facing.getOpposite(), u, v);
                        float[] rot = this.getRotationsWrapped(eye, hitPos.xCoord, hitPos.yCoord, hitPos.zCoord);
                        MovingObjectPosition mop = this.rayTraceBlock(rot[0], rot[1], reach);
                        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !mop.getBlockPos().equals((Object)support) || mop.sideHit != facing.getOpposite()) continue;
                        this.targetBlock = support;
                        this.targetFacing = facing.getOpposite();
                        this.targetHitVec = mop.hitVec;
                        this.aimYaw = rot[0];
                        this.aimPitch = rot[1];
                        return;
                    }
                }
            }
        }
    }

    private Vec3 getHitPosOnFace(BlockPos block, EnumFacing face, double u, double v) {
        double x = (double)block.getX() + 0.5;
        double y = (double)block.getY() + 0.5;
        double z = (double)block.getZ() + 0.5;
        switch (face) {
            case DOWN: {
                y = (double)block.getY() + 0.05;
                x = (double)block.getX() + u;
                z = (double)block.getZ() + v;
                break;
            }
            case UP: {
                y = (double)block.getY() + 1.0 - 0.05;
                x = (double)block.getX() + u;
                z = (double)block.getZ() + v;
                break;
            }
            case NORTH: {
                z = (double)block.getZ() + 0.05;
                x = (double)block.getX() + u;
                y = (double)block.getY() + v;
                break;
            }
            case SOUTH: {
                z = (double)block.getZ() + 1.0 - 0.05;
                x = (double)block.getX() + u;
                y = (double)block.getY() + v;
                break;
            }
            case WEST: {
                x = (double)block.getX() + 0.05;
                z = (double)block.getZ() + u;
                y = (double)block.getY() + v;
                break;
            }
            case EAST: {
                x = (double)block.getX() + 1.0 - 0.05;
                z = (double)block.getZ() + u;
                y = (double)block.getY() + v;
            }
        }
        return new Vec3(x, y, z);
    }

    private boolean isAir(BlockPos pos) {
        net.minecraft.block.Block block = AutoBlockIn.mc.theWorld.getBlockState(pos).getBlock();
        return block == Blocks.air || block == Blocks.water || block == Blocks.flowing_water || block == Blocks.lava || block == Blocks.flowing_lava || block == Blocks.fire;
    }

    private void updateProgress() {
        Vec3 playerPos = AutoBlockIn.mc.thePlayer.getPositionVector();
        BlockPos feetPos = new BlockPos(playerPos.xCoord, playerPos.yCoord, playerPos.zCoord);
        int filled = 0;
        int total = 9;
        if (!this.isAir(feetPos.up(2))) {
            ++filled;
        }
        for (int[] d : DIRS) {
            if (!this.isAir(feetPos.add(d[0], 0, d[2]))) {
                ++filled;
            }
            if (this.isAir(feetPos.add(d[0], 1, d[2]))) continue;
            ++filled;
        }
        this.progress = (float)filled / (float)total;
    }

    private Color getProgressColor() {
        if (this.progress <= 0.33f) {
            return new Color(255, 85, 85);
        }
        if (this.progress <= 0.66f) {
            return new Color(255, 255, 85);
        }
        return new Color(85, 255, 85);
    }

    private MovingObjectPosition rayTraceBlock(float yaw, float pitch, double range) {
        float yawRad = (float)Math.toRadians(yaw);
        float pitchRad = (float)Math.toRadians(pitch);
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3 start = AutoBlockIn.mc.thePlayer.getPositionEyes(1.0f);
        Vec3 end = start.addVector(x * range, y * range, z * range);
        return AutoBlockIn.mc.theWorld.rayTraceBlocks(start, end);
    }

    private boolean withinRotationTolerance(float targetYaw, float targetPitch) {
        float dy = Math.abs(MathHelper.wrapAngleTo180_float((float)(targetYaw - this.serverYaw)));
        float dp = Math.abs(MathHelper.wrapAngleTo180_float((float)(targetPitch - this.serverPitch)));
        return dy <= (float)((Integer)this.rotationTolerance.getValue()).intValue() && dp <= (float)((Integer)this.rotationTolerance.getValue()).intValue();
    }

    private double dist2PointAABB(Vec3 p, int x, int y, int z) {
        double minX = x;
        double maxX = x + 1;
        double minY = y;
        double maxY = y + 1;
        double minZ = z;
        double maxZ = z + 1;
        double cx = this.clamp(p.xCoord, minX, maxX);
        double cy = this.clamp(p.yCoord, minY, maxY);
        double cz = this.clamp(p.zCoord, minZ, maxZ);
        double dx = p.xCoord - cx;
        double dy = p.yCoord - cy;
        double dz = p.zCoord - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    private double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private float[] getRotationsWrapped(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.xCoord;
        double dy = ty - eye.yCoord;
        double dz = tz - eye.zCoord;
        double hd = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        yaw = this.normYaw(yaw);
        float pitch = (float)Math.toDegrees(-Math.atan2(dy, hd));
        return new float[]{yaw, pitch};
    }

    private float normYaw(float yaw) {
        return (yaw = (yaw % 360.0f + 360.0f) % 360.0f) > 180.0f ? yaw - 360.0f : yaw;
    }

    public int getSlot() {
        return this.lastSlot;
    }

    private static class BlockData {
        BlockPos pos;
        double distance;

        BlockData(BlockPos pos, double distance) {
            this.pos = pos;
            this.distance = distance;
        }
    }
}
