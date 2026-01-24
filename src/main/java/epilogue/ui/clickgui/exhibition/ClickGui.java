package epilogue.ui.clickgui.exhibition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;

import epilogue.ui.clickgui.exhibition.components.Button;
import epilogue.ui.clickgui.exhibition.components.CategoryButton;
import epilogue.ui.clickgui.exhibition.components.MainPanel;
import epilogue.ui.clickgui.exhibition.components.Slider;
import epilogue.ui.clickgui.exhibition.components.TextBox;

public class ClickGui extends GuiScreen {

    public MainPanel mainPanel;

    public ArrayList<UI> getThemes() {
        return themes;
    }

    ArrayList<UI> themes;

    public ClickGui() {
        (themes = new ArrayList<>()).add(new SkeetMenu());
        mainPanel = new epilogue.ui.clickgui.exhibition.components.MainPanel("Exhibition", 50, 50, themes.get(0));
        mainPanel.isOpen = true;
        mc = Minecraft.getMinecraft();
    }

    public void grabMouse() {
        Mouse.setGrabbed(true);
    }

    public void releaseMouse() {
        if (Mouse.isGrabbed()) Mouse.setGrabbed(false);
    }

    @Override
    public void initGui() {
        super.initGui();
        releaseMouse();
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        if (Keyboard.getEventKeyState()) {
            char c = Keyboard.getEventCharacter();
            int k = Keyboard.getEventKey();

            boolean inOtherScreen = mc.currentScreen != null && mc.currentScreen != this;
            if (inOtherScreen && (k == Keyboard.KEY_RSHIFT || k == Keyboard.KEY_INSERT || k == Keyboard.KEY_DELETE || k == Keyboard.KEY_ESCAPE) && mainPanel.isOpen) {
                mainPanel.keyPressed(k);
                mainPanel.isOpen = false;
                Keyboard.enableRepeatEvents(false);
                releaseMouse();
                return;
            }

            this.keyTyped(c, k);
        }

        this.mc.dispatchKeypresses();
    }

    @Override
    public void handleMouseInput() throws IOException {
        mainPanel.handleMouseInput();
        super.handleMouseInput();
    }

    public void drawMenu(int mouseX, int mouseY) {
        if (SkeetMenu.opacity.getOpacity() <= 0 && !mainPanel.isOpen) {
            SkeetMenu.opacity.interpolate(0);
            return;
        }
        mainPanel.draw(mouseX, mouseY);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawMenu(mouseX, mouseY);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        mainPanel.mouseMovedOrUp(mouseX, mouseY, mouseButton);
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int clickedButton) {
        try {
            mainPanel.mouseClicked(mouseX, mouseY, clickedButton);
            super.mouseClicked(mouseX, mouseY, clickedButton);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        boolean ignore = false;
        if (keyCode == 1) {
            for (CategoryButton buttonb : mainPanel.typeButton) {
                for (Button button : buttonb.categoryPanel.buttons) {
                    if (button.isBinding) {
                        ignore = true;
                        break;
                    }
                }
            }
        }
        if (!ignore) super.keyTyped(typedChar, keyCode);

        mainPanel.keyPressed(keyCode);
    }

    @Override
    public void onGuiClosed() {
        releaseMouse();
        this.mainPanel.isOpen = false;
        Keyboard.enableRepeatEvents(false);
        for (CategoryButton button : mainPanel.typeButton) {
            for (TextBox textBox : button.categoryPanel.textBoxes) {
                textBox.isTyping = false;
                textBox.isFocused = false;
                Keyboard.enableRepeatEvents(false);
            }
            for (Slider slider : button.categoryPanel.sliders) {
                slider.dragging = false;
            }
            if (button.categoryPanel.configTextBox != null) {
                button.categoryPanel.configTextBox.isTyping = false;
                button.categoryPanel.configTextBox.isFocused = false;
                Keyboard.enableRepeatEvents(false);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
