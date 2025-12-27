package epilogue.ncm.music.dto;

import com.google.gson.JsonObject;

public class Album {
    public long id;
    public String name;
    public String picUrl;

    public static Album from(JsonObject obj) {
        if (obj == null) return null;
        Album a = new Album();
        if (obj.has("id")) a.id = obj.get("id").getAsLong();
        if (obj.has("name")) a.name = obj.get("name").getAsString();
        if (obj.has("picUrl") && !obj.get("picUrl").isJsonNull()) a.picUrl = obj.get("picUrl").getAsString();
        return a;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPicUrl() {
        return picUrl;
    }
}
