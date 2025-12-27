package epilogue.rendering.ui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;

import epilogue.rendering.ui.AbstractWidget;

public class TextFieldWidget extends AbstractWidget<TextFieldWidget> {

    private final GuiTextField textField;
    private String placeholder = "";
    private boolean enabled = true;

    public TextFieldWidget() {
        this.textField = new GuiTextField(0, Minecraft.getMinecraft().fontRendererObj, 0, 0, 0, 0);
        this.textField.setMaxStringLength(512);
    }

    public GuiTextField getTextField() {
        return textField;
    }

    @Override
    public void onRender(double mouseX, double mouseY) {
        this.textField.xPosition = (int) this.getX();
        this.textField.yPosition = (int) this.getY();
        this.textField.width = (int) this.getWidth();
        this.textField.height = (int) this.getHeight();

        this.textField.setTextColor(this.getHexColor());
        this.textField.setDisabledTextColour(this.getHexColor());

        if (!this.textField.isFocused() && this.textField.getText().isEmpty() && placeholder != null && !placeholder.isEmpty()) {
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(placeholder, (float) this.getX() + 2, (float) this.getY() + 6, this.getHexColor());
        }

        this.textField.drawTextBox();
    }

    @Override
    public boolean onKeyTyped(char c, int keyCode) {
        if (this.textField.isFocused()) {
            this.textField.textboxKeyTyped(c, keyCode);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseClicked(double relativeX, double relativeY, int mouseButton) {
        this.textField.mouseClicked((int) (this.getX() + relativeX), (int) (this.getY() + relativeY), mouseButton);
        return true;
    }

    public TextFieldWidget setPlaceholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        return this;
    }

    public TextFieldWidget setFocused(boolean focused) {
        this.textField.setFocused(focused);
        return this;
    }

    public boolean isFocused() {
        return this.textField.isFocused();
    }

    public String getText() {
        return this.textField.getText();
    }

    public TextFieldWidget setText(String text) {
        this.textField.setText(text == null ? "" : text);
        return this;
    }

    public TextFieldWidget setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.textField.setEnabled(enabled);
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public TextFieldWidget drawUnderline(boolean drawUnderline) {
        return this;
    }

    public TextFieldWidget setDisabledTextColor(int color) {
        this.textField.setDisabledTextColour(color);
        return this;
    }

    public TextFieldWidget setTextChangedCallback(TextChangedCallback cb) {
        return this;
    }

    public interface TextChangedCallback {
        void onTextChanged(String newText);
    }
}
