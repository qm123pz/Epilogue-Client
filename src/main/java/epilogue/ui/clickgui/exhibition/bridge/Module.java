package epilogue.ui.clickgui.exhibition.bridge;

import epilogue.Epilogue;

import java.util.LinkedHashMap;
import java.util.Map;

public class Module {
    private final epilogue.module.Module backing;

    public Module(epilogue.module.Module backing) {
        this.backing = backing;
    }

    public epilogue.module.Module backing() {
        return backing;
    }

    public String getName() {
        return backing.getName();
    }

    public ModuleData.Type getType() {
        return ModuleData.from(backing.getCategory());
    }

    public boolean isEnabled() {
        return backing.isEnabled();
    }

    public void toggle() {
        backing.toggle();
    }

    public Map<String, epilogue.value.Value<?>> getSettings() {
        if (Epilogue.valueHandler == null || Epilogue.valueHandler.properties == null) {
            return new LinkedHashMap<>();
        }
        java.util.List<epilogue.value.Value<?>> values = Epilogue.valueHandler.properties.get(backing.getClass());
        if (values == null) return new LinkedHashMap<>();
        Map<String, epilogue.value.Value<?>> out = new LinkedHashMap<>();
        for (epilogue.value.Value<?> v : values) {
            out.put(v.getName(), v);
        }
        return out;
    }
}
