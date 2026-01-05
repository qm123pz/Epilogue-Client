package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.event.EventManager;
import epilogue.event.types.EventType;
import epilogue.events.*;
import epilogue.init.Initializer;
import epilogue.module.modules.combat.NoHitDelay;
import epilogue.ui.chat.GuiChat;
import epilogue.ui.mainmenu.GuiMainMenu;
import epilogue.ui.mainmenu.GuiStartupIntro;
import epiloguemixinbridge.IAccessorGuiChat;
import epiloguemixinbridge.IAccessorMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class MinecraftHooks {
    private static boolean startupIntroShown;

    private MinecraftHooks() {
    }

    public static void onStartGame(CallbackInfo callbackInfo) {
        new Initializer();
    }

    public static void onPostStartGame(CallbackInfo callbackInfo) {
        new Epilogue();
    }

    public static void onRunTick(CallbackInfo callbackInfo) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null && mc.thePlayer != null) {
            EventManager.call(new TickEvent(EventType.PRE));
        }
    }

    public static void onPostRunTick(CallbackInfo callbackInfo) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null && mc.thePlayer != null) {
            EventManager.call(new TickEvent(EventType.POST));
        }
    }

    public static void onLoadWorld(WorldClient worldClient, String string, CallbackInfo callbackInfo) {
        EventManager.call(new LoadWorldEvent());
    }

    public static void onUpdateFramebufferSize(CallbackInfo callbackInfo) {
        EventManager.call(new ResizeEvent());
    }

    public static void onClickMouse(CallbackInfo callbackInfo) {
        Minecraft mc = Minecraft.getMinecraft();
        if (Epilogue.moduleManager != null && Epilogue.moduleManager.modules.get(NoHitDelay.class).isEnabled()) {
            ((IAccessorMinecraft) mc).setLeftClickCounter(0);
        }
        LeftClickMouseEvent event = new LeftClickMouseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    public static void onRightClickMouse(CallbackInfo callbackInfo) {
        RightClickMouseEvent event = new RightClickMouseEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    public static void onSendClickBlockToController(CallbackInfo callbackInfo) {
        HitBlockEvent event = new HitBlockEvent();
        EventManager.call(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.playerController != null) {
                mc.playerController.resetBlockRemoving();
            }
        }
    }

    public static void onSetKeyBindState(int integer, boolean boolean2) {
        KeyBinding.setKeyBindState(integer, boolean2);
        if (boolean2 && Minecraft.getMinecraft().currentScreen == null) {
            EventManager.call(new KeyEvent(integer));
        }
    }

    public static void onChangeCurrentItem(InventoryPlayer inventoryPlayer, int slot) {
        SwapItemEvent event = new SwapItemEvent(-1, slot);
        EventManager.call(event);
        if (!event.isCancelled()) {
            inventoryPlayer.changeCurrentItem(slot);
        }
    }

    public static void onDisplayGuiScreen(GuiScreen guiScreenIn, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof net.minecraft.client.gui.GuiMainMenu) {
            if (!startupIntroShown) {
                startupIntroShown = true;
                mc.currentScreen = new GuiStartupIntro();
            } else {
                mc.currentScreen = new GuiMainMenu();
            }
            net.minecraft.client.gui.ScaledResolution scaledResolution = new net.minecraft.client.gui.ScaledResolution(mc);
            mc.currentScreen.setWorldAndResolution(mc, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
        }
    }

    public static void onReplaceGuiChat(GuiScreen guiScreenIn, CallbackInfo ci) {
        if (!(guiScreenIn instanceof net.minecraft.client.gui.GuiChat)) {
            return;
        }

        String defaultText = "";
        try {
            GuiTextField input = ((IAccessorGuiChat) guiScreenIn).getInputField();
            if (input != null && input.getText() != null) {
                defaultText = input.getText();
            }
        } catch (Throwable ignored) {
        }

        GuiChat.open(defaultText);
        ci.cancel();
    }

    public static void onCreateDisplay(CallbackInfo ci) {
        Display.setTitle(Epilogue.clientName + " " + Epilogue.clientVersion);
        setWindowIcon();
    }

    private static void setWindowIcon() {
        try {
            InputStream iconStream = Minecraft.class.getResourceAsStream("/assets/minecraft/epilogue/logo/EpilogueLogoBlack.png");
            if (iconStream == null) {
                iconStream = Minecraft.class.getResourceAsStream("/assets/minecraft/epilogue/logo/EpilogueBlack.png");
            }
            if (iconStream == null) {
                return;
            }
            BufferedImage originalImage = ImageIO.read(iconStream);
            iconStream.close();
            if (originalImage == null) {
                return;
            }
            BufferedImage icon16 = resizeImage(originalImage, 16, 16);
            BufferedImage icon32 = resizeImage(originalImage, 32, 32);
            ByteBuffer buffer16 = convertImageToByteBuffer(icon16);
            ByteBuffer buffer32 = convertImageToByteBuffer(icon32);
            Display.setIcon(new ByteBuffer[]{buffer16, buffer32});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        int currentWidth = originalImage.getWidth();
        int currentHeight = originalImage.getHeight();
        BufferedImage currentImage = originalImage;
        while (currentWidth > targetWidth * 2 || currentHeight > targetHeight * 2) {
            currentWidth = Math.max(targetWidth, currentWidth / 2);
            currentHeight = Math.max(targetHeight, currentHeight / 2);
            BufferedImage tempImage = new BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = tempImage.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION, java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.drawImage(currentImage, 0, 0, currentWidth, currentHeight, null);
            g.dispose();
            currentImage = tempImage;
        }
        if (currentWidth != targetWidth || currentHeight != targetHeight) {
            BufferedImage finalImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = finalImage.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION, java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.drawImage(currentImage, 0, 0, targetWidth, targetHeight, null);
            g.dispose();
            return finalImage;
        }
        return currentImage;
    }

    private static ByteBuffer convertImageToByteBuffer(BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        buffer.flip();
        return buffer;
    }
}
