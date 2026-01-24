package epilogue.ui.clickgui.menu;

import epilogue.font.CustomFontRenderer;
import epilogue.font.FontTransformer;
import net.minecraft.client.Minecraft;

import java.awt.Font;
import java.util.Locale;

public final class Fonts {
    private static final FontTransformer TRANSFORMER = FontTransformer.getInstance();
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Font FALLBACK_TINY = firstFont(
            TRANSFORMER.getFont("MicrosoftYaHei", 28f),
            TRANSFORMER.getFont("MicrosoftYaHei Bold", 28f)
    );
    private static final Font FALLBACK_SMALL = firstFont(
            TRANSFORMER.getFont("MicrosoftYaHei", 32f),
            TRANSFORMER.getFont("MicrosoftYaHei Bold", 32f)
    );
    private static final Font FALLBACK_MEDIUM = firstFont(
            TRANSFORMER.getFont("MicrosoftYaHei", 36f),
            TRANSFORMER.getFont("MicrosoftYaHei Bold", 36f)
    );
    private static final Font FALLBACK_HEADING = firstFont(
            TRANSFORMER.getFont("MicrosoftYaHei Bold", 44f),
            TRANSFORMER.getFont("MicrosoftYaHei", 44f)
    );
    private static final Font FALLBACK_TITLE = firstFont(
            TRANSFORMER.getFont("MicrosoftYaHei Bold", 54f),
            TRANSFORMER.getFont("MicrosoftYaHei", 54f)
    );
    private static final Font TITLE = firstFont(
            TRANSFORMER.getFont("Inter_Bold", 54f),
            TRANSFORMER.getFont("Inter_SemiBold", 54f)
    );
    private static final Font HEADING = firstFont(
            TRANSFORMER.getFont("Inter_SemiBold", 44f),
            TRANSFORMER.getFont("Inter_Medium", 44f)
    );
    private static final Font MEDIUM_BOLD = firstFont(
            TRANSFORMER.getFont("Inter_SemiBold", 36f),
            TRANSFORMER.getFont("Inter_Medium", 36f)
    );
    private static final Font MEDIUM = firstFont(
            TRANSFORMER.getFont("Inter_Medium", 36f),
            TRANSFORMER.getFont("Inter_Regular", 36f)
    );
    private static final Font SMALL_BOLD = firstFont(
            TRANSFORMER.getFont("Inter_Medium", 32f),
            TRANSFORMER.getFont("Inter_Regular", 32f)
    );
    private static final Font SMALL = firstFont(
            TRANSFORMER.getFont("Inter_Regular", 32f)
    );
    private static final Font SMALLER_BOLD = firstFont(
            TRANSFORMER.getFont("Inter_Medium", 28f),
            TRANSFORMER.getFont("Inter_Regular", 28f)
    );
    private static final Font MINI_BOLD = firstFont(
            TRANSFORMER.getFont("Inter_Medium", 26f),
            TRANSFORMER.getFont("Inter_Regular", 26f)
    );
    private static final Font TINY_BOLD = firstFont(
            TRANSFORMER.getFont("Inter_Medium", 28f),
            TRANSFORMER.getFont("Inter_Regular", 28f)
    );
    private static final Font TINY = firstFont(
            TRANSFORMER.getFont("Inter_Regular", 28f)
    );
    private static final Font MICRO = firstFont(
            TRANSFORMER.getFont("Inter_Regular", 24f)
    );
    private static final Font ICON = firstFont(
            TRANSFORMER.getFont("icon2", 42f)
    );
    private static final Font SKEET_ICON = firstFont(
            TRANSFORMER.getFont("skeetui", 42f)
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

    public static Font mediumBold() {
        return MEDIUM_BOLD;
    }

    public static Font small() {
        return SMALL;
    }

    public static Font smallBold() {
        return SMALL_BOLD;
    }

    public static Font smallerBold() {
        return SMALLER_BOLD;
    }

    public static Font miniBold() {
        return MINI_BOLD;
    }

    public static Font tiny() {
        return TINY;
    }

    public static Font tinyBold() {
        return TINY_BOLD;
    }

    public static Font micro() {
        return MICRO;
    }

    public static Font icon() {
        return ICON;
    }

    public static Font skeetIcon() {
        return SKEET_ICON;
    }

    public static Font msyhSmall() {
        return FALLBACK_SMALL;
    }

    public static Font msyhHeading() {
        return FALLBACK_HEADING;
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
            if (text != null) {
                for (int i = 0; i < text.length(); i++) {
                    if (text.charAt(i) > 255) {
                        Font fallback = font == TITLE ? FALLBACK_TITLE
                                : font == HEADING ? FALLBACK_HEADING
                                : font == MEDIUM ? FALLBACK_MEDIUM
                                : font == SMALL ? FALLBACK_SMALL
                                : font == TINY ? FALLBACK_TINY
                                : FALLBACK_TINY;
                        if (fallback != null) {
                            CustomFontRenderer.drawString(text, x, y, color, fallback);
                            return;
                        }
                        break;
                    }
                }
            }
            CustomFontRenderer.drawString(text, x, y, color, font);
        } else {
            Minecraft.getMinecraft().fontRendererObj.drawString(text, (int) x, (int) y, color, false);
        }
    }

    public static void drawWithShadow(Font font, String text, float x, float y, int color) {
        if (font != null) {
            CustomFontRenderer.drawStringWithShadow(text, x, y, color, font);
        } else {
            Minecraft.getMinecraft().fontRendererObj.drawString(text, (int) x, (int) y, color, true);
        }
    }

    public static void drawMCFormattedWithShadow(Font font, String text, float x, float y, int color) {
        if (font == null) {
            MC.fontRendererObj.drawString(text, (int) x, (int) y, color, true);
            return;
        }
        if (text == null || text.isEmpty()) return;

        int currentColor = color;
        float xx = x;
        StringBuilder segment = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                if (segment.length() > 0) {
                    String seg = segment.toString();
                    CustomFontRenderer.drawStringWithShadow(seg, xx, y, currentColor, font);
                    xx += CustomFontRenderer.getStringWidth(seg, font);
                    segment.setLength(0);
                }
                char code = Character.toLowerCase(text.charAt(i + 1));
                Integer rgb = mcColorCodeToRGB(code);
                if (rgb != null) {
                    currentColor = 0xFF000000 | rgb;
                } else if (code == 'r') {
                    currentColor = color;
                }
                i++;
                continue;
            }
            segment.append(c);
        }

        if (segment.length() > 0) {
            CustomFontRenderer.drawStringWithShadow(segment.toString(), xx, y, currentColor, font);
        }
    }

    public static int widthMCFormatted(Font font, String text) {
        if (font == null) {
            return MC.fontRendererObj.getStringWidth(text);
        }
        if (text == null || text.isEmpty()) return 0;
        int w = 0;
        StringBuilder segment = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                if (segment.length() > 0) {
                    w += CustomFontRenderer.getStringWidth(segment.toString(), font);
                    segment.setLength(0);
                }
                i++;
                continue;
            }
            segment.append(c);
        }
        if (segment.length() > 0) {
            w += CustomFontRenderer.getStringWidth(segment.toString(), font);
        }
        return w;
    }

    private static Integer mcColorCodeToRGB(char code) {
        String codes = "0123456789abcdef";
        int idx = codes.indexOf(code);
        if (idx >= 0) {
            return MC.fontRendererObj.getColorCode(code);
        }
        return null;
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
