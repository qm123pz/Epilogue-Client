package epilogue.ui.clickgui.menu.component;

import epilogue.ui.clickgui.menu.Component;
import epilogue.value.Value;
import net.minecraft.client.Minecraft;

public abstract class SettingComponent<T extends Value<?>> extends Component {
    protected final Minecraft mc = Minecraft.getMinecraft();
    protected final T setting;
    protected float alpha = 1.0f;

    protected SettingComponent(T setting) {
        this.setting = setting;
    }

    public T getSetting() {
        return setting;
    }

    protected void applyValueChange(Object value) {
        if (value == null) {
            setting.parseString(null);
            return;
        }
        boolean changed = false;
        try {
            changed = setting.parseString(String.valueOf(value));
        } catch (Exception ignored) {
        }
        if (!changed) {
            try {
                setting.setValue(value);
            } catch (Exception ignored) {
            }
        }
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public abstract void updateBounds(float x, float y, float width);

    public abstract void draw(int mouseX, int mouseY);

    public abstract void mouseClicked(int mouseX, int mouseY, int button);

    public abstract void mouseReleased(int mouseX, int mouseY, int state);

    public abstract void keyTyped(char typedChar, int keyCode);

    public void tick() {
    }

    public float getDisplayHeight() {
        return height;
    }

    public boolean blocksScroll() {
        return false;
    }
}
