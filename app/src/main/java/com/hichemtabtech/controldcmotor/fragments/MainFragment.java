package com.hichemtabtech.controldcmotor.fragments;

import static com.hichemtabtech.controldcmotor.fragments.SettingsFragment.DEFAULT_MESSAGE_FORMAT;
import static com.hichemtabtech.controldcmotor.fragments.SettingsFragment.PREFS_NAME;
import static com.hichemtabtech.controldcmotor.fragments.SettingsFragment.PREF_MESSAGE_FORMAT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.formatter.ValueFormatter;
import com.hichemtabtech.controldcmotor.MainActivity;
import com.hichemtabtech.controldcmotor.R;
import com.hichemtabtech.controldcmotor.adapters.BluetoothDeviceAdapter;
import com.hichemtabtech.controldcmotor.databinding.FragmentMainBinding;
import com.hichemtabtech.controldcmotor.interfaces.BluetoothDeviceListener;
import com.hichemtabtech.controldcmotor.utils.BluetoothConnectionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Main fragment that contains the Bluetooth section and control panel.
 */
public class MainFragment extends Fragment implements BluetoothDeviceListener {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SerialPortService ID

    private FragmentMainBinding binding;
    private BluetoothAdapter bluetoothAdapter;
    private final List<BluetoothDevice> deviceList = new ArrayList<>();
    private BluetoothDeviceAdapter deviceAdapter;

    private boolean isConnected = false;
    private boolean isRunning = false;
    private String direction = "f"; // Default direction: forward
    private int speed = 0; // Default speed: 0

