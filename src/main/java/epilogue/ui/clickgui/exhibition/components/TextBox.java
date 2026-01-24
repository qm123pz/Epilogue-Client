package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;
import epilogue.ui.clickgui.exhibition.util.Opacity;
import epilogue.ui.clickgui.exhibition.values.Setting;

public class TextBox {

    public String textString;
    public float x;
    public float y;
    public Setting setting;
    public CategoryPanel panel;
    public boolean isFocused;
    public boolean isTyping;
    public Opacity opacity = new Opacity(255);
    public boolean backwards;
    public int cursorPos;
    public float offset;

    public TextBox(Setting setting, float x, float y, CategoryPanel panel) {
        this.x = x;
        this.y = y;
        this.panel = panel;
        this.setting = setting;
        this.textString = setting.getValue().toString();
        this.cursorPos = textString.length();
    }

    public void draw(final float x, final float y) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            if (panel.visible) {
                theme.textBoxDraw(this, x, y);
            }
        }
    }

    public void mouseClicked(final int x, final int y, final int button) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            if (panel.visible) {
                theme.textBoxMouseClicked(this, x, y, button);
            }
        }
    }

    public void keyPressed(int key) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            if (panel.visible) {
                theme.textBoxKeyPressed(this, key);
            }
        }
    }
}
