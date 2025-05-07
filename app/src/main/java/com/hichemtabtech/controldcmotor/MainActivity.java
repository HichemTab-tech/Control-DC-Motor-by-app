package com.hichemtabtech.controldcmotor;

import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.hichemtabtech.controldcmotor.databinding.ActivityMainBinding;
import com.hichemtabtech.controldcmotor.fragments.MainFragment;
import com.hichemtabtech.controldcmotor.fragments.SettingsFragment;
import com.hichemtabtech.controldcmotor.fragments.TestFragment;
import com.hichemtabtech.controldcmotor.utils.BluetoothConnectionManager;

/**
 * Main activity that hosts the fragments for the different sections of the app.
 */
public class MainActivity extends AppCompatActivity implements BluetoothConnectionManager.ConnectionCallback {

    private ActivityMainBinding binding;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    private MainFragment mainFragment;
    private SettingsFragment settingsFragment;
    private TestFragment testFragment;

    private BluetoothConnectionManager connectionManager;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize fragments
        mainFragment = new MainFragment();
        settingsFragment = new SettingsFragment();
        testFragment = new TestFragment();

        // Initialize connection manager
        connectionManager = new BluetoothConnectionManager();

        // Initialize ViewPager and TabLayout
        viewPager = binding.viewPager;
        tabLayout = binding.tabLayout;

        // Set up ViewPager with fragments
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return mainFragment;
                    case 1:
                        return settingsFragment;
                    case 2:
                        return testFragment;
                    default:
                        return mainFragment;
                }
            }

            @Override
            public int getItemCount() {
                return 3; // Number of tabs
            }
        });

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.control_panel);
                    break;
                case 1:
                    tab.setText(R.string.settings);
                    break;
                case 2:
                    tab.setText(R.string.test);
                    break;
            }
        }).attach();

        // Set up page change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Update the test fragment with the connection manager when navigating to it
                if (position == 2) {
                    testFragment.setConnectionManager(connectionManager, isConnected);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Handle menu item clicks
        if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Show the about dialog.
     */
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("DC Motor Control App\nVersion 1.0\n\nA modern Android app for controlling DC motors via Bluetooth.")
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Called when successfully connected to a device.
     *
     * @param socket The Bluetooth socket.
     */
    @Override
    public void onConnected(BluetoothSocket socket) {
        isConnected = true;

        // Update the test fragment with the connection manager
        testFragment.setConnectionManager(connectionManager, true);
    }

    /**
     * Called when connection to a device fails.
     */
    @Override
    public void onConnectionFailed() {
        isConnected = false;

        // Update the test fragment with the connection manager
        testFragment.setConnectionManager(connectionManager, false);
    }

    /**
     * Called when disconnected from a device.
     */
    @Override
    public void onDisconnected() {
        isConnected = false;

        // Update the test fragment with the connection manager
        testFragment.setConnectionManager(connectionManager, false);
    }
}
