package epilogue.interfaces;

import java.util.ArrayList;
import java.util.List;

public interface SharedRenderingConstants {
    List<Runnable> BLOOM = new ArrayList<>();
    List<Runnable> BLUR = new ArrayList<>();

    default int reAlpha(int rgb, double alpha) {
        return epilogue.rendering.rendersystem.RenderSystem.reAlpha(rgb, alpha);
    }

    default int hexColor(float r, float g, float b, float a) {
        return epilogue.rendering.rendersystem.RenderSystem.hexColor(r, g, b, a);
    }

    default void roundedRect(double x, double y, double width, double height, double radius, int color) {
        epilogue.util.render.RenderUtil.drawRoundedRect((float) x, (float) y, (float) (x + width), (float) (y + height), (float) radius, color);
    }

    default void roundedRectTextured(double x, double y, double width, double height, double radius, float alpha) {
        roundedRect(x, y, width, height, radius, hexColor(1, 1, 1, alpha));
    }

    default void scaleAtPos(double x, double y, double scale) {
        epilogue.rendering.rendersystem.RenderSystem.translateAndScale(x, y, scale);
    }
}
