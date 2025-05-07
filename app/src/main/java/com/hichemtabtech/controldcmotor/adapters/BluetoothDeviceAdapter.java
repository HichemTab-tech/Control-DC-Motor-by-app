package com.hichemtabtech.controldcmotor.adapters;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hichemtabtech.controldcmotor.R;
import com.hichemtabtech.controldcmotor.interfaces.BluetoothDeviceListener;

import java.util.List;

/**
 * Adapter for displaying Bluetooth devices in a RecyclerView.
 */
public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {

    private final List<BluetoothDevice> devices;
    private final BluetoothDeviceListener listener;

    public BluetoothDeviceAdapter(List<BluetoothDevice> devices, BluetoothDeviceListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bluetooth_device, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        
        // Check for Bluetooth permissions
        if (ActivityCompat.checkSelfPermission(holder.itemView.getContext(),
                android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            
            // Set device name and address
            holder.tvDeviceName.setText(device.getName() != null ? device.getName() : "Unknown Device");
            holder.tvDeviceAddress.setText(device.getAddress());
            
            // Set click listener for connect button
            holder.btnConnect.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConnectClick(device);
                }
            });
        } else {
            // Handle missing permissions
            holder.tvDeviceName.setText("Unknown Device");
            holder.tvDeviceAddress.setText("Permission required");
            holder.btnConnect.setEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    /**
     * ViewHolder for Bluetooth device items.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceAddress;
        Button btnConnect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
            btnConnect = itemView.findViewById(R.id.btnConnect);
        }
    }
}