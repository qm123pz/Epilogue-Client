package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;

public class ConfigButton {

    public float x;
    public float y;
    public ConfigList configList;
    public ButtonType buttonType;

    public ConfigButton(ConfigList configList, float x, float y, ButtonType buttonType) {
        this.x = x;
        this.y = y;
        this.buttonType = buttonType;
        this.configList = configList;
    }

    public void draw(final float x, final float y) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.configButtonDraw(this, x, y);
        }
    }

    public void mouseClicked(final int x, final int y, final int button) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.configButtonMouseClicked(this, x, y, button);
        }
    }

    public static class ButtonType {
        public static ButtonType LOAD = new ButtonType("Load"), SAVE = new ButtonType("Save"), DELETE = new ButtonType("Delete"), CREATE = new ButtonType("Create"), OPEN_FOLDER = new ButtonType("Open Folder");

        private final String label;

        public ButtonType(String label) {
            this.label = label;
        }

        public String name() {
            return label;
        }
    }
}
