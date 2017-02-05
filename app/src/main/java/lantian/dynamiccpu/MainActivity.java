package lantian.dynamiccpu;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    AppsAdapter apps = new AppsAdapter();
    CPUFreq CPUInstance = new CPUFreq();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView appsList = (RecyclerView) findViewById(R.id.appsList);
        appsList.setLayoutManager(new LinearLayoutManager(this));
        appsList.setAdapter(apps);

        if (!CPUInstance.checkRoot()) {
            Toast.makeText(this, R.string.ui_root_fail, Toast.LENGTH_SHORT).show();
        }

        List<ApplicationInfo> listApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(listApps, new ApplicationInfo.DisplayNameComparator(getPackageManager()));
        apps.pkgs.clear();
        SharedPreferences prefs = getSharedPreferences("apps", Context.MODE_PRIVATE);
        for (ApplicationInfo singleApp : listApps) {
            if (prefs.getInt(singleApp.packageName, 0) != 0) apps.pkgs.add(singleApp);
        }
        for (ApplicationInfo singleApp : listApps) {
            if (prefs.getInt(singleApp.packageName, 0) == 0) apps.pkgs.add(singleApp);
        }
        apps.notifyDataSetChanged();

        SharedPreferences presets = getSharedPreferences("presets", MODE_PRIVATE);
        if (presets.getInt("presets", -1) == -1) {
            presets.edit().putInt("presets", 0).apply();
            String defaultConfig = "";
            List<String> cpuFreqs;
            for (int i = 0; i < CPUInstance.getCPUCount(); i++) {
                cpuFreqs = CPUInstance.getCPUFreq(i);
                defaultConfig += cpuFreqs.get(cpuFreqs.size() - 1) + "\n";
            }
            presets.edit().putString("0", defaultConfig + getString(R.string.conf_default)).apply();
        }
    }

    protected void onResume() {
        super.onResume();
        apps.notifyDataSetChanged();
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.main_presets) {
            Intent it = new Intent(this, PresetsActivity.class);
            startActivity(it);
        } else if (id == R.id.main_show_hide_icon) {
            PackageManager pm = getPackageManager();
            if (PackageManager.COMPONENT_ENABLED_STATE_DISABLED == pm.getComponentEnabledSetting(new ComponentName(this, MainActivity.class))) {
                pm.setComponentEnabledSetting(new ComponentName(this, MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                Toast.makeText(this, R.string.toast_icon_shown, Toast.LENGTH_SHORT).show();
            } else {
                pm.setComponentEnabledSetting(new ComponentName(this, MainActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                Toast.makeText(this, R.string.toast_icon_hidden, Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.main_author) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://lantian.pub"));
            startActivity(browserIntent);
        } else if (id == R.id.main_source) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/xddxdd/lantian-dynamiccpu"));
            startActivity(browserIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.ViewHolder> {
        List<ApplicationInfo> pkgs = new ArrayList<>();

        public AppsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.line_apps, parent, false);
            return new AppsAdapter.ViewHolder(v);
        }

        public void onBindViewHolder(final AppsAdapter.ViewHolder holder, int position) {
            int confID = getSharedPreferences("apps", Context.MODE_PRIVATE).getInt(pkgs.get(position).packageName, 0);
            SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);
            if (confID != 0) {
                holder.pkgName.setChecked(true);
                holder.pkgConfig.setTextColor(Color.RED);
            } else {
                holder.pkgName.setChecked(false);
                holder.pkgConfig.setTextColor(Color.BLACK);
            }
            holder.pkgName.setText(pkgs.get(position).loadLabel(getPackageManager()));
            holder.pkgConfig.setText(prefs.getString(String.valueOf(confID), "").split("\n", CPUInstance.getCPUCount() + 1)[CPUInstance.getCPUCount()]);
            View.OnClickListener changePreset = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.pkgName.setChecked(getSharedPreferences("apps", Context.MODE_PRIVATE).getInt(pkgs.get(holder.getAdapterPosition()).packageName, 0) != 0);
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle(R.string.ui_choose_preset);
                    SharedPreferences prefs = getSharedPreferences("presets", MODE_PRIVATE);
                    int presetCount = prefs.getInt("presets", 0) + 1;
                    String[] presetNames = new String[presetCount];
                    for (int i = 0; i < presetCount; i++) {
                        presetNames[i] = prefs.getString(String.valueOf(i), "").split("\n", CPUInstance.getCPUCount() + 1)[CPUInstance.getCPUCount()];
                    }
                    dialog.setItems(presetNames, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getSharedPreferences("apps", Context.MODE_PRIVATE).edit().putInt(pkgs.get(holder.getAdapterPosition()).packageName, which).apply();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    apps.notifyDataSetChanged();
                                }
                            });
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
            };
            holder.pkgName.setOnClickListener(changePreset);
            holder.pkgConfig.setOnClickListener(changePreset);

        }

        public int getItemCount() {
            return pkgs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox pkgName;
            TextView pkgConfig;

            ViewHolder(LinearLayout v) {
                super(v);
                pkgName = (CheckBox) v.findViewById(R.id.pkgName);
                pkgConfig = (TextView) v.findViewById(R.id.pkgConfig);
            }
        }
    }
}
