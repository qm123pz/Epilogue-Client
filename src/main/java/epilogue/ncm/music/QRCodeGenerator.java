package epilogue.ncm.music;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.Hashtable;

public class QRCodeGenerator {

    public static final ResourceLocation qrCode = new ResourceLocation("epilogue", "ncm_qrcode");

    @SneakyThrows
    public static void generateAndLoadTexture(String address) {
        long t0 = System.currentTimeMillis();
        BufferedImage img = QRCodeGenerator.generateQRCode(address, 128, 128);
        long t1 = System.currentTimeMillis();

        final int w0 = img.getWidth();
        final int h0 = img.getHeight();
        int[] px0 = new int[w0 * h0];
        img.getRGB(0, 0, w0, h0, px0, 0, w0);
        int black = 0;
        int white = 0;
        for (int p : px0) {
            int rgb = p & 0x00FFFFFF;
            if (rgb == 0x000000) black++;
            else if (rgb == 0x00FFFFFF) white++;
        }
        System.out.println("[NCM][QR] generated " + w0 + "x" + h0 + " in " + (t1 - t0) + "ms; black=" + black + " white=" + white + " sample=" + Integer.toHexString(px0[0]));

        new Thread(() -> Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().getTextureManager().getTexture(qrCode) != null) {
                Minecraft.getMinecraft().getTextureManager().deleteTexture(qrCode);
            }
            int w = img.getWidth();
            int h = img.getHeight();
            int[] pixels = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);

            for (int i = 0; i < pixels.length; i++) {
                pixels[i] = (pixels[i] & 0x00FFFFFF) | 0xFF000000;
            }

            DynamicTexture texture = new DynamicTexture(w, h);
            int[] data = texture.getTextureData();
            System.arraycopy(pixels, 0, data, 0, Math.min(pixels.length, data.length));
            texture.updateDynamicTexture();

            Minecraft.getMinecraft().getTextureManager().loadTexture(qrCode, texture);

            ITextureObject texObj = Minecraft.getMinecraft().getTextureManager().getTexture(qrCode);
            int glId = texObj == null ? -1 : texObj.getGlTextureId();
            System.out.println("[NCM][QR] uploaded on thread=" + Thread.currentThread().getName() + " key=" + qrCode + " glId=" + glId + " texData0=" + Integer.toHexString(data.length > 0 ? data[0] : 0));
        }), "NCM-QR-Upload").start();
    }

    public static BufferedImage generateQRCode(String text, int width, int height) throws Exception {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    public static final class MatrixToImageWriter {

        private static final int BLACK = 0xFF000000;
        private static final int WHITE = 0xFFFFFFFF;

        private MatrixToImageWriter() {
        }

        public static BufferedImage toBufferedImage(BitMatrix matrix) {
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
                }
            }
            return image;
        }
    }
}
