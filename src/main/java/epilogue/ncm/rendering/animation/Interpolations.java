package epilogue.ncm.rendering.animation;

public class Interpolations {

    public static float interpBezier(float current, float target, float speed) {
        return (float) (current + (target - current) * speed);
    }

    public static double interpBezier(double current, double target, double speed) {
        return current + (target - current) * speed;
    }
}
