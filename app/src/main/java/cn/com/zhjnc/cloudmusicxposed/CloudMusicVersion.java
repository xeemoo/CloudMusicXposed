package cn.com.zhjnc.cloudmusicxposed;

import de.robv.android.xposed.XposedHelpers;

/**
 * 云音乐类名与方法名
 */

public class CloudMusicVersion {
    private static final String PACKAGE = "com.netease.cloudmusic";


    public static String MALL_ENTRANCE_CLASS  = PACKAGE + ".module.a.b";
    public static String MALL_ENTRANCE_METHOD = "j";
    public static String AD_CLASS             = PACKAGE + ".module.ad.c";
    public static String AD_METHOD            = "a";
    public static String AD_INFO_CLASS        = PACKAGE + ".module.ad.meta.AdInfo";
    public static String LOADINGAD_ACTIVITY = PACKAGE + ".activity.LoadingAdActivity";
    public static String SHARE_LYRICS_CLASS   = PACKAGE + ".module.o.h";
    public static String SHARE_LYRICS_METHOD  = "a";
    public static String UPDATE_CLASS         = PACKAGE + ".module.n.f";
    public static String UPDATE_METHOD        = "a";
    public static String ABOUT_CLASS          = PACKAGE + ".activity.AboutActivity";
    public static String ABOUT_METHOD         = "ab";
    public static String PICKER_CLASS         = PACKAGE + ".ui.MaterialDiloagCommon.a";
    public static String THEME_COLOR_ACTIVITY = PACKAGE + ".activity.ThemeColorDetailActivity";

    public static String PAGERLISTVIEW_CALLBACK = PACKAGE + ".fragment.DailyRcmdMusicFragment$1";
    public static String DAILY_TEXTVIEW_NAME    = "o";

}
