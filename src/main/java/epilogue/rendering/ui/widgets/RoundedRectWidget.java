package epilogue.rendering.ui.widgets;

import epilogue.rendering.ui.AbstractWidget;

public class RoundedRectWidget extends AbstractWidget<RoundedRectWidget> {

    private int color = 0xFFFFFFFF;
    private double radius = 4;

    public RoundedRectWidget setColor(int color) {
        this.color = color;
        return this;
    }

    public RoundedRectWidget setRadius(double radius) {
        this.radius = radius;
        return this;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public void onRender(double mouseX, double mouseY) {
        this.roundedRect(this.getX(), this.getY(), this.getWidth(), this.getHeight(), radius, (color & 0x00FFFFFF) | (((int) (this.getAlpha() * 255) & 0xFF) << 24));
    }
}
