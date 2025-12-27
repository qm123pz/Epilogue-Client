package epilogue.ncm.api;

import com.google.gson.JsonObject;
import epilogue.ncm.OptionsUtil;
import epilogue.ncm.RequestUtil;
import epilogue.ncm.music.CloudMusic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudMusicApi {

    public enum SearchType {
        Single(1),
        Album(10),
        Artist(100),
        PlayList(1000);

        public final int type;

        SearchType(int type) {
            this.type = type;
        }
    }

    public static JsonObject lyricNew(long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("cp", false);
        data.put("tv", 0);
        data.put("lv", 0);
        data.put("rv", 0);
        data.put("kv", 0);
        data.put("yv", 0);
        data.put("ytv", 0);
        data.put("yrv", 0);

        return RequestUtil.createRequest("/api/song/lyric/v1", data, OptionsUtil.createOptions()).toJsonObject();
    }

    public static JsonObject cloudSearch(String keyword, SearchType type) {
        Map<String, Object> data = new HashMap<>();
        data.put("s", keyword == null ? "" : keyword);
        data.put("type", type.type);
        data.put("limit", 100);
        data.put("offset", 0);
        data.put("total", true);

        return RequestUtil.createRequest("/api/cloudsearch/pc", data, OptionsUtil.createOptions()).toJsonObject();
    }

    public static JsonObject songDetail(long id) {
        return songDetail(java.util.Collections.singletonList(id));
    }

    public static JsonObject songDetail(List<Long> ids) {
        List<String> collected = ids.stream().map(pId -> "{\"id\":" + pId + "}").collect(java.util.stream.Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("c", "[" + String.join(",", collected) + "]");
        return RequestUtil.createRequest("/api/v3/song/detail", data, OptionsUtil.createOptions()).toJsonObject();
    }

    public static JsonObject loginStatus() {
        RequestUtil.RequestAnswer request = RequestUtil.createRequest("/api/w/nuser/account/get", new HashMap<>(), OptionsUtil.createOptions("weapi"));
        JsonObject result = request.toJsonObject();
        if (request.getStatus() == 200) {
            JsonObject objResult = new JsonObject();
            objResult.addProperty("status", 200);
            objResult.add("data", request.toJsonObject());
            if (request.getCookies() != null) {
                objResult.addProperty("cookie", String.join(";", request.getCookies()));
            }
            result = objResult;
        }
        return result;
    }

    public static JsonObject likeList(long uid) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        return RequestUtil.createRequest("/api/song/like/get", data, OptionsUtil.createOptions()).toJsonObject();
    }

    public static JsonObject like(long id, boolean like) {
        Map<String, Object> data = new HashMap<>();
        data.put("alg", "itembased");
        data.put("trackId", id);
        data.put("like", like);
        data.put("time", 3);
        return RequestUtil.createRequest("/api/radio/like", data, OptionsUtil.createOptions("weapi")).toJsonObject();
    }

    public static JsonObject loginQrKey() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", 3);

        RequestUtil.RequestAnswer request = RequestUtil.createRequest("/api/login/qrcode/unikey", data, OptionsUtil.createOptions());

        JsonObject obj = new JsonObject();
        obj.addProperty("status", 200);
        obj.add("data", request.toJsonObject());
        if (request.getCookies() != null) {
            obj.addProperty("cookie", String.join(";", request.getCookies()));
        }

        return obj;
    }

    public static JsonObject loginQrCheck(String key) {
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("type", 3);

        RequestUtil.RequestAnswer request = RequestUtil.createRequest("/api/login/qrcode/client/login", data, OptionsUtil.createOptions());
        if (request.getCookies() != null && request.getBody() instanceof Map) {
            ((Map<String, Object>) request.getBody()).put("cookie", String.join(";", request.getCookies()));
        }
        return request.toJsonObject();
    }

    public static JsonObject userPlaylist(long uid, int offset, int limit) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("limit", limit);
        data.put("offset", offset);
        data.put("includeVideo", true);

        return RequestUtil.createRequest("/api/user/playlist", data, OptionsUtil.createOptions("weapi")).toJsonObject();
    }

    public static JsonObject playlistDetail(long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("n", 100000);
        data.put("s", 8);
        return RequestUtil.createRequest("/api/v6/playlist/detail", data, OptionsUtil.createOptions()).toJsonObject();
    }

    public static JsonObject songUrlV1(long id, String level) {
        Map<String, Object> data = new HashMap<>();
        data.put("ids", "[" + id + "]");
        data.put("level", level);
        data.put("encodeType", CloudMusic.preferNonMp3() ? "aac" : "mp3");

        if ("sky".equals(level)) {
            data.put("immerseType", "c51");
        }

        return RequestUtil.createRequest("/api/song/enhance/player/url/v1", data, OptionsUtil.createOptions()).toJsonObject();
    }

    public static JsonObject playlistTrackAll(long id, int s) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("n", 100000);
        data.put("s", s);

        RequestUtil.RequestAnswer v6Detail = RequestUtil.createRequest("/api/v6/playlist/detail", data, OptionsUtil.createOptions());
        JsonObject v6Obj = v6Detail.toJsonObject();

        JsonObject playlist = v6Obj.getAsJsonObject("playlist");
        if (playlist == null || !playlist.has("trackIds")) {
            return v6Obj;
        }

        List<Long> ids = new ArrayList<>();
        for (com.google.gson.JsonElement trackId : playlist.getAsJsonArray("trackIds")) {
            ids.add(trackId.getAsJsonObject().get("id").getAsLong());
        }

        List<String> collected = ids.stream().map(pId -> "{\"id\":" + pId + "}").collect(java.util.stream.Collectors.toList());

        Map<String, Object> dataV3 = new HashMap<>();
        dataV3.put("c", "[" + String.join(",", collected) + "]");

        return RequestUtil.createRequest("/api/v3/song/detail", dataV3, OptionsUtil.createOptions()).toJsonObject();
    }

    public static JsonObject playlistTracks(String operation, long trackId, String musics) {
        String[] split = musics.split(",");
        Map<String, Object> data = new HashMap<>();
        data.put("op", operation);
        data.put("pid", trackId);
        data.put("trackIds", new com.google.gson.Gson().toJson(split));
        data.put("imme", "true");

        RequestUtil.RequestAnswer request = RequestUtil.createRequest("/api/playlist/manipulate/tracks", data, OptionsUtil.createOptions());
        if (request.getStatus() == 512) {
            Map<String, Object> data2 = new HashMap<>();
            data2.put("op", operation);
            data2.put("pid", trackId);
            List<String> list = new ArrayList<>();
            list.addAll(Arrays.asList(split));
            list.addAll(Arrays.asList(split));
            data2.put("trackIds", new com.google.gson.Gson().toJson(list.toArray(new String[0])));
            data2.put("imme", "true");
            return RequestUtil.createRequest("/api/playlist/manipulate/tracks", data2, OptionsUtil.createOptions()).toJsonObject();
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("status", 200);
        obj.add("body", request.toJsonObject());
        return obj;
    }

    public static JsonObject recommendResource() {
        return RequestUtil.createRequest("/api/v1/discovery/recommend/resource", null, OptionsUtil.createOptions("weapi")).toJsonObject();
    }

    public static JsonObject recommendSongs() {
        return RequestUtil.createRequest("/api/v3/discovery/recommend/songs", null, OptionsUtil.createOptions("weapi")).toJsonObject();
    }
}
