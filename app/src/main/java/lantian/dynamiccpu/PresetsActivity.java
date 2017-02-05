package lantian.dynamiccpu;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PresetsActivity extends AppCompatActivity {
    PresetsAdapter presets = new PresetsAdapter();
    CPUFreq CPUInstance = new CPUFreq();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presets);

        RecyclerView presetsList = (RecyclerView) findViewById(R.id.presetsView);
        presetsList.setLayoutManager(new LinearLayoutManager(this));
        presetsList.setAdapter(presets);
    }

    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_presets, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.preset_add) {
            SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);
            prefs.edit().putInt("presets", prefs.getInt("presets", 0) + 1).apply();
            String defaultConfig = "";
            List<String> cpuFreqs;
            for (int i = 0; i < CPUInstance.getCPUCount(); i++) {
                cpuFreqs = CPUInstance.getCPUFreq(i);
                defaultConfig += cpuFreqs.get(cpuFreqs.size() - 1) + "\n";
            }
            prefs.edit().putString(String.valueOf(prefs.getInt("presets", 0)), defaultConfig + "Preset #" + prefs.getInt("presets", 1)).apply();
            presets.notifyDataSetChanged();
            presets.userChoiceOfFrequency(prefs.getInt("presets", 1), 0);
        } else if (id == R.id.preset_rename) {
            final SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);
            AlertDialog.Builder dialog = new AlertDialog.Builder(PresetsActivity.this);
            final String[] presetNames = new String[prefs.getInt("presets", 1) + 1];
            for (int i = 0; i <= prefs.getInt("presets", 1); i++) {
                presetNames[i] = prefs.getString(String.valueOf(i), "").split("\n", CPUInstance.getCPUCount() + 1)[CPUInstance.getCPUCount()];
            }
            dialog.setTitle("Rename profile");
            dialog.setItems(presetNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, final int which) {
                    dialog.dismiss();

                    // Create another dialog to update the name
                    AlertDialog.Builder dialog2 = new AlertDialog.Builder(PresetsActivity.this);
                    final EditText input = new EditText(PresetsActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(prefs.getString(String.valueOf(which), "").split("\n", CPUInstance.getCPUCount() + 1)[CPUInstance.getCPUCount()]);
                    dialog2.setView(input);
                    dialog2.setTitle(R.string.ui_new_name);
                    dialog2.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int uselessWhich) {
                            dialog.dismiss();
                            String[] preset = prefs.getString(String.valueOf(which), "").split("\n", CPUInstance.getCPUCount() + 1);
                            preset[CPUInstance.getCPUCount()] = input.getText().toString();
                            String newPreset = "";
                            for (String presetLine : preset) {
                                newPreset += presetLine + "\n";
                            }
                            prefs.edit().putString(String.valueOf(which), newPreset.trim()).apply();
                            presets.notifyItemChanged(which);
                        }
                    });
                    dialog2.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog2.show();
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        } else if (id == R.id.preset_remove) {
            final SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);
            if (prefs.getInt("presets", 0) == 0) {
                Toast.makeText(this, R.string.ui_no_delete_default_preset, Toast.LENGTH_SHORT).show();
                return false;
            }
            AlertDialog.Builder dialog = new AlertDialog.Builder(PresetsActivity.this);
            final String[] presetNames = new String[prefs.getInt("presets", 1)];
            for (int i = 1; i <= prefs.getInt("presets", 1); i++) {
                presetNames[i - 1] = prefs.getString(String.valueOf(i), "").split("\n", CPUInstance.getCPUCount() + 1)[CPUInstance.getCPUCount()];
            }
            dialog.setTitle(R.string.ui_remove_preset);
            dialog.setItems(presetNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Remove the preset item first
                    for (int i = which + 1; i < prefs.getInt("presets", 0); i++) {
                        prefs.edit().putString(String.valueOf(i), prefs.getString(String.valueOf(i + 1), "")).apply();
                    }
                    prefs.edit().putInt("presets", prefs.getInt("presets", 0) - 1).apply();
                    presets.notifyDataSetChanged();

                    // Scan package configs & update preset IDs
                    SharedPreferences apps = getSharedPreferences("apps", Context.MODE_PRIVATE);
                    List<ApplicationInfo> listApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
                    int value;
                    for (ApplicationInfo singleApp : listApps) {
                        value = apps.getInt(singleApp.packageName, 0);
                        if (value == which) {
                            apps.edit().putInt(singleApp.packageName, 0).apply();
                        } else if (value > which) {
                            apps.edit().putInt(singleApp.packageName, value - 1).apply();
                        }
                    }

                    dialog.dismiss();
                }
            });
            dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    class PresetsAdapter extends RecyclerView.Adapter<PresetsAdapter.ViewHolder> {
        List<ApplicationInfo> pkgs = new ArrayList<>();
        CPUFreq CPUInstance = new CPUFreq();

        public PresetsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.line_presets, parent, false);
            return new PresetsAdapter.ViewHolder(v);
        }

        public void onBindViewHolder(final PresetsAdapter.ViewHolder holder, int position) {
            String preset = getSharedPreferences("presets", Context.MODE_PRIVATE).getString(String.valueOf(position), "");
            String[] presetSplit = preset.split("\n", CPUInstance.getCPUCount() + 1);
            holder.title.setText(presetSplit[CPUInstance.getCPUCount()]);
            String prefString = "";
            for (int i = 0; i < CPUInstance.getCPUCount(); i++) {
                prefString += getString(R.string.ui_cpu_id) + String.valueOf(i) + ": " + presetSplit[i] + " " + getString(R.string.ui_cpu_khz) + "\n";
            }
            holder.freqs.setText(prefString.trim());
            View.OnClickListener editPreset = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    userChoiceOfFrequency(holder.getAdapterPosition(), 0);
                }
            };
            holder.title.setOnClickListener(editPreset);
            holder.freqs.setOnClickListener(editPreset);
        }

        public int getItemCount() {
            return getSharedPreferences("presets", Context.MODE_PRIVATE).getInt("presets", 1) + 1;
        }

        void userChoiceOfFrequency(final int position, final int id) {
            if (id >= CPUInstance.getCPUCount()) return;
            List<String> cpuFreqList = CPUInstance.getCPUFreq(id);
            final String[] cpuFreqs = new String[cpuFreqList.size() + 1];
            cpuFreqs[0] = "0";
            for (int i = 0; i < cpuFreqList.size(); i++) {
                cpuFreqs[i + 1] = cpuFreqList.get(i);
            }
            AlertDialog.Builder dialog = new AlertDialog.Builder(PresetsActivity.this);
            dialog.setTitle(getString(R.string.ui_choose_cpu_id) + String.valueOf(id) + " " + getString(R.string.ui_choose_cpu_id_after));
            dialog.setItems(cpuFreqs, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences prefs = getSharedPreferences("presets", Context.MODE_PRIVATE);
                    String preset = prefs.getString(String.valueOf(position), "");
                    String[] presetSplit = preset.split("\n", CPUInstance.getCPUCount() + 1);
                    presetSplit[id] = cpuFreqs[which];
                    preset = "";
                    Boolean flag = false;
                    for (String presetPart : presetSplit) {
                        if (flag) preset += "\n";
                        flag = true;
                        preset += presetPart;
                    }
                    prefs.edit().putString(String.valueOf(position), preset).apply();
                    dialog.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyItemChanged(position);
                            userChoiceOfFrequency(position, id + 1);
                        }
                    });
                }
            });
            dialog.setNegativeButton(R.string.ui_skip, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyItemChanged(position);
                            userChoiceOfFrequency(position, id + 1);
                        }
                    });
                }
            });
            dialog.show();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView freqs;

            ViewHolder(LinearLayout v) {
                super(v);
                title = (TextView) v.findViewById(R.id.title);
                freqs = (TextView) v.findViewById(R.id.freqs);
            }
        }
    }
}
