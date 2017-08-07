package cn.com.zhjnc.cloudmusicxposed;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class MainHook implements IXposedHookLoadPackage {

    private static final String PACKAGE = "com.netease.cloudmusic";

    private Context mContext;
    private String mVersionName;
    private XC_LoadPackage.LoadPackageParam mParam;

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
        //XposedBridge.log("CloudMusic: " + loadPackageParam.packageName + mVersionName);
        hookCloudMusic(loadPackageParam);

        //showCurrentMusicInfo(loadPackageParam);
        canShareAnyMusic(loadPackageParam);
    }

    private void hookCloudMusic(XC_LoadPackage.LoadPackageParam param) {
        findAndHookMethod("android.widget.TextView", param.classLoader,
                "setText", CharSequence.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!param.args[0].equals(null)) {
                            if (param.args[0].toString().equals("签到")) {
                                TextView t1 = (TextView) param.thisObject;
                                t1.performClick();
                                XposedBridge.log("签到ID=" + t1.getId());
                            } else if (param.args[0].toString().equals("关于网易云音乐")) {
                                TextView t2 = (TextView) ((LinearLayout) ((ScrollView) ((LinearLayout) ((TextView) param.thisObject).getParent().getParent()).getChildAt(2)).getChildAt(0)).getChildAt(1);
                                if (!t2.getText().toString().contains("自动签到") && t2.getText().toString().contains("V")) {
                                    t2.setGravity(Gravity.CENTER);
                                    t2.setText(t2.getText() + "\n云音乐自动签到 v2.0 by MoLulu");
                                    XposedBridge.log("版本ID=" + t2.getId());
                                }
                            }

                        }
                    }
                });

    }

    /**
     * 去除因版权问题无法分享歌词图片的限制
     * 支持版本：V4.1.1.190367
     * @param loadPackageParam
     */
    private void canShareAnyMusic(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        Class<?> MusicInfoClass = findClass(PACKAGE + ".meta.MusicInfo", loadPackageParam.classLoader);
        String className = "";
        if (mVersionName.contains("4.1.1") || mVersionName.contains("4.1.2")) {
            className = PACKAGE + ".e";
        } else {
            // v4.1.3 该类更改了位置
            className = PACKAGE + ".module.o.b";
        }

        findAndHookMethod(className, loadPackageParam.classLoader, "a",
                MusicInfoClass, Context.class, Integer.TYPE, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(false);
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
