package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;
import epilogue.ui.clickgui.exhibition.values.Options;
import epilogue.ui.clickgui.exhibition.values.Setting;

import java.util.ArrayList;
import java.util.List;

public class DropdownBox {

    public Options option;
    public Setting setting;
    public float x;
    public float y;
    public List<DropdownButton> buttons = new ArrayList<>();
    public CategoryPanel panel;
    public boolean active;

    public DropdownBox(Setting setting, float x, float y, CategoryPanel panel) {
        this.setting = setting;
        this.option = (Options) setting.getValue();
        this.panel = panel;
        this.x = x;
        this.y = y;
        panel.categoryButton.panel.theme.dropDownContructor(this, x, y, this.panel);
    }

    public void draw(final float x, final float y) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            if (panel.visible) {
                theme.dropDownDraw(this, x, y, this.panel);
            }
        }
    }

    public void mouseClicked(final int x, final int y, final int button) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.dropDownMouseClicked(this, x, y, button, this.panel);
        }
    }
}
