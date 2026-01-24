package epilogue.ui.clickgui.exhibition.util;

public class Opacity {

    private double opacity;

    public Opacity(int opacity) {
        this.opacity = opacity;
    }

    public double getOpacity() {
        return opacity;
    }

    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }

    public void interp(double target, double speed) {
        this.opacity = this.opacity + (target - this.opacity) / Math.max(1.0, speed);
    }

    public void interpolate(double target) {
        this.opacity = target;
    }

    public float getScale() {
        return (float) (opacity / 255.0);
    }
}
