package epilogue.ui.clickgui.exhibition.util;

public class Translate {

    private double x;
    private double y;
    private long lastMS;

    public Translate(double x, double y) {
        this.x = x;
        this.y = y;
        this.lastMS = System.currentTimeMillis();
    }

    public void interpolate(double targetX, double targetY, float smoothing) {
        long currentMS = System.currentTimeMillis();
        long delta = currentMS - lastMS;
        lastMS = currentMS;

        if (this.x == targetX && this.y == targetY) {
            return;
        }

        int deltaX = (int) (Math.abs(targetX - x) * smoothing);
        int deltaY = (int) (Math.abs(targetY - y) * smoothing);
        x = AnimationUtil.calculateCompensation(targetX, x, delta, deltaX);
        y = AnimationUtil.calculateCompensation(targetY, y, delta, deltaY);
    }

    public void updatePos(double targetX, double targetY, double speed) {
        long currentMS = System.currentTimeMillis();
        long delta = currentMS - lastMS;
        lastMS = currentMS;

        if (this.x == targetX && this.y == targetY) {
            return;
        }

        x = AnimationUtil.calculateCompensation(targetX, x, delta, speed);
        y = AnimationUtil.calculateCompensation(targetY, y, delta, speed);
    }

    public float getX() {
        return (float) x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public float getY() {
        return (float) y;
    }

    public void setY(double y) {
        this.y = y;
    }
}
