package epilogue.rendering;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.util.Stack;

public class StencilClipManager {
    public static class StencilState {
        int stencilValue;
        boolean colorMask;
        boolean depthMask;

        StencilState(int value, boolean color, boolean depth) {
            this.stencilValue = value;
            this.colorMask = color;
            this.depthMask = depth;
        }
    }

    public static Stack<StencilState> stencilStack = new Stack<>();
    public static int currentStencilValue = 0;

    public static void initialize() {
        epilogue.util.render.StencilUtil.checkSetupFBO(Minecraft.getMinecraft().getFramebuffer());
        GL11.glClearStencil(0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
    }

    static boolean depthMask = false;

    public static void beginClip() {
        if (currentStencilValue == 0) {
            initialize();
        }

        boolean colorMask = GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK);
        depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        stencilStack.push(new StencilState(currentStencilValue, colorMask, depthMask));

        GL11.glColorMask(false, false, false, false);
        GL11.glDepthMask(false);

        if (currentStencilValue == 0) {
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        } else {
            GL11.glStencilFunc(GL11.GL_EQUAL, currentStencilValue, 0xFF);
        }

        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INCR);
    }

    public static void updateClip() {
        currentStencilValue++;

        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(depthMask);

        GL11.glStencilFunc(GL11.GL_EQUAL, currentStencilValue, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }

    public static void beginClip(Runnable drawClipShape) {
        beginClip();
        drawClipShape.run();
        updateClip();
    }

    public static void endClip() {
        if (stencilStack.isEmpty()) {
            System.err.println("Stencil stack underflow");
            return;
        }

        StencilState state = stencilStack.pop();
        currentStencilValue = state.stencilValue;

        GL11.glColorMask(state.colorMask, state.colorMask, state.colorMask, state.colorMask);
        GL11.glDepthMask(state.depthMask);

        if (currentStencilValue > 0) {
            GL11.glStencilFunc(GL11.GL_EQUAL, currentStencilValue, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        } else {
            disable();
        }
    }

    public static void disable() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        currentStencilValue = 0;
    }

    public static void clear() {
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        currentStencilValue = 0;
        stencilStack.clear();
    }
}
