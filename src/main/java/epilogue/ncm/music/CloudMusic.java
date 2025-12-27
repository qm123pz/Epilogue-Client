package epilogue.ncm.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import epilogue.ncm.api.CloudMusicApi;
import epilogue.ncm.music.dto.Music;
import epilogue.ncm.music.dto.PlayList;
import epilogue.ncm.music.dto.User;
import epilogue.ncm.LyricParser;
import epilogue.ncm.LyricLine;
import epilogue.ncm.OptionsUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloudMusic {

    public static AudioPlayer player;
    public static List<Music> playList = new ArrayList<>();
    public static int curIdx = 0;
    public static Music currentlyPlaying;
    public static Thread playThread;

    public static User profile;
    public static List<PlayList> playLists;
    public static List<Long> likeList;

    public enum PlayMode {
        Random,
        LoopInList,
        LoopSingle,
        Sequential
    }

    public static PlayMode playMode = PlayMode.Sequential;

    public static Quality quality = Quality.STANDARD;

    public static final File COOKIE_FILE = new File("NCMCookie.txt");

    public static volatile List<LyricLine> lyrics = new ArrayList<>();

    public static void initNCM() {
        String s = loadCookie();

        if (s.isEmpty()) {
            s = OptionsUtil.getCookie();
        }

        if (!s.isEmpty()) {
            OptionsUtil.setCookie(s);
            loadNCM(s);
        }
    }

    private static String loadCookie() {
        try {
            if (!COOKIE_FILE.exists()) {
                return "";
            }
            List<String> strings = Files.readAllLines(COOKIE_FILE.toPath(), StandardCharsets.UTF_8);
            if (strings.isEmpty()) return "";
            return strings.get(0);
        } catch (Exception e) {
            return "";
        }
    }

    public static void saveCookie(String cookie) {
        try {
            OptionsUtil.setCookie(cookie);
            Files.write(COOKIE_FILE.toPath(), cookie.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception ignored) {
        }
    }

    public static void onStop() {
        try {
            Files.write(COOKIE_FILE.toPath(), OptionsUtil.getCookie().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception ignored) {
        }
    }

    public static void loadNCM(String cookie) {
        OptionsUtil.setCookie(cookie);
        profile = getUserProfile();
        if (profile == null) {
            return;
        }

        if (!OptionsUtil.getCookie().isEmpty()) {
            onStop();
        }

        List<PlayList> loaded = new ArrayList<>();
        int page = 0;
        while (true) {
            List<PlayList> pl;
            try {
                pl = profile.playLists(page, 30);
                if (pl.isEmpty()) {
                    break;
                }
                loaded.addAll(pl);
            } catch (Exception ignored) {
            }
            page += 1;
        }

        CloudMusic.playLists = loaded;
        likeList = likeList();
    }

    private static User getUserProfile() {
        try {
            JsonObject json = CloudMusicApi.loginStatus();
            if (json == null) return null;

            JsonObject data = null;
            if (json.has("data") && json.get("data").isJsonObject()) {
                data = json.getAsJsonObject("data");
            } else {
                data = json;
            }

            if (data == null || !data.has("profile") || !data.get("profile").isJsonObject()) {
                return null;
            }

            return User.fromProfile(data.getAsJsonObject("profile"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<Long> likeList() {
        if (profile == null) {
            return new ArrayList<>();
        }
        try {
            JsonObject json = CloudMusicApi.likeList(profile.getId());
            if (json == null || !json.has("ids")) {
                return new ArrayList<>();
            }
            JsonArray ids = json.getAsJsonArray("ids");
            List<Long> list = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                try {
                    list.add(ids.get(i).getAsLong());
                } catch (Exception ignored) {
                }
            }
            return list;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    public static void prev() {
        if (curIdx - 1 < 0) {
            if (playMode == PlayMode.LoopInList) {
                curIdx = playList.size();
            } else if (playMode == PlayMode.LoopSingle) {
                curIdx++;
            } else {
                return;
            }
        }

        if (player != null && !playList.isEmpty()) {
            curIdx--;
            player.close();
            dontAdd = true;
            playing.set(false);
        }
    }

    public static void next() {
        if (curIdx + 1 > playList.size() - 1 && playMode == PlayMode.Sequential) {
            return;
        }
        if (player != null && !playList.isEmpty()) {
            player.close();
            curIdx++;
            dontAdd = true;
            playing.set(false);
        }
    }

    public static volatile boolean dontAdd = false;

    public static void play(List<Music> songs, int startIdx) {
        songs = new ArrayList<>(songs);

        if (playThread != null) {
            doBreak = true;
            playing.set(false);
            try {
                playThread.interrupt();
                playThread.join();
            } catch (Exception ignored) {
            }
        }

        if (startIdx == -1) startIdx = 0;

        playList = songs;
        playThread = new PlayThread(songs, startIdx);
        doBreak = false;
        playing.set(false);
        playThread.start();
    }

    static volatile boolean doBreak = false;
    static AtomicBoolean playing = new AtomicBoolean(true);

    private static class PlayThread extends Thread {
        private final List<Music> songs;
        private final int startIdx;

        public PlayThread(List<Music> songs, int startIdx) {
            this.songs = songs;
            this.setName("Play Thread");
            this.startIdx = startIdx;
        }

        @Override
        public void run() {
            curIdx = startIdx;

            while (curIdx < playList.size() && !doBreak && !this.isInterrupted()) {
                Music song = playList.get(curIdx);

                if (player != null && !player.isFinished()) {
                    player.close();
                    sleep0(250);
                }

                currentlyPlaying = song;

                loadLyric(song);

                Tuple<String, String> playUrl = song.getPlayUrl();
                File musicFile = getMusicFile(playUrl, song);

                try {
                    player = initializePlayer(musicFile);
                } catch (Exception e) {
                    break;
                }

                try {
                    player.play();
                } catch (Exception e) {
                    break;
                }

                playing.set(true);

                player.setAfterPlayed(() -> {
                    playing.set(false);
                });

                while (playing.get()) {
                    if (this.isInterrupted() || doBreak) break;
                    sleep0(100);
                }

                try {
                    player.close();
                } catch (Exception ignored) {
                }

                if (playMode == PlayMode.LoopSingle) {
                    if (dontAdd) {
                        dontAdd = false;
                    }
                    if (curIdx < 0) {
                        curIdx = 0;
                    }
                } else if (playMode == PlayMode.LoopInList || playMode == PlayMode.Random) {
                    if (!dontAdd) {
                        curIdx++;
                    } else {
                        dontAdd = false;
                    }
                    if (curIdx == playList.size()) {
                        curIdx = 0;
                    }
                } else {
                    if (!dontAdd) {
                        curIdx++;
                    } else {
                        dontAdd = false;
                    }
                }
            }
        }

        private void sleep0(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private File getMusicFile(Tuple<String, String> playUrl, Music song) {
            String url = playUrl.getFirst();
            String type = playUrl.getSecond() == null ? "mp3" : playUrl.getSecond().toLowerCase();
            if (type.isEmpty()) type = "mp3";
            return getCachedOrTempFile(url, type, song);
        }

        private File getCachedOrTempFile(String playUrl, String type, Music song) {
            File musicCacheDir = new File("MusicCache");
            if (!musicCacheDir.exists()) {
                musicCacheDir.mkdir();
            }

            File music = new File(musicCacheDir, song.getId() + "." + type);
            if (!music.exists()) {
                downloadMusic(playUrl, music);
            }
            return music;
        }

        private AudioPlayer initializePlayer(File musicFile) {
            AudioPlayer p = CloudMusic.player;
            if (p == null) {
                p = new AudioPlayer(musicFile);
                CloudMusic.player = p;
            } else {
                p.setAudio(musicFile);
            }
            return p;
        }

        private void downloadMusic(String playUrl, File music) {
            try (InputStream is = new BufferedInputStream(new java.net.URL(playUrl).openStream());
                 OutputStream os = Files.newOutputStream(music.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            } catch (Throwable t) {
                try {
                    music.delete();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void loadLyric(Music song) {
        new Thread(() -> {
            try {
                String string = CloudMusicApi.lyricNew(song.getId()).toString();
                string = string.replaceAll("[ - ]", " ");
                JsonObject json = new JsonParser().parse(string).getAsJsonObject();
                lyrics = LyricParser.parse(json);
                if (lyrics.isEmpty()) {
                    lyrics = new ArrayList<>();
                    lyrics.add(new LyricLine(0L, "暂无歌词"));
                }
            } catch (Exception ignored) {
            }
        }, "NCM-Lyric").start();
    }

    public static boolean preferNonMp3() {
        try {
            try (java.io.InputStream a = CloudMusic.class.getResourceAsStream("/sfd.ser")) {
                if (a != null) return false;
            }
            try (java.io.InputStream b = CloudMusic.class.getResourceAsStream("/assets/minecraft/epilogue/sfd.ser")) {
                if (b != null) return false;
            }
            return true;
        } catch (Throwable t) {
            return true;
        }
    }

    public static List<Music> search(String keyWord) {
        List<Music> list = new ArrayList<>();
        JsonObject json = CloudMusicApi.cloudSearch(keyWord, CloudMusicApi.SearchType.Single);
        try {
            JsonObject result = json.getAsJsonObject("result");
            if (result == null) return list;
            JsonArray songs = result.getAsJsonArray("songs");
            if (songs == null) return list;
            for (int i = 0; i < songs.size(); i++) {
                list.add(Music.fromSongDetail(songs.get(i).getAsJsonObject()));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public static String qrCodeLogin() {
        String key = qrKey();

        QRCodeGenerator.generateAndLoadTexture("https://music.163.com/login?codekey=" + key);

        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                return "";
            }
            JsonObject json = CloudMusicApi.loginQrCheck(key);
            int code = json.get("code").getAsInt();
            if (code == 800) {
                key = qrKey();
                QRCodeGenerator.generateAndLoadTexture("https://music.163.com/login?codekey=" + key);
            }
            if (code == 803) {
                String cookie = json.get("cookie").getAsString();
                String[] split = cookie.split(";");
                StringBuilder sb = new StringBuilder();
                for (String s : split) {
                    if (s.contains("MUSIC_U") || s.contains("__csrf")) {
                        sb.append(s).append("; ");
                    }
                }
                if (sb.length() >= 2) {
                    return sb.substring(0, sb.length() - 2);
                }
                return cookie;
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "";
            }
        }
    }

    public static String qrKey() {
        try {
            JsonObject json = CloudMusicApi.loginQrKey();
            if (json == null) {
                System.out.println("[NCM][Login] loginQrKey returned null");
                return "";
            }

            if (json.has("unikey")) {
                return json.get("unikey").getAsString();
            }

            if (!json.has("data") || !json.get("data").isJsonObject()) {
                System.out.println("[NCM][Login] loginQrKey missing data: " + json);
                return "";
            }
            JsonObject data = json.getAsJsonObject("data");
            if (!data.has("unikey")) {
                System.out.println("[NCM][Login] loginQrKey missing unikey: " + json);
                return "";
            }
            return data.get("unikey").getAsString();
        } catch (Throwable t) {
            System.out.println("[NCM][Login] qrKey error: " + t);
            return "";
        }
    }
}
