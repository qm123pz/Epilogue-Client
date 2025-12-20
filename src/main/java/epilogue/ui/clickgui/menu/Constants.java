package epilogue.ui.clickgui.menu;

import java.awt.Color;

public final class Constants {
    public static final int WIDTH = 360;
    public static final int HEIGHT = 380;

    public static final Color DARK_BACKGROUND_PRIMARY = new Color(22, 22, 26, 255);
    public static final Color DARK_BACKGROUND_SECONDARY = new Color(17, 17, 19, 255);
    public static final Color DARK_BACKGROUND_TERTIARY = new Color(15, 15, 17, 255);
    public static final Color DARK_PANEL_BASE = new Color(22, 22, 26, 255);
    public static final Color DARK_PANEL_ACCENT = new Color(29, 29, 35, 255);
    public static final Color DARK_LINE = new Color(30, 30, 30, 255);
    public static final Color DARK_VERSION = new Color(255, 255, 255, 50);
    public static final Color DARK_TEXT = new Color(255, 255, 255, 255);

    public static final Color LIGHT_BACKGROUND_PRIMARY = new Color(250, 250, 254, 255);
    public static final Color LIGHT_BACKGROUND_SECONDARY = new Color(255, 255, 255, 255);
    public static final Color LIGHT_BACKGROUND_TERTIARY = new Color(217, 217, 216, 255);
    public static final Color LIGHT_PANEL_BASE = new Color(246, 248, 252, 255);
    public static final Color LIGHT_PANEL_ACCENT = new Color(234, 236, 243, 255);
    public static final Color LIGHT_LINE = new Color(210, 210, 210, 255);
    public static final Color LIGHT_VERSION = new Color(0, 0, 0, 50);
    public static final Color LIGHT_TEXT = new Color(19, 17, 19, 255);

    public static Color backgroundPrimary = DARK_BACKGROUND_PRIMARY;
    public static Color backgroundSecondary = DARK_BACKGROUND_SECONDARY;
    public static Color backgroundTertiary = DARK_BACKGROUND_TERTIARY;
    public static Color panelBase = DARK_PANEL_BASE;
    public static Color panelAccent = DARK_PANEL_ACCENT;
    public static Color line = DARK_LINE;
    public static Color version = DARK_VERSION;
    public static Color text = DARK_TEXT;

    public static void applyDarkTheme() {
        backgroundPrimary = DARK_BACKGROUND_PRIMARY;
        backgroundSecondary = DARK_BACKGROUND_SECONDARY;
        backgroundTertiary = DARK_BACKGROUND_TERTIARY;
        panelBase = DARK_PANEL_BASE;
        panelAccent = DARK_PANEL_ACCENT;
        line = DARK_LINE;
        version = DARK_VERSION;
        text = DARK_TEXT;
    }

    public static void applyLightTheme() {
        backgroundPrimary = LIGHT_BACKGROUND_PRIMARY;
        backgroundSecondary = LIGHT_BACKGROUND_SECONDARY;
        backgroundTertiary = LIGHT_BACKGROUND_TERTIARY;
        panelBase = LIGHT_PANEL_BASE;
        panelAccent = LIGHT_PANEL_ACCENT;
        line = LIGHT_LINE;
        version = LIGHT_VERSION;
        text = LIGHT_TEXT;
    }

    private Constants() {
    }
}
