package epilogue.util.render.animations.advanced.impl;

import epilogue.util.render.animations.advanced.Animation;
import epilogue.util.render.animations.advanced.Direction;

public class SmoothStepAnimation extends Animation {
    public SmoothStepAnimation(int ms, double endPoint) {
        super(ms, endPoint);
    }

    public SmoothStepAnimation(int ms, double endPoint, Direction direction) {
        super(ms, endPoint, direction);
    }

    @Override
    protected double getEquation(double x) {
        return x * x * (3 - 2 * x);
    }
}
