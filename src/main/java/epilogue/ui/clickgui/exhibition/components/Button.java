package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;
import epilogue.module.Module;

public class Button {

    public float x;
    public float y;
    public String name;
    public CategoryPanel panel;
    public boolean enabled;
    public Module module;
    public boolean isBinding;

    public Button(final CategoryPanel panel, final String name, final float x, final float y, Module module) {
        this.panel = panel;
        this.name = name;
        this.x = x;
        this.y = y;
        this.module = module;
        this.enabled = module.isEnabled();
        panel.categoryButton.panel.theme.buttonContructor(this, this.panel);
    }

    public void draw(final float x, final float y) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            if (panel.visible) {
                theme.buttonDraw(this, x, y, this.panel);
            }
        }
    }

    public void mouseClicked(final int x, final int y, final int button) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.buttonMouseClicked(this, x, y, button, this.panel);
        }
    }

    public void keyPressed(int key) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.buttonKeyPressed(this, key);
        }
    }
}
