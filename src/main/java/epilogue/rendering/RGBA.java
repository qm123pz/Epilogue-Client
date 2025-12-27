package epilogue.rendering;

public class RGBA {

    public static int color(int r, int g, int b) {
        return color(r, g, b, 255);
    }

    public static int color(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24)
                | ((r & 0xFF) << 16)
                | ((g & 0xFF) << 8)
                | (b & 0xFF);
    }

    public static int color(float r, float g, float b, float a) {
        return color((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
    }
}
