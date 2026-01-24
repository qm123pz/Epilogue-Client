package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.util.Opacity;
import epilogue.ui.clickgui.exhibition.util.RenderingUtil;
import epilogue.ui.clickgui.exhibition.util.render.Colors;
import epilogue.ui.clickgui.exhibition.util.render.Depth;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

public class ConfigTextBox {

    public String textString;
    public float x;
    public float y;
    public CategoryPanel panel;
    public boolean isFocused;
    public boolean isTyping;
    public Opacity opacity = new Opacity(255);
    public boolean backwards;
    public int selectedChar;
    public float offset;

    public ConfigTextBox(float x, float y, CategoryPanel panel) {
        this.x = x;
        this.y = y;
        this.panel = panel;
        this.textString = "";
    }

    public void draw(final float x, final float y) {
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        if (this.selectedChar > this.textString.length()) {
            this.selectedChar = this.textString.length();
        } else if (this.selectedChar < 0) {
            this.selectedChar = 0;
        }
        boolean hovering = (x >= xOff + this.x) && (y >= yOff + this.y)
                && (x <= xOff + this.x + 84) && (y <= yOff + this.y + 9);

        RenderingUtil.rectangle(this.x + xOff - 0.3, this.y + yOff - 0.3, this.x + xOff + 84 + 1.5F, this.y + yOff + 7.5F + 0.3, Colors.getColor(10, 255));
        RenderingUtil.drawGradient(this.x + xOff, this.y + yOff, this.x + xOff + 85, this.y + yOff + 7.5F, Colors.getColor(31, 255), Colors.getColor(36, 255));
        if (hovering || this.isFocused) {
            RenderingUtil.rectangleBordered(this.x + xOff, this.y + yOff, this.x + xOff + 85, this.y + yOff + 7.5F, 0.3, Colors.getColor(0, 0), this.isFocused ? Colors.getColor(130, 255) : Colors.getColor(90, 255));
        }

        Depth.pre();
        Depth.mask();
        RenderingUtil.rectangle(this.x + xOff + 2, this.y + yOff, this.x + xOff + 82, this.y + yOff + 7.5F, Colors.getColor(90, 255));
        Depth.render();
        Depth.post();

        if (this.opacity.getOpacity() >= 255) {
            this.backwards = true;
        } else if (this.opacity.getOpacity() <= 40) {
            this.backwards = false;
        }
        this.opacity.interp(this.backwards ? 40 : 255, 7);

        if (this.isFocused) {
            float width = 0;
            float posX = this.x + xOff + width - this.offset;
            RenderingUtil.rectangle(posX - 0.5, this.y + yOff + 1.5, posX, this.y + yOff + 6, Colors.getColor(220, (int) this.opacity.getOpacity()));
        } else {
            this.opacity.setOpacity(255);
        }
    }

    public void mouseClicked(final int x, final int y, final int mouseID) {
        float xOff = panel.categoryButton.panel.dragX;
        float yOff = panel.categoryButton.panel.dragY;
        boolean hovering = (x >= xOff + this.x) && (y >= yOff + this.y) && (x <= xOff + this.x + 84) && (y <= yOff + this.y + 9);

        if (hovering && mouseID == 0 && !this.isFocused) {
            this.isFocused = true;
            Keyboard.enableRepeatEvents(true);
        } else if (!hovering) {
            this.isFocused = false;
            this.isTyping = false;
        }
    }

    public void keyPressed(int key) {
        if (panel.visible) {
            char letter = Keyboard.getEventCharacter();
            if (letter == '\r') {
                this.isFocused = false;
                this.isTyping = false;
                Keyboard.enableRepeatEvents(false);
                return;
            }

            if (this.isFocused)
                switch (key) {
                    case Keyboard.KEY_LEFT: {
                        if (this.selectedChar > 0) this.selectedChar--;
                        break;
                    }
                    case Keyboard.KEY_RIGHT: {
                        if (this.selectedChar < this.textString.length()) this.selectedChar++;
                        break;
                    }
                    case Keyboard.KEY_DOWN: {
                        this.selectedChar = this.textString.length();
                        break;
                    }
                    case Keyboard.KEY_UP: {
                        this.selectedChar = 0;
                        this.offset = 0;
                        break;
                    }
                    case Keyboard.KEY_BACK: {
                        try {
                            if (this.textString.isEmpty()) break;
                            String oldString = this.textString;

                            StringBuilder stringBuilder = new StringBuilder(oldString);
                            stringBuilder.deleteCharAt(this.selectedChar - 1);
                            this.textString = ChatAllowedCharacters.filterAllowedCharacters(stringBuilder.toString());
                            this.selectedChar--;
                            if (this.selectedChar > this.textString.length()) {
                                this.selectedChar = this.textString.length();
                            }
                        } catch (Exception ignored) {
                        }
                        break;
                    }
                }

            if (this.isFocused && ChatAllowedCharacters.isAllowedCharacter(letter)) {
                Keyboard.enableRepeatEvents(true);

                if (!this.isTyping) this.isTyping = true;

                String oldString = this.textString;

                StringBuilder stringBuilder = new StringBuilder(oldString);
                stringBuilder.insert(this.selectedChar, letter);

                this.textString = ChatAllowedCharacters.filterAllowedCharacters(stringBuilder.toString());

                if (this.selectedChar > this.textString.length()) {
                    this.selectedChar = this.textString.length();
                } else {
                    this.selectedChar++;
                }
            }
        }
    }

}
