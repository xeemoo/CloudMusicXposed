package cn.com.zhjnc.cloudmusicxposed;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.BaseBundle;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
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

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
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

        hookMallIn(loadPackageParam);           //阻止签到进入商城
        hookAd(loadPackageParam);               //阻止程序闪屏广告
        hookSearchAd(loadPackageParam);         //阻止搜索页面广告
        hookCommentAd(loadPackageParam);        //阻止评论区的非评论类容
        addMyAd(loadPackageParam);              //添加插件信息

        autoSign(loadPackageParam);             //自动签到
        canShareAnyMusic(loadPackageParam);     //去除无版权歌曲分享限制
        hookShareLrcVIPImage(loadPackageParam); //去除歌词分享图片VIP下载
        hookUpdate(loadPackageParam);           //去除升级提示
        hookDailyFragment(loadPackageParam);    //切换“每日推荐”
    }

    private void hookDailyFragment(XC_LoadPackage.LoadPackageParam param) {
        if (PreferencesUtils.hookDaily()) {
            final Class<?> DailyRcmdFragment = findClass(CloudMusicVersion.DAILY_RCMD_FRAGMENT, param.classLoader);
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
                    Field textViewField = XposedHelpers.findFieldIfExists(DailyRcmdFragment, CloudMusicVersion.DAILY_TEXTVIEW_NAME);
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
                            // TODO new DatePickerDialog(需要一个正确的Context, ....)
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
            MusicInfo = XposedHelpers.findClass(CloudMusicVersion.MUSIC_INFO_CLASS, p.classLoader);
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
                CharSequence.class, TextView.BufferType.class, Boolean.TYPE, Integer.TYPE,
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
        try {
            findAndHookMethod(CloudMusicVersion.MALL_ENTRANCE_CLASS, param.classLoader, CloudMusicVersion.MALL_ENTRANCE_METHOD,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(true);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("[CMX] hookMallIn error.");
        }
    }

    /**
     * 闪屏广告
     * @param param
     */
    private void hookAd(XC_LoadPackage.LoadPackageParam param) {
        Class<?> AdFragment = findClass(CloudMusicVersion.AD_FRAGMENT, param.classLoader);
        Class<?> LoadingAdActivity = findClass(CloudMusicVersion.LOADINGAD_ACTIVITY, param.classLoader);

        /*XposedBridge.hookAllConstructors(LoadingAdActivity, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return null;
            }
        });*/

        /*findAndHookMethod(AdFragment, "onCreateView",
                LayoutInflater.class, ViewGroup.class, Bundle.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        return null;
                    }
                }
        );*/

        // github@ zjns/PureNeteaseCloudMusic-Xposed
        Class<?> BundleClass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? BaseBundle.class : Bundle.class;
        findAndHookMethod(BundleClass, "getSerializable", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0].toString().equals("adInfo")) {
                    param.setResult(null);
                }
            }
        });
    }

    /**
     * 搜索页面的广告
     * github@ zjns/PureNeteaseCloudMusic-Xposed
     * @param param
     */
    private void hookSearchAd(XC_LoadPackage.LoadPackageParam param) {
        Class<?> Search_AD_Class = findClass(CloudMusicVersion.SEARCH_AD_CLASS, param.classLoader);

        XposedBridge.hookAllConstructors(Search_AD_Class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                view.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 去除评论区的非评论内容
     * github@ zjns/PureNeteaseCloudMusic-Xposed
     * @param param
     */
    private void hookCommentAd(XC_LoadPackage.LoadPackageParam param) {
        Class<?> Comment_AD_Class = findClass(CloudMusicVersion.COMMENT_AD_CLASS, param.classLoader);
        try {
            findAndHookMethod(Comment_AD_Class, "a", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ArrayList<?> commList = (ArrayList) param.getResult();
                    if (commList == null || commList.isEmpty()) {
                        return;
                    }
                    Iterator<?> iterator = commList.iterator();
                    while(iterator.hasNext()) {
                        Object entry = iterator.next();
                        if (checkCommentType(entry)) {
                            iterator.remove();
                        }
                    }
                    // param.setResult(commList); TODO Why
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[CMX] hookCommentAd error.");
        }
    }
    private boolean checkCommentType(Object obj) {
        int type = (int) callMethod(obj, "getType");
        return (type == 5 || type == 6 || type == 9 || type == 11 || type == 13);
    }


    /**
     * 去除因版权问题无法分享歌词图片的限制
     * @param param
     */
    private void canShareAnyMusic(XC_LoadPackage.LoadPackageParam param) {
        Class<?> MusicInfoClass = findClass(CloudMusicVersion.MUSIC_INFO_CLASS, param.classLoader);
        try {
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
        } catch (Throwable t) {
            XposedBridge.log("[CMX] canShareAnyMusic error.");
        }
    }

    /**
     * 免VIP下载分享歌词图片
     * @param param
     */
    private void hookShareLrcVIPImage(XC_LoadPackage.LoadPackageParam param) {
        try {
            findAndHookMethod(CloudMusicVersion.VIP_LRC_IMAGE_CLASS, param.classLoader, "e",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(false);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("[CMX] hookShareLrcVIPImage error.");
        }
    }

    /**
     * 阻止网易云音乐升级弹窗
     * @param param
     */
    private void hookUpdate(XC_LoadPackage.LoadPackageParam param) {
        if (PreferencesUtils.update()) {
            try {
                findAndHookMethod(CloudMusicVersion.UPDATE_CLASS, param.classLoader, CloudMusicVersion.UPDATE_METHOD,
                        Boolean.TYPE,
                        new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                                return null;
                            }
                        }
                );
            } catch (Throwable t) {
                XposedBridge.log("[CMX] hookUpdate error.");
            }
        }
    }

    /**
     * "关于网易云音乐"界面，长按版本号，弹窗
     * @param param
     */
    private void addMyAd(XC_LoadPackage.LoadPackageParam param) {
        String className = CloudMusicVersion.ABOUT_CLASS;
        try {
            findAndHookMethod(className, param.classLoader, CloudMusicVersion.ABOUT_METHOD, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String str = (String) param.getResult();
                    if (!str.contains("plugins"))
                        param.setResult(str + "[plugins]  CloudMusicXposed");
                }
            });
        } catch (NoSuchMethodError error) {
            findAndHookMethod(className, param.classLoader, "ab", new XC_MethodHook() {
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
     * 废弃
     * @param param
     */
//    private void hookColorPicker(XC_LoadPackage.LoadPackageParam param) {
//        final Class clazz = XposedHelpers.findClass(CloudMusicVersion.THEME_COLOR_ACTIVITY, param.classLoader);
//        final Class pickerClass = XposedHelpers.findClass(CloudMusicVersion.PICKER_CLASS, param.classLoader);
//
//        findAndHookMethod(clazz, "onCreateOptionsMenu",
//                Menu.class,
//                new XC_MethodHook() {
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        if (PreferencesUtils.argb()) {
//                            Menu m = (Menu) param.args[0];
//                            if (m.size() == 1) {
//                                MenuItemCompat.setShowAsAction(m.add(0, 1, 0, "ARGB"), 2);
//                            }
//                        }
//                    }
//                }
//        );
//
//        findAndHookMethod(clazz, "onOptionsItemSelected",
//                MenuItem.class,
//                new XC_MethodHook() {
//                    @Override
//                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                        if (PreferencesUtils.argb()) {
//                            MenuItem mi = (MenuItem) param.args[0];
//                            if (mi.getItemId() == 1) {
//                                //此处会被调用两次，估计是7.0框架的问题...
//                                callStaticMethod(pickerClass, "a", param.thisObject, 1, null);
//                            }
//                        }
//                    }
//                }
//        );
//    }

}
