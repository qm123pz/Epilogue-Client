package epilogue.ui.clickgui.menu.component;

import epilogue.module.Module;
import epilogue.ui.clickgui.menu.MenuClickGui;
import epilogue.ui.clickgui.menu.Fonts;
import epilogue.ui.clickgui.menu.component.settings.SettingFactory;
import epilogue.ui.clickgui.menu.render.DrawUtil;
import epilogue.util.render.ColorUtil;
import epilogue.util.render.RenderUtil;
import epilogue.util.render.animations.advanced.Animation;
import epilogue.util.render.animations.advanced.Direction;
import epilogue.util.render.animations.advanced.impl.DecelerateAnimation;

import java.util.ArrayList;
import java.util.List;

public class ModuleComponent {
    private final Module module;
    private final MenuClickGui gui;

    private float x;
    private float y;
    private float height;

    private int scroll;
    private boolean left = true;

    private final List<SettingComponent<?>> settings;

    private final Animation enabled = new DecelerateAnimation(250, 1);
    private final Animation hover = new DecelerateAnimation(250, 1);

    public ModuleComponent(Module module, MenuClickGui gui) {
        this.module = module;
        this.gui = gui;
        this.settings = new ArrayList<>(SettingFactory.createFor(module));
        enabled.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    public void initGui() {
        enabled.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float yy = y + 6 + scroll;

        float guiAlpha = gui.getTextAlpha();

        enabled.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
        hover.setDirection(DrawUtil.isHovering(x + 135, yy + 4, 22, 12, mouseX, mouseY) ? Direction.FORWARDS : Direction.BACKWARDS);

        Animation moduleAnimation = module.getAnimation();
        moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);

        if (guiAlpha > 0.0f) {
            Fonts.draw(Fonts.small(), module.getName(), x + 10, yy + 5, ColorUtil.applyOpacity(0xFFFFFFFF, guiAlpha));
        }

        int track = module.isEnabled() ? ColorUtil.applyOpacity(0xFFFFFFFF, 0.22f * guiAlpha) : ColorUtil.applyOpacity(0xFFFFFFFF, 0.12f * guiAlpha);
        if (guiAlpha > 0.0f) {
            RenderUtil.drawRect(x + 135, yy + 4, 20, 10, track);
            DrawUtil.drawCircleCGUI(x + 141 + (float) (moduleAnimation.getOutput() * 9f), yy + 9, 8, ColorUtil.applyOpacity(0xFFFFFFFF, guiAlpha));
        }

        float componentY = yy + 22;
        for (SettingComponent<?> setting : settings) {
            if (!setting.getSetting().isVisible()) continue;

            setting.setAlpha(guiAlpha);

            setting.updateBounds(x, componentY, 165);
            setting.draw(mouseX, mouseY);
            componentY += setting.getDisplayHeight();
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        float yy = y + 6 + scroll;
        if (DrawUtil.isHovering(x + 135, yy + 4, 20, 10, mouseX, mouseY) && mouseButton == 0) {
            module.toggle();
        }
        for (SettingComponent<?> component : settings) {
            if (component.getSetting().isVisible()) {
                component.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        for (SettingComponent<?> component : settings) {
            if (component.getSetting().isVisible()) {
                component.mouseReleased(mouseX, mouseY, state);
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        for (SettingComponent<?> component : settings) {
            if (component.getSetting().isVisible()) {
                component.keyTyped(typedChar, keyCode);
            }
        }
    }

    public int getMaxScroll() {
        return (int) (((y - gui.y) + height) * 4);
    }

    public boolean isAnySettingHovered(int mx, int my) {
        for (SettingComponent<?> c : settings) {
            if (c.getSetting().isVisible() && c.isHovered(mx, my)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBlockingScroll(int mx, int my) {
        for (SettingComponent<?> c : settings) {
            if (!c.getSetting().isVisible()) continue;
            if (c.blocksScroll() && c.isHovered(mx, my)) {
                return true;
            }
        }
        return false;
    }

    public Module getModule() {
        return module;
    }

    public List<SettingComponent<?>> getSettings() {
        return settings;
    }

    public void setScroll(int scroll) {
        this.scroll = scroll;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public boolean isLeft() {
        return left;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getX() {
        return x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getY() {
        return y;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getHeight() {
        return height;
    }

    public void setWidth(float width) {
    }
}
