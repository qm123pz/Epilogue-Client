package epilogue.module.modules.render;

import epilogue.Epilogue;
import net.minecraft.client.Minecraft;
import epilogue.events.Render2DEvent;
import epilogue.event.EventTarget;
import epilogue.module.Module;
import epilogue.value.values.FloatValue;
import epilogue.value.values.ModeValue;

public class WaterMark extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Exhibition"});
    public WaterMark() {
        super("WaterMark", false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;
        
        switch (mode.getValue()) {
            case 0:
                renderExhibitionMode();
                break;
        }
    }
    
    private void renderExhibitionMode() {
        Interface interfaceModule = (Interface) Epilogue.moduleManager.getModule("Interface");
        int fps = Minecraft.getDebugFPS();
        
        float x = 5.0f;
        float y = 5.0f;
        
        int nColor = interfaceModule != null ? interfaceModule.color(0) : 0xFFFFFF;
        int whiteColor = 0xFFFFFF;
        int grayColor = 0xAAAAAA;
        
        mc.fontRendererObj.drawStringWithShadow("E", x, y, nColor);
        float nWidth = mc.fontRendererObj.getStringWidth("E");
        
        mc.fontRendererObj.drawStringWithShadow("pilogue ", x + nWidth, y, whiteColor);
        float nightSkyWidth = mc.fontRendererObj.getStringWidth("Epilogue ");
        
        mc.fontRendererObj.drawStringWithShadow("[", x + nightSkyWidth, y, grayColor);
        float bracketWidth = mc.fontRendererObj.getStringWidth("[");
        
        String fpsText = fps + " FPS";
        mc.fontRendererObj.drawStringWithShadow(fpsText, x + nightSkyWidth + bracketWidth, y, whiteColor);
        float fpsWidth = mc.fontRendererObj.getStringWidth(fpsText);
        
        mc.fontRendererObj.drawStringWithShadow("]", x + nightSkyWidth + bracketWidth + fpsWidth, y, grayColor);
    }
}