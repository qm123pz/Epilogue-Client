package epilogue.management;

import net.minecraft.client.Minecraft;
import epilogue.ui.clickgui.dropdown.DropdownClickGui;
import epilogue.ui.clickgui.menu.MenuClickGui;
import epilogue.ui.clickgui.exhibition.ClickGui;

public class GuiManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final DropdownClickGui dropdownClickGui;
    private MenuClickGui menuClickGui;
    private ClickGui exhibitionClickGui;

    public GuiManager() {
        this.dropdownClickGui = new DropdownClickGui();
    }

    private ClickGui getExhibition() {
        return new ClickGui();
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
        closeExhibitionGui();
    }

    public void openDropdownGui() {
        closeAllGuis();
        mc.displayGuiScreen(dropdownClickGui);
    }

    public void openExhibitionGui() {
        closeAllGuis();
        exhibitionClickGui = getExhibition();
        mc.displayGuiScreen(exhibitionClickGui);
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
        openMenuGui(false);
    }

    public void openMenuGui(boolean embedIntoDynamicIsland) {
        closeAllGuis();
        MenuClickGui gui = getArcane();
        gui.setEmbeddedInDynamicIsland(embedIntoDynamicIsland);
        mc.displayGuiScreen(gui);
    }

    public void closeMenuGui() {
        if (menuClickGui != null && mc.currentScreen == menuClickGui) {
            mc.displayGuiScreen(null);
        }
    }

    public boolean isMenuGuiOpen() {
        return menuClickGui != null && mc.currentScreen == menuClickGui;
    }

    public void closeExhibitionGui() {
        if (exhibitionClickGui != null && mc.currentScreen == exhibitionClickGui) {
            mc.displayGuiScreen(null);
        }
    }

    public boolean isExhibitionGuiOpen() {
        return exhibitionClickGui != null && mc.currentScreen == exhibitionClickGui;
    }
}