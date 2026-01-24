package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;
import epilogue.ui.clickgui.exhibition.values.Setting;

public class Checkbox {

    public CategoryPanel panel;
    public boolean enabled;
    public float x;
    public float y;
    public String name;
    public Setting setting;

    public Checkbox(CategoryPanel panel, String name, float x, float y, Setting setting) {
        this.panel = panel;
        this.name = name;
        this.x = x;
        this.y = y;
        this.setting = setting;
        this.enabled = ((boolean) setting.getValue());
    }

    public void draw(final float x, final float y) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            if (panel.visible) {
                theme.checkBoxDraw(this, x, y, this.panel);
            }
        }
    }

    public void mouseClicked(final int x, final int y, final int button) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.checkBoxMouseClicked(this, x, y, button, this.panel);
        }
    }
}
