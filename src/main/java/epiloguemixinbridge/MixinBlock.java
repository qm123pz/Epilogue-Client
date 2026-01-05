package epiloguemixinbridge;

import epilogue.hooks.BlockHooks;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({Block.class})
public abstract class MixinBlock {
    @Inject(
            method = {"shouldSideBeRendered"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void shouldSideBeRendered(
            IBlockAccess iBlockAccess, BlockPos blockPos, EnumFacing enumFacing, CallbackInfoReturnable<Boolean> callbackInfoReturnable
    ) {
        BlockHooks.onShouldSideBeRendered((Block) (Object) this, iBlockAccess, blockPos, enumFacing, callbackInfoReturnable);
    }

    @Inject(
            method = {"getBlockLayer"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void getBlockLayer(CallbackInfoReturnable<EnumWorldBlockLayer> callbackInfoReturnable) {
        BlockHooks.onGetBlockLayer((Block) (Object) this, callbackInfoReturnable);
    }
}
