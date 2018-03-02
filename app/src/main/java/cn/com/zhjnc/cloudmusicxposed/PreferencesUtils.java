package cn.com.zhjnc.cloudmusicxposed;

import de.robv.android.xposed.XSharedPreferences;

public class PreferencesUtils {
    private static XSharedPreferences instance = null;

    private static XSharedPreferences getInstance() {
        if (instance == null) {
            instance = new XSharedPreferences(PreferencesUtils.class.getPackage().getName());
            instance.makeWorldReadable();
        } else {
            instance.reload();
        }
        return instance;
    }

    public static boolean autoSign() {
        return getInstance().getBoolean("AUTO_SIGN", false);
    }

    public static boolean argb() {
        return getInstance().getBoolean("ARGB", false);
    }

    public static boolean shareLrc() {
        return getInstance().getBoolean("SHARE_LRC", false);
    }

    public static boolean update() {
        return getInstance().getBoolean("NO_UPDATE", false);
    }

    public static boolean hookDaily() {
        return getInstance().getBoolean("HOOK_DAILY", false);
    }
}
