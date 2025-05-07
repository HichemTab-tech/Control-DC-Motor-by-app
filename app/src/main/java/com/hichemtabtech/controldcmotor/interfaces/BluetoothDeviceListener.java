package com.hichemtabtech.controldcmotor.interfaces;

import android.bluetooth.BluetoothDevice;

/**
 * Interface for handling Bluetooth device interactions.
 */
public interface BluetoothDeviceListener {
    /**
     * Called when the connect button is clicked for a Bluetooth device.
     *
     * @param device The Bluetooth device to connect to.
     */
    void onConnectClick(BluetoothDevice device);
}