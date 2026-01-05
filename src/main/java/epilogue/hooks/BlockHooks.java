package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.render.Xray;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class BlockHooks {
    private BlockHooks() {
    }

    public static void onShouldSideBeRendered(Block self, IBlockAccess iBlockAccess, BlockPos blockPos, EnumFacing enumFacing, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (Epilogue.moduleManager != null) {
            Xray xray = (Xray) Epilogue.moduleManager.modules.get(Xray.class);
            if (xray.isEnabled() && xray.mode.getValue() == 1 && xray.shouldRenderSide(Block.getIdFromBlock(self))) {
                BlockPos block = new BlockPos(
                        blockPos.getX() - enumFacing.getDirectionVec().getX(),
                        blockPos.getY() - enumFacing.getDirectionVec().getY(),
                        blockPos.getZ() - enumFacing.getDirectionVec().getZ()
                );
                if (xray.checkBlock(block)) {
                    callbackInfoReturnable.setReturnValue(true);
                }
            }
        }
    }

    public static void onGetBlockLayer(Block self, CallbackInfoReturnable<EnumWorldBlockLayer> callbackInfoReturnable) {
        if (Epilogue.moduleManager != null) {
            Xray xray = (Xray) Epilogue.moduleManager.modules.get(Xray.class);
            if (xray.isEnabled()) {
                int id = Block.getIdFromBlock(self);
                if (!xray.shouldRenderSide(id) || xray.mode.getValue() == 0 && !xray.isXrayBlock(id)) {
                    callbackInfoReturnable.setReturnValue(EnumWorldBlockLayer.TRANSLUCENT);
                }
            }
        }
    }
}
