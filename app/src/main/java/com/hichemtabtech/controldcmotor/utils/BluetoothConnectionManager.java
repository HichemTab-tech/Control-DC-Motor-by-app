package com.hichemtabtech.controldcmotor.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager class for handling Bluetooth connections and data transfer.
 */
public class BluetoothConnectionManager {
    private static final String TAG = "BluetoothConnectionMgr";
    
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ExecutorService executor;
    private boolean isConnected = false;
    private ConnectionCallback callback;
    private boolean stopReading = false;
    
    public BluetoothConnectionManager() {
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Connect to a Bluetooth device.
     *
     * @param device The Bluetooth device to connect to.
     * @param uuid The UUID for the connection.
     * @param callback The callback to notify of connection events.
     */
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public void connect(BluetoothDevice device, UUID uuid, ConnectionCallback callback) {
        this.callback = callback;
        
        // Disconnect any existing connection
        disconnect();
        
        // Connect to the device in a background thread
        executor.execute(() -> {
            try {
                // Create a socket connection
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();
                
                // Get the input and output streams
                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                
                // Mark as connected
                isConnected = true;
                
                // Notify of successful connection
                if (callback != null) {
                    callback.onConnected(bluetoothSocket);
                }
                
                // Start reading data
                startReading();
                
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to device: " + e.getMessage());
                
                // Close resources
                closeConnection();
                
                // Notify of connection failure
                if (callback != null) {
                    callback.onConnectionFailed();
                }
            }
        });
    }
    
    /**
     * Disconnect from the current Bluetooth device.
     */
    public void disconnect() {
        stopReading = true;
        closeConnection();
    }
    
    /**
     * Send data to the connected Bluetooth device.
     *
     * @param data The data to send.
     */
    public void sendData(String data) {
        if (!isConnected || outputStream == null) {
            return;
        }
        
        executor.execute(() -> {
            try {
                // Add newline character to end of message
                String message = data + "\n";
                outputStream.write(message.getBytes());
                outputStream.flush();
                
                Log.d(TAG, "Data sent: " + data);
            } catch (IOException e) {
                Log.e(TAG, "Error sending data: " + e.getMessage());
                handleDisconnection();
            }
        });
    }
    
    /**
     * Start reading data from the connected Bluetooth device.
     */
    private void startReading() {
        stopReading = false;
        
        executor.execute(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            
            // Keep reading data until stopped
            while (!stopReading && isConnected) {
                try {
                    // Read data from the input stream
                    bytes = inputStream.read(buffer);
                    
                    if (bytes > 0) {
                        // Convert the bytes to a string
                        String data = new String(buffer, 0, bytes);
                        Log.d(TAG, "Data received: " + data);
                        
                        // Process the data (not implemented in this example)
                        // processReceivedData(data);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading data: " + e.getMessage());
                    handleDisconnection();
                    break;
                }
            }
        });
    }
    
    /**
     * Close the Bluetooth connection and release resources.
     */
    private void closeConnection() {
        isConnected = false;
        
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing connection: " + e.getMessage());
        }
    }
    
    /**
     * Handle a disconnection event.
     */
    private void handleDisconnection() {
        if (!isConnected) {
            return;
        }
        
        closeConnection();
        
        // Notify of disconnection on the main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) {
                callback.onDisconnected();
            }
        });
    }
    
    /**
     * Interface for connection callbacks.
     */
    public interface ConnectionCallback {
        /**
         * Called when successfully connected to a device.
         *
         * @param socket The Bluetooth socket.
         */
        void onConnected(BluetoothSocket socket);
        
        /**
         * Called when connection to a device fails.
         */
        void onConnectionFailed();
        
        /**
         * Called when disconnected from a device.
         */
        void onDisconnected();
    }
}