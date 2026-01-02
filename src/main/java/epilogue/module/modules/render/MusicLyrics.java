package epilogue.module.modules.render;

import epilogue.event.EventTarget;
import epilogue.events.Render2DEvent;
import epilogue.module.Module;
import epilogue.ncm.LyricLine;
import epilogue.ncm.MusicLyricCache;
import epilogue.ncm.music.CloudMusic;
import epilogue.rendering.StencilClipManager;
import epilogue.ui.clickgui.menu.Fonts;
import epilogue.ui.clickgui.menu.render.DrawUtil;
import epilogue.util.render.RenderUtil;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.util.List;

public class MusicLyrics extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeValue align = new ModeValue("Align", 1, new String[]{"Left", "Center", "Right"});
    public final BooleanValue showTranslation = new BooleanValue("Translation", true);
    public final IntValue width = new IntValue("Width", 320, 160, 900);
    public final IntValue height = new IntValue("Height", 120, 60, 480);
    public final IntValue offX = new IntValue("Offset X", 0, -255, 255);
    public final IntValue offY = new IntValue("Offset Y", 60, -255, 255);

    private long lastSongId = -1;
    private double scrollOffset = 0;
    private LyricLine currentDisplaying = null;

    private final MusicLyricCache lyricCache = new MusicLyricCache();

    private static final double HIGHLIGHT_GRADIENT_PX = 18.0;
    private static final float HIGHLIGHT_TAIL_ALPHA = 0.35f;

    public MusicLyrics() {
        super("MusicLyrics", false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;
        if (mc.thePlayer == null) return;
        if (CloudMusic.player == null || CloudMusic.currentlyPlaying == null) return;

        List<LyricLine> lyrics = CloudMusic.lyrics;
        if (lyrics == null || lyrics.isEmpty()) return;

        long songId = CloudMusic.currentlyPlaying.getId();
        if (songId != lastSongId) {
            lastSongId = songId;
            currentDisplaying = null;
            scrollOffset = 0;
            lyricCache.reset();
        }

        float songProgress = CloudMusic.player.getCurrentTimeMillis();
        updateCurrentDisplaying(lyrics, songProgress);

        double w = Math.max(60, this.width.getValue());
        double h = Math.max(40, this.height.getValue());

        ScaledResolution sr = new ScaledResolution(mc);
        double x = (sr.getScaledWidth() - w) * 0.5 + this.offX.getValue();
        double y = (sr.getScaledHeight() - h) * 0.65 + this.offY.getValue();

        double lineGap = 6;
        double mainH = Fonts.height(Fonts.heading());
        double subH = Fonts.height(Fonts.small());
        boolean hasSecondary = hasSecondaryLyrics(lyrics);
        double lineH = mainH * 0.85 + lineGap + (hasSecondary && showTranslation.getValue() ? (subH + 6) : 0);

        int idx = Math.max(0, lyrics.indexOf(currentDisplaying));
        double target = idx * lineH;

        scrollOffset = interp(scrollOffset, target, 0.2);

        GlStateManager.pushMatrix();
        DrawUtil.resetColor();

        StencilClipManager.beginClip(() -> RenderUtil.drawRect((float) x, (float) y, (float) w, (float) h, -1));

        double baseY = y + h * 0.5 - Fonts.height(Fonts.heading()) * 0.5 - scrollOffset;

        for (int i = 0; i < lyrics.size(); i++) {

            LyricLine line = lyrics.get(i);
            if (line == null) continue;

            double ly = baseY + i * lineH;
            if (ly + lineH < y) continue;
            if (ly > y + h) break;

            float a = (i == idx) ? 1.0f : 0.35f;
            line.alpha = (float) interp(line.alpha, a, 0.1);

            String main = line.getLyric() == null ? "" : line.getLyric();
            String secondary = "";
            if (this.showTranslation.getValue()) {
                if (line.translationText != null && !line.translationText.isEmpty()) secondary = line.translationText;
                else if (line.romanizationText != null && !line.romanizationText.isEmpty()) secondary = line.romanizationText;
            }

            double drawX = alignX(x, w, main);
            int color = new Color(1f, 1f, 1f, Math.min(1f, Math.max(0f, line.alpha))).getRGB();

            Fonts.draw(Fonts.heading(), main, (float) drawX, (float) ly, color);

            if (!secondary.isEmpty()) {
                double sy = ly + mainH + 2;
                int c2 = new Color(1f, 1f, 1f, Math.min(1f, Math.max(0f, line.alpha * 0.75f))).getRGB();
                Fonts.draw(Fonts.small(), secondary, (float) alignXSmall(x, w, secondary), (float) sy, c2);
            }

            if (i == idx) {
                renderCurrentLineEffect(x, w, line, main, drawX, ly, songProgress, hasSecondary && showTranslation.getValue());
            }
        }

        StencilClipManager.endClip();

        DrawUtil.resetColor();
        GlStateManager.popMatrix();
    }

    private void updateCurrentDisplaying(List<LyricLine> lyrics, float songProgress) {
        if (lyrics.isEmpty()) {
            currentDisplaying = null;
            return;
        }

        String curText = lyricCache.getText();
        LyricLine cur = currentDisplaying;
        LyricLine result = cur;
        if (result == null) result = lyrics.get(0);

        if (curText != null && !curText.isEmpty()) {
            for (int i = 0; i < lyrics.size(); i++) {
                LyricLine line = lyrics.get(i);
                if (line == null) continue;
                String t = line.getLyric();
                if (t == null) t = "";
                if (t.equals(curText)) {
                    result = line;
                    break;
                }
            }
        }

        currentDisplaying = result;
        if (cur != currentDisplaying) {
            scrollOffset = 0;
        }
    }

    private double alignX(double x, double w, String text) {
        int a = this.align.getValue();
        double tw = Fonts.width(Fonts.heading(), text);
        if (a == 0) return x;
        if (a == 2) return x + w - tw;
        return x + w * 0.5 - tw * 0.5;
    }

    private double alignXSmall(double x, double w, String text) {
        int a = this.align.getValue();
        double tw = Fonts.width(Fonts.small(), text);
        if (a == 0) return x;
        if (a == 2) return x + w - tw;
        return x + w * 0.5 - tw * 0.5;
    }

    private boolean hasSecondaryLyrics(List<LyricLine> lyrics) {
        if (lyrics == null) return false;
        for (LyricLine l : lyrics) {
            if (l == null) continue;
            if (l.translationText != null && !l.translationText.isEmpty()) return true;
            if (l.romanizationText != null && !l.romanizationText.isEmpty()) return true;
        }
        return false;
    }

    private void renderCurrentLineEffect(double x, double w, LyricLine line, String main, double drawX, double y,
                                         float songProgress, boolean hasSecondary) {
        if (line == null) return;
        if (line.words == null || line.words.isEmpty()) {
            double progress = getLineProgress(songProgress, CloudMusic.lyrics, CloudMusic.lyrics.indexOf(line));
            renderClipFill(drawX, y, Fonts.width(Fonts.heading(), main), Fonts.height(Fonts.heading()) + 4, progress, main);
            return;
        }

        renderScrollMode(x, w, line, main, drawX, y, songProgress);
    }

    private void renderScrollMode(double x, double w, LyricLine line, String main, double drawX, double y, float songProgress) {
        double progressWidth = calcProgressWidth(line, songProgress);
        if (progressWidth <= 0) return;

        double textW = Fonts.width(Fonts.heading(), main);
        double fullH = Fonts.height(Fonts.heading()) + 4;

        double solidW = Math.max(0.0, Math.min(textW, progressWidth - HIGHLIGHT_GRADIENT_PX));
        double tailStart = solidW;
        double tailW = Math.max(0.0, Math.min(HIGHLIGHT_GRADIENT_PX, progressWidth - solidW));

        if (solidW > 0.5) {
            StencilClipManager.beginClip(() -> RenderUtil.drawRect((float) drawX, (float) y, (float) (solidW + 1), (float) fullH, -1));
            Fonts.draw(Fonts.heading(), main, (float) drawX, (float) y, new Color(1f, 1f, 1f, 1f).getRGB());
            StencilClipManager.endClip();
        }

        if (tailW > 0.5) {
            int steps = 5;
            double stepW = tailW / steps;
            for (int i = 0; i < steps; i++) {
                double sx = drawX + tailStart + i * stepW;
                double sw = (i == steps - 1) ? (tailW - i * stepW) : stepW;
                float a = 1f - (float) ((i + 1) / (double) steps);
                a = Math.max(0f, Math.min(1f, a));
                a = HIGHLIGHT_TAIL_ALPHA + (1f - HIGHLIGHT_TAIL_ALPHA) * a;

                StencilClipManager.beginClip(() -> RenderUtil.drawRect((float) sx, (float) y, (float) (sw + 1), (float) fullH, -1));
                Fonts.draw(Fonts.heading(), main, (float) drawX, (float) y, new Color(1f, 1f, 1f, a).getRGB());
                StencilClipManager.endClip();
            }
        }
    }

    private int findCurrentWordIndex(LyricLine line, float songProgress) {
        if (line == null || line.words == null || line.words.isEmpty()) return 0;
        long rel = (long) (songProgress - line.timestamp);
        int idx = 0;
        for (int i = 0; i < line.words.size(); i++) {
            LyricLine.Word w = line.words.get(i);
            if (w == null) continue;
            if (w.timestamp > rel) {
                idx = Math.max(0, i);
                break;
            }
            if (i == line.words.size() - 1) idx = i;
        }
        return idx;
    }

    private double calcWordProgress(LyricLine line, int wordIndex, float songProgress) {
        if (line == null || line.words == null || line.words.isEmpty()) return 0;
        if (wordIndex < 0) return 0;
        if (wordIndex >= line.words.size()) return 1;
        long rel = (long) (songProgress - line.timestamp);
        long prev = wordIndex == 0 ? 0 : line.words.get(wordIndex - 1).timestamp;
        long cur = line.words.get(wordIndex).timestamp;
        long dur = Math.max(1, cur - prev);
        double t = (rel - prev) / (double) dur;
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        return t;
    }

    private double calcProgressWidth(LyricLine line, float songProgress) {
        if (line == null || line.words == null || line.words.isEmpty()) return 0;
        int curIndex = findCurrentWordIndex(line, songProgress);
        StringBuilder before = new StringBuilder();
        for (int i = 0; i < curIndex; i++) {
            LyricLine.Word w = line.words.get(i);
            if (w != null) before.append(w.word);
        }
        String curWord = "";
        if (curIndex >= 0 && curIndex < line.words.size() && line.words.get(curIndex) != null) {
            curWord = line.words.get(curIndex).word;
        }

        double base = Fonts.width(Fonts.heading(), before.toString());
        double p = calcWordProgress(line, curIndex, songProgress);
        double w = Fonts.width(Fonts.heading(), curWord);
        return base + w * p;
    }

    private void renderClipFill(double x, double y, double fullW, double fullH, double perc, String text) {
        if (perc <= 0) return;
        if (perc > 1) perc = 1;
        double finalPerc = perc;
        StencilClipManager.beginClip(() -> RenderUtil.drawRect((float) x, (float) y, (float) (fullW * finalPerc + 1), (float) fullH, -1));
        Fonts.draw(Fonts.heading(), text, (float) x, (float) y, new Color(1f, 1f, 1f, 1f).getRGB());
        StencilClipManager.endClip();
    }

    private double getLineProgress(float songProgress, List<LyricLine> lyrics, int idx) {
        if (idx < 0 || idx >= lyrics.size()) return 0.0;
        LyricLine cur = lyrics.get(idx);
        long start = cur.getTimestamp();
        long end;
        if (idx + 1 < lyrics.size()) {
            end = lyrics.get(idx + 1).getTimestamp();
        } else {
            end = start + 3000;
        }
        long dur = Math.max(150, end - start);
        double t = (songProgress - start) / (double) dur;
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        return t;
    }

    private static double interp(double cur, double target, double speed) {
        return cur + (target - cur) * Math.max(0.0, Math.min(1.0, speed));
    }
}
