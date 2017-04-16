package cn.com.zhjnc.cloudmusicxposed;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class MainHook implements IXposedHookLoadPackage {

    private static final String PACKAGE = "com.netease.cloudmusic";

    private Context mContext;
    private String mVersionName;
    private TextView mTextView;
    private int mSignId = 0;
    private int mVerTextId = 0;
    boolean zici = false; // 国产软件怎么不兹磁

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        String packageName = loadPackageParam.packageName;
        if (!packageName.contains(PACKAGE))
            return;

        Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null),
                "currentActivityThread");
        mContext = (Context) callMethod(activityThread, "getSystemContext");
        mVersionName = mContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
        //XposedBridge.log("CloudMusic: " + loadPackageParam.packageName + mVersionName);
        hookCloudMusic(loadPackageParam, mVersionName);
    }

    private void hookCloudMusic(XC_LoadPackage.LoadPackageParam param, String versionName) {
        if (versionName.contains("3.8.1")) {
            mSignId = 2131690416;
            mVerTextId = 2131689623;
            zici = true;
        } else if(versionName.contains("4.0.0")) {
            mSignId = 2131690481;  //id=a0m
            mVerTextId = 2131689632; //id=dk
            zici = true;
        } else if(versionName.contains("4.0.1") || versionName.contains("4.0.2")) {
            mSignId = 2131690486;  //id=a0r
            mVerTextId = 2131689632; //id=dk
            zici = true;
        }
        if (zici) {
            XposedHelpers.findAndHookMethod(PACKAGE + ".activity.MainActivity", param.classLoader,
                    "onResume", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            mTextView = (TextView) ((Activity) param.thisObject).findViewById(mSignId);
                            //模拟点击
                            mTextView.performClick();
                        }
                    });
            XposedHelpers.findAndHookMethod(PACKAGE + ".activity.AboutActivity", param.classLoader,
                    "onCreate", Bundle.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            TextView textView = (TextView) ((Activity) param.thisObject).findViewById(mVerTextId);
                            textView.setGravity(Gravity.CENTER);
                            textView.setText(textView.getText() + "\n云音乐自动签到 v1.1.2 by MoLulu");
                        }
                    });
        }
    }
}
