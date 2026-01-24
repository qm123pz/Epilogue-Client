package epilogue.ui.clickgui.exhibition;

import epilogue.ui.clickgui.exhibition.components.*;

public abstract class UI {

    public abstract void mainConstructor(ClickGui p0);

    public abstract void mainPanelDraw(epilogue.ui.clickgui.exhibition.components.MainPanel panel, int p0, int p1);

    public abstract void mainPanelKeyPress(epilogue.ui.clickgui.exhibition.components.MainPanel panel, int key);

    public abstract void panelConstructor(epilogue.ui.clickgui.exhibition.components.MainPanel mainPanel, float x, float y);

    public abstract void panelMouseClicked(epilogue.ui.clickgui.exhibition.components.MainPanel mainPanel, int p1, int p2, int p3);

    public abstract void panelMouseMovedOrUp(epilogue.ui.clickgui.exhibition.components.MainPanel mainPanel, int p1, int p2, int p3);

    public abstract void categoryButtonConstructor(final CategoryButton p0, final epilogue.ui.clickgui.exhibition.components.MainPanel p1);

    public abstract void categoryButtonMouseClicked(final CategoryButton p0, final epilogue.ui.clickgui.exhibition.components.MainPanel p1, final int p2, final int p3, final int p4);

    public abstract void categoryButtonDraw(final CategoryButton p0, final float p2, final float p3);

    public abstract void categoryPanelConstructor(final CategoryPanel categoryPanel, final CategoryButton categoryButton, final float x, final float y);

    public abstract void categoryPanelMouseClicked(final CategoryPanel categoryPanel, int p1, int p2, int p3);

    public abstract void categoryPanelDraw(final CategoryPanel categoryPanel, final float x, final float y);

    public abstract void categoryPanelMouseMovedOrUp(final CategoryPanel categoryPanel, final int x, final int y, final int button);

    public abstract void groupBoxConstructor(final GroupBox groupBox, final float x, final float y);

    public abstract void groupBoxMouseClicked(final GroupBox groupBox, int p1, int p2, int p3);

    public abstract void groupBoxDraw(final GroupBox groupBox, final float x, final float y);

    public abstract void groupBoxMouseMovedOrUp(final GroupBox groupBox, final int x, final int y, final int button);

    public abstract void buttonContructor(final Button p0, final CategoryPanel panel);

    public abstract void buttonMouseClicked(final Button p0, final int p2, final int p3, final int p4, final CategoryPanel panel);

    public abstract void buttonDraw(final Button p0, final float p2, final float p3, final CategoryPanel panel);

    public abstract void buttonKeyPressed(final Button button, final int key);

    public abstract void checkBoxMouseClicked(final Checkbox p0, final int p2, final int p3, final int p4, final CategoryPanel panel);

    public abstract void checkBoxDraw(final Checkbox p0, final float p2, final float p3, final CategoryPanel panel);

    public abstract void dropDownContructor(final DropdownBox p0, final float x, final float u, final CategoryPanel panel);

    public abstract void dropDownMouseClicked(final DropdownBox p0, final int x, final int u, final int mouse, final CategoryPanel panel);

    public abstract void dropDownDraw(final DropdownBox p0, final float x, final float y, final CategoryPanel panel);

    public abstract void dropDownButtonMouseClicked(final DropdownButton p0, final DropdownBox p1, final int x, final int y, final int mouse);

    public abstract void dropDownButtonDraw(final DropdownButton p0, final DropdownBox p1, final float x, final float y);

    public abstract void multiDropDownContructor(final MultiDropdownBox p0, final float x, final float u, final CategoryPanel panel);

    public abstract void multiDropDownMouseClicked(final MultiDropdownBox p0, final int x, final int u, final int mouse, final CategoryPanel panel);

    public abstract void multiDropDownDraw(final MultiDropdownBox p0, final float x, final float y, final CategoryPanel panel);

    public abstract void multiDropDownButtonMouseClicked(final MultiDropdownButton p0, final MultiDropdownBox p1, final int x, final int y, final int mouse);

    public abstract void multiDropDownButtonDraw(final MultiDropdownButton p0, final MultiDropdownBox p1, final float x, final float y);

    public abstract void SliderContructor(final Slider p0, final CategoryPanel panel);

    public abstract void SliderMouseClicked(final Slider p0, final int p2, final int p3, final int p4, final CategoryPanel panel);

    public abstract void SliderMouseMovedOrUp(final Slider p0, final int p2, final int p3, final int p4, final CategoryPanel panel);

    public abstract void SliderDraw(final Slider p0, final float p2, final float p3, final CategoryPanel panel);

    public abstract void categoryButtonMouseReleased(CategoryButton categoryButton, int x, int y, int button);

    public abstract void slButtonDraw(SLButton slButton, float x, float y, epilogue.ui.clickgui.exhibition.components.MainPanel panel);

    public abstract void slButtonMouseClicked(SLButton slButton, float x, float y, int button, epilogue.ui.clickgui.exhibition.components.MainPanel panel);

    public abstract void colorConstructor(ColorPreview colorPreview, float x, float y);

    public abstract void colorPrewviewDraw(ColorPreview colorPreview, float x, float y);

    public abstract void colorPickerConstructor(HSVColorPicker slider, float x, float y);

    public abstract void colorPickerDraw(HSVColorPicker slider, float x, float y);

    public abstract void colorPickerClick(HSVColorPicker slider, float x, float y, int mouse);

    public abstract void colorPickerMovedOrUp(HSVColorPicker slider, float x, float y, int mouse);

    public abstract void configButtonDraw(ConfigButton configButton, float x, float y);

    public abstract void configButtonMouseClicked(ConfigButton configButton, float x, float y, int button);

    public abstract void configListDraw(ConfigList configList, float x, float y);

    public abstract void configListMouseClicked(ConfigList configList, float x, float y, int button);

    public abstract void textBoxDraw(TextBox textBox, float x, float y);

    public abstract void textBoxMouseClicked(TextBox textBox, int x, int y, int mouseID);

    public abstract void textBoxKeyPressed(TextBox textBox, int key);

    public abstract void handleMouseInput(epilogue.ui.clickgui.exhibition.components.MainPanel panel);

    public abstract void configHandleMouseInput(ConfigList configList);
}
