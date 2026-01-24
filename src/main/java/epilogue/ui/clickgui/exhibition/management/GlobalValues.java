package epilogue.ui.clickgui.exhibition.management;

import epilogue.ui.clickgui.exhibition.values.Setting;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GlobalValues {

    public static List<Setting> globalValues = new ArrayList<>();

    public static Setting<Boolean> saveVisuals = new Setting<>("Save Visuals", false),
            loadVisuals = new Setting<>("Load Visuals", false);

    public static Setting<Boolean> scaleFix = new Setting<>("Scale Fix", true);
    public static Setting<Boolean> allowDebug = new Setting<>("Debug", false);
    public static Setting<Boolean> showCape = new Setting<>("Show Cape", true);
    public static Setting<Boolean> keepPriority = new Setting<>("Keep Priority", true);
    public static Setting<Boolean> showFlags = new Setting<>("Show Flags", false);

    public static Setting<Boolean> centerNotifs = new Setting<>("Center Notifs", false);
    public static Setting<Boolean> showCursor = new Setting<Boolean>("Show Cursor", true) {

        @Override
        public void setValue(Boolean value) {
            super.setValue(value);

            Minecraft mc = Minecraft.getMinecraft();

            if (mc.thePlayer != null) {
                if (mc.currentScreen != null) {
                    Mouse.setGrabbed(!Mouse.isGrabbed());
                }
            }

        }
    };
    public static Setting<Number> targetHUDWidth = new Setting<>("Target HUD Width", 0.5078125);
    public static Setting<Number> targetHUDHeight = new Setting<>("Target HUD Height", 0.569444444);

    static {
        globalValues.addAll(Arrays.asList(saveVisuals, loadVisuals, centerNotifs, showCursor, scaleFix,
                allowDebug, showCape, keepPriority, showFlags, targetHUDWidth, targetHUDHeight));
    }

}
