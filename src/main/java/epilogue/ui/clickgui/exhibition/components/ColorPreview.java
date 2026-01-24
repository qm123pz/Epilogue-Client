package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;
import epilogue.ui.clickgui.exhibition.management.ColorObject;

import java.util.ArrayList;

public class ColorPreview {

    public String colorName;
    public float x;
    public float y;
    public CategoryButton categoryPanel;
    public ColorObject colorObject;
    public ArrayList<HSVColorPicker> sliders;

    public ColorPreview(ColorObject colorObject, String colorName, float x, float y, CategoryButton categoryPanel) {
        sliders = new ArrayList<>();
        this.colorObject = colorObject;
        this.categoryPanel = categoryPanel;
        this.colorName = colorName;
        this.x = x;
        this.y = y;
        categoryPanel.panel.theme.colorConstructor(this, x, y);
    }

    public void draw(final float x, final float y) {
        for (final UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            if (categoryPanel.enabled) {
                theme.colorPrewviewDraw(this, x, y);
            }
        }
    }
}
