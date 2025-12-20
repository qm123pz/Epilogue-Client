package epilogue.ui.clickgui.menu.component.settings;

import epilogue.Epilogue;
import epilogue.module.Module;
import epilogue.ui.clickgui.menu.component.SettingComponent;
import epilogue.value.Value;
import epilogue.value.values.*;

import java.util.ArrayList;
import java.util.List;

public final class SettingFactory {
    private SettingFactory() {
    }

    public static List<SettingComponent<?>> createFor(Module module) {
        List<SettingComponent<?>> out = new ArrayList<>();
        List<Value<?>> values = Epilogue.valueHandler.properties.get(module.getClass());
        if (values == null) {
            return out;
        }

        for (Value<?> v : values) {
            if (v instanceof BooleanValue) {
                out.add(new BooleanComponent((BooleanValue) v));
            } else if (v instanceof FloatValue) {
                out.add(new NumberComponent.FloatSlider((FloatValue) v));
            } else if (v instanceof IntValue) {
                out.add(new NumberComponent.IntSlider((IntValue) v));
            } else if (v instanceof PercentValue) {
                out.add(new NumberComponent.PercentSlider((PercentValue) v));
            } else if (v instanceof ModeValue) {
                out.add(new ModeComponent((ModeValue) v));
            } else if (v instanceof ColorValue) {
                out.add(new ColorComponent((ColorValue) v));
            }
        }

        return out;
    }
}
