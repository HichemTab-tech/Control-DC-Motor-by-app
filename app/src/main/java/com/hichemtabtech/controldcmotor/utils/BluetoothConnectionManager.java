package com.hichemtabtech.controldcmotor.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    private final ExecutorService executor;
    private boolean isConnected = false;
    private ConnectionCallback callback;
    private boolean stopReading = false;

    private static BluetoothConnectionManager instance;

    private ArrayList<ConnectionCallback> callbacks = new ArrayList<>();

    public BluetoothConnectionManager() {
        executor = Executors.newSingleThreadExecutor();
        callback = new ConnectionCallback() {
            @Override
            public void onConnected(BluetoothSocket socket) {
                for (ConnectionCallback callback : callbacks) {
                    callback.onConnected(socket);
                }
            }

            @Override
            public void onConnectionFailed() {
                for (ConnectionCallback callback : callbacks) {
                    callback.onConnectionFailed();
                }
            }

            @Override
            public void onDisconnected() {
                for (ConnectionCallback callback : callbacks) {
                    callback.onDisconnected();
                }
            }

            @Override
            public void onReceive(String receivedData) {
                ConnectionCallback.super.onReceive(receivedData);
                for (ConnectionCallback callback : callbacks) {
                    callback.onReceive(receivedData);
                }
            }
        };
    }

    public static BluetoothConnectionManager getInstance() {
        if (instance == null) {
            // Create the instance if it doesn't exist
            instance = new BluetoothConnectionManager();
        }
        return instance; // Return the same instance every time
    }

    public void addCallback(ConnectionCallback callback) {
        callbacks.add(callback);
    }

    public void removeCallback(ConnectionCallback callback) {
        callbacks.remove(callback);
    }



    /**
     * Connect to a Bluetooth device.
     *
     * @param device The Bluetooth device to connect to.
     * @param uuid The UUID for the connection.
     */
    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    public void connect(BluetoothDevice device, UUID uuid) {

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

    // Map to store command send times
    private final Map<String, Long> commandSendTimes = new HashMap<>();

    /**
     * Send data to the connected Bluetooth device with response time tracking.
     *
     * @param data The data to send.
     */
    public void sendData(String data) {
        if (!isConnected || outputStream == null) {
            Log.e(TAG, "Error sending data: not connected");
            Log.e(TAG, "isConnected: " + isConnected);
            Log.e(TAG, "outputstream: " + outputStream);
            return;
        }
        Log.e(TAG, "data sent.");

        try {
            // Record send time
            long sendTime = System.currentTimeMillis();

            // Store send time if callback is provided
            if (callback != null) {
                commandSendTimes.put(data, sendTime);
            }

            // Add newline character to end of message
            String message = data + "\n";
            outputStream.write(message.getBytes());
            outputStream.flush();

            Log.d(TAG, "Data sent: " + data);
        } catch (IOException e) {
            Log.e(TAG, "Error sending data: " + e.getMessage());
            handleDisconnection();
        }
    }

    /**
     * Start reading data from the connected Bluetooth device.
     */
    private void startReading() {
        if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
            Log.e(TAG, "Socket is not connected, cannot start reading.");
            return;
        }

        stopReading = false; // Reset stopReading flag to allow reading
        Handler mainHandler = new Handler(Looper.getMainLooper());


        executor.execute(() -> {
            try {
                inputStream = bluetoothSocket.getInputStream(); // Ensure stream is initialized
                byte[] buffer = new byte[1024]; // Buffer for received data
                int bytes;

                while (!stopReading) { // Keep listening until stopped
                    // Check if data is available to read
                    if ((bytes = inputStream.read(buffer)) > 0) {
                        String receivedData = new String(buffer, 0, bytes); // Convert bytes to string
                        Log.d(TAG, "Received: " + receivedData);

                        // Notify/further process received data (optional)
                        if (callback != null) {
                            mainHandler.post(() -> {
                                callback.onReceive(receivedData);
                            });
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading input stream: " + e.getMessage());
                handleDisconnection();
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

        /**
         * Called when disconnected from a device.
         */
        default void onReceive(String receivedData) {
            Log.d("RECEIVED", receivedData);
        }
    }
}
