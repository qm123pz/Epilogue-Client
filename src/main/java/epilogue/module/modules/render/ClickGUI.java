package epilogue.module.modules.render;

import epilogue.Epilogue;
import epilogue.module.Module;
import epilogue.module.ModuleCategory;
import epilogue.value.values.ModeValue;
import org.lwjgl.input.Keyboard;

public class ClickGUI extends Module {
    
    private final ModeValue mode = new ModeValue("Mode", 1, new String[]{"Dropdown", "Menu", "Dynamicisland"});

    public ClickGUI() {
        super("ClickGUI", false);
        this.setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        if (Epilogue.guiManager != null) {
            String selectedMode = mode.getModeString();
            switch (selectedMode) {
                case "Dropdown":
                    if (Epilogue.guiManager.isDropdownGuiOpen()) {
                        Epilogue.guiManager.closeDropdownGui();
                    } else {
                        Epilogue.guiManager.openDropdownGui();
                    }
                    break;
                case "Menu":
                case "Dynamicisland":
                    if (Epilogue.guiManager.isMenuGuiOpen()) {
                        Epilogue.guiManager.closeMenuGui();
                    } else {
                        Epilogue.guiManager.openMenuGui("Dynamicisland".equals(selectedMode));
                    }
                    break;
            }
        }
        this.setEnabled(false);
    }
    
    public ModeValue getMode() {
        return mode;
    }
    

    @Override
    public void onDisabled() {
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.RENDER;
    }
}
