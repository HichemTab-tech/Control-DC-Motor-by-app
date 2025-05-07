package com.hichemtabtech.controldcmotor.fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.hichemtabtech.controldcmotor.R;
import com.hichemtabtech.controldcmotor.adapters.BluetoothDeviceAdapter;
import com.hichemtabtech.controldcmotor.databinding.FragmentMainBinding;
import com.hichemtabtech.controldcmotor.interfaces.BluetoothDeviceListener;
import com.hichemtabtech.controldcmotor.utils.BluetoothConnectionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main fragment that contains the Bluetooth section and control panel.
 */
public class MainFragment extends Fragment implements BluetoothDeviceListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SerialPortService ID

    private FragmentMainBinding binding;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothConnectionManager connectionManager;
    private List<BluetoothDevice> deviceList = new ArrayList<>();
    private BluetoothDeviceAdapter deviceAdapter;
    
    private boolean isConnected = false;
    private boolean isRunning = false;
    private String direction = "f"; // Default direction: forward
    private int speed = 0; // Default speed: 0

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize Bluetooth adapter
        BluetoothManager bluetoothManager = (BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        
        // Initialize connection manager
        connectionManager = new BluetoothConnectionManager();
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
                sendCommand(direction + "," + speed);
            }
        });
        
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
            if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                requestBluetoothPermissions();
            }
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
        if (connectionManager != null) {
            connectionManager.disconnect();
        }
        
        binding = null;
    }

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
                            deviceAdapter.notifyDataSetChanged();
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
        connectionManager.connect(device, MY_UUID, new BluetoothConnectionManager.ConnectionCallback() {
            @Override
            public void onConnected(BluetoothSocket socket) {
                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    isConnected = true;
                    binding.tvBluetoothStatus.setText(R.string.bluetooth_connected);
                    binding.tvBluetoothStatus.setTextColor(requireContext().getColor(R.color.connected));
                    
                    // Enable control panel
                    enableControls();
                    
                    Toast.makeText(requireContext(), "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onConnectionFailed() {
                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
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
        });
    }

    private void startMotor() {
        if (!isConnected) return;
        
        // Send command to start motor
        sendCommand(direction + "," + speed);
        
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

    private void sendCommand(String command) {
        if (!isConnected) return;
        
        connectionManager.sendData(command);
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
}