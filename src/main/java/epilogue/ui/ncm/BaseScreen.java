package epilogue.ui.ncm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public abstract class BaseScreen extends GuiScreen {

    protected final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawScreen((double) mouseX, (double) mouseY);
    }

    public void drawScreen(double mouseX, double mouseY) {
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        this.onKeyTyped(typedChar, keyCode);
    }

    public void onKeyTyped(char typedChar, int keyCode) {
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        this.mouseClicked((double) mouseX, (double) mouseY, mouseButton);
    }

    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
    }
}
