package epilogue.module.modules.render;

import epilogue.Epilogue;
import epilogue.module.Module;
import epilogue.util.render.ColorUtil;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.ColorValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;

public class PostProcessing extends Module {
    private static PostProcessing instance;

    public final BooleanValue blur = new BooleanValue("Blur", true);
    public final IntValue blurStrength = new IntValue("Blur Strength", 12, 1, 200);

    public final BooleanValue testGlow = new BooleanValue("TestGlow", false);

    public final BooleanValue bloom = new BooleanValue("Bloom", false);
    public final ModeValue bloomColorMode = new ModeValue("Bloom Color Mode", 0, new String[]{"Custom", "Dynamic", "Rainbow", "Astolfo", "Fade", "Interface"});
    public final FloatValue bloomColorSpeed = new FloatValue("Bloom Color Speed", 2.0f, 1.0f, 10.0f);
    public final ColorValue bloomMainColor = new ColorValue("Bloom Color", 0xFF000000);
    public final ColorValue bloomSecondColor = new ColorValue("Bloom Second Color", 0xFF000000);
    public final FloatValue bloomAstolfoOffset = new FloatValue("Bloom Astolfo Offset", 5f, 0f, 20f);
    public final FloatValue bloomAstolfoIndex = new FloatValue("Bloom Astolfo Index", 107f, 0f, 200f);
    public final BooleanValue arrayListBloomFromInterface = new BooleanValue("ArrayList Bloom From Interface", true);
    public final BooleanValue newDynamicIslandBloomFromItSelf = new BooleanValue("Flat DynamicIsland Bloom From It Self", false);
    public final IntValue bloomIterations = new IntValue("Bloom Iterations", 5, 1, 10);
    public final IntValue bloomOffset = new IntValue("Bloom Offset", 3, 0, 10);

    public PostProcessing() {
        super("PostProcessing", true);
        instance = this;
    }

    public static PostProcessing getInstance() {
        return instance;
    }

    public static boolean isBlurEnabled() {
        return instance != null && instance.isEnabled() && instance.blur.getValue();
    }

    public static float getBlurStrength() {
        return instance != null ? instance.blurStrength.getValue().floatValue() : 0.0f;
    }

    public static boolean isBloomEnabled() {
        return instance != null && instance.isEnabled() && instance.bloom.getValue();
    }

    public static boolean isTestGlowEnabled() {
        return instance != null && instance.isEnabled() && instance.testGlow.getValue();
    }

    public static int getBloomIterations() {
        return instance != null ? instance.bloomIterations.getValue() : 1;
    }

    public static int getBloomColor() {
        return getBloomColor(0);
    }

    public static int getBloomColor(int counter) {
        if (instance == null) return 0xFF000000;
        String mode = instance.bloomColorMode.getModeString();
        if (mode.equals("Interface")) {
            Interface interfaceModule = (Interface) Epilogue.moduleManager.getModule("Interface");
            return interfaceModule != null ? interfaceModule.color(counter) : instance.calculateBloomColor(counter, 255);
        }
        return instance.calculateBloomColor(counter, 255);
    }

    private int calculateBloomColor(int counter, float opacity) {
        long ms = (long) (bloomColorSpeed.getValue() * 1000L);
        float progress = (float) (System.currentTimeMillis() % ms) / ms;

        int color = new java.awt.Color(bloomMainColor.getValue()).getRGB();
        String mode = bloomColorMode.getModeString();
        switch (mode) {
            case "Custom":
                color = ColorUtil.applyOpacity(new java.awt.Color(bloomMainColor.getValue()).getRGB(), opacity);
                break;
            case "Fade":
                color = ColorUtil.fadeBetween(
                        new java.awt.Color(bloomMainColor.getValue()).getRGB(),
                        new java.awt.Color(bloomSecondColor.getValue()).getRGB(),
                        (float) ((System.currentTimeMillis() + (long) counter * 100L) % ms) / ((float) ms / 2.0f)
                );
                break;
            case "Rainbow":
                color = ColorUtil.swapAlpha(ColorUtil.getRainbow(counter), opacity);
                break;
            case "Astolfo":
                color = ColorUtil.applyOpacity(
                        new java.awt.Color(ColorUtil.astolfoRainbow(
                                (int) (counter + (progress * 100)),
                                bloomAstolfoOffset.getValue().intValue(),
                                bloomAstolfoIndex.getValue().intValue()
                        )).getRGB(),
                        opacity
                );
                break;
            case "Dynamic":
                int main = new java.awt.Color(bloomMainColor.getValue()).getRGB();
                color = ColorUtil.fadeBetween(
                        main,
                        ColorUtil.darker(main, 0.3f),
                        (float) ((System.currentTimeMillis() + (ms + (long) counter * 100L)) % ms) / ((float) ms / 2.0f)
                );
                break;
        }
        return color;
    }

    public static boolean isArrayListBloomFromInterface() {
        return instance != null && instance.arrayListBloomFromInterface.getValue();
    }

    public static boolean isNewDynamicIslandBloomFromItSelf() {
        return instance != null && instance.newDynamicIslandBloomFromItSelf.getValue();
    }

    public static int getBloomOffset() {
        return instance != null ? instance.bloomOffset.getValue() : 1;
    }
}
