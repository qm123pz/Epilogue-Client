package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;
import epilogue.ui.clickgui.exhibition.values.Setting;

public class Slider {

    public float x;
    public float y;
    public String name;
    public Setting setting;
    public CategoryPanel panel;
    public boolean dragging;
    public double dragX;
    public double lastDragX;

    public Slider(CategoryPanel panel, float x, float y, Setting setting) {
        this.panel = panel;
        this.x = x;
        this.y = y;
        this.setting = setting;
        panel.categoryButton.panel.theme.SliderContructor(this, panel);
    }

    public void draw(final float x, final float y) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.SliderDraw(this, x, y, this.panel);
        }
    }

    public void mouseClicked(final int x, final int y, final int button) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.SliderMouseClicked(this, x, y, button, this.panel);
        }
    }

    public void mouseReleased(final int x, final int y, final int button) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.SliderMouseMovedOrUp(this, x, y, button, this.panel);
        }
    }
}
