package epilogue.module.modules.render.dynamicisland;

import epilogue.Epilogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScorePlayerTeam;
import epilogue.event.EventTarget;
import epilogue.events.Render2DEvent;
import epilogue.module.Module;
import epilogue.module.modules.player.ChestStealer;
import epilogue.module.modules.misc.Nick;
import epilogue.module.modules.render.dynamicisland.notification.*;
import epilogue.util.GetIPUtil;
import epilogue.util.render.PostProcessing;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.ModeValue;
import epilogue.font.FontTransformer;
import epilogue.font.CustomFontRenderer;
import epilogue.ui.command.CommandInterface;
import epilogue.ui.clickgui.menu.MenuClickGui;
import epilogue.ui.clickgui.dropdown.DropdownClickGui;
import epilogue.events.KeyEvent;
import epilogue.ncm.LyricLine;
import epilogue.ncm.music.CloudMusic;
import epilogue.ncm.music.CoverTextureCache;
import epilogue.ncm.MusicLyricCache;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DynamicIsland extends Module {

    public final ModeValue mode = new ModeValue("Mode", 1, new String[]{"Normal", "Flat"});
    private final FloatValue animationSpeed = new FloatValue("AnimationSpeed", 12.0f, 1.0f, 20.0f, () -> mode.getValue() == 0);
    public final IntValue bgAlpha = new IntValue("BackgroundAlpha", 40, 1, 255, () -> mode.getValue() == 0);
    private final BooleanValue showNotifications = new BooleanValue("ShowNotifications", true, () -> mode.getValue() == 0);
    private final ModeValue animationMode = new ModeValue("AnimationMode", 0, new String[]{"Normal", "Custom"}, () -> mode.getValue() == 0);
    private final FloatValue customMass = new FloatValue("CustomMass", 1.0f, 0.1f, 5.0f, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final FloatValue customStiffness = new FloatValue("CustomStiffness", 0.1f, 0.01f, 1.0f, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final BooleanValue enableBounce = new BooleanValue("EnableBounce", true, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final FloatValue bounceIntensity = new FloatValue("BounceIntensity", 0.09f, 0.0f, 0.3f, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final IntValue bounceCount = new IntValue("BounceCount", 2, 0, 5, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final FloatValue bounceDuration = new FloatValue("BounceDuration", 450.0f, 100.0f, 1000.0f, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final BooleanValue enableBreathing = new BooleanValue("EnableBreathing", true, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final FloatValue breathingIntensity = new FloatValue("BreathingIntensity", 0.03f, 0.0f, 0.1f, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final FloatValue breathingSpeed = new FloatValue("BreathingSpeed", 3000.0f, 1000.0f, 10000.0f, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final ModeValue easingType = new ModeValue("EasingType", 10, new String[]{"Linear", "EaseInSine", "EaseOutSine", "EaseInOutSine", "EaseInQuad", "EaseOutQuad", "EaseInOutQuad", "EaseInCubic", "EaseOutCubic", "EaseInOutCubic", "EaseInQuart", "EaseOutQuart", "EaseInOutQuart", "EaseInQuint", "EaseOutQuint", "EaseInOutQuint", "EaseInExpo", "EaseOutExpo", "EaseInOutExpo", "EaseInCirc", "EaseOutCirc", "EaseInOutCirc", "EaseInBack", "EaseOutBack", "EaseInOutBack", "EaseInElastic", "EaseOutElastic", "EaseInOutElastic", "EaseInBounce", "EaseOutBounce", "EaseInOutBounce", "CustomSpring"}, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final FloatValue customSpringTension = new FloatValue("CustomSpringTension", 0.8f, 0.1f, 2.0f, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final FloatValue customSpringFriction = new FloatValue("CustomSpringFriction", 0.3f, 0.1f, 1.0f, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final BooleanValue enableOvershoot = new BooleanValue("EnableOvershoot", true, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final FloatValue overshootAmount = new FloatValue("OvershootAmount", 0.1f, 0.0f, 0.5f, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final BooleanValue enableAnticipation = new BooleanValue("EnableAnticipation", false, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final FloatValue anticipationAmount = new FloatValue("AnticipationAmount", 0.05f, 0.0f, 0.2f, () -> mode.getValue() == 0 && animationMode.getValue() == 1);
    private final FloatValue anticipationDuration = new FloatValue("AnticipationDuration", 100.0f, 50.0f, 300.0f, () -> mode.getValue() == 0 && animationMode.getValue() == 1);

    private final Minecraft mc = Minecraft.getMinecraft();

    private double currentWidth = 0;
    private double currentHeight = 0;
    private double targetWidth = 0;
    private double targetHeight = 0;
    private long animationStartTime = 0;
    private double lastTargetWidth = 0;
    private double lastTargetHeight = 0;
    private boolean isExpanding = false;
    private int currentBounceCount = 0;
    private long bounceStartTime = 0;
    private boolean wasInNoticeState = false;

    private double animationVelocityX = 0;
    private double animationVelocityY = 0;
    private double springPositionX = 0;
    private double springPositionY = 0;
    private long anticipationStartTime = 0;
    private boolean isInAnticipation = false;

    private final float textSpacing = 10f;

    private boolean showingCommandResult = false;
    private String commandResultType = "";
    private String commandResultTitle = "";
    private String commandResultContent = "";
    private long commandResultStartTime = 0;
    private static final long COMMAND_RESULT_DURATION = 3000;

    private final long breathingStartTime = System.currentTimeMillis();

    private boolean chestExpanded = false;
    private float currentRadius = 32.0f;
    private float targetRadius = 32.0f;
    private float lastTargetRadius = 32.0f;

    private final CommandInterface commandInterface = new CommandInterface();

    private boolean tabPressed = false;
    private boolean showingPlayerList = false;
    private float playerListAlpha = 0.0f;

    private long lastUpdateTime = System.currentTimeMillis();

    private boolean wasShowingNotifications = false;
    private float mainInterfaceSlideOffset = 0.0f;
    private long mainInterfaceSlideStartTime = 0;
    private static final long MAIN_INTERFACE_SLIDE_DURATION = 400L;

    private final List<Integer> availableTextIndices = new ArrayList<>();
    private int currentTextIndex = 0;
    private long lastTextChangeTime = System.currentTimeMillis();
    private static final long TEXT_CHANGE_INTERVAL = 5000;
    private float textAlpha = 1.0f;
    private boolean textFadingOut = false;
    private int nextTextIndex = 0;

    private float minLine2Width = 0.0f;
    private float currentDisplayWidth = 0.0f;

    private String cachedServerIP = "";
    private int cachedPing = 0;
    private long lastServerInfoUpdate = 0;
    private static final long SERVER_INFO_UPDATE_INTERVAL = 1000;

    private boolean musicActive = false;
    private long lastMusicSongId = -1;
    private String cachedMusicTitle = "";
    private net.minecraft.util.ResourceLocation cachedCoverLoc = null;

    private final MusicLyricCache lyricCache = new MusicLyricCache();

    //神秘。
    private final String[] islandText = new String[]{
            "       赛博上帝会梦到AI天堂吗      ",
            "好想用一行/kill @e结束这名为生活的Minecraft",
            "在人生的自动售货机中堆满了一个又一个的emoji",
            "灵魂在404界面找到了他的归宿   ",
            "这里没有加载中的圆圈，只有永恒的静默",
            "你是新版本，不再兼容我的旧依赖",
            "梦想.zip (确定要永久删除这一项吗)",
            "人生是一场没有热更新的游戏     ",
            "Hello, World!                       ",
            "孤独.dll（系统文件，无法删除） ",
            "情绪.css（该文件已损坏）      ",
            "我们都在制造一场前所未有的混乱...",
            "  希望.ini（0kb）                  ",
            "人生协议未读即签，灵魂权限让渡未知",
            "回忆.zip.7z.tar.gz.null         ",
            "我设置了早上6点自动关机，逃避那迟迟加载不出的明天",
            "信仰在在虚拟机上完美运行，却忘了连接真实世界的接口",
            "热爱在启动时占用所有资源，崩溃后却找不到一丝痕迹",
            "我们都在参加一场没有终点的开发  ",
            "群体智慧.hivemind                "
    };

    private final DynamicIslandFlat flatImpl = new DynamicIslandFlat();

    public DynamicIsland() {
        super("DynamicIsland", true);
        initializeTextIndices();
    }

    private void initializeTextIndices() {
        availableTextIndices.clear();
        for (int i = 0; i < islandText.length; i++) {
            availableTextIndices.add(i);
        }
        java.util.Collections.shuffle(availableTextIndices);
        currentTextIndex = availableTextIndices.remove(0);
    }

    public void showCommandResult(String type, String title, String content) {
        this.commandResultType = type;
        this.commandResultTitle = title;
        this.commandResultContent = content;
        this.showingCommandResult = true;
        this.commandResultStartTime = System.currentTimeMillis();
    }

    public static DynamicIsland getInstance() {
        return (DynamicIsland) Epilogue.moduleManager.getModule("DynamicIsland");
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (event.getKey() == Keyboard.KEY_PERIOD && !commandInterface.isActive() && mc.currentScreen == null) {
            commandInterface.activate();
        }

        if (event.getKey() == Keyboard.KEY_TAB && mc.currentScreen == null) {
            if (!tabPressed) {
                tabPressed = true;
                showingPlayerList = true;
            }
        }
    }

    public boolean isCommandActive() {
        return commandInterface.isActive();
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) {
            return;
        }

        if (mode.getModeString().equals("Flat")) {
            flatImpl.onRender2D(event);
            return;
        }

        NotificationRenderer.toggleBgRadius = 18f;
        NotificationRenderer.toggleButtonRadius = 10f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        updateTextRotation(deltaTime);
        updateTabState(deltaTime);

        ChestStealer chestStealer = (ChestStealer) Epilogue.moduleManager.modules.get(ChestStealer.class);
        chestExpanded = chestStealer != null && chestStealer.isEnabled() && ChestData.getInstance().isChestOpen() && !commandInterface.isActive();

        boolean embeddedClickGui = mc.currentScreen instanceof MenuClickGui && ((MenuClickGui) mc.currentScreen).isEmbeddedInDynamicIsland();
        boolean clickGuiOpen = mc.currentScreen instanceof MenuClickGui || mc.currentScreen instanceof DropdownClickGui;

        ScaledResolution sr = new ScaledResolution(mc);
        buildDisplayText();

        updateMusicState();

        List<Notification> activeNotifications = NotificationManager.getInstance().getActiveNotifications();
        boolean hasNotifications = !activeNotifications.isEmpty() && showNotifications.getValue() && !clickGuiOpen;

        if (wasShowingNotifications && !hasNotifications) {
            mainInterfaceSlideStartTime = currentTime;
            mainInterfaceSlideOffset = (float) currentWidth;
        }
        wasShowingNotifications = hasNotifications;

        if (!hasNotifications && mainInterfaceSlideOffset > 0) {
            long elapsed = currentTime - mainInterfaceSlideStartTime;
            float progress = Math.min(1.0f, elapsed / (float) MAIN_INTERFACE_SLIDE_DURATION);
            float eased = easeOutCubic(progress);
            mainInterfaceSlideOffset = (float) currentWidth * (1 - eased);

            if (progress >= 1.0f) {
                mainInterfaceSlideOffset = 0.0f;
            }
        }

        double padding = 12;

        if (chestExpanded) {
            ChestData chestData = ChestData.getInstance();
            int chestSize = chestData.getChestSize();
            int rows = Math.max(3, chestSize / 9);
            targetWidth = 9 * 20 + 12 * 3;
            targetHeight = rows * 20 + 12 * 3;
        } else if (embeddedClickGui) {
            MenuClickGui gui = (MenuClickGui) mc.currentScreen;
            double paddingX = 12;
            double paddingY = 18;
            targetWidth = gui.w + paddingX * 2;
            targetHeight = gui.h + paddingY * 2;
        } else if (commandInterface.isActive()) {
            targetWidth = commandInterface.getExpandedWidth();
            targetHeight = commandInterface.getExpandedHeight();

            if (mc.currentScreen == null) {
                mc.displayGuiScreen(new net.minecraft.client.gui.GuiScreen() {
                    @Override
                    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
                        commandInterface.handleMouseMove(mouseX, mouseY);
                    }

                    @Override
                    public boolean doesGuiPauseGame() {
                        return true;
                    }

                    @Override
                    protected void keyTyped(char typedChar, int keyCode) {
                        commandInterface.handleKeyInput(keyCode, typedChar);
                    }

                    @Override
                    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
                        commandInterface.handleMouseClick(mouseX, mouseY, mouseButton);
                    }

                    @Override
                    public void handleMouseInput() throws java.io.IOException {
                        super.handleMouseInput();
                        commandInterface.handleMouseScroll();
                    }
                });
            }
        } else {
            if (mc.currentScreen != null && mc.currentScreen.getClass().isAnonymousClass() &&
                    mc.currentScreen.getClass().getEnclosingClass() == DynamicIsland.class) {
                mc.displayGuiScreen(null);
            }
        }

        if (!embeddedClickGui && !commandInterface.isActive()) {
            if (showingPlayerList) {
                Collection<NetworkPlayerInfo> playerInfos = mc.getNetHandler().getPlayerInfoMap();
                double playerListWidth = calculatePlayerListWidth(playerInfos);
                targetWidth = playerListWidth + padding * 2;
                targetHeight = calculatePlayerListHeight(playerInfos) + padding * 2;
            } else if (showingCommandResult) {
                double commandResultSize = calculateCommandResultSize();
                targetWidth = commandResultSize + padding * 2;
                targetHeight = calculateCommandResultHeight() + padding * 2;
            } else if (hasNotifications) {
                double notificationContentWidth = calculateNotificationContentWidth(activeNotifications);
                targetWidth = notificationContentWidth + padding;

                float scale = 0.8f;
                float totalHeight = 0;
                for (Notification notification : activeNotifications) {
                    totalHeight += calculateNotificationItemHeight(notification.getType()) * scale;
                }
                targetHeight = totalHeight + padding * 2;
            } else {
                String serverIP = getServerIP();
                int ping = mc.getCurrentServerData() != null ? (int) mc.getCurrentServerData().pingToServer : 0;
                int fps = net.minecraft.client.Minecraft.getDebugFPS();

                float scale = 0.58f;

                FontTransformer transformer = FontTransformer.getInstance();
                Font mainFont = transformer.getFont(musicActive ? "OpenSansSemiBold" : "MicrosoftYaHei", musicActive ? 44 : 50);
                Font islandTextFont = transformer.getFont(musicActive ? "MicrosoftYaHei" : "MicrosoftYaHei", musicActive ? 36 : 38);
                Font contentFont = transformer.getFont("MicrosoftYaHei", 40);

                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("M/d");
                java.text.SimpleDateFormat weekFormat = new java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH);
                java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
                java.util.Date now = new java.util.Date();

                String dateStr = dateFormat.format(now);
                String weekStr = weekFormat.format(now);
                String timeStr = timeFormat.format(now);

                String line2Base = dateStr + " " + weekStr + " " + timeStr + " " + ping + "ms to " + serverIP + " " + fps + " FPS";
                float line2BaseWidth = CustomFontRenderer.getStringWidth(line2Base, contentFont) * scale;

                if (minLine2Width == 0.0f) {
                    minLine2Width = line2BaseWidth;
                }

                String currentText = musicActive ? getCurrentLyricText() : islandText[currentTextIndex];

                float nightskyWidth;
                if (musicActive) {
                    float iconW = 26f;
                    float gapW = 8f;
                    float titleW = cachedMusicTitle == null ? 0f : CustomFontRenderer.getStringWidth(cachedMusicTitle, mainFont);
                    nightskyWidth = iconW + gapW + titleW;
                } else {
                    nightskyWidth = CustomFontRenderer.getStringWidth("Epilogue", mainFont);
                }
                float currentTextWidth = CustomFontRenderer.getStringWidth(currentText, islandTextFont);
                if (musicActive) {
                    currentTextWidth = Math.max(currentTextWidth, line2BaseWidth / scale);
                }

                float line1BaseWidth = nightskyWidth + 10;
                float line1TotalWidth = (line1BaseWidth + currentTextWidth) * scale;

                float requiredWidth = line1TotalWidth + 80 * scale;
                if (musicActive) {
                    requiredWidth += currentTextWidth * 0.25f * scale;
                }
                float maxWidth = sr.getScaledWidth() * 0.90f;
                requiredWidth = Math.min(requiredWidth, maxWidth);

                if (currentDisplayWidth == 0.0f) {
                    currentDisplayWidth = requiredWidth;
                } else {
                    float widthDelta = requiredWidth - currentDisplayWidth;
                    float widthSpeed = 3.5f;
                    if (Math.abs(widthDelta) > 0.5f) {
                        currentDisplayWidth += widthDelta * widthSpeed * deltaTime;
                    } else {
                        currentDisplayWidth = requiredWidth;
                    }
                }

                int mainFontHeight = CustomFontRenderer.getFontHeight(mainFont);
                int contentFontHeight = CustomFontRenderer.getFontHeight(contentFont);
                float lineSpacing = 4 * scale;

                targetWidth = currentDisplayWidth + padding * 3.5;
                targetHeight = (mainFontHeight * scale + contentFontHeight * scale + lineSpacing) + padding * 1.2;
            }
        }

        updateAnimations();

        double finalWidth = getBreathingWidth();
        double finalHeight = getBreathingHeight();

        if (animationStartTime > 0) {
            finalWidth = Math.max(finalWidth, getBounceWidth());
            finalHeight = Math.max(finalHeight, getBounceHeight());
        }

        drawDynamicIsland(sr, finalWidth, finalHeight);

        if (embeddedClickGui) {
            double x = (sr.getScaledWidth() - finalWidth) / 2.0;
            double y = 8;
            int clipX = (int) x;
            int clipY = (int) y;
            int clipW = (int) finalWidth;
            int clipH = (int) finalHeight;
            ((MenuClickGui) mc.currentScreen).setExternalClip(clipX, clipY, clipW, clipH);
        }
    }

    private void updateTextRotation(float deltaTime) {
        if (musicActive) {
            textAlpha = 1.0f;
            textFadingOut = false;
            return;
        }
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastTextChangeTime >= TEXT_CHANGE_INTERVAL) {
            if (!textFadingOut) {
                textFadingOut = true;
                if (availableTextIndices.isEmpty()) {
                    initializeTextIndices();
                }
                nextTextIndex = availableTextIndices.remove(0);
            }
        }

        float fadeSpeed = 3.0f;

        if (textFadingOut) {
            textAlpha -= fadeSpeed * deltaTime;
            if (textAlpha <= 0.0f) {
                textAlpha = 0.0f;
                currentTextIndex = nextTextIndex;
                textFadingOut = false;
                lastTextChangeTime = currentTime;
            }
        } else {
            if (textAlpha < 1.0f) {
                textAlpha += fadeSpeed * deltaTime;
                if (textAlpha > 1.0f) {
                    textAlpha = 1.0f;
                }
            }
        }
    }

    private void updateTabState(float deltaTime) {
        if (tabPressed && !Keyboard.isKeyDown(Keyboard.KEY_TAB)) {
            tabPressed = false;
            showingPlayerList = false;
            playerListAlpha = 0.0f;
        }

        float PLAYER_LIST_FADE_SPEED = 8.0f;
        if (showingPlayerList) {
            playerListAlpha = Math.min(1.0f, playerListAlpha + PLAYER_LIST_FADE_SPEED * deltaTime);
        } else {
            playerListAlpha = Math.max(0.0f, playerListAlpha - PLAYER_LIST_FADE_SPEED * deltaTime);
        }
    }

    private float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    private float applyEasing(float t, String type) {
        switch (type) {
            case "Linear":
                return t;
            case "EaseInSine":
                return (float) Math.sin((t - 1) * Math.PI / 2) + 1;
            case "EaseOutSine":
                return (float) Math.sin(t * Math.PI / 2);
            case "EaseInOutSine":
                return -(float) Math.cos(Math.PI * t) / 2 + 0.5f;
            case "EaseInQuad":
                return t * t;
            case "EaseOutQuad":
                return 1 - (1 - t) * (1 - t);
            case "EaseInOutQuad":
                return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
            case "EaseInCubic":
                return t * t * t;
            case "EaseOutCubic":
                return 1 - (float) Math.pow(1 - t, 3);
            case "EaseInOutCubic":
                return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
            case "EaseInQuart":
                return t * t * t * t;
            case "EaseOutQuart":
                return 1 - (float) Math.pow(1 - t, 4);
            case "EaseInOutQuart":
                return t < 0.5f ? 8 * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 4) / 2;
            case "EaseInQuint":
                return t * t * t * t * t;
            case "EaseOutQuint":
                return 1 - (float) Math.pow(1 - t, 5);
            case "EaseInOutQuint":
                return t < 0.5f ? 16 * t * t * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 5) / 2;
            case "EaseInExpo":
                return t == 0 ? 0 : (float) Math.pow(2, 10 * t - 10);
            case "EaseOutExpo":
                return t == 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);
            case "EaseInOutExpo":
                return t == 0 ? 0 : t == 1 ? 1 : t < 0.5f ? (float) Math.pow(2, 20 * t - 10) / 2 : (2 - (float) Math.pow(2, -20 * t + 10)) / 2;
            case "EaseInCirc":
                return 1 - (float) Math.sqrt(1 - Math.pow(t, 2));
            case "EaseOutCirc":
                return (float) Math.sqrt(1 - Math.pow(t - 1, 2));
            case "EaseInOutCirc":
                return t < 0.5f ? (1 - (float) Math.sqrt(1 - Math.pow(2 * t, 2))) / 2 : ((float) Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2;
            case "EaseInBack":
                float c1 = 1.70158f;
                float c3 = c1 + 1;
                return c3 * t * t * t - c1 * t * t;
            case "EaseOutBack":
                float c2 = 1.70158f;
                return 1 + c2 * (float) Math.pow(t - 1, 3) + c2 * (float) Math.pow(t - 1, 2);
            case "EaseInOutBack":
                float c4 = 1.70158f * 1.525f;
                return t < 0.5f ? (float) Math.pow(2 * t, 2) * ((c4 + 1) * 2 * t - c4) / 2 : ((float) Math.pow(2 * t - 2, 2) * ((c4 + 1) * (t * 2 - 2) + c4) + 2) / 2;
            case "EaseInElastic":
                if (t == 0 || t == 1) return t;
                return -(float) Math.pow(2, 10 * t - 10) * (float) Math.sin((t * 10 - 10.75) * (2 * Math.PI) / 3);
            case "EaseOutElastic":
                if (t == 0 || t == 1) return t;
                return (float) Math.pow(2, -10 * t) * (float) Math.sin((t * 10 - 0.75) * (2 * Math.PI) / 3) + 1;
            case "EaseInOutElastic":
                if (t == 0 || t == 1) return t;
                return t < 0.5f ? -(float) Math.pow(2, 20 * t - 10) * (float) Math.sin((20 * t - 11.125) * (2 * Math.PI) / 4.5) / 2 : (float) Math.pow(2, -20 * t + 10) * (float) Math.sin((20 * t - 11.125) * (2 * Math.PI) / 4.5) / 2 + 1;
            case "EaseInBounce":
                return 1 - applyEasing(1 - t, "EaseOutBounce");
            case "EaseOutBounce":
                float n1 = 7.5625f;
                float d1 = 2.75f;
                if (t < 1 / d1) {
                    return n1 * t * t;
                } else if (t < 2 / d1) {
                    return n1 * (t -= 1.5f / d1) * t + 0.75f;
                } else if (t < 2.5 / d1) {
                    return n1 * (t -= 2.25f / d1) * t + 0.9375f;
                } else {
                    return n1 * (t -= 2.625f / d1) * t + 0.984375f;
                }
            case "EaseInOutBounce":
                return t < 0.5f ? (1 - applyEasing(1 - 2 * t, "EaseOutBounce")) / 2 : (1 + applyEasing(2 * t - 1, "EaseOutBounce")) / 2;
            case "CustomSpring":
                return applyCustomSpring(t);
            default:
                return easeOutCubic(t);
        }
    }

    private float applyCustomSpring(float t) {
        float tension = customSpringTension.getValue();
        float friction = customSpringFriction.getValue();
        float mass = customMass.getValue();

        springPositionX += animationVelocityX * mass;
        springPositionY += animationVelocityY * mass;

        animationVelocityX -= (springPositionX - targetWidth) * tension / mass;
        animationVelocityY -= (springPositionY - targetHeight) * tension / mass;

        animationVelocityX *= friction;
        animationVelocityY *= friction;

        springPositionX = currentWidth;
        springPositionY = currentHeight;

        return t;
    }

    private void buildDisplayText() {
        if (showNotifications.getValue()) {
            NotificationManager.getInstance().getCurrentNotification();
        }
    }

    private void updateAnimations() {
        long currentTime = System.currentTimeMillis();

        List<Notification> activeNotifications = NotificationManager.getInstance().getActiveNotifications();
        boolean clickGuiOpen = mc.currentScreen instanceof MenuClickGui || mc.currentScreen instanceof DropdownClickGui;
        boolean hasNotifications = !activeNotifications.isEmpty() && showNotifications.getValue() && !clickGuiOpen;

        updateTargetRadius(chestExpanded, hasNotifications);
        if (targetRadius != lastTargetRadius) {
            lastTargetRadius = targetRadius;
        }

        float radiusDiff = targetRadius - currentRadius;
        float speed = animationSpeed.getValue() * 0.01f;

        if (Math.abs(radiusDiff) > 0.1f) {
            currentRadius += radiusDiff * speed;
        } else {
            currentRadius = targetRadius;
        }

        if (chestExpanded) {
            ChestData chestData = ChestData.getInstance();
            int chestSize = chestData.getChestSize();
            int rows = Math.max(3, chestSize / 9);
            targetWidth = 9 * 20 + 12 * 3;
            targetHeight = rows * 20 + 12 * 3;
        }

        if (targetWidth != lastTargetWidth || targetHeight != lastTargetHeight) {
            if (animationMode.getModeString().equals("Custom") && enableAnticipation.getValue()) {
                isInAnticipation = true;
                anticipationStartTime = currentTime;
            }
            animationStartTime = currentTime;
            bounceStartTime = currentTime;
            lastTargetWidth = targetWidth;
            lastTargetHeight = targetHeight;
            isExpanding = targetWidth > currentWidth || targetHeight > currentHeight;
            currentBounceCount = 0;

            if (animationMode.getModeString().equals("Custom")) {
                animationVelocityX = 0;
                animationVelocityY = 0;
                springPositionX = currentWidth;
                springPositionY = currentHeight;
            }
        }

        wasInNoticeState = hasNotifications;

        if (animationMode.getModeString().equals("Normal")) {
            updateNormalAnimations();
        } else if (animationMode.getModeString().equals("Custom")) {
            updateCustomAnimations(currentTime);
        }
    }

    private void updateNormalAnimations() {
        double widthDiff = targetWidth - currentWidth;
        double heightDiff = targetHeight - currentHeight;

        float speed = animationSpeed.getValue() * 0.01f;

        if (Math.abs(widthDiff) > 0.1) {
            currentWidth += widthDiff * speed;
        } else {
            currentWidth = targetWidth;
        }

        if (Math.abs(heightDiff) > 0.1) {
            currentHeight += heightDiff * speed;
        } else {
            currentHeight = targetHeight;
        }

        if (Math.abs(widthDiff) < 0.5) {
            currentWidth = targetWidth;
        }
        if (Math.abs(heightDiff) < 0.5) {
            currentHeight = targetHeight;
        }
    }

    private void updateCustomAnimations(long currentTime) {
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;

        if (isInAnticipation) {
            long anticipationElapsed = currentTime - anticipationStartTime;
            if (anticipationElapsed < anticipationDuration.getValue()) {
                float anticipationProgress = anticipationElapsed / anticipationDuration.getValue();
                float anticipationScale = 1.0f - anticipationAmount.getValue() * applyEasing(anticipationProgress, easingType.getModeString());

                double centerX = (currentWidth + targetWidth) / 2.0;
                double centerY = (currentHeight + targetHeight) / 2.0;

                currentWidth = centerX + (currentWidth - centerX) * anticipationScale;
                currentHeight = centerY + (currentHeight - centerY) * anticipationScale;
                return;
            } else {
                isInAnticipation = false;
                animationStartTime = currentTime;
            }
        }

        long animationElapsed = currentTime - animationStartTime;
        float animationDuration = 500.0f / (animationSpeed.getValue() / 12.0f);
        float progress = Math.min(animationElapsed / animationDuration, 1.0f);

        String easing = easingType.getModeString();
        float easedProgress = applyEasing(progress, easing);

        double startWidth = lastTargetWidth;
        double startHeight = lastTargetHeight;

        if (easing.equals("CustomSpring")) {
            float tension = customSpringTension.getValue();
            float friction = customSpringFriction.getValue();
            float mass = customMass.getValue();
            float stiffness = customStiffness.getValue();

            double widthForce = (targetWidth - currentWidth) * stiffness;
            double heightForce = (targetHeight - currentHeight) * stiffness;

            animationVelocityX += widthForce / mass;
            animationVelocityY += heightForce / mass;

            animationVelocityX *= friction;
            animationVelocityY *= friction;

            currentWidth += animationVelocityX * deltaTime;
            currentHeight += animationVelocityY * deltaTime;

            if (enableOvershoot.getValue() && Math.abs(targetWidth - currentWidth) < 1.0) {
                double overshootForce = (targetWidth - currentWidth) * overshootAmount.getValue();
                animationVelocityX += overshootForce / mass;
            }

            if (enableOvershoot.getValue() && Math.abs(targetHeight - currentHeight) < 1.0) {
                double overshootForce = (targetHeight - currentHeight) * overshootAmount.getValue();
                animationVelocityY += overshootForce / mass;
            }
        } else {
            currentWidth = startWidth + (targetWidth - startWidth) * easedProgress;
            currentHeight = startHeight + (targetHeight - startHeight) * easedProgress;

            if (enableOvershoot.getValue() && progress < 1.0f) {
                float overshoot = overshootAmount.getValue();
                if (easedProgress > 0.8f) {
                    float overshootProgress = (easedProgress - 0.8f) / 0.2f;
                    float overshootEasing = (float) Math.sin(overshootProgress * Math.PI) * overshoot;

                    if (isExpanding) {
                        currentWidth = targetWidth * (1.0f + overshootEasing);
                        currentHeight = targetHeight * (1.0f + overshootEasing);
                    }
                }
            }
        }

        if (progress >= 1.0f && Math.abs(targetWidth - currentWidth) < 0.5 && Math.abs(targetHeight - currentHeight) < 0.5) {
            currentWidth = targetWidth;
            currentHeight = targetHeight;
            animationVelocityX = 0;
            animationVelocityY = 0;
        }
    }

    private void updateTargetRadius(boolean isChestExpanded, boolean hasNotifications) {
        if (isChestExpanded) {
            targetRadius = 12.0f;
        } else if (hasNotifications) {
            targetRadius = 36.0f;
        } else {
            targetRadius = 32.0f;
        }
    }

    private double getBounceWidth() {
        if (animationMode.getModeString().equals("Custom") && !enableBounce.getValue()) {
            return currentWidth;
        }

        long currentTime = System.currentTimeMillis();

        if (targetWidth != lastTargetWidth || targetHeight != lastTargetHeight) {
            return currentWidth;
        }

        List<Notification> activeNotifications = NotificationManager.getInstance().getActiveNotifications();
        boolean clickGuiOpen = mc.currentScreen instanceof MenuClickGui || mc.currentScreen instanceof DropdownClickGui;
        boolean hasNotifications = !activeNotifications.isEmpty() && showNotifications.getValue() && !clickGuiOpen;
        boolean isEnteringNoticeState = hasNotifications && !wasInNoticeState;

        float animationDuration = isExpanding ? bounceDuration.getValue() : bounceDuration.getValue() + 50.0f;
        int maxBounces = (isExpanding && isEnteringNoticeState) ? bounceCount.getValue() : 0;
        float currentBounceDuration = isExpanding ? (currentBounceCount == 0 ? 0.0f : bounceDuration.getValue()) : 0.0f;

        float totalProgress = Math.min((currentTime - animationStartTime) / animationDuration, 1.0f);
        float bounceProgress = Math.min((currentTime - bounceStartTime) / currentBounceDuration, 1.0f);

        if (totalProgress < 1.0f && currentBounceCount < maxBounces) {
            if (bounceProgress >= 1.0f) {
                currentBounceCount++;
                bounceStartTime = currentTime;
                bounceProgress = 0;
            }

            float intensity = animationMode.getModeString().equals("Custom") ? bounceIntensity.getValue() : 0.09f;
            float bounceIntensityValue = currentBounceCount == 0 ? 0.0f : intensity;

            if (bounceProgress < 0.5f) {
                float expandProgress = bounceProgress * 2;
                float overshoot = (float) Math.sin(expandProgress * Math.PI) * bounceIntensityValue;
                return currentWidth * (1.0f + overshoot);
            } else {
                float contractProgress = (bounceProgress - 0.5f) * 2;
                float undershoot = (float) Math.sin(contractProgress * Math.PI) * bounceIntensityValue * 0.5f;
                return currentWidth * (1.0f - undershoot);
            }
        }

        return currentWidth;
    }

    private double getBounceHeight() {
        if (animationMode.getModeString().equals("Custom") && !enableBounce.getValue()) {
            return currentHeight;
        }

        long currentTime = System.currentTimeMillis();

        if (targetWidth != lastTargetWidth || targetHeight != lastTargetHeight) {
            return currentHeight;
        }

        List<Notification> activeNotifications = NotificationManager.getInstance().getActiveNotifications();
        boolean clickGuiOpen = mc.currentScreen instanceof MenuClickGui || mc.currentScreen instanceof DropdownClickGui;
        boolean hasNotifications = !activeNotifications.isEmpty() && showNotifications.getValue() && !clickGuiOpen;
        boolean isEnteringNoticeState = hasNotifications && !wasInNoticeState;

        float animationDuration = isExpanding ? bounceDuration.getValue() : bounceDuration.getValue() + 50.0f;
        int maxBounces = (isExpanding && isEnteringNoticeState) ? bounceCount.getValue() : 0;
        float currentBounceDuration = isExpanding ? (currentBounceCount == 0 ? 0.0f : bounceDuration.getValue()) : 0.0f;

        float totalProgress = Math.min((currentTime - animationStartTime) / animationDuration, 1.0f);
        float bounceProgress = Math.min((currentTime - bounceStartTime) / currentBounceDuration, 1.0f);

        if (totalProgress < 1.0f && currentBounceCount < maxBounces) {
            if (bounceProgress >= 1.0f) {
                currentBounceCount++;
                bounceStartTime = currentTime;
                bounceProgress = 0;
            }

            float intensity = animationMode.getModeString().equals("Custom") ? bounceIntensity.getValue() : 0.09f;
            float bounceIntensityValue = currentBounceCount == 0 ? 0.0f : intensity;

            if (bounceProgress < 0.5f) {
                float expandProgress = bounceProgress * 2;
                float overshoot = (float) Math.sin(expandProgress * Math.PI) * bounceIntensityValue;
                return currentHeight * (1.0f + overshoot);
            } else {
                float contractProgress = (bounceProgress - 0.5f) * 2;
                float undershoot = (float) Math.sin(contractProgress * Math.PI) * bounceIntensityValue * 0.5f;
                return currentHeight * (1.0f - undershoot);
            }
        }

        return currentHeight;
    }

    private double getBreathingWidth() {
        if (animationMode.getModeString().equals("Custom") && !enableBreathing.getValue()) {
            return currentWidth;
        }

        List<Notification> activeNotifications = NotificationManager.getInstance().getActiveNotifications();
        boolean clickGuiOpen = mc.currentScreen instanceof MenuClickGui || mc.currentScreen instanceof DropdownClickGui;
        boolean hasNotifications = !activeNotifications.isEmpty() && showNotifications.getValue() && !clickGuiOpen;

        if (!hasNotifications && !chestExpanded && !commandInterface.isActive()) {
            long currentTime = System.currentTimeMillis();
            float intensity = animationMode.getModeString().equals("Custom") ? breathingIntensity.getValue() : 0.03f;
            float speed = animationMode.getModeString().equals("Custom") ? breathingSpeed.getValue() : 3000.0f;
            float breathingCycle = (currentTime - breathingStartTime) / speed;
            float breathingWave = (float) (Math.sin(breathingCycle * Math.PI * 2) * 0.5 + 0.5);
            breathingWave = breathingWave * breathingWave * (3.0f - 2.0f * breathingWave);
            float intensityMul = clickGuiOpen ? 0.35f : 1.0f;
            float breathingScale = 1.0f + breathingWave * intensity * intensityMul;
            return currentWidth * breathingScale;
        } else if (commandInterface.isActive() && commandInterface.isShowingKeyboard()) {
            long currentTime = System.currentTimeMillis();
            float intensity = animationMode.getModeString().equals("Custom") ? breathingIntensity.getValue() * 0.3f : 0.01f;
            float speed = animationMode.getModeString().equals("Custom") ? breathingSpeed.getValue() : 3000.0f;
            float breathingCycle = (currentTime - breathingStartTime) / speed;
            float breathingWave = (float) (Math.sin(breathingCycle * Math.PI * 2) * 0.5 + 0.5);
            breathingWave = breathingWave * breathingWave * (3.0f - 2.0f * breathingWave);
            float intensityMul = clickGuiOpen ? 0.35f : 1.0f;
            float breathingScale = 1.0f + breathingWave * intensity * intensityMul;
            return currentWidth * breathingScale;
        }
        return currentWidth;
    }

    private double getBreathingHeight() {
        if (animationMode.getModeString().equals("Custom") && !enableBreathing.getValue()) {
            return currentHeight;
        }

        List<Notification> activeNotifications = NotificationManager.getInstance().getActiveNotifications();
        boolean clickGuiOpen = mc.currentScreen instanceof MenuClickGui || mc.currentScreen instanceof DropdownClickGui;
        boolean hasNotifications = !activeNotifications.isEmpty() && showNotifications.getValue() && !clickGuiOpen;

        if (!hasNotifications && !chestExpanded && !commandInterface.isActive()) {
            long currentTime = System.currentTimeMillis();
            float intensity = animationMode.getModeString().equals("Custom") ? breathingIntensity.getValue() : 0.03f;
            float speed = animationMode.getModeString().equals("Custom") ? breathingSpeed.getValue() : 3000.0f;
            float breathingCycle = (currentTime - breathingStartTime) / speed;
            float breathingWave = (float) (Math.sin(breathingCycle * Math.PI * 2) * 0.5 + 0.5);
            breathingWave = breathingWave * breathingWave * (3.0f - 2.0f * breathingWave);
            float intensityMul = clickGuiOpen ? 0.35f : 1.0f;
            float breathingScale = 1.0f + breathingWave * intensity * intensityMul;
            return currentHeight * breathingScale;
        } else if (commandInterface.isActive() && commandInterface.isShowingKeyboard()) {
            long currentTime = System.currentTimeMillis();
            float intensity = animationMode.getModeString().equals("Custom") ? breathingIntensity.getValue() * 0.3f : 0.01f;
            float speed = animationMode.getModeString().equals("Custom") ? breathingSpeed.getValue() : 3000.0f;
            float breathingCycle = (currentTime - breathingStartTime) / speed;
            float breathingWave = (float) (Math.sin(breathingCycle * Math.PI * 2) * 0.5 + 0.5);
            breathingWave = breathingWave * breathingWave * (3.0f - 2.0f * breathingWave);
            float intensityMul = clickGuiOpen ? 0.35f : 1.0f;
            float breathingScale = 1.0f + breathingWave * intensity * intensityMul;
            return currentHeight * breathingScale;
        }
        return currentHeight;
    }

    private void drawDynamicIsland(ScaledResolution sr, double width, double height) {

        double x = (sr.getScaledWidth() - width) / 2.0;
        double y;
        y = 8;

        float radius = currentRadius;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        boolean embeddedClickGui = mc.currentScreen instanceof MenuClickGui && ((MenuClickGui) mc.currentScreen).isEmbeddedInDynamicIsland();

        float finalRadius = radius;
        PostProcessing.drawBlur((float) x, (float) y, (float) (x + width), (float) (y + height), () -> () -> epilogue.util.render.RenderUtil.drawRoundedRect((float) x, (float) y, (float) width, (float) height, finalRadius, -1));

        epilogue.util.render.RenderUtil.drawRoundedRect((float) x, (float) y, (float) width, (float) height, radius, new Color(0, 0, 0, bgAlpha.getValue()));

        List<Notification> activeNotifications = NotificationManager.getInstance().getActiveNotifications();
        boolean clickGuiOpen = mc.currentScreen instanceof MenuClickGui || mc.currentScreen instanceof DropdownClickGui;
        boolean hasNotifications = !activeNotifications.isEmpty() && showNotifications.getValue() && !clickGuiOpen;

        if (chestExpanded) {
            radius = 12.0f;
        } else if (hasNotifications) {
            radius = 36.0f;
        } else {
            radius = 32.0f;
        }

        Framebuffer bloomBuffer = PostProcessing.beginBloom();
        if (bloomBuffer != null) {
            epilogue.util.render.RenderUtil.drawRoundedRect((float) x, (float) y, (float) width, (float) height, radius, epilogue.module.modules.render.PostProcessing.getBloomColor());
            mc.getFramebuffer().bindFramebuffer(false);
        }

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        epilogue.util.render.RenderUtil.drawRoundedRect((float) x, (float) y, (float) width, (float) height, radius, new Color(0, 0, 0, bgAlpha.getValue()));

        PostProcessing.endBloom(bloomBuffer);

        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        double contentWidth = currentWidth;
        double contentHeight = currentHeight;
        double contentX = (sr.getScaledWidth() - contentWidth) / 2.0;

        if (showingCommandResult && System.currentTimeMillis() - commandResultStartTime > COMMAND_RESULT_DURATION) {
            showingCommandResult = false;
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scaleFactor = sr.getScaleFactor();
        int scissorX = (int) (x * scaleFactor);
        int scissorY = (int) (mc.displayHeight - (y + height) * scaleFactor);
        int scissorWidth = (int) (width * scaleFactor);
        int scissorHeight = (int) (height * scaleFactor);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);

        if (chestExpanded) {
            drawChestInterface((float)contentX, (float)y, (float)contentWidth, (float)contentHeight);
        } else
            if (commandInterface.isActive()) {
            commandInterface.render((float)contentX, (float)y, (float)contentWidth, (float)contentHeight);
        } else if (showingPlayerList) {
            drawPlayerList((float)contentX, (float)y, (float)contentWidth, (float)contentHeight);
        } else if (showingCommandResult) {
            drawCommandResult((float)contentX, (float)y);
        } else if (hasNotifications) {
            NotificationRenderer.drawMultipleNotifications(activeNotifications, (float)contentX, (float)y, (float)contentWidth, (float)contentHeight, 0.8f);
        } else {
            float slideX = (float)contentX - mainInterfaceSlideOffset;
            float slideAlpha = mainInterfaceSlideOffset > 0 ? 1.0f - (mainInterfaceSlideOffset / (float)contentWidth) : 1.0f;
            
            GlStateManager.pushMatrix();
            GlStateManager.color(1.0f, 1.0f, 1.0f, slideAlpha);
            drawMainInterface(slideX, (float)y, (float)contentWidth);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private double calculateNotificationContentWidth(List<Notification> notifications) {
        double maxWidth = 0;
        double padding = 4;
        float scale = 0.8f;
        
        for (Notification notification : notifications) {
            double contentWidth;
            
            switch (notification.getType()) {
                case SCAFFOLDING:
                    contentWidth = calculateScaffoldingWidth(padding);
                    break;
                case BED_NUKER:
                    contentWidth = calculateBedNukerWidth(padding);
                    break;
                case MODULE_ENABLED:
                case MODULE_DISABLED:
                    contentWidth = calculateModuleNotificationWidth(notification, padding);
                    break;
                default:
                    contentWidth = calculateRegularNotificationWidth(notification, padding);
                    break;
            }
            
            maxWidth = Math.max(maxWidth, contentWidth * scale);
        }
        
        return maxWidth;
    }
    
    private double calculateScaffoldingWidth(double padding) {
        FontTransformer transformer = FontTransformer.getInstance();
        Font otherFont = transformer.getFont("OpenSansSemiBold", 35);
        
        String title = "Scaffold Toggled";
        double titleWidth = CustomFontRenderer.getStringWidth(title, otherFont);
        
        ScaffoldData scaffoldData = ScaffoldData.getInstance();
        String status = scaffoldData.getBlocksLeft() + " blocks left · " + 
                       String.format("%.2f", scaffoldData.getBlocksPerSecond()) + " block/s";
        double statusWidth = CustomFontRenderer.getStringWidth(status, otherFont);
        
        double iconWidth = 12;
        double iconPadding = 18;
        
        double maxTextWidth = Math.max(titleWidth, statusWidth);
        return padding + iconWidth + iconPadding + maxTextWidth;
    }
    
    private double calculateBedNukerWidth(double padding) {
        FontTransformer transformer = FontTransformer.getInstance();
        Font otherFont = transformer.getFont("OpenSansSemiBold", 35);
        
        epilogue.module.modules.render.dynamicisland.notification.BedNukerData bedNukerData = epilogue.module.modules.render.dynamicisland.notification.BedNukerData.getInstance();
        
        String breakingText = "Breaking " + bedNukerData.getTargetBlockName();
        double breakingWidth = CustomFontRenderer.getStringWidth(breakingText, otherFont);
        
        String progressText = "Break Progress: 100%";
        double progressWidth = CustomFontRenderer.getStringWidth(progressText, otherFont);
        
        double iconWidth = 12;
        double iconPadding = 18;
        
        double maxTextWidth = Math.max(breakingWidth, progressWidth);
        return padding + iconWidth + iconPadding + maxTextWidth;
    }
    
    private double calculateModuleNotificationWidth(Notification notification, double padding) {
        FontTransformer transformer = FontTransformer.getInstance();
        Font otherFont = transformer.getFont("OpenSansSemiBold", 37);
        
        String title = "ModuleToggled";
        double titleWidth = CustomFontRenderer.getStringWidth(title, otherFont);
        
        String moduleName = notification.getTitle();
        String hasBeenText = " Has Been ";
        String statusText = notification.getType() == Notification.NotificationType.MODULE_ENABLED ? "Enabled" : "Disabled";
        String exclamation = "!";
        
        double statusLineWidth = CustomFontRenderer.getStringWidth(moduleName + hasBeenText + statusText + exclamation, otherFont);
        
        double switchWidth = 32;
        double switchPadding = 8;
        
        double maxTextWidth = Math.max(titleWidth, statusLineWidth);
        return padding + switchWidth + switchPadding + maxTextWidth;
    }
    
    private double calculateRegularNotificationWidth(Notification notification, double padding) {
        FontTransformer transformer = FontTransformer.getInstance();
        Font otherFont = transformer.getFont("OpenSansSemiBold", 35);
        
        String typeText = getNotificationTypeDisplayName(notification.getType());
        double typeWidth = CustomFontRenderer.getStringWidth(typeText, otherFont);
        
        String displayText = notification.getTitle();
        if (!notification.getMessage().isEmpty()) {
            displayText += " " + notification.getMessage();
        }
        double contentWidth = CustomFontRenderer.getStringWidth(displayText, otherFont);
        
        double maxTextWidth = Math.max(typeWidth, contentWidth);
        return padding + maxTextWidth;
    }
    
    private String getNotificationTypeDisplayName(Notification.NotificationType type) {
        switch (type) {
            case ERROR:
                return "Error";
            case WARNING:
                return "Warning";
            case INFO:
                return "Info";
            case SCAFFOLDING:
                return "Scaffolding";
            case BED_NUKER:
                return "BedNuker";
            default:
                return "Notification";
        }
    }
    
    private double calculatePlayerListWidth(Collection<NetworkPlayerInfo> playerInfos) {
        FontTransformer transformer = FontTransformer.getInstance();
        Font otherFont = transformer.getFont("OpenSansSemiBold", 35);
        
        double maxWidth = 200;
        for (NetworkPlayerInfo playerInfo : playerInfos) {
            String playerName = getPlayerName(playerInfo);
            String pingText = playerInfo.getResponseTime() + "ms";
            String cleanName = net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(playerName);
            String fullText = cleanName + " " + pingText;
            double textWidth = CustomFontRenderer.getStringWidth(fullText, otherFont);
            maxWidth = Math.max(maxWidth, textWidth + 40);
        }
        
        String serverInfo = getServerIP();
        String clientInfo = "Epilogue";
        String pingInfo = "Ping: " + getPing() + "ms";
        
        Nick nick = (Nick) Epilogue.moduleManager.modules.get(Nick.class);
        if (nick != null && nick.isEnabled()) {
            serverInfo = nick.replaceNick(serverInfo);
        }
        
        maxWidth = Math.max(maxWidth, CustomFontRenderer.getStringWidth(serverInfo, otherFont) + 40);
        maxWidth = Math.max(maxWidth, CustomFontRenderer.getStringWidth(clientInfo, otherFont) + 40);
        maxWidth = Math.max(maxWidth, CustomFontRenderer.getStringWidth(pingInfo, otherFont) + 40);
        
        return Math.min(maxWidth, 400);
    }
    
    private double calculatePlayerListHeight(Collection<NetworkPlayerInfo> playerInfos) {
        FontTransformer transformer = FontTransformer.getInstance();
        Font otherFont = transformer.getFont("OpenSansSemiBold", 35);
        int fontHeight = CustomFontRenderer.getFontHeight(otherFont);
        
        int playerCount = Math.min(playerInfos.size(), 16);
        double playerListHeight = playerCount * (fontHeight + 6);
        double headerHeight = fontHeight + 8;
        double footerHeight = fontHeight * 3 + 16;
        return Math.max(playerListHeight + headerHeight + footerHeight, 120);
    }
    
    private void drawPlayerList(float x, float y, float width, float height) {
        FontTransformer transformer = FontTransformer.getInstance();
        Font otherFont = transformer.getFont("OpenSansSemiBold", 35);
        int fontHeight = CustomFontRenderer.getFontHeight(otherFont);
        
        Collection<NetworkPlayerInfo> playerInfos = mc.getNetHandler().getPlayerInfoMap();
        float padding = 12;
        float currentY = y + padding;
        
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        
        String headerText = "Players (" + playerInfos.size() + ")";
        float headerX = x + (width - CustomFontRenderer.getStringWidth(headerText, otherFont)) / 2;
        CustomFontRenderer.drawStringWithShadow(headerText, headerX, currentY, 0x55AAFF, otherFont);
        currentY += fontHeight + 8;
        
        epilogue.util.render.RenderUtil.drawRect(x + padding, currentY - 2, width - padding * 2, 1, 0x33FFFFFF);
        currentY += 6;
        
        int displayCount = 0;
        int maxPlayers = Math.min(16, playerInfos.size());
        
        for (NetworkPlayerInfo playerInfo : playerInfos) {
            if (displayCount >= maxPlayers) break;
            
            String playerName = getPlayerName(playerInfo);
            String pingText = playerInfo.getResponseTime() + "ms";
            
            Color pingColor = getPingColor(playerInfo.getResponseTime());
            
            float playerY = currentY + displayCount * (fontHeight + 6);
            
            float alpha = Math.min(1.0f, playerListAlpha * (1.0f + displayCount * 0.1f));
            int nameColor = (int)(255 * alpha) << 24 | 0xFFFFFF;
            int pingColorWithAlpha = (int)(255 * alpha) << 24 | (pingColor.getRGB() & 0xFFFFFF);
            
            drawPlayerNameWithColors(playerName, x + padding, playerY, nameColor, alpha, otherFont);
            
            float pingX = x + width - padding - CustomFontRenderer.getStringWidth(pingText, otherFont);
            CustomFontRenderer.drawStringWithShadow(pingText, pingX, playerY, pingColorWithAlpha, otherFont);
            
            displayCount++;
        }
        
        float footerY = y + height - fontHeight * 3 - padding;
        epilogue.util.render.RenderUtil.drawRect(x + padding, footerY - 4, width - padding * 2, 1, 0x33FFFFFF);
        
        drawPlayerListFooter(x, footerY, width, otherFont);
        
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    
    private void drawPlayerNameWithColors(String name, float x, float y, int defaultColor, float alpha, Font font) {
        if (name.contains("§")) {
            float currentX = x;
            String[] parts = name.split("§");
            
            CustomFontRenderer.drawStringWithShadow(parts[0], currentX, y, defaultColor, font);
            currentX += CustomFontRenderer.getStringWidth(parts[0], font);
            
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    char colorCode = parts[i].charAt(0);
                    String text = parts[i].substring(1);
                    
                    int color = getMinecraftColor(colorCode, alpha);
                    CustomFontRenderer.drawStringWithShadow(text, currentX, y, color, font);
                    currentX += CustomFontRenderer.getStringWidth(text, font);
                }
            }
        } else {
            CustomFontRenderer.drawStringWithShadow(name, x, y, defaultColor, font);
        }
    }
    
    private int getMinecraftColor(char colorCode, float alpha) {
        int baseColor;
        switch (colorCode) {
            case '0': baseColor = 0x000000; break;
            case '1': baseColor = 0x0000AA; break;
            case '2': baseColor = 0x00AA00; break;
            case '3': baseColor = 0x00AAAA; break;
            case '4': baseColor = 0xAA0000; break;
            case '5': baseColor = 0xAA00AA; break;
            case '6': baseColor = 0xFFAA00; break;
            case '7': baseColor = 0xAAAAAA; break;
            case '8': baseColor = 0x555555; break;
            case '9': baseColor = 0x5555FF; break;
            case 'a': baseColor = 0x55FF55; break;
            case 'b': baseColor = 0x55FFFF; break;
            case 'c': baseColor = 0xFF5555; break;
            case 'd': baseColor = 0xFF55FF; break;
            case 'e': baseColor = 0xFFFF55; break;
            default: baseColor = 0xFFFFFF; break;
        }
        return (int)(255 * alpha) << 24 | baseColor;
    }
    
    private void drawPlayerListFooter(float x, float y, float width, Font font) {
        int fontHeight = CustomFontRenderer.getFontHeight(font);
        float padding = 12;
        float currentY = y;
        
        String serverInfo = getServerIP();
        String clientName = Epilogue.clientName;
        String clientVersion = Epilogue.clientVersion;
        String pingInfo = "Ping: " + getPing() + "ms";
        
        Nick nick = (Nick) Epilogue.moduleManager.modules.get(Nick.class);
        if (nick != null && nick.isEnabled()) {
            serverInfo = nick.replaceNick(serverInfo);
        }
        
        CustomFontRenderer.drawStringWithShadow(serverInfo, x + padding, currentY, 0xAAAAAA, font);
        currentY += fontHeight + 4;
        
        CustomFontRenderer.drawStringWithShadow(clientName, x + padding, currentY, 0x55AAFF, font);
        float versionX = x + width - padding - CustomFontRenderer.getStringWidth(clientVersion, font);
        CustomFontRenderer.drawStringWithShadow(clientVersion, versionX, currentY, 0x55AAFF, font);
        currentY += fontHeight + 4;
        
        CustomFontRenderer.drawStringWithShadow(pingInfo, x + padding, currentY, getPingColor().getRGB(), font);
    }
    
    private String getPlayerName(NetworkPlayerInfo playerInfo) {
        String name = playerInfo.getDisplayName() != null ? 
            playerInfo.getDisplayName().getFormattedText() : 
            ScorePlayerTeam.formatPlayerName(playerInfo.getPlayerTeam(), playerInfo.getGameProfile().getName());
        
        Nick nick = (Nick) Epilogue.moduleManager.modules.get(Nick.class);
        if (nick != null && nick.isEnabled()) {
            name = nick.replaceNick(name);
        }
        
        return name;
    }
    
    private Color getPingColor(int ping) {
        if (ping < 50) {
            return new Color(85, 255, 85);
        } else if (ping < 100) {
            return new Color(255, 255, 85);
        } else if (ping < 200) {
            return new Color(255, 170, 85);
        } else {
            return new Color(255, 85, 85);
        }
    }

    private String getServerIP() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastServerInfoUpdate > SERVER_INFO_UPDATE_INTERVAL) {
            updateCachedServerInfo();
            lastServerInfoUpdate = currentTime;
        }
        return cachedServerIP;
    }

    private void drawChestInterface(float x, float y, float width, float height) {
        FontTransformer transformer = FontTransformer.getInstance();
        Font otherFont = transformer.getFont("OpenSansSemiBold", 35);

        ChestData chestData = ChestData.getInstance();
        if (!chestData.isChestOpen()) return;

        float padding = 8;
        float slotSize = 18;
        int chestSize = chestData.getChestSize();

        if (chestData.isChestEmpty()) {
            String emptyText = "Empty Chest";
            float textX = x + (width - CustomFontRenderer.getStringWidth(emptyText, otherFont)) / 2.0f;
            float textY = y + (height - CustomFontRenderer.getFontHeight(otherFont)) / 2.0f;
            CustomFontRenderer.drawStringWithShadow(emptyText, textX, textY, 0xAAAAAA, otherFont);
            return;
        }

        float startX = x + padding;
        float startY = y + padding;

        GlStateManager.enableRescaleNormal();
        RenderHelper.enableGUIStandardItemLighting();

        for (int slot = 0; slot < chestSize; slot++) {
            int row = slot / 9;
            int col = slot % 9;

            float slotX = startX + col * 20;
            float slotY = startY + row * 20;

            ItemStack itemStack = chestData.getItemInSlot(slot);
            if (itemStack != null) {
                mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, (int)slotX + 1, (int)slotY + 1);
                mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, itemStack, (int)slotX + 1, (int)slotY + 1, null);
            }

            ChestData.ClickAnimation animation = chestData.getClickAnimations().get(slot);
            if (animation != null) {
                float alpha = animation.getAlpha();
                if (alpha > 0) {
                    float centerX = slotX + slotSize / 2;
                    float centerY = slotY + slotSize / 2;
                    float radius = slotSize / 2;

                    GlStateManager.pushMatrix();
                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();

                    epilogue.util.render.RenderUtil.drawRoundedRect(centerX - radius, centerY - radius, radius * 2, radius * 2, radius, new Color(255, 255, 255, (int)(alpha * 255)));

                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                    GlStateManager.popMatrix();
                }
            }
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();

        String stealingText = "Stealing Chest...";
        float textX = x + (width - CustomFontRenderer.getStringWidth(stealingText, otherFont)) / 2.0f;
        float textY = y + height - CustomFontRenderer.getFontHeight(otherFont) - 4;
        CustomFontRenderer.drawStringWithShadow(stealingText, textX, textY, 0x888888, otherFont);
    }

    private void updateMusicState() {
        musicActive = CloudMusic.player != null && CloudMusic.currentlyPlaying != null;

        if (!musicActive) {
            lastMusicSongId = -1;
            cachedMusicTitle = "";
            cachedCoverLoc = null;
            lyricCache.reset();
            return;
        }

        long songId;
        try {
            songId = CloudMusic.currentlyPlaying.getId();
        } catch (Throwable t) {
            songId = -1;
        }

        if (songId != lastMusicSongId) {
            lastMusicSongId = songId;
            try {
                cachedMusicTitle = CloudMusic.currentlyPlaying.getName();
            } catch (Throwable t) {
                cachedMusicTitle = "";
            }
            try {
                cachedCoverLoc = CoverTextureCache.getOrRequest(CloudMusic.currentlyPlaying.getCoverUrl(64), 64);
            } catch (Throwable t) {
                cachedCoverLoc = null;
            }
        } else {
            try {
                if (cachedCoverLoc == null && CloudMusic.currentlyPlaying != null) {
                    cachedCoverLoc = CoverTextureCache.getOrRequest(CloudMusic.currentlyPlaying.getCoverUrl(64), 64);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private String getCurrentLyricText() {
        return lyricCache.getText();
    }

    private void updateCachedServerInfo() {
        if (mc.getCurrentServerData() != null) {
            if (GetIPUtil.containsPattern("us-test")) {
                cachedServerIP = "hypixel.net";
            } else if (GetIPUtil.containsPattern(".de")) {
                cachedServerIP = "NyaProxy";
            } else if(GetIPUtil.containsPattern(".us.")) {
                cachedServerIP = "hypixel.net";
            } else {
                cachedServerIP = mc.getCurrentServerData().serverIP;
            }
            cachedPing = (int) mc.getCurrentServerData().pingToServer;
        } else {
            cachedServerIP = "Singleplayer";
            cachedPing = 0;
        }
    }

    private void drawMainInterface(float x, float y, float width) {
        String serverIP = getServerIP();
        int ping = getPing();
        int fps = net.minecraft.client.Minecraft.getDebugFPS();

        float scale = 0.7f;
        float padding = 8;

        FontTransformer transformer = FontTransformer.getInstance();
        Font mainFont = transformer.getFont("MicrosoftYaHei", 60);
        Font islandTextFont = transformer.getFont("MicrosoftYaHei", 46);
        Font contentFont = transformer.getFont("MicrosoftYaHei", 40);

        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("M/d");
        java.text.SimpleDateFormat weekFormat = new java.text.SimpleDateFormat("EEEE", java.util.Locale.ENGLISH);
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
        java.util.Date now = new java.util.Date();

        String dateStr = dateFormat.format(now);
        String weekStr = weekFormat.format(now);
        String timeStr = timeFormat.format(now);

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);

        float scaledX = (x + padding) / scale;
        float scaledY = (y + padding) / scale;

        int mainFontHeight = CustomFontRenderer.getFontHeight(mainFont);
        float lineSpacing = 4;

        Font WaterMarkFont = FontTransformer.getInstance().getFont("SuperJoyful", 60);

        if (musicActive) {
            Font musicTitleFont = transformer.getFont("MicrosoftYaHei", 51);
            Font lyricFont = transformer.getFont("MicrosoftYaHei", 44);

            float coverSize = 26f;
            float coverRadius = 6f;
            float gap = 8f;

            float coverX = scaledX;
            float coverY = scaledY - 4f;

            if (cachedCoverLoc != null) {
                net.minecraft.client.renderer.texture.ITextureObject tex = Minecraft.getMinecraft().getTextureManager().getTexture(cachedCoverLoc);
                if (tex != null) {
                    GlStateManager.pushMatrix();
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    Minecraft.getMinecraft().getTextureManager().bindTexture(cachedCoverLoc);
                    net.minecraft.client.gui.Gui.drawScaledCustomSizeModalRect((int) coverX, (int) coverY, 0f, 0f, 64, 64, (int) coverSize, (int) coverSize, 64f, 64f);
                    GlStateManager.popMatrix();
                    GlStateManager.disableBlend();
                } else {
                    epilogue.util.render.RenderUtil.drawRoundedRect(coverX, coverY, coverSize, coverSize, coverRadius, new Color(40, 40, 40, 255));
                }
            } else {
                epilogue.util.render.RenderUtil.drawRoundedRect(coverX, coverY, coverSize, coverSize, coverRadius, new Color(40, 40, 40, 255));
            }

            float titleX = coverX + coverSize + gap;
            String title = cachedMusicTitle == null ? "" : cachedMusicTitle;
            CustomFontRenderer.drawStringWithShadow(title, titleX, scaledY + 1f, 0xFFFFFF, musicTitleFont);

            float textX = titleX + CustomFontRenderer.getStringWidth(title, musicTitleFont) + 10f;
            String displayText = getCurrentLyricText();
            int textColor = (int) (255 * textAlpha) << 24 | 0xFFFFFF;
            CustomFontRenderer.drawStringWithShadow(displayText, textX, scaledY + 3f, textColor, lyricFont);
        } else {
            drawNightSkyTitle(scaledX, scaledY, WaterMarkFont);
            float textX = scaledX + CustomFontRenderer.getStringWidth("Epilogue", WaterMarkFont) + 10;
            String displayText = islandText[currentTextIndex];

            int textColor = (int) (255 * textAlpha) << 24 | 0xFFFFFF;
            CustomFontRenderer.drawStringWithShadow(displayText, textX, scaledY, textColor, islandTextFont);
        }

        scaledY += mainFontHeight + lineSpacing;

        float availableWidth = (float) ((width / scale) - padding * 3.5);

        String[] line2Parts = {dateStr, weekStr, timeStr, ping + "ms to " + serverIP, fps + " FPS"};
        float[] partWidths = new float[line2Parts.length];
        float totalBaseWidth = 0;
        for (int i = 0; i < line2Parts.length; i++) {
            partWidths[i] = CustomFontRenderer.getStringWidth(line2Parts[i], contentFont);
            totalBaseWidth += partWidths[i];
        }

        float minSpacing = textSpacing;
        float totalSpacing = availableWidth - totalBaseWidth;
        float dynamicSpacing = Math.max(minSpacing, totalSpacing / (line2Parts.length - 1));

        float currentX = scaledX;
        for (int i = 0; i < line2Parts.length; i++) {
            int color = 0xFFFFFF;
            if (i == 3) {
                color = getPingColor().getRGB();
            }
            CustomFontRenderer.drawStringWithShadow(line2Parts[i], currentX, scaledY, color, contentFont);
            if (i < line2Parts.length - 1) {
                currentX += partWidths[i] + dynamicSpacing;
            }
        }

        GlStateManager.popMatrix();
    }

    private void drawNightSkyTitle(float x, float y, Font cheriFont) {
        CustomFontRenderer.drawStringWithShadow("Epilogue", x, y, 0xFFFFFF, cheriFont);
    }

    private void drawGaussianBlurredRect(float x, float y, float width, float height, float cornerRadius, float alpha) {
        int blurLayers = 3;
        float blurSpread = 2.0f;
        
        for (int layer = 0; layer < blurLayers; layer++) {
            float layerProgress = (float)layer / (float)blurLayers;
            float layerAlpha = alpha * (1.0f - layerProgress * 0.3f);
            float layerExpand = layerProgress * blurSpread;
            
            float layerX = x - layerExpand;
            float layerY = y - layerExpand;
            float layerWidth = width + layerExpand * 2.0f;
            float layerHeight = height + layerExpand * 2.0f;
            
            epilogue.util.render.RenderUtil.drawRoundedRect(
                layerX, layerY, layerWidth, layerHeight,
                cornerRadius,
                new Color(0, 0, 0, (int)(layerAlpha * 255))
            );
        }
    }

    private double calculateCommandResultSize() {
        if (!showingCommandResult) return 200;
        
        FontTransformer transformer = FontTransformer.getInstance();
        Font otherFont = transformer.getFont("OpenSansSemiBold", 35);
        
        double maxWidth = 200;

        String typeText = commandResultType + " " + commandResultTitle;
        double titleWidth = CustomFontRenderer.getStringWidth(typeText, otherFont);
        maxWidth = Math.max(maxWidth, titleWidth);

        String[] lines = commandResultContent.split("\n");
        for (String line : lines) {
            double lineWidth = CustomFontRenderer.getStringWidth(line, otherFont);
            maxWidth = Math.max(maxWidth, lineWidth);
        }
        
        return Math.min(maxWidth + 16, 600);
    }
    
    private double calculateCommandResultHeight() {
        if (!showingCommandResult) return 60;
        
        FontTransformer transformer = FontTransformer.getInstance();
        Font otherFont = transformer.getFont("OpenSansSemiBold", 35);
        int fontHeight = CustomFontRenderer.getFontHeight(otherFont);
        
        double height = 16;

        height += fontHeight + 4;

        String[] lines = commandResultContent.split("\n");
        for (int i = 0; i < lines.length; i++) {
            height += fontHeight;
            if (i < lines.length - 1) {
                height += 2;
            }
        }
        
        return Math.min(height, 400);
    }
    
    private float calculateNotificationItemHeight(Notification.NotificationType type) {
        switch (type) {
            case MODULE_ENABLED:
            case MODULE_DISABLED:
            case INFO:
            case ERROR:
            case WARNING:
            case COMMAND_RESULT:
                return 30f;
            default:
                return 40f;
        }
    }
    
    private void drawCommandResult(float x, float y) {
        FontTransformer transformer = FontTransformer.getInstance();
        Font otherFont = transformer.getFont("OpenSansSemiBold", 35);
        int fontHeight = CustomFontRenderer.getFontHeight(otherFont);
        
        float padding = 8;
        float currentY = y + padding;

        Color typeColor;
        switch (commandResultType.toLowerCase()) {
            case "info":
                typeColor = new Color(100, 200, 255);
                break;
            case "warning":
                typeColor = new Color(255, 200, 100);
                break;
            case "error":
                typeColor = new Color(255, 100, 100);
                break;
            default:
                typeColor = new Color(255, 255, 255);
                break;
        }

        String typeText = commandResultType + " " + commandResultTitle;
        CustomFontRenderer.drawStringWithShadow(typeText, x + padding, currentY, typeColor.getRGB(), otherFont);
        currentY += fontHeight + 4;

        String[] lines = commandResultContent.split("\n");
        for (String line : lines) {
            CustomFontRenderer.drawStringWithShadow(line, x + padding, currentY, 0xFFFFFF, otherFont);
            currentY += fontHeight + 2;
        }
    }

    private int getPing() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastServerInfoUpdate > SERVER_INFO_UPDATE_INTERVAL) {
            updateCachedServerInfo();
            lastServerInfoUpdate = currentTime;
        }
        return cachedPing;
    }
    private Color getPingColor() {
        if (mc.getCurrentServerData() != null) {
            int ping = (int)mc.getCurrentServerData().pingToServer;
            if (ping < 130) return new Color(76, 175, 80);
            if (ping < 270) return new Color(255, 193, 7);
            if (ping < 500) return new Color(255, 152, 0);
            return new Color(244, 67, 54);
        }
        return new Color(160, 160, 160);
    }
}