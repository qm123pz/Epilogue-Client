package epilogue.ui.clickgui.exhibition;

public final class ClickGuiHolder {
    private static ClickGui clickGui;

    private ClickGuiHolder() {
    }

    public static ClickGui getClickGui() {
        if (clickGui == null) {
            clickGui = new ClickGui();
        }
        return clickGui;
    }
}
