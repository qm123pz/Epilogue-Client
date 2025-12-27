package epilogue.ncm;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
public class LyricLine {
    @Getter
    @NonNull
    public Long timestamp;

    @Getter
    @Setter
    @NonNull
    public String lyric;

    @Getter
    public String translationText;

    @Getter
    public String romanizationText;

    public double posY = 0;
    public double height = 0;
    public float alpha = .4f;
    public float hoveringAlpha = 0f;
    public float blurAlpha = 0f;
    public boolean shouldUpdatePosition = false;
    public double reboundAnimation = 0;

    public double scrollWidth = 0;
    public double offsetX = 0;
    public double targetOffsetX = 0;

    public double offsetY = Double.MIN_VALUE;

    public final List<Word> words = new CopyOnWriteArrayList<>();

    public static class Word {
        public final String word;
        public final long timestamp;
        public final double[] emphasizes;

        public float alpha = 0.0f;
        public double progress = 0.0;

        public Word(String word, long timestamp) {
            this.word = word;
            this.timestamp = timestamp;
            this.emphasizes = new double[word.length()];
        }
    }

    private boolean heightComputed = false;

    public void markDirty() {
        heightComputed = false;
    }

    public void computeHeight(double width, epilogue.ui.clickgui.menu.Fonts fonts, java.awt.Font fontMain, java.awt.Font fontTranslation) {
        if (heightComputed) return;

        boolean canSetComputed = true;

        if (!this.words.isEmpty()) {
            double h = epilogue.ui.clickgui.menu.Fonts.height(fontMain);

            double w = 0;
            for (Word word : words) {
                double wordWidth = epilogue.ui.clickgui.menu.Fonts.width(fontMain, word.word);
                if (w + wordWidth > width) {
                    w = wordWidth;
                    h += epilogue.ui.clickgui.menu.Fonts.height(fontMain) * .85 + 4;
                } else {
                    w += wordWidth;
                }
            }

            this.height = h;
        } else {
            String text = this.lyric == null ? "" : this.lyric;
            double lineHeight = epilogue.ui.clickgui.menu.Fonts.height(fontMain);
            double curW = 0;
            int lines = 1;
            for (int i = 0; i < text.length(); i++) {
                String ch = String.valueOf(text.charAt(i));
                double cw = epilogue.ui.clickgui.menu.Fonts.width(fontMain, ch);
                if (curW + cw > width) {
                    lines++;
                    curW = cw;
                } else {
                    curW += cw;
                }
            }
            this.height = lines * lineHeight * .85 + lines * 4;
        }

        if (translationText != null) {
            String t = translationText;
            double lineHeight = epilogue.ui.clickgui.menu.Fonts.height(fontTranslation);
            double curW = 0;
            int lines = 1;
            for (int i = 0; i < t.length(); i++) {
                String ch = String.valueOf(t.charAt(i));
                double cw = epilogue.ui.clickgui.menu.Fonts.width(fontTranslation, ch);
                if (curW + cw > width) {
                    lines++;
                    curW = cw;
                } else {
                    curW += cw;
                }
            }
            this.height += lineHeight * lines + 4 * (lines - 1) + 8;
        }

        heightComputed = canSetComputed;
    }
}
