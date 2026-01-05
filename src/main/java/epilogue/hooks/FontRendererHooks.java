package epilogue.hooks;

import epilogue.Epilogue;
import epilogue.module.modules.misc.GhostHunter;
import epilogue.module.modules.misc.Nick;

public final class FontRendererHooks {
    private FontRendererHooks() {
    }

    public static String onRenderString(String string) {
        if (Epilogue.moduleManager == null) {
            return string;
        }
        GhostHunter antiObfuscate = (GhostHunter) Epilogue.moduleManager.modules.get(GhostHunter.class);
        if (antiObfuscate.isEnabled()) {
            string = antiObfuscate.stripObfuscated(string);
        }
        Nick nick = (Nick) Epilogue.moduleManager.modules.get(Nick.class);
        return nick.isEnabled() ? nick.replaceNick(string) : string;
    }

    public static String onGetStringWidth(String string) {
        if (Epilogue.moduleManager == null) {
            return string;
        }
        GhostHunter antiObfuscate = (GhostHunter) Epilogue.moduleManager.modules.get(GhostHunter.class);
        if (antiObfuscate.isEnabled()) {
            string = antiObfuscate.stripObfuscated(string);
        }
        Nick nick = (Nick) Epilogue.moduleManager.modules.get(Nick.class);
        return nick.isEnabled() ? nick.replaceNick(string) : string;
    }

    public static char onGetStringWidthCharAt(String string, int index) {
        char charAt = string.charAt(index);
        return charAt != '0'
                && charAt != '1'
                && charAt != '2'
                && charAt != '3'
                && charAt != '4'
                && charAt != '5'
                && charAt != '6'
                && charAt != '7'
                && charAt != '8'
                && charAt != '9'
                && charAt != 'a'
                && charAt != 'A'
                && charAt != 'b'
                && charAt != 'B'
                && charAt != 'c'
                && charAt != 'C'
                && charAt != 'd'
                && charAt != 'D'
                && charAt != 'e'
                && charAt != 'E'
                && charAt != 'f'
                && charAt != 'F'
                ? charAt
                : 'r';
    }
}
