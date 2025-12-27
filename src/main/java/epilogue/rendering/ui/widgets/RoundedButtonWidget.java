package epilogue.rendering.ui.widgets;

import java.awt.*;
import java.util.function.Supplier;

public class RoundedButtonWidget extends RoundedRectWidget {

    private final LabelWidget lw;

    public RoundedButtonWidget(Supplier<String> label) {
        lw = new LabelWidget(label);
        this.addChild(lw);
        lw.setClickable(false);
        lw.setBeforeRenderCallback(lw::center);
        this.setShouldSetMouseCursor(true);
    }

    public RoundedButtonWidget(String label) {
        this(() -> label);
    }

    public RoundedButtonWidget setTextColor(int color) {
        lw.setColor(color);
        return this;
    }

    public RoundedButtonWidget setTextColor(Color color) {
        lw.setColor(color.getRGB());
        return this;
    }
}
