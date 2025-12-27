package epilogue.ncm.music.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import epilogue.ncm.api.CloudMusicApi;

import java.util.ArrayList;
import java.util.List;

public class User {
    public long id;
    public String name;

    public static User fromProfile(JsonObject profile) {
        if (profile == null) return null;
        User u = new User();
        if (profile.has("userId")) u.id = profile.get("userId").getAsLong();
        if (profile.has("nickname")) u.name = profile.get("nickname").getAsString();
        return u;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<PlayList> playLists(int page, int limit) {
        int offset = page * limit;
        JsonObject json = CloudMusicApi.userPlaylist(this.id, offset, limit);
        if (json == null) return new ArrayList<>();
        JsonArray playlist = json.has("playlist") ? json.getAsJsonArray("playlist") : null;
        if (playlist == null) return new ArrayList<>();

        List<PlayList> list = new ArrayList<>();
        for (JsonElement e : playlist) {
            if (e != null && e.isJsonObject()) {
                PlayList p = PlayList.from(e.getAsJsonObject());
                if (p != null) list.add(p);
            }
        }
        return list;
    }
}
