package epilogue.ui.clickgui.exhibition.util.render;

import com.google.common.collect.Lists;
import net.minecraft.client.renderer.GlStateManager;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class Depth {

    private static final List<Integer> depth = Lists.newArrayList();

    public static void pre() {
        if (depth.isEmpty()) {
            GlStateManager.clearDepth(1.0);
            GlStateManager.clear(GL_DEPTH_BUFFER_BIT);
        }
    }

    public static void mask() {
        depth.add(0, glGetInteger(GL_DEPTH_FUNC));
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL_LESS);
        GlStateManager.colorMask(false, false, false, true);
    }

    public static void render() {
        render(GL_EQUAL);
    }

    public static void render(int gl) {
        GlStateManager.depthFunc(gl);
        GlStateManager.colorMask(true, true, true, true);
    }

    public static void post() {
        GlStateManager.depthFunc(depth.get(0));
        depth.remove(0);
    }
}
