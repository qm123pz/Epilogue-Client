package epilogue.rendering.ui.widgets;

import epilogue.rendering.ui.AbstractWidget;
import epilogue.ui.clickgui.menu.Fonts;

import java.util.function.Supplier;

public class LabelWidget extends AbstractWidget<LabelWidget> {

    private Supplier<String> text;
    private int color = 0xFFFFFFFF;
    private double maxWidth = -1;

    public LabelWidget(String text) {
        this(() -> text);
    }

    public LabelWidget(Supplier<String> text) {
        this.text = text;
        this.setClickable(false);
    }

    public LabelWidget setColor(int color) {
        this.color = color;
        return this;
    }

    public LabelWidget setMaxWidth(double maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    public String getText() {
        return text == null ? "" : String.valueOf(text.get());
    }

    @Override
    public void onRender(double mouseX, double mouseY) {
        String t = getText();
        Fonts.draw(Fonts.small(), t, (float) this.getX(), (float) this.getY(), color);
        double w = Fonts.width(Fonts.small(), t);
        this.setBounds(maxWidth > 0 ? Math.min(maxWidth, w) : w, Fonts.height(Fonts.small()));
    }
}
