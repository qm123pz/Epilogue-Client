package epilogue.ui.clickgui.exhibition.values;

import epilogue.value.Value;

public class Setting<T> {
    private final String name;
    private T value;
    private final Value<?> backing;

    public Setting(String name, T value) {
        this.name = name;
        this.value = value;
        this.backing = null;
    }

    public Setting(String name, T value, Value<?> backing) {
        this.name = name;
        this.value = value;
        this.backing = backing;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Value<?> getBacking() {
        return backing;
    }
}
