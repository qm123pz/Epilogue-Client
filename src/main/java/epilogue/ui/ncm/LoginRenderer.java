package epilogue.ui.ncm;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import epilogue.interfaces.SharedRenderingConstants;
import epilogue.ncm.OptionsUtil;
import epilogue.ncm.music.CloudMusic;
import epilogue.ncm.music.QRCodeGenerator;
import epilogue.rendering.Image;
import epilogue.rendering.Rect;
import epilogue.rendering.animation.Interpolations;
import epilogue.rendering.rendersystem.RenderSystem;

public class LoginRenderer implements SharedRenderingConstants {

    public boolean closing = false;
    Thread loginThread;
    boolean success = false;
    float screeMaskAlpha = 0;
    double scale = 1;

    public boolean avatarLoaded = false;
    public ResourceLocation tempAvatar = new ResourceLocation("epilogue", "textures/ncm/TempAvatar");
    public String tempUsername = "";

    public LoginRenderer() {
        loginThread = new Thread(() -> {
            String cookie = CloudMusic.qrCodeLogin();
            OptionsUtil.setCookie(cookie);
            CloudMusic.saveCookie(cookie);
            success = true;
            this.closing = true;
        }, "NCM-Login");

        loginThread.start();
    }

    public void render(double mouseX, double mouseY, double posX, double posY, double width, double height, float alpha) {
        screeMaskAlpha = (float) (Interpolations.interpBezier(screeMaskAlpha * 255, this.isClosing() ? 0 : 120, 0.3f) * RenderSystem.DIVIDE_BY_255);

        GlStateManager.pushMatrix();
        scale = 1;

        double qWidth = 96, qHeight = 96;

        double startY = posY + height / 6.0;

        int textColor = reAlpha(0xFFFFFF, alpha);

        String str = "Scan QR Code to login";
        epilogue.ui.clickgui.menu.Fonts.draw(epilogue.ui.clickgui.menu.Fonts.small(), str, (float) (posX + width / 2.0 - epilogue.ui.clickgui.menu.Fonts.width(epilogue.ui.clickgui.menu.Fonts.small(), str) / 2.0), (float) startY, textColor);

        double qrX = posX + width / 2.0 - qWidth / 2.0;
        double qrY = posY + height / 6.0 * 4.0 - qHeight / 2.0;

        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();

        ITextureObject qrCode = textureManager.getTexture(QRCodeGenerator.qrCode);
        double qrNudgeX = -2;
        double qrNudgeY = -2;

        if (qrCode != null) {
            GlStateManager.color(1, 1, 1, alpha);
            Image.draw(qrCode.getGlTextureId(), posX + width / 2.0 - qWidth / 2.0 + qrNudgeX, posY + height / 6.0 * 4.0 - qHeight / 2.0 + qrNudgeY, qWidth, qHeight, Image.Type.NoColor);
        } else {
            Rect.draw(posX + width / 2.0 - qWidth / 2.0 + qrNudgeX, posY + height / 6.0 * 4.0 - qHeight / 2.0 + qrNudgeY, qWidth, qHeight, hexColor(128, 128, 128, (int) (alpha * 255)));
        }

        GlStateManager.popMatrix();
    }

    public boolean canClose() {
        return this.isClosing() && this.screeMaskAlpha <= 0.05 && success;
    }

    public boolean isClosing() {
        return closing;
    }
}
