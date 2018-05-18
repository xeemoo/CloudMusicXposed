package cn.com.zhjnc.cloudmusicxposed;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
    private XSharedPreferences xPref;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
//        xPref = new XSharedPreferences(MY_PACKAGE, "settings");
//        xPref.makeWorldReadable();
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
        XposedBridge.log("[CMX] CloudMusicVersion:" + loadPackageParam.packageName + mVersionName);

        FileUtils.MOD_Context = mContext.createPackageContext(MY_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);

        hookMallIn(loadPackageParam);        //阻止签到进入商城
        hookAd(loadPackageParam);            //阻止程序启动广告
        //addMyAd(loadPackageParam);         //添加插件信息(4.1.3没有)

        autoSign(loadPackageParam);          //自动签到
        //hookColorPicker(loadPackageParam);   //个性换肤的自选颜色增加ARGB快捷入口
        canShareAnyMusic(loadPackageParam);  //去除无版权歌曲分享限制
        //hookUpdate(loadPackageParam);        //去除升级提示
        hookDailyFragment(loadPackageParam); //切换“每日推荐”
    }

    private void hookDailyFragment(XC_LoadPackage.LoadPackageParam param) {
        if (PreferencesUtils.hookDaily()) {
            final Class<?> DailyRcmdFragment = findClass(PACKAGE + ".fragment.DailyRcmdMusicFragment", param.classLoader);
            final Class<?> PagerListCallback = findClass(CloudMusicVersion.PAGERLISTVIEW_CALLBACK, param.classLoader);

            XposedBridge.hookAllMethods(DailyRcmdFragment, "onCreateView", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        findAndHookMethod(PagerListCallback, "a", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                List list = (List) param.getResult();
                                String name = FileUtils.createFileNameByTime();
                                if (!name.contains("0000_00_00")) {
                                    FileUtils.saveDailyList(name, list);
                                }
                                XposedBridge.log("[CMX] Save music list : " + name);
                            }
                        });
                }
            });

            ActivityHook iHook = new ActivityHook();
            findAndHookMethod("android.app.Instrumentation", param.classLoader, "newActivity",
                    ClassLoader.class, String.class, Intent.class, iHook);
            XposedBridge.hookAllMethods(DailyRcmdFragment, "onCreateView", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Field textViewField = XposedHelpers.findFieldIfExists(DailyRcmdFragment, "n");
                    Field adapterField = XposedHelpers.findFieldIfExists(DailyRcmdFragment, "c");
                    textViewField.setAccessible(true);
                    adapterField.setAccessible(true);
                    final TextView tv = (TextView) textViewField.get(param.thisObject);
                    final Object listAdapter = adapterField.get(param.thisObject);
                    tv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final Calendar c = Calendar.getInstance();
                            int year = c.get(Calendar.YEAR);
                            int month = c.get(Calendar.MONTH);
                            int day = c.get(Calendar.DAY_OF_MONTH);
                            final DatePickerDialog dialog = new DatePickerDialog(ActivityHook.getCurrentActivity(),
                                    new DateSetListener(mParam, tv, listAdapter), year, month, day);
                            ActivityHook.getCurrentActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.show();
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    class DateSetListener implements DatePickerDialog.OnDateSetListener {
        TextView mTextView;
        Object listAdapter;
        XC_LoadPackage.LoadPackageParam mParam;
        Class<?> MusicInfo;

        public DateSetListener(XC_LoadPackage.LoadPackageParam p, TextView tv, Object adapter) {
            mParam = p;
            mTextView = tv;
            listAdapter = adapter;
            MusicInfo = XposedHelpers.findClass(PACKAGE + ".meta.MusicInfo", p.classLoader);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            String name = year + "_" + (month+1) + "_" + dayOfMonth;
            if (! name.contains("0000_00_00")) {
                List<JSONObject> jsonList = FileUtils.getDailyList(name);
                if (jsonList != null && jsonList.size() > 0) {
                    mTextView.setText(String.valueOf(dayOfMonth));
                    resetDailyList(name, jsonList);
                } else {
                    XposedBridge.log("[CMX] " + name + " 这一天没有保存日推");
                    //Toast.makeText(mContext, "这一天没有保存日推", Toast.LENGTH_SHORT).show();
                }
            }
        }

        private void resetDailyList(final String name, final List<JSONObject> jsonList) {
            ActivityHook.getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List musicList = new ArrayList();
                    for (JSONObject obj : jsonList) {
                        Object o = callStaticMethod(MusicInfo, "buildMusicInfoByJsonMeta", obj);
                        if (o != null) {
                            musicList.add(o);
                        }
                    }

                    final List testList = musicList;
                    XposedBridge.log("[CMX] Get music list : " + name + " , length = " + testList.size());
                    callMethod(listAdapter, "a", new Class[]{List.class}, testList);
                }
            });
        }
    }

    private void autoSign(XC_LoadPackage.LoadPackageParam param) {
        findAndHookMethod("android.widget.TextView", param.classLoader, "setText",
                CharSequence.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (PreferencesUtils.autoSign()) {
                            if (param.args[0] != null) {
                                if (!param.args[0].equals(null)) {
                                    if (param.args[0].toString().equals("签到")) {
                                        TextView t1 = (TextView) param.thisObject;
                                        t1.performClick();
                                    }
                                }
                            }
                        }
                    }
                }
        );
    }

    /**
     * 签到按钮打开的页面
     * @param param
     */
    private void hookMallIn(XC_LoadPackage.LoadPackageParam param) {
        findAndHookMethod(CloudMusicVersion.MALL_ENTRANCE_CLASS, param.classLoader, CloudMusicVersion.MALL_ENTRANCE_METHOD,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                }
        );
    }

    /**
     * 闪屏广告
     * @param param
     */
    private void hookAd(XC_LoadPackage.LoadPackageParam param) {
        Class<?> AdClass = findClass(PACKAGE + ".module.ad.c.a", param.classLoader);
        Class<?> AdInfoClass = findClass(CloudMusicVersion.AD_INFO_CLASS, param.classLoader);

        findAndHookMethod(CloudMusicVersion.AD_CLASS, param.classLoader, "a", AdClass,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        return null;
                    }
                }
        );

        findAndHookMethod(CloudMusicVersion.LOADINGAD_ACTIVITY, param.classLoader, "a",
                Context.class, AdInfoClass, Integer.TYPE,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        return null;
                    }
                }
        );
    }

    /**
     * 去除因版权问题无法分享歌词图片的限制
     * @param param
     */
    private void canShareAnyMusic(XC_LoadPackage.LoadPackageParam param) {
        Class<?> MusicInfoClass = findClass(PACKAGE + ".meta.MusicInfo", param.classLoader);

        findAndHookMethod(CloudMusicVersion.SHARE_LYRICS_CLASS, param.classLoader, CloudMusicVersion.SHARE_LYRICS_METHOD,
                MusicInfoClass, Context.class, Integer.TYPE,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (PreferencesUtils.shareLrc()) {
                            param.setResult(false);
                        }
                    }
                }
        );
    }

    /**
     * 阻止网易云音乐升级弹窗
     * @param param
     */
    private void hookUpdate(XC_LoadPackage.LoadPackageParam param) {
        if (PreferencesUtils.update()) {
            findAndHookMethod(CloudMusicVersion.UPDATE_CLASS, param.classLoader, CloudMusicVersion.UPDATE_METHOD,
                    Boolean.TYPE,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            return null;
                        }
                    }
            );
        }
    }

    /**
     * "关于网易云音乐"界面，长按版本号，弹窗
     * @param param
     */
    private void addMyAd(XC_LoadPackage.LoadPackageParam param) {
        String className = CloudMusicVersion.ABOUT_CLASS;
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

    /**
     * 个性换肤 - 自选颜色
     * @param param
     */
    private void hookColorPicker(XC_LoadPackage.LoadPackageParam param) {
        final Class clazz = XposedHelpers.findClass(CloudMusicVersion.THEME_COLOR_ACTIVITY, param.classLoader);
        final Class pickerClass = XposedHelpers.findClass(CloudMusicVersion.PICKER_CLASS, param.classLoader);

        findAndHookMethod(clazz, "onCreateOptionsMenu",
                Menu.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (PreferencesUtils.argb()) {
                            Menu m = (Menu) param.args[0];
                            if (m.size() == 1) {
                                MenuItemCompat.setShowAsAction(m.add(0, 1, 0, "ARGB"), 2);
                            }
                        }
                    }
                }
        );

        findAndHookMethod(clazz, "onOptionsItemSelected",
                MenuItem.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (PreferencesUtils.argb()) {
                            MenuItem mi = (MenuItem) param.args[0];
                            if (mi.getItemId() == 1) {
                                //此处会被调用两次，估计是7.0框架的问题...
                                callStaticMethod(pickerClass, "a", param.thisObject, 1, null);
                            }
                        }
                    }
                }
        );
    }

}
