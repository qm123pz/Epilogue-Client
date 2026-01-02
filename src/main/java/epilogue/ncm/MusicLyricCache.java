package epilogue.ncm;

import epilogue.font.CustomFontRenderer;
import epilogue.ncm.music.CloudMusic;

import java.awt.Font;
import java.util.List;

public class MusicLyricCache {

    private int lastIndex = 0;
    private float lastTimeMs = -1f;

    private String cachedText = "";
    private float cachedWidth = 0f;
    private Font cachedFont = null;

    public String getText() {
        if (CloudMusic.player == null || CloudMusic.lyrics == null || CloudMusic.lyrics.isEmpty()) {
            reset();
            return "";
        }

        float t;
        try {
            t = CloudMusic.player.getCurrentTimeMillis();
        } catch (Throwable ignored) {
            return cachedText;
        }

        List<LyricLine> lines = CloudMusic.lyrics;
        if (lastIndex < 0) lastIndex = 0;
        if (lastIndex >= lines.size()) lastIndex = lines.size() - 1;

        int idx = lastIndex;

        if (lastTimeMs >= 0f && t + 1f < lastTimeMs) {
            idx = 0;
        }

        while (idx + 1 < lines.size()) {
            LyricLine next = lines.get(idx + 1);
            if (next == null) {
                idx++;
                continue;
            }
            if (next.getTimestamp() <= t + 300f) {
                idx++;
                continue;
            }
            break;
        }

        while (idx > 0) {
            LyricLine cur = lines.get(idx);
            if (cur == null) {
                idx--;
                continue;
            }
            if (cur.getTimestamp() > t + 300f) {
                idx--;
                continue;
            }
            break;
        }

        lastIndex = idx;
        lastTimeMs = t;

        String text;
        try {
            LyricLine line = lines.get(idx);
            text = line == null ? "" : line.getLyric();
        } catch (Throwable ignored) {
            text = "";
        }
        if (text == null) text = "";

        if (!text.equals(cachedText)) {
            cachedText = text;
            cachedFont = null;
            cachedWidth = 0f;
        }

        return cachedText;
    }

    public float getWidth(Font font) {
        String text = getText();
        if (font == null) return 0f;
        if (cachedFont != font) {
            cachedFont = font;
            cachedWidth = CustomFontRenderer.getStringWidth(text, font);
        }
        return cachedWidth;
    }

    public void reset() {
        lastIndex = 0;
        lastTimeMs = -1f;
        cachedText = "";
        cachedWidth = 0f;
        cachedFont = null;
    }
}
