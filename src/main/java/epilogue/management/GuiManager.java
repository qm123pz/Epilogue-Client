package epilogue.management;
import net.minecraft.client.Minecraft;
import epilogue.ui.clickgui.dropdown.DropdownClickGui;
import epilogue.ui.clickgui.menu.MenuClickGui;
public class GuiManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final DropdownClickGui dropdownClickGui;
    private MenuClickGui menuClickGui;
    public GuiManager() {
        this.dropdownClickGui = new DropdownClickGui();
    }
    private MenuClickGui getArcane() {
        if (menuClickGui == null) {
            menuClickGui = new MenuClickGui();
        }
        return menuClickGui;
    }
    public void closeAllGuis() {
        closeDropdownGui();
        closeMenuGui();
    }
    public void openDropdownGui() {
        closeAllGuis();
        mc.displayGuiScreen(dropdownClickGui);
    }
    public void closeDropdownGui() {
        if (mc.currentScreen == dropdownClickGui) {
            mc.displayGuiScreen(null);
        }
    }
    public boolean isDropdownGuiOpen() {
        return mc.currentScreen == dropdownClickGui;
    }
    public void openMenuGui() {
        closeAllGuis();
        mc.displayGuiScreen(getArcane());
    }
    public void closeMenuGui() {
        if (menuClickGui != null && mc.currentScreen == menuClickGui) {
            mc.displayGuiScreen(null);
        }
    }
    public boolean isMenuGuiOpen() {
        return menuClickGui != null && mc.currentScreen == menuClickGui;
    }
}