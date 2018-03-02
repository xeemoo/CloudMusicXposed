package cn.com.zhjnc.cloudmusicxposed.ui;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

import java.io.File;

import cn.com.zhjnc.cloudmusicxposed.R;

import static android.content.Context.MODE_WORLD_READABLE;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setWorldReadable();
        //getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.pref);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SetWorldReadable")
    private void setWorldReadable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File dataDir = new File(getActivity().getApplicationInfo().dataDir);
            File prefsDir = new File(dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, getPreferenceManager().getSharedPreferencesName() + ".xml");
            if (prefsFile.exists()) {
                for (File file : new File[]{dataDir, prefsDir, prefsFile}) {
                    file.setReadable(true, false);
                    file.setExecutable(true, false);
                }
            }
        } else {
            getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_READABLE);
        }
    }
}
