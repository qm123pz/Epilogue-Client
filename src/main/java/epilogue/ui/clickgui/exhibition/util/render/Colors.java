package epilogue.ui.clickgui.exhibition.util.render;

import net.minecraft.util.MathHelper;

import java.awt.*;

public class Colors {
    public static int getColor(Color color) {
        return getColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public static int getColor(Color color, int alpha) {
        return getColor(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp_int(alpha, 0, 255));
    }

    public static int getColor(int brightness) {
        return getColor(brightness, brightness, brightness, 255);
    }

    public static int getColor(int brightness, int alpha) {
        return getColor(brightness, brightness, brightness, alpha);
    }

    public static int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }

    public static int getColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= MathHelper.clamp_int(alpha, 0, 255) << 24;
        color |= MathHelper.clamp_int(red, 0, 255) << 16;
        color |= MathHelper.clamp_int(green, 0, 255) << 8;
        color |= MathHelper.clamp_int(blue, 0, 255);
        return color;
    }

    public static int getColorOpacity(int color, int alpha) {
        int red = (color >> 16 & 0xFF);
        int green = (color >> 8 & 0xFF);
        int blue = (color & 0xFF);
        return getColor(red, green, blue, MathHelper.clamp_int(alpha, 0, 255));
    }

}
