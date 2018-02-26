package cn.com.zhjnc.cloudmusicxposed;

import android.content.Context;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String PACKAGE = "com.netease.cloudmusic";
    private static final String MY_PACKAGE = "cn.com.zhjnc.cloudmusicxposed";

    private Context mContext;
    private String mVersionName;
    private XC_LoadPackage.LoadPackageParam mParam;
    private CloudMusicVersion mMusicVersion;
    private XSharedPreferences xPref;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        xPref = new XSharedPreferences(MY_PACKAGE, "settings");
        xPref.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        String packageName = loadPackageParam.packageName;
        if (!packageName.contains(PACKAGE))
            return;

        mParam = loadPackageParam;
        Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null),
                "currentActivityThread");
        mContext = (Context) callMethod(activityThread, "getSystemContext");
        mVersionName = mContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
        XposedBridge.log("CloudMusic: " + loadPackageParam.packageName + mVersionName);
        mMusicVersion = new CloudMusicVersion(mVersionName);

        //hookMallIn(loadPackageParam);      //暂时保留，为了乐签
        hookAd(loadPackageParam);            //阻止程序启动广告
        addMyAd(loadPackageParam);           //添加插件信息

        if (xPref.getBoolean("AUTO_SIGN", true)) {
            XposedBridge.log("------ do AUTO_SIGN");
            autoSign(loadPackageParam);          //自动签到
        }

        if (xPref.getBoolean("ARGB", true)) {
            XposedBridge.log("------ do ARGB");
            hookColorPicker(loadPackageParam);   //个性换肤的自选颜色增加ARGB快捷入口
        }

        if (xPref.getBoolean("SHARE_LRC", true)) {
            XposedBridge.log("------ do SHARE_LRC");
            canShareAnyMusic(loadPackageParam);  //去除无版权歌曲分享限制
        }

        if (xPref.getBoolean("NO_UPDATE", true)) {
            XposedBridge.log("------ do NO_UPDATE");
            hookUpdate(loadPackageParam);        //去除升级提示
        }
    }

    private void autoSign(XC_LoadPackage.LoadPackageParam param) {
        findAndHookMethod("android.widget.TextView", param.classLoader,
                "setText", CharSequence.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args[0] != null) {
                            if (!param.args[0].equals(null)) {
                                if (param.args[0].toString().equals("签到")) {
                                    TextView t1 = (TextView) param.thisObject;
                                    t1.performClick();
//                                    XposedBridge.log("签到ID=" + t1.getId());
                                }
                            }
                        }
                    }
                });

    }

    private void hookMallIn(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        findAndHookMethod(mMusicVersion.MALL_ENTRANCE_CLASS, loadPackageParam.classLoader,
                mMusicVersion.MALL_ENTRANCE_METHOD, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (mMusicVersion.VERSION_NAME.contains("4.3.4") || mMusicVersion.VERSION_NAME.contains("4.3.5")) {
                    param.setResult(true);
                } else {
                    param.setResult(false);
                }
            }
        });
    }

    private void hookAd(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        Class<?> AdClass = findClass(PACKAGE + ".module.ad.c.b", loadPackageParam.classLoader);
        String adClassName = mMusicVersion.AD_CLASS;
        if (mMusicVersion.VERSION_NAME.contains("4.2.0")) {
            findAndHookMethod(adClassName, loadPackageParam.classLoader, "a", AdClass,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            return null;
                        }
                    });
        } else if (mMusicVersion.VERSION_NAME.contains("4.2.3")
                || mMusicVersion.VERSION_NAME.contains("4.3.4")
                || mMusicVersion.VERSION_NAME.contains("4.3.5")) {
            findAndHookMethod(adClassName, loadPackageParam.classLoader, "a",
                    Boolean.TYPE, AdClass, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            return null;
                        }
                    });
        }

        Class<?> AdInfoClass = findClass(PACKAGE + ".module.ad.meta.AdInfo", loadPackageParam.classLoader);
        findAndHookMethod(PACKAGE + ".activity.LoadingAdActivity", loadPackageParam.classLoader, "a",
                Context.class, AdInfoClass, Integer.TYPE, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        return null;
                    }
                });
    }

    /**
     * 去除因版权问题无法分享歌词图片的限制
     * 支持版本：V4.1.1.190367 - V4.2.0.310743
     * @param loadPackageParam
     */
    private void canShareAnyMusic(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        Class<?> MusicInfoClass = findClass(PACKAGE + ".meta.MusicInfo", loadPackageParam.classLoader);
        String className = "";
        if (mVersionName.contains("4.1.1") || mVersionName.contains("4.1.2")) {
            className = PACKAGE + ".e";
        } else {
            // v4.1.3 该类更改了位置
            className = mMusicVersion.SHARE_LYRICS_CLASS;
        }

        findAndHookMethod(className, loadPackageParam.classLoader, mMusicVersion.SHARE_LYRICS_METHOD,
                MusicInfoClass, Context.class, Integer.TYPE, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });
    }

    private void hookUpdate(XC_LoadPackage.LoadPackageParam param) {
        String className = mMusicVersion.UPDATE_CLASS;
        findAndHookMethod(className, param.classLoader, mMusicVersion.UPDATE_METHOD,
                Boolean.TYPE, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        return null;
                    }
        });

    }

    /**
     * "关于网易云音乐"界面，长按版本号，弹窗
     * @param param
     */
    private void addMyAd(XC_LoadPackage.LoadPackageParam param) {
        String className = mMusicVersion.ABOUT_CLASS;
        try {
            findAndHookMethod(className, param.classLoader, "ab", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String str = (String) param.getResult();
                    if (!str.contains("plugins"))
                        param.setResult(str + "[plugins]  CloudMusicXposed");
                }
            });
        } catch (NoSuchMethodError error) {
            findAndHookMethod(className, param.classLoader, "ac", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String str = (String) param.getResult();
                    if (!str.contains("plugins"))
                        param.setResult(str + "[plugins]  CloudMusicXposed");
                }
            });
        }
    }

    private void hookColorPicker(XC_LoadPackage.LoadPackageParam param) {
        final Class clazz = XposedHelpers.findClass(PACKAGE + ".activity.ThemeColorDetailActivity", param.classLoader);
        final Class pickerClass = XposedHelpers.findClass(mMusicVersion.PICKER_CLASS, param.classLoader);
        findAndHookMethod(clazz, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Menu m = (Menu) param.args[0];
                if (m.size() == 1) {
                    MenuItemCompat.setShowAsAction(m.add(0, 1, 0, "ARGB"), 2);
                }
            }
        });
        findAndHookMethod(clazz, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                MenuItem mi = (MenuItem)param.args[0];
                if (mi.getItemId() == 1) {
                    //此处会被调用两次，估计是7.0框架的问题...
                    callStaticMethod(pickerClass, "a", param.thisObject, 1, null);
                }
            }
        });
    }

    /**
     * 也许还有用的代码
     * hook歌词分享界面，可以得到当前Music信息
     * 支持版本：V4.1.1.190367
     * @param loadPackageParam
     */
    private void showCurrentMusicInfo(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        Class<?> MusicInfoClass = findClass(PACKAGE + ".meta.MusicInfo", loadPackageParam.classLoader);
        Class<?> CommonLyricLineClass = findClass(PACKAGE + ".meta.CommonLyricLine", loadPackageParam.classLoader);
        findAndHookMethod(PACKAGE + ".ui.LyricView", loadPackageParam.classLoader, "a",
                MusicInfoClass, CommonLyricLineClass, Integer.TYPE, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String info = param.args[0].toString();
                        XposedBridge.log("MusicInfo=" + info);
                    }
                });
    }

    /**
     * This method can't work
     */
    public void gotoAbout(){
        callStaticMethod(findClass("com.netease.cloudmusic.activity.AboutActivity", mParam.classLoader), "a",
                findClass("com.netease.cloudmusic.activity.MainActivity", mParam.classLoader));
    }
}
