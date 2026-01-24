package epilogue.ui.clickgui.exhibition.components;

import epilogue.ui.clickgui.exhibition.ClickGuiHolder;
import epilogue.ui.clickgui.exhibition.UI;

import java.util.ArrayList;

public class CategoryPanel {

    public float x;
    public float y;
    public boolean visible;
    public CategoryButton categoryButton;
    public String headerString;
    public ArrayList<Button> buttons = new ArrayList<>();
    public ArrayList<Slider> sliders = new ArrayList<>();
    public ArrayList<DropdownBox> dropdownBoxes = new ArrayList<>();
    public ArrayList<MultiDropdownBox> multiDropdownBoxes = new ArrayList<>();
    public ArrayList<Checkbox> checkboxes = new ArrayList<>();
    public ArrayList<ColorPreview> colorPreviews = new ArrayList<>();
    public ArrayList<GroupBox> groupBoxes = new ArrayList<>();
    public ArrayList<TextBox> textBoxes = new ArrayList<>();
    public ConfigTextBox configTextBox;
    public ConfigList configList;

    public float scroll;
    public float minScroll;
    public float maxScroll;
    public float clipX;
    public float clipY;
    public float clipW;
    public float clipH;

    public float contentMaxY;

    public CategoryPanel(String name, CategoryButton categoryButton, float x, float y) {
        this.headerString = name;
        this.x = x;
        this.y = y;
        this.categoryButton = categoryButton;
        this.visible = false;
        this.scroll = 0;
        this.minScroll = 0;
        this.maxScroll = 0;
        this.contentMaxY = 0;
        categoryButton.panel.theme.categoryPanelConstructor(this, categoryButton, x, y);
    }

    public void draw(final float x, final float y) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.categoryPanelDraw(this, x, y);
        }
    }

    public void mouseClicked(final int x, final int y, final int button) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.categoryPanelMouseClicked(this, x, y, button);
        }
    }

    public void mouseReleased(final int x, final int y, final int button) {
        for (UI theme : ClickGuiHolder.getClickGui().getThemes()) {
            theme.categoryPanelMouseMovedOrUp(this, x, y, button);
        }
    }
}
