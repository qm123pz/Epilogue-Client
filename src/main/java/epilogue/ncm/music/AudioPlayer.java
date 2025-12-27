package epilogue.ncm.music;

import lombok.Getter;
import lombok.SneakyThrows;
import java.io.File;
import processing.sound.*;

public class AudioPlayer {
    public SoundFile player;
    public Runnable afterPlayed;

    @Getter
    public float volume = 0.25f;

    public AudioPlayer(File file) {
        finished = false;
        this.player = new SoundFile(file.getAbsolutePath());
        this.setListeners();
    }

    public void setAudio(File file) {
        this.close();
        this.player = new SoundFile(file.getAbsolutePath());
        this.setListeners();
        finished = false;
    }

    @Getter
    FFT fft = new FFT(128, callback);

    public static final JSynFFT.FFTCalcCallback callback = fft -> {
    };

    @Getter
    Waveform waveform = new Waveform(0.05f);

    public void setListeners() {
        fft.removeInput();
        waveform.removeInput();
        waveform.resize(0.05f);
        waveform.input(this.player);
        fft.input(this.player);
        player.setOnFinished(() -> finished = true);
    }

    public void play() {
        finished = false;
        this.player.play();
        this.player.amp(volume);
    }

    @SneakyThrows
    public void setPlaybackTime(float millis) {
        this.player.jump(millis / 1000F);
        this.player.amp(volume);
    }

    @SneakyThrows
    public void close() {
        if (player == null) return;
        this.player.jump(0);
        player.stop();
        player.cleanUp();
    }

    @Getter
    private boolean finished = false;

    public void setAfterPlayed(Runnable runnable) {
        this.afterPlayed = runnable;
        this.player.setOnFinished(() -> {
            finished = true;
            runnable.run();
        });
    }

    public float getTotalTimeSeconds() {
        return (int) this.player.duration();
    }

    public float getCurrentTimeSeconds() {
        return (int) (getCurrentTimeMillis() / 1000);
    }

    public float getTotalTimeMillis() {
        return getTotalTimeSeconds() * 1000;
    }

    public float getCurrentTimeMillis() {
        return this.player.position() * 1000;
    }

    public boolean isPausing() {
        return !this.player.isPlaying();
    }

    public void setVolume(float volume) {
        this.volume = volume;
        this.player.amp(this.getVolume());
    }

    public void pause() {
        this.player.pause();
    }

    public void unpause() {
        this.play();
    }
}
