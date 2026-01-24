package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;

import java.util.ArrayList;

public class MainPanel {

    public boolean isOpen;
    public float x;
    public float y;
    public String headerString;
    public float dragX;
    public float dragY;
    public float lastDragX;
    public float lastDragY;
    public boolean dragging;
    public UI theme;

    public ArrayList<CategoryButton> typeButton;
    public ArrayList<CategoryPanel> typePanel;
    public ArrayList<SLButton> slButtons;

    public MainPanel(String header, float x, float y, UI theme) {
        this.headerString = header;
        this.x = x;
        this.y = y;
        this.theme = theme;
        typeButton = new ArrayList<>();
        typePanel = new ArrayList<>();
        slButtons = new ArrayList<>();
        theme.panelConstructor(this, x, y);
    }

    public void mouseClicked(final int x, final int y, final int state) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.panelMouseClicked(this, x, y, state);
        }
    }

    public void mouseMovedOrUp(final int x, final int y, final int state) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.panelMouseMovedOrUp(this, x, y, state);
        }
    }

    public void draw(int mouseX, int mouseY) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.mainPanelDraw(this, mouseX, mouseY);
        }
    }

    public void keyPressed(int key) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.mainPanelKeyPress(this, key);
        }
    }

    public void handleMouseInput() {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.handleMouseInput(this);
        }
    }
}
