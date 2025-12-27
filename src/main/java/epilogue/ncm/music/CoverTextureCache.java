package epilogue.ncm.music;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CoverTextureCache {

    private static final int RETRY = 3;

    private static final int MAX = 128;

    private static final Map<String, ResourceLocation> CACHE = new LinkedHashMap<String, ResourceLocation>(MAX, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ResourceLocation> eldest) {
            boolean remove = size() > MAX;
            if (remove) {
                try {
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        try {
                            Minecraft.getMinecraft().getTextureManager().deleteTexture(eldest.getValue());
                        } catch (Throwable ignored) {
                        }
                    });
                } catch (Throwable ignored) {
                }
            }
            return remove;
        }
    };

    private static final ConcurrentHashMap<String, Boolean> LOADING = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> FAIL_UNTIL = new ConcurrentHashMap<>();

    private CoverTextureCache() {
    }

    public static ResourceLocation getOrRequest(String url, int size) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        String full = url;
        if (size > 0) {
            if (full.indexOf('?') >= 0) {
                full = full + "&param=" + size + "y" + size;
            } else {
                full = full + "?param=" + size + "y" + size;
            }
        }

        long now = System.currentTimeMillis();
        Long until = FAIL_UNTIL.get(full);
        if (until != null && until > now) {
            return null;
        }

        synchronized (CACHE) {
            ResourceLocation cached = CACHE.get(full);
            if (cached != null) {
                return cached;
            }
        }

        if (LOADING.putIfAbsent(full, true) != null) {
            return null;
        }

        String finalFull = full;
        new Thread(() -> {
            try {
                BufferedImage img = null;
                Throwable lastErr = null;
                for (int attempt = 0; attempt < RETRY && img == null; attempt++) {
                    try {
                        img = tryDownload(finalFull);
                        if (img == null && finalFull.startsWith("https://")) {
                            img = tryDownload("http://" + finalFull.substring("https://".length()));
                        }
                    } catch (Throwable t) {
                        lastErr = t;
                    }
                }

                if (img == null) {
                    if (lastErr != null) {
                        System.err.println("[CoverTextureCache] download failed: " + finalFull + " : " + lastErr);
                    }
                    FAIL_UNTIL.put(finalFull, System.currentTimeMillis() + 3000L);
                    LOADING.remove(finalFull);
                    return;
                }

                BufferedImage cropped = centerCropSquare(img);
                if (cropped != null) img = cropped;
                final BufferedImage finalImg = img;

                ResourceLocation loc = new ResourceLocation("epilogue", "ncm_cover_" + sha1Hex(finalFull));

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    try {
                        if (Minecraft.getMinecraft().getTextureManager().getTexture(loc) != null) {
                            Minecraft.getMinecraft().getTextureManager().deleteTexture(loc);
                        }
                        DynamicTexture tex = new DynamicTexture(finalImg);
                        Minecraft.getMinecraft().getTextureManager().loadTexture(loc, tex);
                        synchronized (CACHE) {
                            CACHE.put(finalFull, loc);
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        synchronized (CACHE) {
                            LOADING.remove(finalFull);
                        }
                    }
                });
            } catch (Throwable ignored) {
                FAIL_UNTIL.put(finalFull, System.currentTimeMillis() + 3000L);
                LOADING.remove(finalFull);
            }
        }, "NCM-Cover").start();

        return null;
    }

    private static String sha1Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Throwable t) {
            return Integer.toHexString(s.hashCode());
        }
    }

    private static BufferedImage tryDownload(String url) throws Throwable {
        URLConnection conn = new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        conn.setRequestProperty("Referer", "https://music.163.com/");
        try (InputStream is = conn.getInputStream()) {
            return ImageIO.read(is);
        }
    }

    private static BufferedImage centerCropSquare(BufferedImage src) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 0 || h <= 0) return null;
        int size = Math.min(w, h);
        if (size <= 0) return null;
        if (w == h) return src;
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        try {
            BufferedImage sub = src.getSubimage(x, y, size, size);
            BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            out.getGraphics().drawImage(sub, 0, 0, null);
            return out;
        } catch (Throwable t) {
            return src;
        }
    }

    public static boolean isReady(String url, int size) {
        if (url == null || url.isEmpty()) return false;
        String full = url;
        if (size > 0) {
            if (full.indexOf('?') >= 0) {
                full = full + "&param=" + size + "y" + size;
            } else {
                full = full + "?param=" + size + "y" + size;
            }
        }
        synchronized (CACHE) {
            return CACHE.containsKey(full);
        }
    }

    public static ITextureObject getTexture(ResourceLocation loc) {
        if (loc == null) return null;
        try {
            return Minecraft.getMinecraft().getTextureManager().getTexture(loc);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
