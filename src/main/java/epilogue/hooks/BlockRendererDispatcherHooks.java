package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.render.BedESP;
import epilogue.module.modules.render.Xray;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class BlockRendererDispatcherHooks {
    private BlockRendererDispatcherHooks() {
    }

    public static void onRenderBlock(
            IBlockState iBlockState,
            BlockPos blockPos,
            IBlockAccess iBlockAccess,
            WorldRenderer worldRenderer,
            CallbackInfoReturnable<Boolean> callbackInfoReturnable
    ) {
        if (Epilogue.moduleManager != null) {
            BedESP bedESP = (BedESP) Epilogue.moduleManager.modules.get(BedESP.class);
            if (bedESP.isEnabled() && iBlockState.getBlock() instanceof BlockBed && iBlockState.getValue(BlockBed.PART) == EnumPartType.HEAD) {
                bedESP.beds.add(new BlockPos(blockPos));
            }
            Xray xray = (Xray) Epilogue.moduleManager.modules.get(Xray.class);
            if (xray.isEnabled() && xray.isXrayBlock(Block.getIdFromBlock(iBlockState.getBlock()))) {
                if (xray.checkBlock(blockPos)) {
                    xray.trackedBlocks.add(new BlockPos(blockPos));
                } else {
                    xray.trackedBlocks.remove(blockPos);
                }
            }
        }
    }
}
