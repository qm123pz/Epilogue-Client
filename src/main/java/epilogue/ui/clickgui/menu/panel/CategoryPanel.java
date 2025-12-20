package epilogue.ui.clickgui.menu.panel;

import epilogue.Epilogue;
import epilogue.module.Module;
import epilogue.module.ModuleCategory;
import epilogue.ui.clickgui.menu.MenuClickGui;
import epilogue.ui.clickgui.menu.component.ModuleComponent;
import epilogue.ui.clickgui.menu.render.DrawUtil;
import epilogue.util.render.RenderUtil;
import epilogue.util.render.animations.advanced.Animation;
import epilogue.util.render.animations.advanced.Direction;
import epilogue.util.render.animations.advanced.impl.DecelerateAnimation;
import epilogue.util.render.animations.advanced.impl.SmoothStepAnimation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CategoryPanel {
    private final ModuleCategory category;
    private final MenuClickGui gui;

    private boolean selected;

    private float maxScroll = Float.MAX_VALUE;
    private float rawScroll;
    private float scroll;
    private Animation scrollAnimation = new SmoothStepAnimation(0, 0, Direction.BACKWARDS);

    private final Animation animation = new DecelerateAnimation(250, 1);

    private final List<ModuleComponent> moduleComponents = new ArrayList<>();

    public CategoryPanel(ModuleCategory category, MenuClickGui gui) {
        this.category = category;
        this.gui = gui;

        for (Module module : Epilogue.moduleManager.modules.values()) {
            if (module.getCategory() == category) {
                moduleComponents.add(new ModuleComponent(module, gui));
            }
        }
        moduleComponents.sort(Comparator.comparing(c -> c.getModule().getName()));
    }

    public void initGui() {
        for (ModuleComponent component : moduleComponents) {
            component.initGui();
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        animation.setDirection(selected ? Direction.FORWARDS : Direction.BACKWARDS);
        if (!selected) return;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtil.scissorStart(getPosX(), getPosY() + 75, gui.w, gui.h - 120);

        float left = 0;
        float right = 0;

        for (int i = 0; i < moduleComponents.size(); i++) {
            ModuleComponent module = moduleComponents.get(i);
            float componentOffset = getComponentOffset(i, left, right);

            module.drawScreen(mouseX, mouseY, partialTicks);

            double s = getScroll();
            module.setScroll((int) (Math.round(s * 2.0) / 2.0));
            onScroll(30, mouseX, mouseY);

            maxScroll = Math.max(0, moduleComponents.isEmpty() ? 0 : moduleComponents.get(moduleComponents.size() - 1).getMaxScroll());

            if ((i + 1) % 2 == 0) {
                left += 30 + componentOffset;
            } else {
                right += 30 + componentOffset;
            }
        }

        RenderUtil.scissorEnd();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private float getComponentOffset(int i, float left, float right) {
        ModuleComponent component = moduleComponents.get(i);

        component.setLeft((i + 1) % 2 != 0);
        component.setX(component.isLeft() ? getPosX() + 10 : getPosX() + 185);
        component.setHeight(24);
        component.setY(getPosY() + 54 + component.getHeight() + ((i + 1) % 2 == 0 ? left : right));

        float componentOffset = 0;
        for (int j = 0; j < component.getSettings().size(); j++) {
            if (component.getSettings().get(j).getSetting().isVisible()) {
                componentOffset += component.getSettings().get(j).getDisplayHeight();
            }
        }
        component.setHeight(component.getHeight() + componentOffset);
        return componentOffset;
    }

    public void onScroll(int ms, int mx, int my) {
        scroll = (float) (rawScroll - scrollAnimation.getOutput());

        boolean hovered = DrawUtil.isHovering(getPosX(), getPosY() + 75, gui.w, gui.h - 120, mx, my);
        boolean blocking = false;
        for (ModuleComponent m : moduleComponents) {
            if (m.isBlockingScroll(mx, my)) {
                blocking = true;
                break;
            }
        }

        int wheel = Mouse.getDWheel();
        if (hovered && !blocking && wheel != 0) {
            rawScroll += wheel > 0 ? 30f : -30f;
        }

        rawScroll = Math.max(Math.min(0, rawScroll), -maxScroll);
        scrollAnimation = new SmoothStepAnimation(ms, rawScroll - scroll, Direction.BACKWARDS);
    }

    public float getScroll() {
        scroll = (float) (rawScroll - scrollAnimation.getOutput());
        return scroll;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!selected) return;
        for (ModuleComponent moduleComponent : moduleComponents) {
            moduleComponent.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (!selected) return;
        for (ModuleComponent moduleComponent : moduleComponents) {
            moduleComponent.mouseReleased(mouseX, mouseY, state);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!selected) return;
        for (ModuleComponent moduleComponent : moduleComponents) {
            moduleComponent.keyTyped(typedChar, keyCode);
        }
    }

    public boolean handleCategoryClick(int mouseX, int mouseY) {
        if (DrawUtil.isHovering(getIconX(), getIconY(), 15, 15, mouseX, mouseY)) {
            gui.selectCategory(category);
            return true;
        }
        return false;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public int getPosX() {
        return gui.x;
    }

    public int getPosY() {
        return gui.y;
    }

    public float getIconX() {
        int o = category.ordinal();
        return (o >= 5 ? gui.x + 3 + o * 28 : o == 4 ? gui.x + 8.5f + o * 28 : o == 3 ? gui.x + 10.5f + o * 28 : o == 2 ? gui.x + 10 + o * 28 : o == 1 ? gui.x + 13 + o * 28 : gui.x + 15);
    }

    public float getIconY() {
        int o = category.ordinal();
        return o >= 6 ? gui.y + 44 : gui.y + gui.h - 30.5f;
    }

    public String getIconChar() {
        switch (category) {
            case COMBAT:
                return "A";
            case MOVEMENT:
                return "B";
            case PLAYER:
                return "C";
            case RENDER:
                return "D";
            case MISC:
                return "E";
            default:
                return "A";
        }
    }

    public float getIconTextX() {
        int o = category.ordinal();
        return (o >= 6 ? gui.x + 62 : o == 5 ? gui.x + 6.5f + o * 28 : o == 4 ? gui.x + 10 + o * 28 : o == 3 ? gui.x + 12.5f + o * 28 : o == 2 ? gui.x + 13 + o * 28 : o == 1 ? gui.x + 17 + o * 28 : gui.x + 18);
    }

    public float getIconTextY() {
        int o = category.ordinal();
        return o >= 6 ? gui.y + 54.5f : gui.y + gui.h - 25;
    }

    public List<ModuleComponent> getModules() {
        return moduleComponents;
    }
}
