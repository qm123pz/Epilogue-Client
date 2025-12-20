package epilogue.ui.clickgui.menu.component.settings;

import epilogue.ui.clickgui.menu.Fonts;
import epilogue.ui.clickgui.menu.component.SettingComponent;
import epilogue.ui.clickgui.menu.render.DrawUtil;
import epilogue.util.render.ColorUtil;
import epilogue.util.render.RenderUtil;
import epilogue.util.render.animations.advanced.Animation;
import epilogue.util.render.animations.advanced.Direction;
import epilogue.util.render.animations.advanced.impl.DecelerateAnimation;
import epilogue.value.values.ColorValue;
import net.minecraft.util.MathHelper;

import java.awt.*;

public class ColorComponent extends SettingComponent<ColorValue> {
    private final Animation open = new DecelerateAnimation(250, 1);
    private boolean opened;
    private boolean pickingHue;
    private boolean picking;

    public ColorComponent(ColorValue setting) {
        super(setting);
        this.height = 22;
        open.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void updateBounds(float x, float y, float width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        open.setDirection(opened ? Direction.FORWARDS : Direction.BACKWARDS);
        this.height = (float) (24 + 66 * open.getOutput());

        DrawUtil.resetColor();
        DrawUtil.drawCircle(x + 149, y + 7, 0, 360, 5, 2, true, setting.getValue());
        DrawUtil.resetColor();

        Fonts.draw(Fonts.small(), setting.getName(), x + 10, y + 4, ColorUtil.applyOpacity(0xFFFFFFFF, 0.4f));

        if (open.getOutput() > 0) {
            float gradientWidth = 60;
            float gradientHeight = (float) (60 * open.getOutput());
            float gradientX = x + 34;
            float gradientY = y + 18;

            float h = setting.getHue();
            float s = setting.getSaturation();
            float b = setting.getBrightness();

            for (float i = 0; i <= 60 * open.getOutput(); i++) {
                RenderUtil.drawRect(x + 21, y + 18 + i, 8, 1, Color.getHSBColor((float) (i / 60f * open.getOutput()), 1f, 1f).getRGB());
            }
            RenderUtil.drawRect(x + 20, (float) (y + 18 + h * 60 * open.getOutput()), 10, 1, 0xFFFFFFFF);

            float pickerY = (gradientY + 2) + (gradientHeight * (1 - b));
            float pickerX = (gradientX) + (gradientWidth * s - 1);
            pickerY = Math.max(Math.min(gradientY + gradientHeight - 2, pickerY), gradientY);
            pickerX = Math.max(Math.min(gradientX + gradientWidth - 2, pickerX), gradientX + 2);

            if (pickingHue) {
                setting.setHue(MathHelper.clamp_float((mouseY - (y + 18)) / 60f, 0, 1));
            }
            if (picking) {
                setting.setBrightness(MathHelper.clamp_float(1 - ((mouseY - gradientY) / 60f), 0, 1));
                setting.setSaturation(MathHelper.clamp_float((mouseX - gradientX) / 60f, 0, 1));
            }

            for (float iy = 0; iy < gradientHeight; iy++) {
                float br = 1f - (iy / Math.max(1f, gradientHeight));
                for (float ix = 0; ix < gradientWidth; ix++) {
                    float sat = ix / gradientWidth;
                    RenderUtil.drawRect(gradientX + ix, gradientY + iy, 1, 1, Color.getHSBColor(h, sat, br).getRGB());
                }
            }

            DrawUtil.drawCircle((int) pickerX, (int) pickerY, 0, 360, 2, .1f, false, setting.getValue());
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (DrawUtil.isHovering(x + 144, y + 2, 10, 10, mouseX, mouseY) && mouseButton == 1) {
            opened = !opened;
        }

        if (opened && mouseButton == 0) {
            if (DrawUtil.isHovering(x + 34, y + 18, 60, 60, mouseX, mouseY)) {
                picking = true;
            }

            if (DrawUtil.isHovering(x + 21, y + 18, 8, 60, mouseX, mouseY)) {
                pickingHue = true;
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            pickingHue = false;
            picking = false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }

    @Override
    public boolean blocksScroll() {
        return pickingHue || picking;
    }
}
