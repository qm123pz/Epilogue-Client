package epilogue.rendering.ui.container;

import epilogue.rendering.Rect;
import epilogue.rendering.StencilClipManager;
import epilogue.rendering.animation.Interpolations;
import epilogue.rendering.ui.AbstractWidget;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class ScrollPanel extends AbstractWidget<ScrollPanel> {

    private double spacing = 0;
    public double actualScrollOffset = 0, targetScrollOffset = 0;

    private Alignment alignment = Alignment.VERTICAL;

    public enum Alignment {
        VERTICAL,
        HORIZONTAL,
        VERTICAL_WITH_HORIZONTAL_FILL
    }

    public ScrollPanel setAlignment(Alignment alignment) {
        if (alignment == null) throw new IllegalArgumentException("Alignment cannot be null!");
        this.alignment = alignment;
        return this;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public double getSpacing() {
        return spacing;
    }

    public ScrollPanel setSpacing(double spacing) {
        this.spacing = spacing;
        return this;
    }

    @Override
    public void onRender(double mouseX, double mouseY) {
        this.actualScrollOffset = Interpolations.interpBezier(this.actualScrollOffset, this.targetScrollOffset, 1f);
        this.alignChildren();
    }

    @Override
    public boolean onDWheel(double mouseX, double mouseY, int dWheel) {
        if (!this.testHovered(mouseX, mouseY)) return false;
        this.performScroll(dWheel);
        return true;
    }

    private void performScroll(int dWheel) {
        if (dWheel != 0) {
            double strength = 12;
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) strength *= 2;
            if (dWheel > 0) this.targetScrollOffset -= strength;
            else this.targetScrollOffset += strength;
        }

        this.targetScrollOffset = Math.max(this.targetScrollOffset, 0);

        switch (this.alignment) {
            case VERTICAL: {
                double childrenHeightSum = this.getChildrenHeightSum();
                if (childrenHeightSum > this.getHeight())
                    this.targetScrollOffset = Math.min(this.targetScrollOffset, childrenHeightSum - this.getHeight());
                else
                    this.targetScrollOffset = 0;
                break;
            }
            case HORIZONTAL: {
                double childrenWidthSum = this.getChildrenWidthSum();
                if (childrenWidthSum > this.getWidth())
                    this.targetScrollOffset = Math.min(this.targetScrollOffset, childrenWidthSum - this.getWidth());
                else
                    this.targetScrollOffset = 0;
                break;
            }
            case VERTICAL_WITH_HORIZONTAL_FILL: {
                double childrenHeightSum = this.getChildrenHeightSumHorizontalFill();
                if (childrenHeightSum > this.getHeight())
                    this.targetScrollOffset = Math.min(this.targetScrollOffset, childrenHeightSum - this.getHeight());
                else
                    this.targetScrollOffset = 0;
                break;
            }
        }
    }

    public void alignChildren() {
        double offsetX = 0;
        double offsetY = 0;

        if (this.alignment == Alignment.VERTICAL || this.alignment == Alignment.VERTICAL_WITH_HORIZONTAL_FILL)
            offsetY = -this.actualScrollOffset;
        else
            offsetX = -this.actualScrollOffset;

        for (AbstractWidget<?> child : this.getChildren()) {
            double width = child.getWidth();
            double height = child.getHeight();
            if (child.isHidden()) continue;

            switch (this.alignment) {
                case VERTICAL: {
                    child.setPosition(child.getRelativeX(), offsetY);
                    offsetY += height + spacing;
                    break;
                }
                case HORIZONTAL: {
                    child.setPosition(offsetX, child.getRelativeY());
                    offsetX += width + spacing;
                    break;
                }
                case VERTICAL_WITH_HORIZONTAL_FILL: {
                    if (offsetX + width > this.getWidth()) {
                        offsetX = 0;
                        offsetY += height + spacing;
                    }
                    child.setPosition(offsetX, offsetY);
                    offsetX += width + spacing;
                    break;
                }
            }
        }
    }

    protected double getChildrenHeightSum() {
        double result = 0;
        List<AbstractWidget<?>> children = this.getChildren();
        for (AbstractWidget<?> child : children) {
            if (child.isHidden()) continue;
            result += child.getHeight() + this.spacing;
        }
        if (result > 0) result -= this.spacing;
        return result;
    }

    protected double getChildrenWidthSum() {
        double result = 0;
        List<AbstractWidget<?>> children = this.getChildren();
        for (AbstractWidget<?> child : children) {
            if (child.isHidden()) continue;
            result += child.getWidth() + this.spacing;
        }
        if (result > 0) result -= this.spacing;
        return result;
    }

    protected double getChildrenHeightSumHorizontalFill() {
        double result = 0;
        double offsetX = 0;
        List<AbstractWidget<?>> children = this.getChildren();
        for (AbstractWidget<?> child : children) {
            double width = child.getWidth();
            double height = child.getHeight();
            if (child.isHidden()) continue;
            if (offsetX == 0 && result == 0) result += height + spacing;
            if (offsetX + width > this.getWidth()) {
                offsetX = 0;
                result += height + spacing;
            }
            offsetX += width + spacing;
        }
        if (result > 0) result -= this.spacing;
        return result;
    }

    @Override
    public void renderWidget(double mouseX, double mouseY, int dWheel) {
        StencilClipManager.beginClip(() -> Rect.draw(this.getX(), this.getY(), this.getWidth(), this.getHeight(), -1));
        super.renderWidget(mouseX, mouseY, dWheel);
        StencilClipManager.endClip();
    }

    @Override
    protected boolean shouldRenderChildren(AbstractWidget<?> child, double mouseX, double mouseY) {
        switch (this.getAlignment()) {
            case VERTICAL:
            case VERTICAL_WITH_HORIZONTAL_FILL:
                if (child.getRelativeY() + child.getHeight() < 0) return false;
                if (child.getRelativeY() > this.getHeight()) return false;
                break;
            case HORIZONTAL:
                if (child.getRelativeX() + child.getWidth() < 0) return false;
                if (child.getRelativeX() > this.getWidth()) return false;
                break;
        }
        return true;
    }

    @Override
    protected boolean shouldClickChildren(double mouseX, double mouseY) {
        return this.testHovered(mouseX, mouseY);
    }

    @Override
    public boolean canBeScrolled() {
        return true;
    }
}
