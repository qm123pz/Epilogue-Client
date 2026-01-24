package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;
import epilogue.ui.clickgui.exhibition.values.MultiBool;
import epilogue.ui.clickgui.exhibition.values.Setting;

import java.util.ArrayList;
import java.util.List;

public class MultiDropdownBox {

    public String name;
    public MultiBool multiBool;
    public Setting setting;
    public float x;
    public float y;
    public List<MultiDropdownButton> buttons = new ArrayList<>();
    public CategoryPanel panel;
    public boolean active;

    public MultiDropdownBox(MultiBool multiBool, Setting setting, float x, float y, CategoryPanel panel) {
        this.name = multiBool.getName();
        this.multiBool = multiBool;
        this.setting = setting;
        this.panel = panel;
        this.x = x;
        this.y = y;
        panel.categoryButton.panel.theme.multiDropDownContructor(this, x, y, this.panel);
    }

    public void draw(final float x, final float y) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            if (panel.visible) {
                theme.multiDropDownDraw(this, x, y, this.panel);
            }
        }
    }

    public void mouseClicked(final int x, final int y, final int button) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.multiDropDownMouseClicked(this, x, y, button, this.panel);
        }
    }
}
