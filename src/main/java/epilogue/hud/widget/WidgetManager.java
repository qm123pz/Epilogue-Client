package epilogue.hud.widget;

import epilogue.event.EventTarget;
import epilogue.events.ChatGUIEvent;
import epilogue.events.Render2DEvent;
import epilogue.util.render.PostProcessing;
import epilogue.util.render.animations.advanced.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.List;

public class WidgetManager {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final List<Widget> widgets = new ArrayList<>();

    public void register(Widget widget) {
        widgets.add(widget);
    }

    public Widget get(String name) {
        for (Widget w : widgets) {
            if (w.name.equalsIgnoreCase(name)) return w;
        }
        return null;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.gameSettings.showDebugInfo) return;
        ScaledResolution sr = new ScaledResolution(mc);
        for (Widget w : widgets) {
            if (!w.shouldRender()) continue;
            w.updatePos(sr);
            w.render(event.getPartialTicks());
            w.updatePos(sr);
        }

        PostProcessing.applyTestGlow();
    }

    @EventTarget
    public void onChatGUI(ChatGUIEvent event) {
        if (mc.gameSettings.showDebugInfo) return;
        if (!(mc.currentScreen instanceof GuiChat)) return;
        ScaledResolution sr = new ScaledResolution(mc);

        boolean prevSuppress = PostProcessing.isInternalBloomSuppressed();
        PostProcessing.setInternalBloomSuppressed(true);

        Widget draggingWidget = null;
        for (Widget w : widgets) {
            if (w.shouldRender() && w.dragging) {
                draggingWidget = w;
                break;
            }
        }

        try {
            for (Widget w : widgets) {
                if (!w.shouldRender()) continue;
                if (!w.hoverAnimation.getDirection().equals(Direction.BACKWARDS)) {
                    w.hoverAnimation.setDirection(Direction.BACKWARDS);
                }
                w.updatePos(sr);
                w.render(0.0f);
                w.updatePos(sr);

                w.onChatGUI(sr, event.getMouseX(), event.getMouseY(), draggingWidget == null || draggingWidget == w);
                if (w.dragging) draggingWidget = w;
                w.updatePos(sr);
            }
        } finally {
            PostProcessing.setInternalBloomSuppressed(prevSuppress);
        }

        PostProcessing.applyTestGlow();
    }
}
