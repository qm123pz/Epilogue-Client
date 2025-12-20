package epilogue.ui.clickgui.menu;

import epilogue.font.CustomFontRenderer;
import epilogue.font.FontTransformer;
import net.minecraft.client.Minecraft;

import java.awt.Font;

public final class Fonts {
    private static final FontTransformer TRANSFORMER = FontTransformer.getInstance();
    private static final Font TITLE = firstFont(
            TRANSFORMER.getFont("Inter_Bold", 60f)
    );
    private static final Font HEADING = firstFont(
            TRANSFORMER.getFont("Inter_SemiBold", 48f)
    );
    private static final Font MEDIUM = firstFont(
            TRANSFORMER.getFont("Inter_Medium", 38f)
    );
    private static final Font SMALL = firstFont(
            TRANSFORMER.getFont("Inter_Regular", 34f)
    );
    private static final Font TINY = firstFont(
            TRANSFORMER.getFont("Inter_Regular", 30f)
    );
    private static final Font ICON = firstFont(
            TRANSFORMER.getFont("icon2", 42f)
    );

    private Fonts() {
    }

    public static Font title() {
        return TITLE;
    }

    public static Font heading() {
        return HEADING;
    }

    public static Font medium() {
        return MEDIUM;
    }

    public static Font small() {
        return SMALL;
    }

    public static Font tiny() {
        return TINY;
    }

    public static Font icon() {
        return ICON;
    }

    private static Font firstFont(Font... fonts) {
        if (fonts == null) return null;
        for (Font f : fonts) {
            if (f != null) return f;
        }
        return null;
    }

    public static void draw(Font font, String text, float x, float y, int color) {
        if (font != null) {
            CustomFontRenderer.drawString(text, x, y, color, font);
        } else {
            Minecraft.getMinecraft().fontRendererObj.drawString(text, (int) x, (int) y, color, false);
        }
    }

    public static int width(Font font, String text) {
        if (font != null) {
            return (int) Math.ceil(CustomFontRenderer.getStringWidth(text, font));
        }
        return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
    }

    public static int height(Font font) {
        if (font != null) {
            return (int) Math.ceil(CustomFontRenderer.getFontHeight(font));
        }
        return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
    }
}
