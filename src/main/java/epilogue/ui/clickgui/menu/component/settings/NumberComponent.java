package epilogue.ui.clickgui.menu.component.settings;

import epilogue.ui.clickgui.menu.Fonts;
import epilogue.ui.clickgui.menu.component.SettingComponent;
import epilogue.ui.clickgui.menu.render.DrawUtil;
import epilogue.util.render.ColorUtil;
import epilogue.util.render.RenderUtil;
import epilogue.value.Value;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.PercentValue;
import net.minecraft.util.MathHelper;

public abstract class NumberComponent<T extends Value<?>> extends SettingComponent<T> {
    protected boolean dragging;
    protected float anim;

    protected NumberComponent(T setting) {
        super(setting);
        this.height = 30;
    }

    protected abstract double getMin();

    protected abstract double getMax();

    protected abstract double getValue();

    protected abstract void setValueFromDouble(double v);

    protected abstract String format();

    @Override
    public void updateBounds(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        int w = 145;
        double min = getMin();
        double max = getMax();
        double val = getValue();
        anim = DrawUtil.animate(anim, (float) (w * (val - min) / (max - min)), 50);

        Fonts.draw(Fonts.small(), this.setting.getName(), x + 10, y + 4, ColorUtil.applyOpacity(0xFFFFFFFF, 0.4f));
        String valueStr = format();
        Fonts.draw(Fonts.small(), valueStr, x + 155 - Fonts.width(Fonts.small(), valueStr), y + 4, ColorUtil.applyOpacity(0xFFFFFFFF, 0.4f));

        int base = ColorUtil.applyOpacity(0xFFFFFFFF, 0.18f);
        RenderUtil.drawRect(x + 10, y + 18, w, 2, base);
        RenderUtil.drawRect(x + 10, y + 18, anim, 2, 0xFFFFFFFF);
        RenderUtil.drawRect(x + 5 + anim, y + 16.5f, 6, 6, 0xFFFFFFFF);

        if (dragging) {
            double difference = max - min;
            double v = min + MathHelper.clamp_double((mouseX - (x + 10)) / (double) w, 0, 1) * difference;
            setValueFromDouble(v);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        int w = 145;
        if (DrawUtil.isHovering(x + 10, y + 16, w, 6, mouseX, mouseY) && button == 0) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            dragging = false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }

    @Override
    public boolean blocksScroll() {
        return dragging;
    }

    public static final class FloatSlider extends NumberComponent<FloatValue> {
        public FloatSlider(FloatValue setting) {
            super(setting);
        }

        @Override
        protected double getMin() {
            return setting.getMinimum();
        }

        @Override
        protected double getMax() {
            return setting.getMaximum();
        }

        @Override
        protected double getValue() {
            return setting.getValue();
        }

        @Override
        protected void setValueFromDouble(double v) {
            setting.setValue((float) v);
        }

        @Override
        protected String format() {
            return String.valueOf(setting.getValue());
        }
    }

    public static final class IntSlider extends NumberComponent<IntValue> {
        public IntSlider(IntValue setting) {
            super(setting);
        }

        @Override
        protected double getMin() {
            return setting.getMinimum();
        }

        @Override
        protected double getMax() {
            return setting.getMaximum();
        }

        @Override
        protected double getValue() {
            return setting.getValue();
        }

        @Override
        protected void setValueFromDouble(double v) {
            setting.setValue((int) Math.round(v));
        }

        @Override
        protected String format() {
            return String.valueOf(setting.getValue());
        }
    }

    public static final class PercentSlider extends NumberComponent<PercentValue> {
        public PercentSlider(PercentValue setting) {
            super(setting);
        }

        @Override
        protected double getMin() {
            return setting.getMinimum();
        }

        @Override
        protected double getMax() {
            return setting.getMaximum();
        }

        @Override
        protected double getValue() {
            return setting.getValue();
        }

        @Override
        protected void setValueFromDouble(double v) {
            setting.setValue((int) Math.round(v));
        }

        @Override
        protected String format() {
            return setting.getValue() + "%";
        }
    }
}
