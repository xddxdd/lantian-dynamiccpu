package lantian.dynamiccpu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

class CPUFreq {
    private int cpuCnt = 0;

    private String getCPUPath(int id) {
        return "/sys/devices/system/cpu/cpu" + String.valueOf(id) + "/cpufreq/";
    }

    Boolean checkRoot() {
        try {
            Process su = Runtime.getRuntime().exec(new String[]{"su", "-c", "echo lantian"});
            return su.waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }
    }

    private String readFile(String filename) {
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            return "";
        }
        return text.toString().trim();
    }

    int getCPUCount() {
        if (cpuCnt != 0) return cpuCnt;
        int i = 0;
        File f = new File(getCPUPath(i));
        while (f.exists()) {
            i++;
            f = new File(getCPUPath(i));
        }
        cpuCnt = i;
        return i;
    }

    List<String> getCPUFreq(int id) {
        return Arrays.asList(readFile(getCPUPath(id) + "scaling_available_frequencies").split(" "));
    }

    Boolean setCPUFreq(int id, String freq) {
        if (freq.equals(readFile(getCPUPath(id) + "scaling_max_freq"))) return false;
        if (freq.equals("0")) return false;
        try {
            Process su = Runtime.getRuntime().exec(new String[]{"su", "-c", "echo " + freq + " > " + getCPUPath(id) + "scaling_max_freq"});
            return su.waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
