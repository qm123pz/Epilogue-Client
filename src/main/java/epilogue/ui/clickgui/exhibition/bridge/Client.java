package epilogue.ui.clickgui.exhibition.bridge;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;

public class Client {
    private static final ModuleManager moduleManager = new ModuleManager();

    public static epilogue.ui.clickgui.exhibition.ClickGui getClickGui() {
        return ClickGuiHolder.getClickGui();
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }
}
