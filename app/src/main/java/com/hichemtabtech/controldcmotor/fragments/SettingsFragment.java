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

import com.hichemtabtech.controldcmotor.R;
import com.hichemtabtech.controldcmotor.databinding.FragmentSettingsBinding;

import yuku.ambilwarna.AmbilWarnaDialog;


/**
 * Fragment for handling application settings.
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences preferences;

    // Constants for preferences
    public static final String PREFS_NAME = "DCMotorControlPrefs";
    private static final String PREF_BAUD_RATE = "baudRate";
    private static final String PREF_MESSAGE_FORMAT = "messageFormat";
    private static final String PREF_DEFAULT_DIRECTION = "defaultDirection";
    private static final String PREF_DEFAULT_SPEED = "defaultSpeed";
    public static final String PREF_THEME_COLOR = "themeColor";

    // Default values
    private static final String DEFAULT_MESSAGE_FORMAT = "{direction},{speed}";
    private static final String DEFAULT_DIRECTION = "f";
    private static final int DEFAULT_SPEED = 0;

    // Baud rate options
    private final String[] baudRates = {"9600", "19200", "38400", "57600", "115200"};

    private int currentColor;

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

        binding.btnPickColor.setOnClickListener(v -> {
            AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(requireContext(), currentColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onCancel(AmbilWarnaDialog dialog) {
                    // Do nothing if the dialog is canceled
                }

                @Override
                public void onOk(AmbilWarnaDialog dialog, int selectedColor) {

                    // Update the preview or app theme
                    updateColorPreview(selectedColor);

                    // Set the newly selected color as the current color
                    currentColor = selectedColor;
                }
            });

            // Show the dialog
            colorPicker.show();

        });

        // Load saved settings
        loadSettings();
    }

    /**
     * Update the color preview based on the selected color.
     *
     * @param color The color
     */
    private void updateColorPreview(int color) {
        binding.colorPreview.setBackgroundColor(color);
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

        // Load theme color
        int themeColor = preferences.getInt(PREF_THEME_COLOR, getResources().getColor(R.color.primary, requireActivity().getTheme()));
        currentColor = themeColor;
        // Update color preview
        updateColorPreview(themeColor);
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

        // Save theme color*
        int oldColor = preferences.getInt(PREF_THEME_COLOR, getResources().getColor(R.color.primary, requireActivity().getTheme()));
        editor.putInt(PREF_THEME_COLOR, currentColor);

        // Apply changes
        editor.apply();

        // Apply theme color to the app
        applyThemeColor(currentColor);

        Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show();
        if (oldColor != currentColor) {
            requireActivity().recreate();
        }
    }

    /**
     * Apply the selected theme color to the app.
     *
     * @param color The color.
     */
    private void applyThemeColor(int color) {
        // This method would normally update the app's theme
        // For this implementation, we'll just update the UI elements that use the theme color
        // In a real app, you would use a ThemeManager or similar to handle this

        // For now, we'll just show a toast message
        Toast.makeText(requireContext(), "Theme color changed to " + color, Toast.LENGTH_SHORT).show();

        // In a real implementation, you would update the app's theme here
        // For example:
        // ThemeManager.applyTheme(requireActivity(), colorName);
    }

    public static void openGitHub(AppCompatActivity activity) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HichemTab-tech"));
        activity.startActivity(browserIntent);
    }
}
