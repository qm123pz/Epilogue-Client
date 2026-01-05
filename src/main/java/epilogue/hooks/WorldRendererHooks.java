package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.render.Xray;

import java.nio.IntBuffer;

public final class WorldRendererHooks {
    private WorldRendererHooks() {
    }

    public static IntBuffer onPutColorMultiplier(IntBuffer intBuffer, int integer2, int integer3) {
        if (Epilogue.moduleManager == null) {
            return intBuffer.put(integer2, integer3);
        }
        Xray xray = (Xray) Epilogue.moduleManager.modules.get(Xray.class);
        return xray.isEnabled()
                ? intBuffer.put(integer2, integer3 & 16777215 | (int) ((float) xray.opacity.getValue().intValue() * 255.0F / 100.0F) << 24)
                : intBuffer.put(integer2, integer3);
    }
}
