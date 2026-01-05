package epiloguemixinbridge;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockModelRenderer.class)
public interface IInvokerBlockModelRenderer {
    @Invoker("renderModelAmbientOcclusion")
    boolean callRenderModelAmbientOcclusion(
            IBlockAccess iBlockAccess,
            IBakedModel iBakedModel,
            Block block,
            BlockPos blockPos,
            WorldRenderer worldRenderer,
            boolean boolean6
    );
}
