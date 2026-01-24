package epilogue.ui.clickgui.exhibition.bridge;

import epilogue.Epilogue;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public List<Module> getArray() {
        List<Module> out = new ArrayList<>();
        if (Epilogue.moduleManager == null || Epilogue.moduleManager.modules == null) return out;
        for (epilogue.module.Module m : Epilogue.moduleManager.modules.values()) {
            out.add(new Module(m));
        }
        return out;
    }

    public Module get(Class<?> cls) {
        if (Epilogue.moduleManager == null) return null;
        epilogue.module.Module m = Epilogue.moduleManager.getModule(cls.getSimpleName());
        if (m == null) return null;
        return new Module(m);
    }

    public static void saveSettings() {
    }

    public static void saveStatus() {
    }
}
