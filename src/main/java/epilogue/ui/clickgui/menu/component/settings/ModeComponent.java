package epilogue.ui.clickgui.menu.component.settings;

import epilogue.ui.clickgui.menu.Fonts;
import epilogue.ui.clickgui.menu.component.SettingComponent;
import epilogue.ui.clickgui.menu.render.DrawUtil;
import epilogue.util.render.ColorUtil;
import epilogue.util.render.RenderUtil;
import epilogue.util.render.animations.advanced.Animation;
import epilogue.util.render.animations.advanced.Direction;
import epilogue.util.render.animations.advanced.impl.DecelerateAnimation;
import epilogue.util.render.animations.advanced.impl.SmoothStepAnimation;
import epilogue.value.values.ModeValue;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;

public class ModeComponent extends SettingComponent<ModeValue> {
    private float maxScroll = Float.MAX_VALUE;
    private float rawScroll;
    private float scroll;
    private Animation scrollAnimation = new SmoothStepAnimation(0, 0, Direction.BACKWARDS);

    private final Animation open = new DecelerateAnimation(175, 1);
    private boolean opened;

    public ModeComponent(ModeValue setting) {
        super(setting);
        this.height = 38;
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
        Fonts.draw(Fonts.small(), setting.getName(), x + 10, y + 4, ColorUtil.applyOpacity(0xFFFFFFFF, 0.4f));

        open.setDirection(opened ? Direction.FORWARDS : Direction.BACKWARDS);

        if (open.getOutput() > 0.1) {
            float totalHeight = (float) ((setting.getModes().length * 20 + 2) * open.getOutput());
            float halfTotalHeight = getHalfTotalHeight();

            GlStateManager.translate(0, 0, 2f);

            RenderUtil.drawRect(x + 10, y + 32, 145, totalHeight, 0xFF000000);

            String[] modes = setting.getModes();
            for (int i = 0; i < modes.length; i++) {
                String str = modes[i];
                if (str.equals(setting.getModeString())) {
                    RenderUtil.drawRect(x + 12, (float) (y + 34 + (i * 20) * open.getOutput()) + getScroll(), 141, 18, ColorUtil.applyOpacity(0xFFFFFFFF, (float) (0.12f * open.getOutput())));
                }
                Fonts.draw(Fonts.tiny(), str, x + 14, (float) (y + 40 + (i * 20) * open.getOutput()) + getScroll(), ColorUtil.applyOpacity(0xFFFFFFFF, (float) open.getOutput()));
            }

            onScroll(30, mouseX, mouseY);
            maxScroll = Math.max(0, modes.length == 0 ? 0 : (modes.length - 6) * 20);

            GlStateManager.translate(0, 0, -2f);
        }

        RenderUtil.drawRect(x + 10, y + 14, 145, 14, 0xFF000000);

        Fonts.draw(Fonts.tiny(), setting.getModeString(), x + 14, y + 15 + (Fonts.height(Fonts.tiny()) * 0.1f), ColorUtil.applyOpacity(0xFFFFFFFF, 1f));
        Fonts.draw(Fonts.icon(), "U", x + 145, y + 20, ColorUtil.applyOpacity(0xFFFFFFFF, 1f));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouse) {
        if (DrawUtil.isHovering(x + 10, y + 14, 145, 14, mouseX, mouseY) && mouse == 1) {
            opened = !opened;
        }

        if (opened) {
            String[] modes = setting.getModes();
            for (int i = 0; i < modes.length; i++) {
                String str = modes[i];
                if (DrawUtil.isHovering(x + 12, (float) (y + 34 + (i * 20) * open.getOutput()) + getScroll(), 141, 18, mouseX, mouseY) && mouse == 0) {
                    setting.setValue(i);
                }
            }
        }
    }

    public void onScroll(int ms, int mx, int my) {
        scroll = (float) (rawScroll - scrollAnimation.getOutput());
        float halfTotalHeight = getHalfTotalHeight();
        float yy = (y + 12 - halfTotalHeight) < 0 ? 0 : (y + 12 - halfTotalHeight);
        float visibleHeight = getVisibleHeight();

        if (DrawUtil.isHovering(x + 115, yy, 80f, visibleHeight, mx, my)) {
            rawScroll += (float) Mouse.getDWheel() * 20;
        }

        rawScroll = Math.max(Math.min(0, rawScroll), -maxScroll);
        scrollAnimation = new SmoothStepAnimation(ms, rawScroll - scroll, Direction.BACKWARDS);
    }

    private float getVisibleHeight() {
        return (float) ((y + 12 - getSize() * 20 * open.getOutput() / 2f < 0 ? MathHelper.clamp_double(y + 12 - getSize() * 20 * open.getOutput() / 2f, 0, 999) : 122) * open.getOutput());
    }

    private float getHalfTotalHeight() {
        return (float) ((getSize() * 20 * open.getOutput()) / 2f);
    }

    private int getSize() {
        return Math.min(4, (setting.getModes().length - 1));
    }

    public float getScroll() {
        scroll = (float) (rawScroll - scrollAnimation.getOutput());
        return scroll;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
    }

    @Override
    public boolean blocksScroll() {
        return opened;
    }

    @Override
    public boolean isHovered(float mouseX, float mouseY) {
        return opened && DrawUtil.isHovering(x + 115, (y + 12 - getHalfTotalHeight()) < 0 ? 0 : (y + 12 - getHalfTotalHeight()), 80f, (float) ((y + 12 - getSize() * 20 * open.getOutput() / 2f < 0 ? MathHelper.clamp_double(y + 12 - getHalfTotalHeight(), 0, 999) : 122) * open.getOutput()), mouseX, mouseY);
    }
}