    // Chart related fields
    private LineChart responseChart;
    private LineDataSet commandDataSet;
    private LineDataSet responseDataSet;
    private final ArrayList<Entry> commandEntries = new ArrayList<>();
    private final ArrayList<Entry> responseEntries = new ArrayList<>();
    private long startTime = 0;
    private float timeWindow = 3.0f; // Default time window in seconds

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Bluetooth has been enabled successfully
                    Toast.makeText(requireContext(), "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                } else {
                    // The user declined to enable Bluetooth
                    Toast.makeText(requireContext(), "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
                }
            });


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Bluetooth adapter
        BluetoothManager bluetoothManager = (BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothConnectionManager.getInstance().addCallback(new BluetoothConnectionManager.ConnectionCallback() {
            @Override
            public void onConnected(BluetoothSocket socket) {
                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    ((MainActivity)requireActivity()).onConnected(socket);
                    isConnected = true;
                    binding.tvBluetoothStatus.setText(R.string.bluetooth_connected);
                    binding.tvBluetoothStatus.setTextColor(requireContext().getColor(R.color.connected));

                    // Enable control panel
                    enableControls();
                    Toast.makeText(requireContext(), "Connected to a device (please see permissions)", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onConnectionFailed() {
                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    ((MainActivity)requireActivity()).onConnectionFailed();
                    binding.tvBluetoothStatus.setText(R.string.bluetooth_disconnected);
                    binding.tvBluetoothStatus.setTextColor(requireContext().getColor(R.color.disconnected));

                    Toast.makeText(requireContext(), R.string.error_bluetooth_connection, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onDisconnected() {
                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    isConnected = false;
                    isRunning = false;
                    binding.tvBluetoothStatus.setText(R.string.bluetooth_disconnected);
                    binding.tvBluetoothStatus.setTextColor(requireContext().getColor(R.color.disconnected));

                    // Disable control panel
                    disableControls();

                    Toast.makeText(requireContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onReceive(String receivedData) {
                //split data if ist has "speed:"
                String[] data = receivedData.split(":");
                if (data.length > 1 && data[0].equals("speed")) {
                    try {
                        // Parse the response time
                        float response = Float.parseFloat(data[1]);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> addResponseToChart(response));
                        }
                    } catch (NumberFormatException e) {
                        Log.d("MainFragment", "Invalid response time format: " + data[1]);
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up RecyclerView
        deviceAdapter = new BluetoothDeviceAdapter(deviceList, this);
        binding.rvDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDevices.setAdapter(deviceAdapter);

        // Set up click listeners
        binding.btnScan.setOnClickListener(v -> startBluetoothScan());
        binding.btnStart.setOnClickListener(v -> startMotor());
        binding.btnStop.setOnClickListener(v -> stopMotor());
        binding.btnReset.setOnClickListener(v -> resetConnection());

        // Set up chart zoom buttons
        binding.btnZoomIn.setOnClickListener(v -> changeTimeWindow(timeWindow / 2));
        binding.btnZoomOut.setOnClickListener(v -> changeTimeWindow(timeWindow * 2));

        // Set up direction switch
        binding.switchDirection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            direction = isChecked ? "b" : "f";
            binding.tvDirectionValue.setText(isChecked ? R.string.backward : R.string.forward);
        });

        // Set up speed slider
        binding.sliderSpeed.addOnChangeListener((slider, value, fromUser) -> {
            speed = (int) value;
            binding.tvSpeedValue.setText(String.valueOf(speed));

            // If motor is running, update speed in real time
            if (isRunning) {
                SharedPreferences preferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String messageFormat = preferences.getString(PREF_MESSAGE_FORMAT, DEFAULT_MESSAGE_FORMAT);
                String formattedMessage = messageFormat
                        .replace("{speed}", String.valueOf(speed))
                        .replace("{direction}", direction);
                // Send command to start motor
                sendCommand(formattedMessage);
            }
        });

        // Initialize chart
        initializeChart();

        // Check if Bluetooth is supported
        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            disableControls();
            return;
        }

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        requireActivity().registerReceiver(receiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if Bluetooth is enabled
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Unregister broadcast receiver
        try {
            requireActivity().unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }

        // Disconnect from device
        if (BluetoothConnectionManager.getInstance() != null) {
            BluetoothConnectionManager.getInstance().disconnect();
        }

        binding = null;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void startBluetoothScan() {
        // Check for permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissions();
                return;
            }
        } else {
            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        }

        // Clear previous list
        deviceList.clear();
        deviceAdapter.notifyDataSetChanged();

        // Show scanning progress
        binding.progressScanning.setVisibility(View.VISIBLE);
        binding.tvNoDevices.setVisibility(View.GONE);

        // Start discovery
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{
                            android.Manifest.permission.BLUETOOTH_SCAN,
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery found a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    if (device.getName() != null) {
                        // Add device to list if not already present
                        if (!deviceList.contains(device)) {
                            deviceList.add(device);
                            deviceAdapter.notifyItemInserted(deviceList.size() - 1);
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Discovery finished
                binding.progressScanning.setVisibility(View.GONE);

                // Show message if no devices found
                if (deviceList.isEmpty()) {
                    binding.tvNoDevices.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    @Override
    public void onConnectClick(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermissions();
            return;
        }

        // Cancel discovery as it's resource intensive
        bluetoothAdapter.cancelDiscovery();

        // Update UI to show connecting state
        binding.tvBluetoothStatus.setText(R.string.bluetooth_connecting);
        binding.tvBluetoothStatus.setTextColor(requireContext().getColor(R.color.connecting));

        // Connect to device
        BluetoothConnectionManager.getInstance().connect(device, MY_UUID);
    }

    private void startMotor() {
        if (!isConnected) return;

        SharedPreferences preferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String messageFormat = preferences.getString(PREF_MESSAGE_FORMAT, DEFAULT_MESSAGE_FORMAT);
        String formattedMessage = messageFormat
                .replace("{speed}", String.valueOf(speed))
                .replace("{direction}", direction);
        // Send command to start motor
        sendCommand(formattedMessage);

        // Update UI
        isRunning = true;
        binding.btnStart.setEnabled(false);
        binding.switchDirection.setEnabled(false);
        binding.btnStop.setEnabled(true);
    }

    private void stopMotor() {
        if (!isConnected) return;

        // Send command to stop motor
        sendCommand("s");

        // Update UI
        isRunning = false;
        binding.btnStart.setEnabled(true);
        binding.switchDirection.setEnabled(true);
        binding.btnStop.setEnabled(false);
    }

    private void resetConnection() {
        if (!isConnected) return;

        // Send reset command
        sendCommand("r");

        // Reset UI
        isRunning = false;
        binding.btnStart.setEnabled(true);
        binding.switchDirection.setEnabled(true);
        binding.btnStop.setEnabled(false);
        binding.sliderSpeed.setValue(0);
        binding.tvSpeedValue.setText("0");
        binding.switchDirection.setChecked(false);
        binding.tvDirectionValue.setText(R.string.forward);
        direction = "f";
        speed = 0;
    }

    /**
     * Change the time window for the chart.
     * 
     * @param newTimeWindow The new time window in seconds.
     */
    private void changeTimeWindow(float newTimeWindow) {
        // Limit the time window to a reasonable range (0.5 to 10 seconds)
        timeWindow = Math.max(0.5f, Math.min(10f, newTimeWindow));

        // Log the new time window
        Log.d("Chart", "Time window changed to: " + timeWindow + " seconds");

        // Update the chart immediately to reflect the new time window
        float currentTime = (System.currentTimeMillis() - startTime) / 1000f;
        float cutoffTime = currentTime - timeWindow;

        // Set visible range to show the last timeWindow seconds
        responseChart.getXAxis().setAxisMinimum(cutoffTime);
        responseChart.getXAxis().setAxisMaximum(currentTime);

        // Refresh chart
        responseChart.invalidate();
    }

    private void sendCommand(String command) {
        if (!isConnected) return;

        // Send the command
        BluetoothConnectionManager.getInstance().sendData(command);
    }

    private void enableControls() {
        binding.btnStart.setEnabled(true);
        binding.switchDirection.setEnabled(true);
        binding.sliderSpeed.setEnabled(true);
        binding.btnReset.setEnabled(true);
    }

    private void disableControls() {
        binding.btnStart.setEnabled(false);
        binding.btnStop.setEnabled(false);
        binding.switchDirection.setEnabled(false);
        binding.sliderSpeed.setEnabled(false);
        binding.btnReset.setEnabled(false);
    }

    /**
     * Initialize the response time chart.
     */
    private void initializeChart() {
        // Get chart from layout
        responseChart = binding.responseTimeChart;

        // Configure chart appearance
        responseChart.setDrawGridBackground(false);
        responseChart.setDrawBorders(false);
        responseChart.setTouchEnabled(true);
        responseChart.setDragEnabled(true);

        // Allow zooming only on X axis
        responseChart.setScaleXEnabled(true);
        responseChart.setScaleYEnabled(false);
        responseChart.setPinchZoom(false);
        responseChart.setDoubleTapToZoomEnabled(true);

        // Set description
        Description description = new Description();
        description.setText("Response over time");
        description.setTextSize(12f);
        responseChart.setDescription(description);

        // Configure X axis
        XAxis xAxis = responseChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(0.1f); // More granular for time-based data
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Format as seconds with one decimal place
                return String.format(Locale.ENGLISH, "%.1fs", value);
            }
        });

        // Configure Y axis
        YAxis leftAxis = responseChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(255f); // Set max value to 255 as requested
        leftAxis.setGranularity(25f);

        // Disable right Y axis
        responseChart.getAxisRight().setEnabled(false);

        // Initialize start time
        startTime = System.currentTimeMillis();

        // Create data sets
        commandDataSet = new LineDataSet(commandEntries, "Command");
        commandDataSet.setColor(Color.BLUE);
        commandDataSet.setCircleColor(Color.BLUE);
        commandDataSet.setLineWidth(2f);
        commandDataSet.setCircleRadius(3f);
        commandDataSet.setDrawCircleHole(false);
        commandDataSet.setValueTextSize(9f);
        commandDataSet.setDrawValues(true); // Show values

        responseDataSet = new LineDataSet(responseEntries, "Response");
        responseDataSet.setColor(Color.RED);
        responseDataSet.setCircleColor(Color.RED);
        responseDataSet.setLineWidth(2f);
        responseDataSet.setCircleRadius(3f);
        responseDataSet.setDrawCircleHole(false);
        responseDataSet.setValueTextSize(9f);
        responseDataSet.setDrawValues(true); // Show values

        // Create line data with both data sets
        LineData lineData = new LineData(commandDataSet, responseDataSet);
        responseChart.setData(lineData);

        // Refresh chart
        responseChart.invalidate();
    }

    /**
     * Add a command-response pair to the chart.
     *
     * @param response The response value.
     */
    private void addResponseToChart(float response) {
        // Calculate time in seconds since start
        float timeInSeconds = (System.currentTimeMillis() - startTime) / 1000f;

        // Add command entry (current speed from slider)
        commandEntries.add(new Entry(timeInSeconds, speed));

        // Add response entry
        responseEntries.add(new Entry(timeInSeconds, response));

        // Remove entries older than timeWindow seconds
        float cutoffTime = timeInSeconds - timeWindow;

        // Remove old command entries
        for (int i = commandEntries.size() - 1; i >= 0; i--) {
            if (commandEntries.get(i).getX() < cutoffTime) {
                commandEntries.remove(i);
            } else {
                break; // Entries are in time order, so we can stop once we find one that's recent enough
            }
        }

        // Remove old response entries
        for (int i = responseEntries.size() - 1; i >= 0; i--) {
            if (responseEntries.get(i).getX() < cutoffTime) {
                responseEntries.remove(i);
            } else {
                break; // Entries are in time order, so we can stop once we find one that's recent enough
            }
        }

        // Update data sets
        commandDataSet.notifyDataSetChanged();
        responseDataSet.notifyDataSetChanged();

        // Update chart data
        responseChart.getData().notifyDataChanged();
        responseChart.notifyDataSetChanged();

        // Set visible range to show the last timeWindow seconds
        responseChart.getXAxis().setAxisMinimum(cutoffTime);
        responseChart.getXAxis().setAxisMaximum(timeInSeconds);

        // Log the current values (since we don't have a TextView for this)
        Log.d("Chart", "Speed: " + speed + ", Response: " + response);

        // Refresh chart
        responseChart.invalidate();
    }
}
