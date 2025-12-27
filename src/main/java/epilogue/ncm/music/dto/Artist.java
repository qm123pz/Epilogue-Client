package epilogue.ncm.music.dto;

import com.google.gson.JsonObject;

public class Artist {
    public long id;
    public String name;

    public static Artist from(JsonObject obj) {
        if (obj == null) return null;
        Artist a = new Artist();
        if (obj.has("id")) a.id = obj.get("id").getAsLong();
        if (obj.has("name")) a.name = obj.get("name").getAsString();
        return a;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
