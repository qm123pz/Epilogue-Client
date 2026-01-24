package epilogue.ui.clickgui.exhibition.util;

import net.minecraft.util.MathHelper;

public class AnimationUtil {

    public static double calculateCompensation(double target, double current, long delta, double speed) {
        if (target == current) return target;
        double calculatedStep = (Math.max(speed * Math.max(1, delta) / (1000D / 60D), 0.1));
        return current + MathHelper.clamp_double(target - current, -calculatedStep, calculatedStep);
    }
}
