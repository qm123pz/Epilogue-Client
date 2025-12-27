package epilogue.ncm.music.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import epilogue.ncm.api.CloudMusicApi;
import epilogue.ncm.music.CloudMusic;
import epilogue.ncm.music.dto.Album;
import epilogue.ncm.music.dto.Artist;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.List;

public class Music {
    public long id;
    public String name;
    public Album album;
    public List<Artist> artists = new ArrayList<>();

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public String getArtistsName() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < artists.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(artists.get(i).name);
        }
        return sb.toString();
    }

    public String getCoverUrl(int size) {
        if (album == null) return "";
        if (album.picUrl == null) return "";
        return album.picUrl + "?param=" + size + "y" + size;
    }

    public Tuple<String, String> getPlayUrl() {
        Tuple<String, String> primary = getPlayUrl0(CloudMusic.quality == null ? "standard" : CloudMusic.quality.getQuality().toLowerCase());
        if (primary != null && primary.getFirst() != null && !primary.getFirst().isEmpty()) {
            return primary;
        }
        Tuple<String, String> fallback = getPlayUrl0("standard");
        return fallback == null ? new Tuple<>("", "") : fallback;
    }

    private Tuple<String, String> getPlayUrl0(String level) {
        try {
            JsonObject json = CloudMusicApi.songUrlV1(this.id, level);
            if (json == null || !json.has("data") || !json.get("data").isJsonArray()) {
                return new Tuple<>("", "");
            }
            JsonArray arr = json.getAsJsonArray("data");
            if (arr.size() <= 0 || !arr.get(0).isJsonObject()) {
                return new Tuple<>("", "");
            }
            JsonObject o = arr.get(0).getAsJsonObject();
            if (o.has("code") && o.get("code").isJsonPrimitive()) {
                try {
                    if (o.get("code").getAsInt() != 200) {
                        return new Tuple<>("", "");
                    }
                } catch (Exception ignored) {
                }
            }
            String url = o.has("url") && !o.get("url").isJsonNull() ? o.get("url").getAsString() : "";
            String type = o.has("type") && !o.get("type").isJsonNull() ? o.get("type").getAsString() : "mp3";
            if (type == null || type.isEmpty()) type = "mp3";
            if (url == null) url = "";
            return new Tuple<>(url, type);
        } catch (Exception ignored) {
            return new Tuple<>("", "");
        }
    }

    public static Music fromSongDetail(JsonObject song) {
        Music m = new Music();
        m.id = song.get("id").getAsLong();
        m.name = song.get("name").getAsString();
        if (song.has("al")) {
            JsonObject al = song.getAsJsonObject("al");
            m.album = Album.from(al);
        }
        if (song.has("ar")) {
            for (JsonElement e : song.getAsJsonArray("ar")) {
                JsonObject ar = e.getAsJsonObject();
                Artist a = Artist.from(ar);
                if (a != null) {
                    m.artists.add(a);
                }
            }
        }
        return m;
    }
}
