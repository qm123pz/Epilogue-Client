package epilogue.ui.clickgui.menu.component.settings;

import epilogue.ui.clickgui.menu.Fonts;
import epilogue.ui.clickgui.menu.component.SettingComponent;
import epilogue.ui.clickgui.menu.render.DrawUtil;
import epilogue.util.render.ColorUtil;
import epilogue.util.render.RenderUtil;
import epilogue.util.render.animations.advanced.Animation;
import epilogue.util.render.animations.advanced.Direction;
import epilogue.util.render.animations.advanced.impl.DecelerateAnimation;
import epilogue.value.values.BooleanValue;

public class BooleanComponent extends SettingComponent<BooleanValue> {
    private final Animation enabled = new DecelerateAnimation(250, 1);
    private final Animation hover = new DecelerateAnimation(250, 1);

    public BooleanComponent(BooleanValue setting) {
        super(setting);
        this.height = 22;
        enabled.setDirection(setting.getValue() ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    @Override
    public void updateBounds(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        if (alpha <= 0.0f) {
            return;
        }
        enabled.setDirection(setting.getValue() ? Direction.FORWARDS : Direction.BACKWARDS);
        hover.setDirection(DrawUtil.isHovering(x + 135, y + 4, 22, 12, mouseX, mouseY) ? Direction.FORWARDS : Direction.BACKWARDS);

        Fonts.draw(Fonts.small(), setting.getName(), x + 10, y + 4, ColorUtil.applyOpacity(0xFFFFFFFF, 0.4f * alpha));

        int track = setting.getValue() ? ColorUtil.applyOpacity(0xFFFFFFFF, 0.22f * alpha) : ColorUtil.applyOpacity(0xFFFFFFFF, 0.12f * alpha);
        RenderUtil.drawRect(x + 135, y + 4, 20, 10, track);
        DrawUtil.drawCircleCGUI(x + 141 + (float) (enabled.getOutput() * 9f), y + 9, 8, ColorUtil.applyOpacity(0xFFFFFFFF, alpha));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (DrawUtil.isHovering(x + 135, y + 4, 20, 10, mouseX, mouseY) && button == 0) {
            setting.setValue(!setting.getValue());
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }
}
