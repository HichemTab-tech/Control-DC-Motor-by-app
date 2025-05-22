package com.hichemtabtech.controldcmotor.fragments;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.hichemtabtech.controldcmotor.MainActivity;
import com.hichemtabtech.controldcmotor.R;
import com.hichemtabtech.controldcmotor.databinding.FragmentTestBinding;
import com.hichemtabtech.controldcmotor.utils.BluetoothConnectionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment for testing Bluetooth communication by sending manual commands.
 */
public class TestFragment extends Fragment {

    private FragmentTestBinding binding;
    private SpannableStringBuilder terminalOutput;
    private boolean isConnected = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        terminalOutput = new SpannableStringBuilder();
        BluetoothConnectionManager.getInstance().addCallback(new BluetoothConnectionManager.ConnectionCallback() {
            @Override
            public void onConnected(BluetoothSocket socket) {

            }

            @Override
            public void onConnectionFailed() {

            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onReceive(String receivedData) {
                appendToTerminal(receivedData, TerminalMessageType.RECEIVED);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTestBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set up click listeners
        binding.btnSendCommand.setOnClickListener(v -> sendCommand());
        binding.btnClear.setOnClickListener(v -> clearTerminal());
        
        // Initialize terminal
        binding.tvTerminal.setText(terminalOutput);
        
        // Add initial message
        appendToTerminal("Terminal ready. Connect to a device from the Control Panel tab to start sending commands.", TerminalMessageType.SYSTEM);


        MainActivity mainActivity = (MainActivity) requireActivity();
        mainActivity.getConnectionLiveData().observe(getViewLifecycleOwner(), socket -> {
            this.isConnected = socket != null;

            Log.d("TestFragment.log", "binding: " + (binding != null));

            // Update UI on the main thread
            if (binding != null) {
                Log.d("TestFragment.log", "setConnectionManager: " + this.isConnected);
                binding.btnSendCommand.setEnabled(this.isConnected);

                if (this.isConnected) {
                    appendToTerminal("Connected to device. Ready to send commands.", TerminalMessageType.SYSTEM);
                } else {
                    appendToTerminal("Disconnected from device.", TerminalMessageType.SYSTEM);
                }
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Send a command to the connected Bluetooth device.
     */
    private void sendCommand() {
        if (!isConnected || BluetoothConnectionManager.getInstance() == null) {
            Toast.makeText(requireContext(), "Not connected to any device", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String command = binding.etCommand.getText().toString().trim();
        if (command.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a command", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Send the command
        BluetoothConnectionManager.getInstance().sendData(command);
        
        // Add to terminal
        appendToTerminal("Sent: " + command, TerminalMessageType.SENT);
        
        // Clear the input field
        binding.etCommand.setText("");
    }

    /**
     * Clear the terminal output.
     */
    private void clearTerminal() {
        terminalOutput = new SpannableStringBuilder();
        binding.tvTerminal.setText(terminalOutput);
        
        // Add initial message
        appendToTerminal("Terminal cleared.", TerminalMessageType.SYSTEM);
    }

    /**
     * Add a message to the terminal with appropriate formatting.
     *
     * @param message The message to add.
     * @param type The type of message (sent, received, or system).
     */
    public void appendToTerminal(String message, TerminalMessageType type) {
        if (binding == null) return;
        
        // Get current timestamp
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        
        // Create the formatted message
        String formattedMessage = "[" + timestamp + "] ";
        
        switch (type) {
            case SENT:
                formattedMessage += ">> " + message;
                break;
            case RECEIVED:
                formattedMessage += "<< " + message;
                break;
            case SYSTEM:
                formattedMessage += "-- " + message;
                break;
        }
        
        formattedMessage += "\n";
        
        // Create a spannable string with the appropriate color
        SpannableString spannable = new SpannableString(formattedMessage);
        int color;
        
        switch (type) {
            case SENT:
                color = ContextCompat.getColor(requireContext(), R.color.primary);
                break;
            case RECEIVED:
                color = ContextCompat.getColor(requireContext(), R.color.accent);
                break;
            case SYSTEM:
                color = ContextCompat.getColor(requireContext(), R.color.text_secondary);
                break;
            default:
                color = ContextCompat.getColor(requireContext(), R.color.text_primary);
                break;
        }
        
        spannable.setSpan(new ForegroundColorSpan(color), 0, formattedMessage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        // Append to the terminal
        terminalOutput.append(spannable);
        binding.tvTerminal.setText(terminalOutput);
        
        // Scroll to the bottom
        binding.tvTerminal.post(() -> {
            final int scrollAmount = binding.tvTerminal.getLayout().getLineTop(binding.tvTerminal.getLineCount()) - binding.tvTerminal.getHeight();
            if (scrollAmount > 0) {
                binding.tvTerminal.scrollTo(0, scrollAmount);
            } else {
                binding.tvTerminal.scrollTo(0, 0);
            }
        });
    }

    /**
     * Enum for terminal message types.
     */
    public enum TerminalMessageType {
        SENT,
        RECEIVED,
        SYSTEM
    }
}