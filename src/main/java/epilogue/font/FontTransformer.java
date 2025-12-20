package epilogue.font;

import net.minecraft.client.Minecraft;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FontTransformer {
    private static FontTransformer instance;
    private final Map<String, Font> fontCache = new HashMap<>();
    private String selectedFontName = "Arial";
    private float selectedFontSize = 18.0f;

    private FontTransformer() {
        loadFonts();
    }

    public static FontTransformer getInstance() {
        if (instance == null) {
            instance = new FontTransformer();
        }
        return instance;
    }

    private void loadFonts() {
        loadFont("icon", "/assets/minecraft/epilogue/font/icon.ttf");
        loadFont("icon2", "/assets/minecraft/epilogue/font/icon2.ttf");
        loadFont("Arial", "/assets/minecraft/epilogue/font/Arial.ttf");
        loadFont("ArialBold", "/assets/minecraft/epilogue/font/ArialBold.ttf");
        loadFont("RobotoMedium", "/assets/minecraft/epilogue/font/Roboto-Medium.ttf");
        loadFont("JetBrainsMono", "/assets/minecraft/epilogue/font/jetbrains.ttf");
        loadFont("MicrosoftYaHei", "/assets/minecraft/epilogue/font/msyh-regular.ttf");
        loadFont("MicrosoftYaHei Bold", "/assets/minecraft/epilogue/font/msyh-bold.ttf");
        loadFont("bold", "/assets/minecraft/epilogue/font/bold.ttf");
        loadFont("Inter_Regular", "/assets/minecraft/epilogue/font/Inter_Regular.ttf");
        loadFont("Inter_Medium", "/assets/minecraft/epilogue/font/Inter_Medium.ttf");
        loadFont("Inter_SemiBold", "/assets/minecraft/epilogue/font/Inter_SemiBold.ttf");
        loadFont("Inter_Bold", "/assets/minecraft/epilogue/font/Inter_Bold.ttf");
        loadFont("RalewayExtraBold", "/assets/minecraft/epilogue/font/raleway-extrabold.ttf");
        loadFont("RobotoBlack", "/assets/minecraft/epilogue/font/roboto-black.ttf");
        loadFont("RobotoRegular", "/assets/minecraft/epilogue/font/roboto-regular.ttf");
        loadFont("ESP", "/assets/minecraft/epilogue/font/esp-1.ttf");
        loadFont("ESPBold", "/assets/minecraft/epilogue/font/esp-bold-3.ttf");
        loadFont("ESPItalic", "/assets/minecraft/epilogue/font/esp-ital-4.ttf");
        loadFont("ESPBoldItalic", "/assets/minecraft/epilogue/font/esp-bdit-2.ttf");
        loadFont("Consolas", "/assets/minecraft/epilogue/font/consola-1.ttf");
        loadFont("OpenSansBold", "/assets/minecraft/epilogue/font/OpenSans-Bold.ttf");
        loadFont("OpenSansBoldItalic", "/assets/minecraft/epilogue/font/OpenSans-BoldItalic.ttf");
        loadFont("OpenSansExtraBold", "/assets/minecraft/epilogue/font/OpenSans-ExtraBold.ttf");
        loadFont("OpenSansExtraBoldItalic", "/assets/minecraft/epilogue/font/OpenSans-ExtraBoldItalic.ttf");
        loadFont("OpenSansItalic", "/assets/minecraft/epilogue/font/OpenSans-Italic.ttf");
        loadFont("OpenSansLight", "/assets/minecraft/epilogue/font/OpenSans-Light.ttf");
        loadFont("OpenSansLightItalic", "/assets/minecraft/epilogue/font/OpenSans-LightItalic.ttf");
        loadFont("OpenSansRegular", "/assets/minecraft/epilogue/font/OpenSans-Regular.ttf");
        loadFont("OpenSansSemiBold", "/assets/minecraft/epilogue/font/OpenSans-Semibold.ttf");
        loadFont("OpenSansSemiBoldItalic", "/assets/minecraft/epilogue/font/OpenSans-SemiboldItalic.ttf");
        loadFont("SuperJoyful", "/assets/minecraft/epilogue/font/Super Joyful.ttf");
        loadFont("Cheri", "/assets/minecraft/epilogue/font/Cheri.ttf");
        loadFont("Cherl", "/assets/minecraft/epilogue/font/Cherl.ttf");
        loadFont("Fortalesia", "/assets/minecraft/epilogue/font/Fortalesia.ttf");
        loadFont("HarmonyOSRegular", "/assets/minecraft/epilogue/font/HarmonyOS-Regular.ttf");
        loadFont("HarmonyOSBold", "/assets/minecraft/epilogue/font/HarmonyOS-Bold.ttf");
        loadFont("HarmonyOSBlack", "/assets/minecraft/epilogue/font/HarmonyOS-Black.ttf");
        loadFont("JelloLight", "/assets/minecraft/epilogue/font/jellolight.ttf");
        loadFont("JelloMedium", "/assets/minecraft/epilogue/font/jellomedium.ttf");
        loadFont("JelloRegular", "/assets/minecraft/epilogue/font/jelloregular.ttf");
    }

    private void loadFont(String name, String path) {
        try {
            InputStream fontStream = FontTransformer.class.getResourceAsStream(path);
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                fontCache.put(name, font);
                fontStream.close();
            }
        } catch (Exception e) {
        }
    }

    public void setFont(String fontName, float size) {
        this.selectedFontName = fontName;
        this.selectedFontSize = size;
    }

    public String getSelectedFontName() {
        return selectedFontName;
    }

    public float getSelectedFontSize() {
        return selectedFontSize;
    }

    public Font getFont(String fontName, float size) {
        if (fontName.equals("minecraft")) {
            return null;
        }

        Font baseFont = fontCache.get(fontName);
        if (baseFont == null) {
            return null;
        }

        float scaledSize = size * 0.5f;
        return baseFont.deriveFont(scaledSize);
    }

    public boolean isMinecraftFont() {
        return selectedFontName.equals("minecraft");
    }

    public String[] getAvailableFonts() {
        String[] fonts = new String[fontCache.size() + 1];
        fonts[0] = "minecraft";
        int i = 1;
        for (String fontName : fontCache.keySet()) {
            fonts[i++] = fontName;
        }
        return fonts;
    }


    public int getStringWidth(String text, String fontName, float size) {
        if (fontName.equals("minecraft")) {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        }

        Font font = fontCache.get(fontName);
        if (font == null) {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        }

        float scaledSize = size * 0.5f;
        font = font.deriveFont(scaledSize);
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), false, false);
        Rectangle2D bounds = font.getStringBounds(text, frc);
        return (int)Math.round(bounds.getWidth());
    }

    public int getFontHeight(String fontName, float size) {
        if (fontName.equals("minecraft")) {
            return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
        }

        Font font = fontCache.get(fontName);
        if (font == null) {
            return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
        }

        float scaledSize = size * 0.5f;
        font = font.deriveFont(scaledSize);
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), false, false);
        Rectangle2D bounds = font.getStringBounds("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", frc);
        return (int) Math.round(bounds.getHeight());
    }

}
