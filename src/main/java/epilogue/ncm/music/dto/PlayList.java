package epilogue.ncm.music.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import epilogue.ncm.api.CloudMusicApi;

import java.util.ArrayList;
import java.util.List;

public class PlayList {
    public long id;
    public String name;
    public long userId;
    public long playCount;
    public long trackCount;
    public String coverImgUrl;
    public boolean subscribed;

    public static PlayList from(JsonObject obj) {
        if (obj == null) return null;
        PlayList p = new PlayList();
        if (obj.has("id")) p.id = obj.get("id").getAsLong();
        if (obj.has("name")) p.name = obj.get("name").getAsString();
        if (obj.has("userId")) p.userId = obj.get("userId").getAsLong();
        if (obj.has("playCount")) p.playCount = obj.get("playCount").getAsLong();
        if (obj.has("trackCount")) p.trackCount = obj.get("trackCount").getAsLong();
        if (obj.has("coverImgUrl") && !obj.get("coverImgUrl").isJsonNull()) p.coverImgUrl = obj.get("coverImgUrl").getAsString();
        if (obj.has("subscribed")) p.subscribed = obj.get("subscribed").getAsBoolean();
        return p;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public List<Music> tracks() {
        JsonObject json = CloudMusicApi.playlistTrackAll(this.id, 8);
        if (json == null) return new ArrayList<>();
        JsonArray songs = json.has("songs") ? json.getAsJsonArray("songs") : null;
        if (songs == null) {
            JsonObject playlist = json.has("playlist") ? json.getAsJsonObject("playlist") : null;
            if (playlist != null && playlist.has("tracks")) {
                songs = playlist.getAsJsonArray("tracks");
            }
        }
        if (songs == null) return new ArrayList<>();

        List<Music> list = new ArrayList<>();
        for (JsonElement e : songs) {
            if (e != null && e.isJsonObject()) {
                list.add(Music.fromSongDetail(e.getAsJsonObject()));
            }
        }
        return list;
    }
}
