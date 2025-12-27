package epilogue.module.modules.misc;

import epilogue.module.Module;
import net.minecraft.client.Minecraft;

public class CloudMusicModule extends Module {
    public CloudMusicModule() {
        super("CloudMusic", false);
    }

    @Override
    public void onEnabled() {
        Minecraft.getMinecraft().displayGuiScreen(epilogue.ui.ncm.NCMScreen.getInstance());
        this.setEnabled(false);
    }
}
