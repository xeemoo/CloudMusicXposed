package cn.com.zhjnc.cloudmusicxposed;

import android.app.Activity;

import de.robv.android.xposed.XC_MethodHook;

/**
 * 在任意处获取当前Activity的引用
 * <br/>
 * <br/><b>Sample Code:</b><br/>
 * ActivityHook iHook = new ActivityHook();<br/>
 * findAndHookMethod("android.app.Instrumentation", param.classLoader, "newActivity", ClassLoader.class, String.class, Intent.class, iHook);<br/>
 * Activity act = ActivityHook.getCurrentActivity();
 */

public class ActivityHook extends XC_MethodHook {
    private static volatile Activity _currentActivity = null;

    public static Activity getCurrentActivity() {
        return _currentActivity;
    }

    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        _currentActivity = (Activity) param.getResult();
    }
}
