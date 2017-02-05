package lantian.dynamiccpu;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.content.Intent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ActivityHook implements IXposedHookLoadPackage {
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XC_MethodHook onAppComeFront = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Intent it = new Intent("lantian.dynamiccpu.APP_SWITCHED");
                it.putExtra("pkg", lpparam.packageName);
                AndroidAppHelper.currentApplication().sendBroadcast(it);
            }
        };
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", onAppComeFront);
    }
}
