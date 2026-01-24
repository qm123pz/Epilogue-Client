package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;

public class SLButton {

    public float x;
    public float y;
    public String name;
    public MainPanel panel;
    public boolean load;

    public SLButton(final MainPanel panel, final String name, final float x, final float y, boolean load) {
        this.panel = panel;
        this.name = name;
        this.x = x;
        this.y = y;
        this.load = load;
    }

    public void draw(final float x, final float y) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.slButtonDraw(this, x, y, this.panel);
        }
    }

    public void mouseClicked(final int x, final int y, final int button) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.slButtonMouseClicked(this, x, y, button, this.panel);
        }
    }
}
