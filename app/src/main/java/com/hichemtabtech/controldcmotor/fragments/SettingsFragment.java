package com.hichemtabtech.controldcmotor.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.hichemtabtech.controldcmotor.databinding.FragmentSettingsBinding;

/**
 * Fragment for handling application settings.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences preferences;
    
    // Constants for preferences
    private static final String PREFS_NAME = "DCMotorControlPrefs";
    private static final String PREF_BAUD_RATE = "baudRate";
    private static final String PREF_MESSAGE_FORMAT = "messageFormat";
    private static final String PREF_DEFAULT_DIRECTION = "defaultDirection";
    private static final String PREF_DEFAULT_SPEED = "defaultSpeed";
    
    // Default values
    private static final String DEFAULT_MESSAGE_FORMAT = "{direction},{speed}";
    private static final String DEFAULT_DIRECTION = "f";
    private static final int DEFAULT_SPEED = 0;
    
    // Baud rate options
    private final String[] baudRates = {"9600", "19200", "38400", "57600", "115200"};

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize SharedPreferences
        preferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set up baud rate spinner
        ArrayAdapter<String> baudRateAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                baudRates
        );
        baudRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerBaudRate.setAdapter(baudRateAdapter);
        
        // Set up click listeners
        binding.tvPoweredBy.setOnClickListener(v -> openGitHub((AppCompatActivity) requireActivity()));
        binding.btnSaveSettings.setOnClickListener(v -> saveSettings());
        
        // Load saved settings
        loadSettings();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Load saved settings from SharedPreferences.
     */
    private void loadSettings() {
        // Load baud rate
        String baudRate = preferences.getString(PREF_BAUD_RATE, baudRates[0]);
        for (int i = 0; i < baudRates.length; i++) {
            if (baudRates[i].equals(baudRate)) {
                binding.spinnerBaudRate.setSelection(i);
                break;
            }
        }
        
        // Load message format
        String messageFormat = preferences.getString(PREF_MESSAGE_FORMAT, DEFAULT_MESSAGE_FORMAT);
        binding.etMessageFormat.setText(messageFormat);
        
        // Load default direction
        String defaultDirection = preferences.getString(PREF_DEFAULT_DIRECTION, DEFAULT_DIRECTION);
        binding.rbForward.setChecked(defaultDirection.equals("f"));
        binding.rbBackward.setChecked(defaultDirection.equals("b"));
        
        // Load default speed
        int defaultSpeed = preferences.getInt(PREF_DEFAULT_SPEED, DEFAULT_SPEED);
        binding.sliderDefaultSpeed.setValue(defaultSpeed);
        binding.tvDefaultSpeedValue.setText(String.valueOf(defaultSpeed));
        
        // Set up speed slider listener
        binding.sliderDefaultSpeed.addOnChangeListener((slider, value, fromUser) -> binding.tvDefaultSpeedValue.setText(String.valueOf((int) value)));
    }

    /**
     * Save settings to SharedPreferences.
     */
    private void saveSettings() {
        SharedPreferences.Editor editor = preferences.edit();
        
        // Save baud rate
        String baudRate = baudRates[binding.spinnerBaudRate.getSelectedItemPosition()];
        editor.putString(PREF_BAUD_RATE, baudRate);
        
        // Save message format
        String messageFormat = binding.etMessageFormat.getText().toString();
        if (messageFormat.isEmpty()) {
            messageFormat = DEFAULT_MESSAGE_FORMAT;
        }
        editor.putString(PREF_MESSAGE_FORMAT, messageFormat);
        
        // Save default direction
        String defaultDirection = binding.rbForward.isChecked() ? "f" : "b";
        editor.putString(PREF_DEFAULT_DIRECTION, defaultDirection);
        
        // Save default speed
        int defaultSpeed = (int) binding.sliderDefaultSpeed.getValue();
        editor.putInt(PREF_DEFAULT_SPEED, defaultSpeed);
        
        // Apply changes
        editor.apply();
        
        Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show();
    }

    public static void openGitHub(AppCompatActivity activity) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HichemTab-tech"));
        activity.startActivity(browserIntent);
    }
}