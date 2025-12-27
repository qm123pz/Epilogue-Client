package epilogue.rendering.ui.widgets;

import epilogue.rendering.Rect;
import epilogue.rendering.ui.AbstractWidget;

public class RectWidget extends AbstractWidget<RectWidget> {

    private int color = 0xFFFFFFFF;

    public RectWidget setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public void onRender(double mouseX, double mouseY) {
        Rect.draw(this.getX(), this.getY(), this.getWidth(), this.getHeight(), (color & 0x00FFFFFF) | (((int) (this.getAlpha() * 255) & 0xFF) << 24));
    }
}
