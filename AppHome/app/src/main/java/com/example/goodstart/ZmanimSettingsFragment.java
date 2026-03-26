package com.example.goodstart;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class ZmanimSettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "ZmanimSettings";

    public static ZmanimSettingsFragment newInstance() {
        return new ZmanimSettingsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_zmanim_settings, container, false);

        setupSwitch(view.findViewById(R.id.switchSunrise), "show_sunrise", true);
        setupSwitch(view.findViewById(R.id.switchShmaGra), "show_shma_gra", true);
        setupSwitch(view.findViewById(R.id.switchShmaMga), "show_shma_mga", false);
        setupSwitch(view.findViewById(R.id.switchTfilaGra), "show_tfila_gra", true);
        setupSwitch(view.findViewById(R.id.switchSunset), "show_sunset", true);

        EditText editOffset = view.findViewById(R.id.editAlarmOffset);
        editOffset.setText(String.valueOf(prefs.getInt("alarm_offset", 0)));
        editOffset.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                try {
                    prefs.edit().putInt("alarm_offset", Integer.parseInt(s.toString())).apply();
                } catch (Exception ignored) {}
            }
        });

        // Auto-scroll speed for Rambam reader (stored in RambamPrefs, values 0-9 → speed 1-10)
        SharedPreferences rambamPrefs =
                requireContext().getSharedPreferences("RambamPrefs", Context.MODE_PRIVATE);
        SeekBar seekSpeed = view.findViewById(R.id.seekAutoScrollSpeed);
        TextView speedLabel = view.findViewById(R.id.autoScrollSpeedLabel);

        int savedSpeed = rambamPrefs.getInt("auto_scroll_speed", 3);
        seekSpeed.setProgress(savedSpeed);
        speedLabel.setText(String.valueOf(savedSpeed + 1));

        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speedLabel.setText(String.valueOf(progress + 1));
                rambamPrefs.edit().putInt("auto_scroll_speed", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Text size for Rambam reader (stored as sp value, 14–28; seekBar 0-7 → step 2sp each)
        SeekBar seekTextSize = view.findViewById(R.id.seekTextSize);
        TextView textSizeLabel = view.findViewById(R.id.textSizeLabel);

        int savedTextSizeSp = rambamPrefs.getInt("text_size_sp", 20);
        int textSizeProgress = Math.max(0, Math.min(7, (savedTextSizeSp - 14) / 2));
        seekTextSize.setProgress(textSizeProgress);
        textSizeLabel.setText(String.valueOf(savedTextSizeSp));

        seekTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int sp = 14 + progress * 2; // 14, 16, 18, 20, 22, 24, 26, 28
                textSizeLabel.setText(String.valueOf(sp));
                rambamPrefs.edit().putInt("text_size_sp", sp).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        return view;
    }

    private void setupSwitch(SwitchMaterial sw, String key, boolean defaultValue) {
        sw.setChecked(prefs.getBoolean(key, defaultValue));
        sw.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(key, isChecked).apply());
    }
}

