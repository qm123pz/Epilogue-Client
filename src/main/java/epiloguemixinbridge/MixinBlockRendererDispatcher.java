package epiloguemixinbridge;

import epilogue.hooks.BlockRendererDispatcherHooks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin({BlockRendererDispatcher.class})
public abstract class MixinBlockRendererDispatcher {
    @Inject(
            method = {"renderBlock"},
            at = {@At("HEAD")}
    )
    private void renderBlock(
            IBlockState iBlockState,
            BlockPos blockPos,
            IBlockAccess iBlockAccess,
            WorldRenderer worldRenderer,
            CallbackInfoReturnable<Boolean> callbackInfoReturnable
    ) {
        BlockRendererDispatcherHooks.onRenderBlock(iBlockState, blockPos, iBlockAccess, worldRenderer, callbackInfoReturnable);
    }
}
