package lantian.dynamiccpu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class AppSwitchReceiver extends BroadcastReceiver {
    SharedPreferences apps;
    SharedPreferences presets;
    CPUFreq CPUInstance = new CPUFreq();

    public AppSwitchReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        apps = context.getSharedPreferences("apps", Context.MODE_PRIVATE);
        presets = context.getSharedPreferences("presets", Context.MODE_PRIVATE);
        int newMode = apps.getInt(intent.getStringExtra("pkg"), 0);
        if (presets.getString(String.valueOf(newMode), "").isEmpty()) return;
        String[] newPreset = presets.getString(String.valueOf(newMode), "").split("\n");
        for (int i = 0; i < CPUInstance.getCPUCount(); i++) {
            CPUInstance.setCPUFreq(i, newPreset[i]);
        }
        //Toast.makeText(context, "Switched to " + newPreset[CPUInstance.getCPUCount()], Toast.LENGTH_SHORT).show();
    }
}
