package epilogue.rendering.ui;
import net.minecraft.client.renderer.GlStateManager;
import epilogue.interfaces.SharedRenderingConstants;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractWidget<SELF extends AbstractWidget<SELF>> implements SharedRenderingConstants {

    public interface OnClickCallback {
        boolean onClick(double relativeX, double relativeY, int mouseButton);
    }

    public interface OnKeyTypedCallback {
        boolean onKeyTyped(char character, int keyCode);
    }

    public interface OnDWheelCallback {
        boolean onDWheel(double mouseX, double mouseY, int dWheel);
    }

    public interface RenderCallback {
        void onRender();
    }

    AbstractWidget<?> parent = null;

    List<AbstractWidget<?>> children = new CopyOnWriteArrayList<>();

    public static class Bounds {
        private double x, y;
        private double width, height;
    }

    private final Bounds bounds = new Bounds();

    private RenderCallback beforeRenderCallback = () -> {
    };

    private boolean clickable = true;

    private boolean hovering = false;

    private boolean hidden = false;

    private boolean shouldSetMouseCursor = false;

    private boolean bloom = false;

    private boolean blur = false;

    private int color = 0xFFFFFFFF;

    protected OnClickCallback clickCallback = null;

    private OnKeyTypedCallback keyTypedCallback = null;

    private OnDWheelCallback dWheelCallback = null;

    private Runnable transformations = null, onTick = null;

    private float alpha = 1.0f;

    public abstract void onRender(double mouseX, double mouseY);

    public int getHexColor() {
        int a = ((int) (this.alpha * 255)) & 0xFF;
        return (this.color & 0x00FFFFFF) | (a << 24);
    }

    public int getColor() {
        return color;
    }

    public SELF setColor(int color) {
        this.color = color;
        return (SELF) this;
    }

    protected boolean shouldRenderChildren(AbstractWidget<?> child, double mouseX, double mouseY) {
        return true;
    }

    public void renderWidget(double mouseX, double mouseY, int dWheel) {
        if (this.isHidden())
            return;

        boolean shouldResetMatrixState = this.transformations != null;

        if (shouldResetMatrixState) {
            GlStateManager.pushMatrix();
            this.transformations.run();
        }

        this.beforeRenderCallback.onRender();

        if (this.isBloom() || this.isBlur()) {
            Runnable shader = () -> {
                if (shouldResetMatrixState) {
                    GlStateManager.pushMatrix();
                    this.transformations.run();
                }

                this.onRender(mouseX, mouseY);

                if (shouldResetMatrixState)
                    GlStateManager.popMatrix();
            };

            if (this.isBloom())
                SharedRenderingConstants.BLOOM.add(shader);

            if (this.isBlur())
                SharedRenderingConstants.BLUR.add(shader);
        }

        this.onRender(mouseX, mouseY);

        boolean childHovering = false;

        for (AbstractWidget<?> child : this.getChildren()) {
            if (child.isHidden())
                continue;
            if (!this.shouldRenderChildren(child, mouseX, mouseY))
                continue;
            child.renderWidget(mouseX, mouseY, dWheel);
            if (!childHovering && child.isClickable() && child.testHovered(mouseX, mouseY)) {
                childHovering = true;
            }
        }

        if (shouldResetMatrixState)
            GlStateManager.popMatrix();

        this.hovering = !childHovering && this.testHovered(mouseX, mouseY);

        if (dWheel != 0)
            this.onDWheelReceived(mouseX, mouseY, dWheel);
    }

    public void onTick() {
    }

    public void onTickReceived() {
        if (this.onTick != null) {
            this.onTick.run();
        }

        this.onTick();

        this.getChildren().forEach(AbstractWidget::onTickReceived);
    }

    public void addChild(AbstractWidget<?>... children) {
        for (AbstractWidget<?> child : children) {
            this.children.add(child);
            child.setParent(this);
        }
    }

    public void addChild(List<AbstractWidget<?>> children) {
        this.children.addAll(children);
        children.forEach(child -> child.setParent(this));
    }

    public SELF setParent(AbstractWidget<?> parent) {
        this.parent = parent;
        return (SELF) this;
    }

    public AbstractWidget<?> getParent() {
        return parent;
    }

    public double getX() {
        return parent == null ? bounds.x : parent.getX() + bounds.x;
    }

    public double getY() {
        return parent == null ? bounds.y : parent.getY() + bounds.y;
    }

    public double getWidth() {
        return bounds.width;
    }

    public double getHeight() {
        return bounds.height;
    }

    public double getRelativeX() {
        return bounds.x;
    }

    public double getRelativeY() {
        return bounds.y;
    }

    public double getParentWidth() {
        return parent == null ? getWidth() : parent.getWidth();
    }

    public double getParentHeight() {
        return parent == null ? getHeight() : parent.getHeight();
    }

    public SELF setBounds(double width, double height) {
        bounds.width = width;
        bounds.height = height;
        return (SELF) this;
    }

    public SELF setBounds(double x, double y, double width, double height) {
        bounds.x = x;
        bounds.y = y;
        bounds.width = width;
        bounds.height = height;
        return (SELF) this;
    }

    public SELF setPosition(double x, double y) {
        bounds.x = x;
        bounds.y = y;
        return (SELF) this;
    }

    public SELF center() {
        bounds.x = (getParentWidth() - getWidth()) * 0.5;
        bounds.y = (getParentHeight() - getHeight()) * 0.5;
        return (SELF) this;
    }

    public SELF centerHorizontally() {
        bounds.x = (getParentWidth() - getWidth()) * 0.5;
        return (SELF) this;
    }

    public SELF centerVertically() {
        bounds.y = (getParentHeight() - getHeight()) * 0.5;
        return (SELF) this;
    }

    public boolean testHovered(double mouseX, double mouseY) {
        return testHovered(mouseX, mouseY, 0);
    }

    public boolean testHovered(double mouseX, double mouseY, double expand) {
        double x = getX() - expand;
        double y = getY() - expand;
        double w = getWidth() + expand * 2;
        double h = getHeight() + expand * 2;
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    public float getAlpha() {
        return alpha;
    }

    public SELF setAlpha(float alpha) {
        this.alpha = alpha;
        return (SELF) this;
    }

    public SELF setHidden(boolean hidden) {
        this.hidden = hidden;
        return (SELF) this;
    }

    public SELF setClickable(boolean clickable) {
        this.clickable = clickable;
        return (SELF) this;
    }

    public SELF setBeforeRenderCallback(RenderCallback cb) {
        this.beforeRenderCallback = cb == null ? () -> {
        } : cb;
        return (SELF) this;
    }

    public SELF setOnClickCallback(OnClickCallback cb) {
        this.clickCallback = cb;
        return (SELF) this;
    }

    public SELF setOnKeyTypedCallback(OnKeyTypedCallback cb) {
        this.keyTypedCallback = cb;
        return (SELF) this;
    }

    public boolean onKeyTypedReceived(char c, int keyCode) {
        if (this.onKeyTyped(c, keyCode)) {
            return true;
        }
        if (this.keyTypedCallback != null) {
            if (this.keyTypedCallback.onKeyTyped(c, keyCode)) {
                return true;
            }
        }
        for (AbstractWidget<?> child : this.children) {
            if (child.onKeyTypedReceived(c, keyCode)) {
                return true;
            }
        }
        return false;
    }

    public boolean onKeyTyped(char c, int keyCode) {
        return false;
    }

    public void onMouseClickReceived(double mouseX, double mouseY, int mouseButton) {
        if (!this.shouldClickChildren(mouseX, mouseY)) {
            return;
        }

        for (AbstractWidget<?> child : this.children) {
            if (child.isHidden())
                continue;
            if (!child.isClickable())
                continue;
            if (child.testHovered(mouseX, mouseY)) {
                child.onMouseClickReceived(mouseX, mouseY, mouseButton);
                return;
            }
        }

        if (this.onMouseClicked(mouseX - getX(), mouseY - getY(), mouseButton)) {
            return;
        }

        if (this.clickCallback != null && this.isClickable() && this.testHovered(mouseX, mouseY)) {
            this.clickCallback.onClick(mouseX - getX(), mouseY - getY(), mouseButton);
        }
    }

    protected boolean shouldClickChildren(double mouseX, double mouseY) {
        return true;
    }

    public boolean onMouseClicked(double relativeX, double relativeY, int mouseButton) {
        return false;
    }

    public SELF setOnDWheelCallback(OnDWheelCallback cb) {
        this.dWheelCallback = cb;
        return (SELF) this;
    }

    public void onDWheelReceived(double mouseX, double mouseY, int dWheel) {
        if (this.dWheelCallback != null) {
            if (this.dWheelCallback.onDWheel(mouseX, mouseY, dWheel)) {
                return;
            }
        }

        if (this.canBeScrolled() && this.onDWheel(mouseX, mouseY, dWheel)) {
            return;
        }

        for (AbstractWidget<?> child : this.children) {
            child.onDWheelReceived(mouseX, mouseY, dWheel);
        }
    }

    public boolean onDWheel(double mouseX, double mouseY, int dWheel) {
        return false;
    }

    public boolean canBeScrolled() {
        return false;
    }

    public boolean isBloom() {
        return bloom;
    }

    public boolean isBlur() {
        return blur;
    }

    public boolean isClickable() {
        return clickable;
    }

    public boolean isHidden() {
        return hidden;
    }

    public List<AbstractWidget<?>> getChildren() {
        return children;
    }

    public boolean isShouldSetMouseCursor() {
        return shouldSetMouseCursor;
    }

    public SELF setShouldSetMouseCursor(boolean should) {
        this.shouldSetMouseCursor = should;
        return (SELF) this;
    }

    public SELF setBloom(boolean bloom) {
        this.bloom = bloom;
        return (SELF) this;
    }

    public SELF setBlur(boolean blur) {
        this.blur = blur;
        return (SELF) this;
    }

    public SELF setTransformations(Runnable transformations) {
        this.transformations = transformations;
        return (SELF) this;
    }

    public SELF setOnTick(Runnable onTick) {
        this.onTick = onTick;
        return (SELF) this;
    }
}
