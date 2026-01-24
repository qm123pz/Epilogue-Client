package epilogue.ui.clickgui.exhibition.values;

public class Options {
    private final String[] options;
    private String selected;

    public Options(String... options) {
        this.options = options;
        this.selected = options != null && options.length > 0 ? options[0] : null;
    }

    public String[] getOptions() {
        return options;
    }

    public String getSelected() {
        return selected;
    }

    public void setSelected(String selected) {
        this.selected = selected;
    }
}
