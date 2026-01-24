package epilogue.ui.clickgui.exhibition.bridge;

import epilogue.module.ModuleCategory;

public class ModuleData {
    public enum Type {
        Combat,
        Movement,
        Player,
        Visuals,
        Other,
        Minigames
    }

    public static Type from(ModuleCategory cat) {
        if (cat == null) return Type.Other;
        switch (cat) {
            case COMBAT:
                return Type.Combat;
            case MOVEMENT:
                return Type.Movement;
            case PLAYER:
                return Type.Player;
            case RENDER:
                return Type.Visuals;
            case MISC:
            default:
                return Type.Other;
        }
    }
}
