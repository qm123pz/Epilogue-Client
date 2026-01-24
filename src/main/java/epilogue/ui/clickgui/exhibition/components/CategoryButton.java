package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;
import epilogue.ui.clickgui.exhibition.util.Opacity;

public class CategoryButton {

    public float x;
    public float y;
    public String name;
    public MainPanel panel;
    public boolean enabled;
    public CategoryPanel categoryPanel;
    public Opacity fade;

    public CategoryButton(MainPanel panel, String name, float x, float y) {
        this.fade = new Opacity(0);
        this.panel = panel;
        this.name = name;
        this.x = x;
        this.y = y;
        panel.theme.categoryButtonConstructor(this, this.panel);
    }

    public void draw(final float x, final float y) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.categoryButtonDraw(this, x, y);
        }
    }

    public void mouseClicked(final int x, final int y, final int button) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.categoryButtonMouseClicked(this, this.panel, x, y, button);
        }
    }

    public void mouseReleased(final int x, final int y, final int button) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.categoryButtonMouseReleased(this, x, y, button);
        }
    }
}
