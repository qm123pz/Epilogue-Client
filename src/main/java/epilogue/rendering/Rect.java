package epilogue.rendering;

import epilogue.rendering.rendersystem.RenderSystem;

public class Rect {

    public static void draw(double x, double y, double x2, double y2, int color, RectType type) {
        if (type == RectType.EXPAND) {
            RenderSystem.drawRect(x, y, x + x2, y + y2, color);
        } else if (type == RectType.ABSOLUTE_POSITION) {
            RenderSystem.drawRect(x, y, x2, y2, color);
        }
    }

    public static void draw(double x, double y, double width, double height, int color) {
        Rect.draw(x, y, width, height, color, RectType.EXPAND);
    }

    public enum RectType {
        EXPAND,
        ABSOLUTE_POSITION
    }
}
