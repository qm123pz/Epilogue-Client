package epilogue.module.modules.render;

import epilogue.event.EventTarget;
import epilogue.events.Render3DEvent;
import epiloguemixinbridge.IAccessorRenderManager;
import epilogue.module.Module;
import epilogue.value.values.ColorValue;
import epilogue.util.RenderUtil;
import epilogue.value.values.*;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.ModeValue;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.BlockObsidian;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;

public class BedESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final CopyOnWriteArraySet<BlockPos> beds = new CopyOnWriteArraySet<>();
    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Default", "Full"});
    public final ModeValue color = new ModeValue("Color", 0, new String[]{"Custom"});
    public final ColorValue customColorValue;
    public final PercentValue opacity;
    public final BooleanValue outline;
    public final BooleanValue obsidian;

    private java.awt.Color getColor() {
        switch (this.color.getValue()) {
            case 0:
                return new java.awt.Color(this.customColorValue.getValue());
            default:
                return new java.awt.Color(-1);
        }
    }

    private void drawObsidianBox(AxisAlignedBB axisAlignedBB) {
        if (this.outline.getValue()) {
            RenderUtil.drawBoundingBox(axisAlignedBB, 170, 0, 170, 255, 1.5F);
        }
        RenderUtil.drawFilledBox(axisAlignedBB, 170, 0, 170);
    }

    private void drawObsidian(BlockPos blockPos) {
        if (this.outline.getValue()) {
            RenderUtil.drawBlockBoundingBox(blockPos, 1.0, 170, 0, 170, 255, 1.5F);
        }
        RenderUtil.drawBlockBox(
                blockPos, 1.0, 170, 0, 170
        );
    }

    public BedESP() {
        super("BedESP", false);
        this.customColorValue = new ColorValue("custom-color", (int) 8085714755840333141L, () -> this.color.getValue() == 0);
        this.opacity = new PercentValue("opacity", 25);
        this.outline = new BooleanValue("outline", false);
        this.obsidian = new BooleanValue("obsidian", true);
    }

    public double getHeight() {
        return this.mode.getValue() == 1 ? 1.0 : 0.5625;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            RenderUtil.enableRenderState();
            for (BlockPos blockPos : this.beds) {
                IBlockState state = mc.theWorld.getBlockState(blockPos);
                if (state.getBlock() instanceof BlockBed && state.getValue(BlockBed.PART) == EnumPartType.HEAD) {
                    BlockPos opposite = blockPos.offset(state.getValue(BlockBed.FACING).getOpposite());
                    IBlockState oppositeState = mc.theWorld.getBlockState(opposite);
                    if (oppositeState.getBlock() instanceof BlockBed && oppositeState.getValue(BlockBed.PART) == EnumPartType.FOOT) {
                        if (this.obsidian.getValue()) {
                            for (EnumFacing facing : Arrays.asList(EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST)) {
                                BlockPos offsetX = blockPos.offset(facing);
                                BlockPos offsetZ = opposite.offset(facing);
                                boolean xObsidian = mc.theWorld.getBlockState(offsetX).getBlock() instanceof BlockObsidian;
                                boolean zObsidian = mc.theWorld.getBlockState(offsetZ).getBlock() instanceof BlockObsidian;
                                if (xObsidian && zObsidian) {
                                    this.drawObsidianBox(
                                            new AxisAlignedBB(
                                                    Math.min(offsetX.getX(), offsetZ.getX()),
                                                    offsetX.getY(),
                                                    Math.min(offsetX.getZ(), offsetZ.getZ()),
                                                    Math.max((double) offsetX.getX() + 1.0, (double) offsetZ.getX() + 1.0),
                                                    (double) offsetX.getY() + 1.0,
                                                    Math.max((double) offsetX.getZ() + 1.0, (double) offsetZ.getZ() + 1.0)
                                            )
                                                    .offset(
                                                            -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                                                            -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                                                            -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
                                                    )
                                    );
                                } else if (xObsidian) {
                                    this.drawObsidian(offsetX);
                                } else if (zObsidian) {
                                    this.drawObsidian(offsetZ);
                                }
                            }
                        }
                        AxisAlignedBB aabb = new AxisAlignedBB(
                                Math.min(blockPos.getX(), opposite.getX()),
                                blockPos.getY(),
                                Math.min(blockPos.getZ(), opposite.getZ()),
                                Math.max((double) blockPos.getX() + 1.0, (double) opposite.getX() + 1.0),
                                (double) blockPos.getY() + this.getHeight(),
                                Math.max((double) blockPos.getZ() + 1.0, (double) opposite.getZ() + 1.0)
                        )
                                .offset(
                                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
                                );
                        java.awt.Color color = this.getColor();
                        if (this.outline.getValue()) {
                            RenderUtil.drawBoundingBox(aabb, color.getRed(), color.getGreen(), color.getBlue(), 255, 1.5F);
                        }
                        RenderUtil.drawFilledBox(
                                aabb,
                                color.getRed(),
                                color.getGreen(),
                                color.getBlue()
                        );
                    }
                } else {
                    this.beds.remove(blockPos);
                }
            }
            RenderUtil.disableRenderState();
        }
    }

    @Override
    public void onEnabled() {
        if (mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
    }
}
