package epilogue.rendering.ui.widgets;

import epilogue.rendering.ui.AbstractWidget;
import epilogue.ui.clickgui.menu.Fonts;

public class IconWidget extends AbstractWidget<IconWidget> {

    private String icon;
    private int color = 0xFFFFFFFF;

    public IconWidget(String icon) {
        this.icon = icon;
    }

    public IconWidget setIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public IconWidget setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public void onRender(double mouseX, double mouseY) {
        Fonts.draw(Fonts.heading(), icon == null ? "" : icon, (float) this.getX(), (float) this.getY(), color);
    }
}
