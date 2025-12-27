package epilogue;

import epilogue.powershell.Handler;
import epilogue.powershell.shells.*;
import epilogue.event.EventManager;
import epilogue.management.*;
import epilogue.module.modules.combat.*;
import epilogue.module.modules.misc.*;
import epilogue.module.modules.movement.*;
import epilogue.module.modules.player.*;
import epilogue.module.modules.render.*;
import epilogue.module.modules.render.dynamicisland.*;
import epilogue.module.ModuleManager;
import epilogue.util.MathUtil;
import epilogue.value.Value;
import epilogue.value.ValueHandler;

import java.lang.reflect.Field;

public class Epilogue {
    public static String clientName = "Epilogue ";
    public static String clientVersion = "v1";

    public static RotationManager rotationManager;
    public static FloatManager floatManager;
    public static BlinkManager blinkManager;
    public static DelayManager delayManager;
    public static LagManager lagManager;
    public static PlayerStateManager playerStateManager;
    public static FriendManager friendManager;
    public static TargetManager targetManager;
    public static ValueHandler valueHandler;
    public static ModuleManager moduleManager;
    public static Handler handler;
    public static GuiManager guiManager;
    public static MathUtil rand;

    public Epilogue() {
        this.init();
    }

    public void init() {
        rotationManager = new RotationManager();
        floatManager = new FloatManager();
        blinkManager = new BlinkManager();
        delayManager = new DelayManager();
        lagManager = new LagManager();
        playerStateManager = new PlayerStateManager();
        friendManager = new FriendManager();
        targetManager = new TargetManager();
        valueHandler = new ValueHandler();
        moduleManager = new ModuleManager();
        handler = new Handler();
        guiManager = new GuiManager();
        EventManager.register(rotationManager);
        EventManager.register(floatManager);
        EventManager.register(blinkManager);
        EventManager.register(delayManager);
        EventManager.register(lagManager);
        EventManager.register(moduleManager);
        EventManager.register(handler);
        moduleManager.modules.put(AimAssist.class, new AimAssist());
        moduleManager.modules.put(Animations.class, new Animations());
        moduleManager.modules.put(NoDebuff.class, new NoDebuff());
        moduleManager.modules.put(AntiFireball.class, new AntiFireball());
        moduleManager.modules.put(GhostHunter.class, new GhostHunter());
        moduleManager.modules.put(AntiVoid.class, new AntiVoid());
        moduleManager.modules.put(ArrayList.class, new ArrayList());
        moduleManager.modules.put(AutoClicker.class, new AutoClicker());
        moduleManager.modules.put(AutoHeal.class, new AutoHeal());
        moduleManager.modules.put(AutoTool.class, new AutoTool());
        moduleManager.modules.put(AutoBlockIn.class, new AutoBlockIn());
        moduleManager.modules.put(BedNuker.class, new BedNuker());
        moduleManager.modules.put(BedESP.class, new BedESP());
        moduleManager.modules.put(BedChecker.class, new BedChecker());
        moduleManager.modules.put(Blink.class, new Blink());
        moduleManager.modules.put(Camera.class, new Camera());
        moduleManager.modules.put(Chams.class, new Chams());
        moduleManager.modules.put(ChestAura.class, new ChestAura());
        moduleManager.modules.put(ChestESP.class, new ChestESP());
        moduleManager.modules.put(ChestStealer.class, new ChestStealer());
        moduleManager.modules.put(ChestView.class, new ChestView());
        moduleManager.modules.put(ClickGUI.class, new ClickGUI());
        moduleManager.modules.put(CloudMusicModule.class, new CloudMusicModule());
        moduleManager.modules.put(DynamicIsland.class, new DynamicIsland());
        moduleManager.modules.put(Disabler.class, new Disabler());
        moduleManager.modules.put(Eagle.class, new Eagle());
        moduleManager.modules.put(ESP.class, new ESP());
        moduleManager.modules.put(FastPlace.class, new FastPlace());
        moduleManager.modules.put(Fly.class, new Fly());
        moduleManager.modules.put(FullBright.class, new FullBright());
        moduleManager.modules.put(FontRender.class, new FontRender());
        moduleManager.modules.put(GhostHand.class, new GhostHand());
        moduleManager.modules.put(Indicators.class, new Indicators());
        moduleManager.modules.put(Interface.class, new Interface());
        moduleManager.modules.put(JumpCircles.class, new JumpCircles());
        moduleManager.modules.put(InvManager.class, new InvManager());
        moduleManager.modules.put(InvWalk.class, new InvWalk());
        moduleManager.modules.put(ItemESP.class, new ItemESP());
        moduleManager.modules.put(Jesus.class, new Jesus());
        moduleManager.modules.put(KeepSprint.class, new KeepSprint());
        moduleManager.modules.put(Aura.class, new Aura());
        moduleManager.modules.put(FakeLag.class, new FakeLag());
        moduleManager.modules.put(LongJump.class, new LongJump());
        moduleManager.modules.put(MCF.class, new MCF());
        moduleManager.modules.put(NameTags.class, new NameTags());
        moduleManager.modules.put(Nick.class, new Nick());
        moduleManager.modules.put(NoFall.class, new NoFall());
        moduleManager.modules.put(NoHitDelay.class, new NoHitDelay());
        moduleManager.modules.put(NoHurtCam.class, new NoHurtCam());
        moduleManager.modules.put(NoJumpDelay.class, new NoJumpDelay());
        moduleManager.modules.put(NoRotate.class, new NoRotate());
        moduleManager.modules.put(NoSlow.class, new NoSlow());
        moduleManager.modules.put(Notification.class, new Notification());
        moduleManager.modules.put(PotionEffects.class, new PotionEffects());
        moduleManager.modules.put(PostProcessing.class, new PostProcessing());
        moduleManager.modules.put(PartySpammer.class, new PartySpammer());
        moduleManager.modules.put(Range.class, new Range());
        moduleManager.modules.put(Refill.class, new Refill());
        moduleManager.modules.put(SafeWalk.class, new SafeWalk());
        moduleManager.modules.put(Scaffold.class, new Scaffold());
        moduleManager.modules.put(Scoreboard.class, new Scoreboard());
        moduleManager.modules.put(Session.class, new Session());
        moduleManager.modules.put(Spammer.class, new Spammer());
        moduleManager.modules.put(Speed.class, new Speed());
        moduleManager.modules.put(SpeedMine.class, new SpeedMine());
        moduleManager.modules.put(Sprint.class, new Sprint());
        moduleManager.modules.put(SuperKnockBack.class, new SuperKnockBack());
        moduleManager.modules.put(TargetESP.class, new TargetESP());
        moduleManager.modules.put(TargetHUD.class, new TargetHUD());
        moduleManager.modules.put(TargetStrafe.class, new TargetStrafe());
        moduleManager.modules.put(Tracers.class, new Tracers());
        moduleManager.modules.put(Trajectories.class, new Trajectories());
        moduleManager.modules.put(MusicLyrics.class, new MusicLyrics());
        moduleManager.modules.put(Velocity.class, new Velocity());
        moduleManager.modules.put(ViewClip.class, new ViewClip());
        moduleManager.modules.put(WaterMark.class, new WaterMark());
        moduleManager.modules.put(Xray.class, new Xray());
        handler.powerShells.add(new Bind());
        handler.powerShells.add(new Config());
        handler.powerShells.add(new Denick());
        handler.powerShells.add(new Friend());
        handler.powerShells.add(new Help());
        handler.powerShells.add(new Hide());
        handler.powerShells.add(new Name());
        handler.powerShells.add(new Item());
        handler.powerShells.add(new Player());
        handler.powerShells.add(new Show());
        handler.powerShells.add(new Target());
        handler.powerShells.add(new Toggle());
        handler.powerShells.add(new VerticalClip());
        handler.powerShells.add(new Binds());
        handler.powerShells.add(new AIChat());

        for (epilogue.module.Module module : moduleManager.modules.values()) {
            java.util.ArrayList<Value<?>> properties = new java.util.ArrayList<>();
            for (final Field field : module.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                final Object obj;
                try {
                    obj = field.get(module);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                if (obj instanceof Value<?>) {
                    ((Value<?>) obj).setOwner(module);
                    properties.add((Value<?>) obj);
                }
            }
            valueHandler.properties.put(module.getClass(), properties);
            EventManager.register(module);
        }
        epilogue.config.Config config = new epilogue.config.Config("default", true);
        if (config.file.exists()) {
            config.load();
        }
        if (friendManager.file.exists()) {
            friendManager.load();
        }
        if (targetManager.file.exists()) {
            targetManager.load();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(config::save));
    }
}
