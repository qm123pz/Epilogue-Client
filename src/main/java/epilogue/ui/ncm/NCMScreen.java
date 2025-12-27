package epilogue.ui.ncm;

import epilogue.ncm.OptionsUtil;
import epilogue.ncm.api.CloudMusicApi;
import epilogue.ncm.music.CloudMusic;
import epilogue.ncm.music.CoverTextureCache;
import epilogue.ncm.music.dto.Music;
import epilogue.ncm.music.dto.PlayList;
import epilogue.ui.clickgui.menu.Fonts;
import epilogue.ui.clickgui.menu.render.DrawUtil;
import epilogue.rendering.StencilClipManager;
import epilogue.util.render.ColorUtil;
import epilogue.util.render.RenderUtil;
import epilogue.util.render.RoundedUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NCMScreen extends BaseScreen {

    private enum Page {
        HOME,
        PLAYLIST,
        SEARCH
    }

    private static final NCMScreen instance = new NCMScreen();

    public static NCMScreen getInstance() {
        return instance;
    }

    private static final Executor NCM_ASYNC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NCM-Async");
        t.setDaemon(true);
        return t;
    });

    private final List<PlayList> myPlaylists = new ArrayList<>();
    private final List<PlayList> subPlaylists = new ArrayList<>();
    private final List<Music> searchResults = new ArrayList<>();

    private final List<PlayList> homeRecommendPlaylists = new ArrayList<>();
    private final List<Music> homeRecommendSongs = new ArrayList<>();
    private final List<Music> playlistTracks = new ArrayList<>();

    private volatile boolean homeLoading = false;
    private volatile boolean playlistLoading = false;

    private boolean draggingProgress = false;

    private Page page = Page.HOME;
    private PlayList viewingPlaylist;

    private final Deque<PageState> history = new ArrayDeque<>();

    private static final class PageState {
        private final Page page;
        private final PlayList playlist;

        private PageState(Page page, PlayList playlist) {
            this.page = page;
            this.playlist = playlist;
        }
    }

    private enum SongAction {
        LIKE,
        ADD,
        REMOVE
    }

    private static final class SongHit {
        private final SongAction action;
        private final Music music;

        private SongHit(SongAction action, Music music) {
            this.action = action;
            this.music = music;
        }
    }

    private SongHit clickedSongHit;

    private PlayList clickedHomeCard;

    private void handleSongHit(SongHit hit) {
        if (hit == null || hit.music == null) return;
        long id = hit.music.getId();
        if (id <= 0) return;

        if (hit.action == SongAction.LIKE) {
            boolean liked = CloudMusic.likeList != null && CloudMusic.likeList.contains(id);
            boolean next = !liked;
            if (CloudMusic.likeList != null) {
                if (next) {
                    if (!CloudMusic.likeList.contains(id)) CloudMusic.likeList.add(id);
                } else {
                    CloudMusic.likeList.remove(id);
                }
            }
            NCM_ASYNC.execute(() -> {
                try {
                    CloudMusicApi.like(id, next);
                } catch (Throwable ignored) {
                }
            });
            return;
        }

        if (viewingPlaylist == null) return;

        if (hit.action == SongAction.ADD) {
            NCM_ASYNC.execute(() -> {
                try {
                    CloudMusicApi.playlistTracks("add", viewingPlaylist.getId(), String.valueOf(id));
                } catch (Throwable ignored) {
                }
            });
        } else if (hit.action == SongAction.REMOVE) {
            NCM_ASYNC.execute(() -> {
                try {
                    CloudMusicApi.playlistTracks("del", viewingPlaylist.getId(), String.valueOf(id));
                } catch (Throwable ignored) {
                }
            });
        }
    }

    private boolean drawSongRow(int x, float y, int w, String text, int color, int mx, int my, Music music, boolean allowModifyPlaylist) {
        boolean hover = DrawUtil.isHovering(x, y, w, 14, mx, my);
        if (hover) {
            RenderUtil.drawRect(x, y - 1, w, 14, 0x22000000);
        }

        int btnW = 14;
        int btnGap = 4;
        int right = x + w;
        int btnY = (int) (y - 1);

        int likeX = right - btnW;
        boolean liked = music != null && CloudMusic.likeList != null && CloudMusic.likeList.contains(music.getId());
        boolean likeHover = DrawUtil.isHovering(likeX, btnY, btnW, 14, mx, my);
        if (hover || likeHover) {
            drawMiniButton(likeX, btnY, btnW, 14, liked ? "♥" : "♡", mx, my, 0xFFFFFFFF);
        }
        if (likeHover && music != null) {
            clickedSongHit = new SongHit(SongAction.LIKE, music);
        }

        int addX = likeX - btnGap - btnW;
        if (allowModifyPlaylist) {
            boolean addHover = DrawUtil.isHovering(addX, btnY, btnW, 14, mx, my);
            if (hover || addHover) {
                drawMiniButton(addX, btnY, btnW, 14, "+", mx, my, 0xFFFFFFFF);
            }
            if (addHover && music != null) {
                clickedSongHit = new SongHit(SongAction.ADD, music);
            }
        }

        int removeX = addX - btnGap - btnW;
        if (allowModifyPlaylist) {
            boolean rmHover = DrawUtil.isHovering(removeX, btnY, btnW, 14, mx, my);
            if (hover || rmHover) {
                drawMiniButton(removeX, btnY, btnW, 14, "-", mx, my, 0xFFFFFFFF);
            }
            if (rmHover && music != null) {
                clickedSongHit = new SongHit(SongAction.REMOVE, music);
            }
        }

        int reserved = btnW;
        if (allowModifyPlaylist) reserved += (btnW + btnGap) * 2;

        int leftPad = 4;
        int textW = w - reserved - leftPad - 2;
        if (textW < 10) textW = 10;

        RenderUtil.scissorStart(x, (int) y - 1, w - reserved, 14);
        Fonts.draw(Fonts.tiny(), text == null ? "" : text, x + leftPad, y + 4, color);
        RenderUtil.scissorEnd();

        return hover;
    }

    private void drawMiniButton(int x, int y, int w, int h, String text, int mx, int my, int color) {
        boolean hover = DrawUtil.isHovering(x, y, w, h, mx, my);
        RenderUtil.drawRect(x, y, w, h, hover ? 0xFF1C1C1C : 0xFF151515);
        Fonts.draw(Fonts.tiny(), text, x + 4, y + 4, color);
    }

    private String searchText = "";
    private boolean searchFocused = false;

    private float playlistScroll = 0f;
    private float playlistScrollRaw = 0f;
    private float resultsScroll = 0f;
    private float resultsScrollRaw = 0f;

    private float homeScroll = 0f;
    private float homeScrollRaw = 0f;
    private float plScroll = 0f;
    private float plScrollRaw = 0f;

    private LoginRenderer loginRenderer;

    private boolean initialized = false;

    private int x;
    private int y;
    private int w = 560;
    private int h = 360;
    private int sidebarW = 170;
    private int controlsH = 46;

    private PlayList clickedPlaylist;
    private Music clickedMusic;

    private NCMScreen() {
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        ScaledResolution sr = new ScaledResolution(mc);
        this.x = (sr.getScaledWidth() - this.w) / 2;
        this.y = (sr.getScaledHeight() - this.h) / 2;
        if (this.y < 20) this.y = 20;

        if (!initialized) {
            initialized = true;
            CloudMusic.initNCM();
            if (!OptionsUtil.getCookie().isEmpty()) {
                NCM_ASYNC.execute(() -> {
                    try {
                        CloudMusic.loadNCM(OptionsUtil.getCookie());
                    } catch (Throwable ignored) {
                    }
                    Minecraft.getMinecraft().addScheduledTask(this::refreshData);
                });
            }
        }

        refreshData();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void refreshData() {
        myPlaylists.clear();
        subPlaylists.clear();
        if (CloudMusic.playLists != null) {
            for (PlayList p : CloudMusic.playLists) {
                if (p == null) continue;
                if (p.isSubscribed()) subPlaylists.add(p);
                else myPlaylists.add(p);
            }
        }

        if (page == null) {
            page = Page.HOME;
        }

        boolean loggedIn = CloudMusic.profile != null && OptionsUtil.getCookie() != null && !OptionsUtil.getCookie().isEmpty();
        if (loggedIn && page == Page.HOME && homeRecommendPlaylists.isEmpty() && homeRecommendSongs.isEmpty() && !homeLoading) {
            loadHomeAsync();
        }
    }

    private void loadHomeAsync() {
        homeLoading = true;
        NCM_ASYNC.execute(() -> {
            List<PlayList> pls = new ArrayList<>();
            List<Music> songs = new ArrayList<>();
            try {
                com.google.gson.JsonObject json = CloudMusicApi.recommendResource();
                if (json != null && json.has("recommend") && json.get("recommend").isJsonArray()) {
                    for (com.google.gson.JsonElement e : json.getAsJsonArray("recommend")) {
                        if (e != null && e.isJsonObject()) {
                            PlayList p = PlayList.from(e.getAsJsonObject());
                            if (p != null) pls.add(p);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            try {
                com.google.gson.JsonObject json = CloudMusicApi.recommendSongs();
                if (json != null && json.has("recommend") && json.get("recommend").isJsonArray()) {
                    for (com.google.gson.JsonElement e : json.getAsJsonArray("recommend")) {
                        if (e != null && e.isJsonObject()) {
                            try {
                                songs.add(Music.fromSongDetail(e.getAsJsonObject()));
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            Minecraft.getMinecraft().addScheduledTask(() -> {
                homeRecommendPlaylists.clear();
                homeRecommendSongs.clear();
                homeRecommendPlaylists.addAll(pls);
                homeRecommendSongs.addAll(songs);
                homeScrollRaw = 0f;
                homeLoading = false;
            });
        });
    }

    private void openPlaylist(PlayList p) {
        history.push(new PageState(page, viewingPlaylist));
        viewingPlaylist = p;
        page = Page.PLAYLIST;
        playlistTracks.clear();
        plScrollRaw = 0f;
        if (p != null) {
            loadPlaylistTracksAsync(p);
        }
    }

    private void goBack() {
        PageState prev = history.pollFirst();
        if (prev == null) {
            page = Page.HOME;
            viewingPlaylist = null;
            return;
        }
        page = prev.page == null ? Page.HOME : prev.page;
        viewingPlaylist = prev.playlist;
    }

    private void loadPlaylistTracksAsync(PlayList p) {
        playlistLoading = true;
        NCM_ASYNC.execute(() -> {
            List<Music> tracks;
            try {
                tracks = p.tracks();
            } catch (Throwable t) {
                tracks = new ArrayList<>();
            }
            List<Music> finalTracks = tracks;
            Minecraft.getMinecraft().addScheduledTask(() -> {
                playlistTracks.clear();
                if (finalTracks != null) playlistTracks.addAll(finalTracks);
                playlistLoading = false;
            });
        });
    }

    @Override
    public void drawScreen(double mouseX, double mouseY) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();

        clickedPlaylist = null;
        clickedMusic = null;
        clickedSongHit = null;
        clickedHomeCard = null;

        if (x <= 0 || y <= 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            this.x = (sr.getScaledWidth() - this.w) / 2;
            this.y = (sr.getScaledHeight() - this.h) / 2;
            if (this.y < 20) this.y = 20;
        }

        RenderUtil.drawRect(0, 0, new ScaledResolution(mc).getScaledWidth(), new ScaledResolution(mc).getScaledHeight(), 0xAA000000);

        int panelBg = 0xFF0E0E0E;
        int sidebarBg = 0xFF121212;
        int headerBg = 0xFF0B0B0B;
        int controlsBg = 0xFF101010;
        int textPrimary = 0xFFFFFFFF;
        int textSecondary = 0x66FFFFFF;
        int accent = 0xFFC30218;

        RenderUtil.drawRect(x, y, w, h, panelBg);
        RenderUtil.drawRect(x, y, w, 32, headerBg);
        RenderUtil.drawRect(x, y, sidebarW, h, sidebarBg);
        RenderUtil.drawRect(x + sidebarW, y + h - controlsH, w - sidebarW, controlsH, controlsBg);

        Fonts.draw(Fonts.small(), "Epilogue Music", x + 10, y + 10, textPrimary);

        boolean loggedIn = CloudMusic.profile != null && OptionsUtil.getCookie() != null && !OptionsUtil.getCookie().isEmpty();

        int searchX = x + 10;
        int searchY = y + 38;
        int searchW = sidebarW - 20;
        int searchH = 18;

        int fieldBg = searchFocused ? 0xFF1D1D1D : 0xFF181818;
        RenderUtil.drawRect(searchX, searchY, searchW, searchH, fieldBg);
        String showing = searchText.isEmpty() ? "Search..." : searchText;
        int fieldColor = searchText.isEmpty() ? textSecondary : textPrimary;
        Fonts.draw(Fonts.tiny(), showing, searchX + 4, searchY + 6, fieldColor);

        int listX = x + 8;
        int listY = y + 62;
        int listW = sidebarW - 16;
        int listH = h - 62 - 24;

        int wheel = Mouse.getDWheel();
        boolean hoverList = DrawUtil.isHovering(listX, listY, listW, listH, mx, my);
        if (hoverList && wheel != 0) {
            playlistScrollRaw += wheel > 0 ? -18f : 18f;
        }

        float maxScroll = 0f;
        int itemH = 14;
        int total = 0;
        total += 1;
        total += 1;
        total += myPlaylists.size();
        total += 1;
        total += subPlaylists.size();
        float contentH = total * (itemH + 2);
        maxScroll = Math.max(0f, contentH - listH);
        if (playlistScrollRaw < 0f) playlistScrollRaw = 0f;
        if (playlistScrollRaw > maxScroll) playlistScrollRaw = maxScroll;
        playlistScroll = playlistScrollRaw;

        RenderUtil.scissorStart(listX, listY, listW, listH);
        float cy = listY - playlistScroll;

        cy = drawSectionHeader(listX, cy, "Home", textSecondary);
        drawRow(listX, cy, listW, "Home", accent, mx, my);
        cy += itemH + 2;

        cy = drawSectionHeader(listX, cy, "My Playlists", textSecondary);
        cy += 2;
        for (PlayList p : myPlaylists) {
            if (p == null) continue;
            if (drawRow(listX, cy, listW, p.getName(), textPrimary, mx, my)) {
                clickedPlaylist = p;
            }
            cy += itemH + 2;
        }

        cy = drawSectionHeader(listX, cy, "Subscribed", textSecondary);
        cy += 2;
        for (PlayList p : subPlaylists) {
            if (p == null) continue;
            if (drawRow(listX, cy, listW, p.getName(), textPrimary, mx, my)) {
                clickedPlaylist = p;
            }
            cy += itemH + 2;
        }

        RenderUtil.scissorEnd();

        String userText = loggedIn ? CloudMusic.profile.getName() : "Not logged in";
        Fonts.draw(Fonts.tiny(), userText, x + 10, y + h - 16, textSecondary);

        int contentX = x + sidebarW + 10;
        int contentY = y + 42;
        int contentW = w - sidebarW - 20;
        int contentHh = h - 42 - controlsH - 10;

        RenderUtil.drawRect(x + sidebarW, y + 32, w - sidebarW, 1, 0xFF1F1F1F);

        String title;
        if (page == Page.HOME) title = "Home";
        else if (page == Page.PLAYLIST) title = viewingPlaylist == null ? "Playlist" : viewingPlaylist.getName();
        else title = "Search Results";
        Fonts.draw(Fonts.small(), title, contentX, y + 10, textPrimary);

        int headerBtnY = y + 8;
        if (page == Page.PLAYLIST) {
            int shuffleW = 70;
            int playW = 54;
            int backW = 28;
            int gap = 6;
            int btnX = contentX + contentW - (shuffleW + gap + playW + gap + backW);
            drawButton(btnX, headerBtnY, backW, 16, "<", mx, my, textPrimary);
            drawButton(btnX + backW + gap, headerBtnY, playW, 16, "Play", mx, my, textPrimary);
            drawButton(btnX + backW + gap + playW + gap, headerBtnY, shuffleW, 16, "Shuffle", mx, my, textPrimary);
        }

        if (page == Page.SEARCH) {
            boolean hoverResults = DrawUtil.isHovering(contentX, contentY, contentW, contentHh, mx, my);
            if (hoverResults && wheel != 0) {
                resultsScrollRaw += wheel > 0 ? -18f : 18f;
            }

            float resultsMax = Math.max(0f, searchResults.size() * (itemH + 4) - contentHh);
            if (resultsScrollRaw < 0f) resultsScrollRaw = 0f;
            if (resultsScrollRaw > resultsMax) resultsScrollRaw = resultsMax;
            resultsScroll = resultsScrollRaw;

            StencilClipManager.beginClip(() -> RenderUtil.drawRect(contentX, contentY, contentW, contentHh, -1));
            float ry = contentY - resultsScroll;
            if (searchResults.isEmpty()) {
                Fonts.draw(Fonts.tiny(), loggedIn ? "Type and press Enter to search" : "Login required", contentX, ry, textSecondary);
            } else {
                for (Music m : searchResults) {
                    if (m == null) continue;
                    String row = m.getArtistsName() + " - " + m.getName();
                    boolean rowHover = drawSongRow(contentX, ry, contentW, row, textPrimary, mx, my, m, false);
                    if (rowHover) {
                        clickedMusic = m;
                    }
                    ry += itemH + 4;
                }
            }
            StencilClipManager.endClip();
        } else if (page == Page.PLAYLIST) {
            boolean hover = DrawUtil.isHovering(contentX, contentY, contentW, contentHh, mx, my);
            if (hover && wheel != 0) {
                plScrollRaw += wheel > 0 ? -18f : 18f;
            }
            float max = Math.max(0f, playlistTracks.size() * (itemH + 4) - contentHh);
            if (plScrollRaw < 0f) plScrollRaw = 0f;
            if (plScrollRaw > max) plScrollRaw = max;
            plScroll = plScrollRaw;

            StencilClipManager.beginClip(() -> RenderUtil.drawRect(contentX, contentY, contentW, contentHh, -1));
            float ry = contentY - plScroll;
            if (!loggedIn) {
                Fonts.draw(Fonts.tiny(), "Login required", contentX, ry, textSecondary);
            } else if (viewingPlaylist == null) {
                Fonts.draw(Fonts.tiny(), "No playlist selected", contentX, ry, textSecondary);
            } else {
                if (playlistLoading) {
                    Fonts.draw(Fonts.tiny(), "Loading...", contentX, ry, textSecondary);
                    ry += itemH + 4;
                }
                if (playlistTracks.isEmpty() && !playlistLoading) {
                    Fonts.draw(Fonts.tiny(), "Empty", contentX, ry, textSecondary);
                } else {
                    for (int i = 0; i < playlistTracks.size(); i++) {
                        Music m = playlistTracks.get(i);
                        if (m == null) continue;
                        String row = (i + 1) + ". " + m.getArtistsName() + " - " + m.getName();
                        boolean rowHover = drawSongRow(contentX, ry, contentW, row, textPrimary, mx, my, m, true);
                        if (rowHover) {
                            clickedMusic = m;
                        }
                        ry += itemH + 4;
                    }
                }
            }
            StencilClipManager.endClip();
        } else {
            boolean hover = DrawUtil.isHovering(contentX, contentY, contentW, contentHh, mx, my);
            if (hover && wheel != 0) {
                homeScrollRaw += wheel > 0 ? -32f : 32f;
            }

            int margin = 12;
            int topPad = 44;
            int rowH = 40;
            int cover = 32;
            int gapY = 6;
            float homeContentH = margin + topPad + homeRecommendPlaylists.size() * (rowH + gapY) + margin;
            float max = Math.max(0f, homeContentH - contentHh);
            if (homeScrollRaw < 0f) homeScrollRaw = 0f;
            if (homeScrollRaw > max) homeScrollRaw = max;
            homeScroll = homeScrollRaw;

            float ry = contentY - homeScroll;
            StencilClipManager.beginClip(() -> RenderUtil.drawRect(contentX, contentY, contentW, contentHh, -1));

            if (!loggedIn) {
                Fonts.draw(Fonts.small(), "Login required", contentX + margin, ry + margin, textSecondary);
                StencilClipManager.endClip();
            } else {
                Fonts.draw(Fonts.heading(), "欢迎来到Epilogue Client", contentX + margin, ry + margin, textPrimary);
                Fonts.draw(Fonts.small(), "推荐歌单", contentX + margin, ry + margin + 26, textSecondary);

                if (homeLoading) {
                    Fonts.draw(Fonts.tiny(), "Loading...", contentX + margin, ry + margin + 44, textSecondary);
                }

                float homeListY = ry + margin + topPad;
                float homeListX = contentX + margin;
                int homeListW = contentW - margin * 2;

                int idx = 0;
                for (PlayList p : homeRecommendPlaylists) {
                    if (p == null) continue;

                    float rowY = homeListY + idx * (rowH + gapY);
                    boolean hov = DrawUtil.isHovering(homeListX, rowY, homeListW, rowH, mx, my);
                    int bg = hov ? 0x1AFFFFFF : 0x0EFFFFFF;
                    RoundedUtil.drawRound(homeListX, rowY, homeListW, rowH, 6, new java.awt.Color(bg, true));

                    float imgX = homeListX + 6;
                    float imgY = rowY + (rowH - cover) / 2f;
                    RenderUtil.drawRect(imgX, imgY, cover, cover, 0xFF151515);
                    net.minecraft.util.ResourceLocation loc = CoverTextureCache.getOrRequest(p.coverImgUrl, 64);
                    if (loc != null) {
                        ITextureObject tex = Minecraft.getMinecraft().getTextureManager().getTexture(loc);
                        if (tex != null) {
                            GlStateManager.pushMatrix();
                            GlStateManager.enableBlend();
                            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                            GlStateManager.color(1f, 1f, 1f, 1f);
                            Minecraft.getMinecraft().getTextureManager().bindTexture(loc);
                            Gui.drawScaledCustomSizeModalRect((int) imgX, (int) imgY, 0f, 0f, 64, 64, cover, cover, 64f, 64f);
                            GlStateManager.popMatrix();
                        }
                    }

                    float textX2 = imgX + cover + 8;
                    float textY2 = rowY + 12;
                    Fonts.draw(Fonts.small(), p.getName(), textX2, textY2, textPrimary);

                    if (hov) clickedHomeCard = p;

                    idx++;
                }

                StencilClipManager.endClip();
            }
        }

        int barX = x + sidebarW;
        int barY = y + h - controlsH;
        int barW = w - sidebarW;

        int coverSize = controlsH - 10;
        int coverX = barX + 10;
        int coverY = barY + (controlsH - coverSize) / 2;

        Music cur = CloudMusic.currentlyPlaying;
        RenderUtil.scissorStart(coverX, coverY, coverSize, coverSize);
        if (cur != null) {
            net.minecraft.util.ResourceLocation loc = CoverTextureCache.getOrRequest(cur.getCoverUrl(64), 64);
            if (loc != null) {
                ITextureObject tex = Minecraft.getMinecraft().getTextureManager().getTexture(loc);
                if (tex != null) {
                    RoundedUtil.drawRound(coverX, coverY, coverSize, coverSize, 4, new java.awt.Color(0xFF151515, true));
                    GlStateManager.pushMatrix();
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.color(1f, 1f, 1f, 1f);
                    Minecraft.getMinecraft().getTextureManager().bindTexture(loc);
                    Gui.drawScaledCustomSizeModalRect(coverX, coverY, 0f, 0f, 64, 64, coverSize, coverSize, 64f, 64f);
                    GlStateManager.popMatrix();
                    DrawUtil.resetColor();
                    GlStateManager.disableBlend();
                    GlStateManager.enableTexture2D();
                } else {
                    RenderUtil.drawRect(coverX, coverY, coverSize, coverSize, 0xFF151515);
                }
            } else {
                RenderUtil.drawRect(coverX, coverY, coverSize, coverSize, 0xFF151515);
            }
        } else {
            RenderUtil.drawRect(coverX, coverY, coverSize, coverSize, 0xFF151515);
        }
        RenderUtil.scissorEnd();

        int textX = coverX + coverSize + 8;
        int textY = coverY + 2;
        String name = cur == null ? "未在播放" : cur.getName();
        String artist = cur == null ? "" : (cur.getArtistsName() + " - " + (cur.album == null ? "" : cur.album.getName()));

        int rightPad = 10;
        int timeReserve = 118;
        int infoW = Math.max(10, barW - (textX - barX) - timeReserve - rightPad);
        RenderUtil.scissorStart(textX, textY, infoW, controlsH - 8);
        Fonts.draw(Fonts.small(), name, textX, textY, textPrimary);
        Fonts.draw(Fonts.tiny(), artist, textX, textY + 14, textSecondary);
        RenderUtil.scissorEnd();

        int controlsCenterX = barX + barW / 2;
        int btn = 18;
        int btnY = barY + 6;
        int prevX = controlsCenterX - 50;
        int playX = controlsCenterX - btn / 2;
        int nextX = controlsCenterX + 32;
        drawButton(prevX, btnY, btn, btn, "<", mx, my, textPrimary);
        String playIcon = (CloudMusic.player == null || CloudMusic.player.isPausing()) ? ">" : "||";
        drawButton(playX, btnY, btn, btn, playIcon, mx, my, textPrimary);
        drawButton(nextX, btnY, btn, btn, ">", mx, my, textPrimary);

        int pbW = 150;
        int pbH = 4;
        int pbX = controlsCenterX - pbW / 2;
        int pbY = barY + controlsH - 10;
        RenderUtil.drawRect(pbX, pbY, pbW, pbH, 0xFF2A2A2A);

        if (draggingProgress && !Mouse.isButtonDown(0)) {
            draggingProgress = false;
        }
        if (draggingProgress && Mouse.isButtonDown(0)) {
            if (CloudMusic.player != null && CloudMusic.player.getTotalTimeMillis() > 0) {
                double dx = Math.max(0, Math.min(pbW, mx - pbX));
                float t = (float) (dx / (double) pbW);
                CloudMusic.player.setPlaybackTime(t * CloudMusic.player.getTotalTimeMillis());
            }
        }

        float perc = 0f;
        if (CloudMusic.player != null && CloudMusic.player.getTotalTimeMillis() > 0) {
            perc = CloudMusic.player.getCurrentTimeMillis() / CloudMusic.player.getTotalTimeMillis();
            if (perc < 0f) perc = 0f;
            if (perc > 1f) perc = 1f;
        }
        RenderUtil.drawRect(pbX, pbY, pbW * perc, pbH, accent);

        int timeX = barX + barW - 10;
        String curTime = CloudMusic.player == null ? "00:00" : formatDuration(CloudMusic.player.getCurrentTimeMillis());
        String totalTime = CloudMusic.player == null ? "00:00" : formatDuration(CloudMusic.player.getTotalTimeMillis());
        String timeText = curTime + " / " + totalTime;
        int timeW = 110;
        int timeDrawX = timeX - (int) Fonts.width(Fonts.tiny(), timeText);
        RenderUtil.scissorStart(timeX - timeW, barY, timeW + 2, controlsH);
        Fonts.draw(Fonts.tiny(), timeText, timeDrawX, barY + controlsH / 2 + 4, textSecondary);
        RenderUtil.scissorEnd();

        if (!loggedIn) {
            if (loginRenderer == null) loginRenderer = new LoginRenderer();
            loginRenderer.render(mouseX, mouseY, x, y, w, h, 1.0f);
            if (loginRenderer.canClose() && !OptionsUtil.getCookie().isEmpty()) {
                loginRenderer = null;
                NCM_ASYNC.execute(() -> {
                    try {
                        CloudMusic.loadNCM(OptionsUtil.getCookie());
                    } catch (Throwable ignored) {
                    }
                    Minecraft.getMinecraft().addScheduledTask(this::refreshData);
                });
            }
        }
    }

    @Override
    public void onKeyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }

        if (searchFocused) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                String kw = searchText == null ? "" : searchText.trim();
                if (!kw.isEmpty()) {
                    page = Page.SEARCH;
                    new Thread(() -> {
                        List<Music> results = CloudMusic.search(kw);
                        Minecraft.getMinecraft().addScheduledTask(() -> {
                            searchResults.clear();
                            if (results != null) searchResults.addAll(results);
                            resultsScrollRaw = 0f;
                        });
                    }, "NCM-Search").start();
                }
                return;
            }

            if (keyCode == Keyboard.KEY_BACK) {
                if (searchText != null && !searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                return;
            }

            if (keyCode == Keyboard.KEY_V && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                return;
            }

            if (typedChar >= 32 && typedChar != 127) {
                searchText = (searchText == null ? "" : searchText) + typedChar;
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        int searchX = x + 10;
        int searchY = y + 38;
        int searchW = sidebarW - 20;
        int searchH = 18;
        searchFocused = DrawUtil.isHovering(searchX, searchY, searchW, searchH, mx, my);

        int barX = x + sidebarW;
        int barY = y + h - controlsH;
        int barW = w - sidebarW;
        int controlsCenterX = barX + barW / 2;
        int btn = 18;
        int btnY = barY + 6;
        int prevX = controlsCenterX - 50;
        int playX = controlsCenterX - btn / 2;
        int nextX = controlsCenterX + 32;

        int pbW = 150;
        int pbH = 8;
        int pbX = controlsCenterX - pbW / 2;
        int pbY = barY + controlsH - 14;

        if (mouseButton == 0) {
            int contentX = x + sidebarW + 10;
            int headerBtnY = y + 8;

            if (DrawUtil.isHovering(pbX, pbY, pbW, pbH, mx, my)) {
                if (CloudMusic.player != null && CloudMusic.player.getTotalTimeMillis() > 0) {
                    draggingProgress = true;
                    double dx = Math.max(0, Math.min(pbW, mx - pbX));
                    float t = (float) (dx / (double) pbW);
                    CloudMusic.player.setPlaybackTime(t * CloudMusic.player.getTotalTimeMillis());
                }
                return;
            }

            if (page == Page.PLAYLIST) {
                if (DrawUtil.isHovering(contentX, headerBtnY, 28, 16, mx, my)) {
                    goBack();
                    return;
                }
                if (DrawUtil.isHovering(contentX + 34, headerBtnY, 54, 16, mx, my)) {
                    if (!playlistTracks.isEmpty()) {
                        CloudMusic.play(new ArrayList<>(playlistTracks), 0);
                    }
                    return;
                }
                if (DrawUtil.isHovering(contentX + 92, headerBtnY, 70, 16, mx, my)) {
                    if (!playlistTracks.isEmpty()) {
                        ArrayList<Music> list = new ArrayList<>(playlistTracks);
                        java.util.Collections.shuffle(list);
                        CloudMusic.play(list, 0);
                    }
                    return;
                }
            }

            if (DrawUtil.isHovering(prevX, btnY, btn, btn, mx, my)) {
                CloudMusic.prev();
                return;
            }
            if (DrawUtil.isHovering(playX, btnY, btn, btn, mx, my)) {
                boolean has = CloudMusic.player != null && CloudMusic.currentlyPlaying != null;
                if (has) {
                    if (CloudMusic.player.isPausing())
                        CloudMusic.player.unpause();
                    else
                        CloudMusic.player.pause();
                }
                return;
            }
            if (DrawUtil.isHovering(nextX, btnY, btn, btn, mx, my)) {
                CloudMusic.next();
                return;
            }

            if (isHomeRow(mx, my)) {
                page = Page.HOME;
                viewingPlaylist = null;
                searchResults.clear();
                if (CloudMusic.profile != null && !homeLoading && homeRecommendPlaylists.isEmpty() && homeRecommendSongs.isEmpty()) {
                    loadHomeAsync();
                }
                return;
            }

            if (page == Page.HOME && clickedHomeCard != null) {
                openPlaylist(clickedHomeCard);
                return;
            }

            if (clickedSongHit != null && clickedSongHit.music != null) {
                handleSongHit(clickedSongHit);
                return;
            }

            if (DrawUtil.isHovering(x + 8, y + 62, sidebarW - 16, h - 62 - 24, mx, my)) {
                PlayList pl = getPlaylistAt(mx, my);
                if (pl != null) {
                    openPlaylist(pl);
                    return;
                }
            }

            if (page == Page.HOME) {
                PlayList pl = getHomePlaylistAt(mx, my);
                if (pl != null) {
                    openPlaylist(pl);
                    return;
                }
            }

            if (page == Page.PLAYLIST) {
                int idx = getPlaylistMusicIndexAt(mx, my);
                if (idx >= 0 && idx < playlistTracks.size()) {
                    CloudMusic.play(new ArrayList<>(playlistTracks), idx);
                    return;
                }
            }

            Music m = getMusicAt(mx, my);
            if (m != null) {
                List<Music> list = new ArrayList<>();
                list.add(m);
                CloudMusic.play(list, 0);
                return;
            }
        }
    }

    private PlayList getHomePlaylistAt(int mx, int my) {
        int contentX = x + sidebarW + 10;
        int contentY = y + 42;
        int contentW = w - sidebarW - 20;
        int contentHh = h - 42 - controlsH - 10;
        if (!DrawUtil.isHovering(contentX, contentY, contentW, contentHh, mx, my)) {
            return null;
        }
        float itemH = 14;
        float rowH = itemH + 4;
        float ry = contentY - homeScroll;
        if (homeLoading) {
            ry += itemH + 8;
        }
        ry += itemH + 6;
        for (PlayList p : homeRecommendPlaylists) {
            if (p == null) continue;
            if (DrawUtil.isHovering(contentX, ry, contentW, itemH, mx, my)) {
                return p;
            }
            ry += rowH;
        }
        return null;
    }

    private boolean isHomeRow(int mx, int my) {
        int listX = x + 8;
        int listY = y + 62;
        int listW = sidebarW - 16;
        float cy = listY - playlistScroll;
        cy += 14;
        return DrawUtil.isHovering(listX, cy, listW, 14, mx, my);
    }

    private int getPlaylistMusicIndexAt(int mx, int my) {
        int contentX = x + sidebarW + 10;
        int contentY = y + 42;
        int contentW = w - sidebarW - 20;
        int contentHh = h - 42 - controlsH - 10;
        if (!DrawUtil.isHovering(contentX, contentY, contentW, contentHh, mx, my)) {
            return -1;
        }
        if (playlistTracks.isEmpty()) {
            return -1;
        }
        float itemH = 14;
        float rowH = itemH + 4;
        float ry = contentY - plScroll;
        for (int i = 0; i < playlistTracks.size(); i++) {
            if (DrawUtil.isHovering(contentX, ry, contentW, itemH, mx, my)) {
                return i;
            }
            ry += rowH;
        }
        return -1;
    }

    private PlayList getPlaylistAt(int mx, int my) {
        int listX = x + 8;
        int listY = y + 62;
        int listW = sidebarW - 16;
        int listH = h - 62 - 24;

        if (!DrawUtil.isHovering(listX, listY, listW, listH, mx, my)) {
            return null;
        }

        float itemH = 14;
        float cy = listY - playlistScroll;

        cy += 14;
        cy += itemH + 2;

        cy += 14;
        cy += 2;
        for (PlayList p : myPlaylists) {
            if (p == null) continue;
            if (DrawUtil.isHovering(listX, cy, listW, itemH, mx, my)) {
                return p;
            }
            cy += itemH + 2;
        }

        cy += 14;
        cy += 2;
        for (PlayList p : subPlaylists) {
            if (p == null) continue;
            if (DrawUtil.isHovering(listX, cy, listW, itemH, mx, my)) {
                return p;
            }
            cy += itemH + 2;
        }

        return null;
    }

    private Music getMusicAt(int mx, int my) {
        int contentX = x + sidebarW + 10;
        int contentY = y + 42;
        int contentW = w - sidebarW - 20;
        int contentHh = h - 42 - controlsH - 10;

        if (!DrawUtil.isHovering(contentX, contentY, contentW, contentHh, mx, my)) {
            return null;
        }

        if (page != Page.SEARCH && page != Page.HOME) {
            return null;
        }

        if (page == Page.HOME) {
            if (!DrawUtil.isHovering(contentX, contentY, contentW, contentHh, mx, my)) {
                return null;
            }
            float itemH = 14;
            float rowH = itemH + 4;
            float ry = contentY - homeScroll;
            if (homeLoading) {
                ry += itemH + 8;
            }
            ry += itemH + 6;
            ry += homeRecommendPlaylists.size() * rowH;
            ry += 6;
            ry += itemH + 6;
            for (Music m : homeRecommendSongs) {
                if (m == null) continue;
                if (DrawUtil.isHovering(contentX, ry, contentW, itemH, mx, my)) {
                    return m;
                }
                ry += rowH;
            }
            return null;
        }

        if (searchResults.isEmpty()) {
            return null;
        }

        float itemH = 14;
        float rowH = itemH + 4;
        float ry = contentY - resultsScroll;
        for (Music m : searchResults) {
            if (m == null) continue;
            if (DrawUtil.isHovering(contentX, ry, contentW, itemH, mx, my)) {
                return m;
            }
            ry += rowH;
        }

        return null;
    }

    private float drawSectionHeader(int x, float y, String text, int color) {
        Fonts.draw(Fonts.tiny(), text, x + 2, y, color);
        return y + 14;
    }

    private boolean drawRow(int x, float y, int w, String text, int color, int mx, int my) {
        boolean hover = DrawUtil.isHovering(x, y, w, 14, mx, my);
        if (hover) {
            RenderUtil.drawRect(x, y - 1, w, 14, 0x22000000);
        }
        RenderUtil.scissorStart(x, (int) y - 1, w, 14);
        Fonts.draw(Fonts.tiny(), text, x + 4, y + 4, color);
        RenderUtil.scissorEnd();
        return hover;
    }

    private void drawButton(int x, int y, int w, int h, String text, int mx, int my, int color) {
        boolean hover = DrawUtil.isHovering(x, y, w, h, mx, my);
        RenderUtil.drawRect(x, y, w, h, hover ? 0xFF1C1C1C : 0xFF151515);
        Fonts.draw(Fonts.tiny(), text, x + 5, y + 5, color);
    }

    private String formatDuration(float totalMillis) {
        float totalSeconds = totalMillis / 1000f;
        int minutes = (int) (totalSeconds / 60f);
        int seconds = (int) (totalSeconds % 60f);
        return String.format("%02d:%02d", minutes, seconds);
    }
}
