package com.hichemtabtech.controldcmotor;

import static com.hichemtabtech.controldcmotor.fragments.SettingsFragment.PREFS_NAME;
import static com.hichemtabtech.controldcmotor.fragments.SettingsFragment.PREF_THEME_COLOR;

import android.app.AlertDialog;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.hichemtabtech.controldcmotor.databinding.ActivityMainBinding;
import com.hichemtabtech.controldcmotor.fragments.MainFragment;
import com.hichemtabtech.controldcmotor.fragments.SettingsFragment;
import com.hichemtabtech.controldcmotor.fragments.TestFragment;
import com.hichemtabtech.controldcmotor.utils.BluetoothConnectionManager;

import java.util.Objects;

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

    private FragmentStateAdapter pagerAdapter;
    private TabLayoutMediator tabLayoutMediator;

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

        // Create a custom adapter for the ViewPager
        FragmentStateAdapter pagerAdapter = new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return switch (position) {
                    case 1 -> settingsFragment;
                    case 2 -> testFragment;
                    default -> mainFragment;
                };
            }

            @Override
            public int getItemCount() {
                // When not connected, only show the main fragment
                return isConnected ? 3 : 2;
            }
        };

        // Set the adapter
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager
        TabLayoutMediator tabLayoutMediator = getTabLayoutMediator();

        // Store the adapter and mediator for later use
        this.pagerAdapter = pagerAdapter;
        this.tabLayoutMediator = tabLayoutMediator;

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

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int themeColor = preferences.getInt(PREF_THEME_COLOR, getResources().getColor(R.color.primary, getTheme()));
        findViewById(R.id.toolbar).setBackgroundColor(Color.TRANSPARENT);
        findViewById(R.id.headerLayout).setBackgroundColor(themeColor);
        findViewById(R.id.tabLayout).setBackgroundColor(themeColor);
        // from themeColor
        Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(new ColorDrawable(themeColor));
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        appBarLayout.setBackgroundColor(themeColor);

    }

    @NonNull
    private TabLayoutMediator getTabLayoutMediator() {
        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
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
        });
        tabLayoutMediator.attach();
        return tabLayoutMediator;
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
        SpannableString message = new SpannableString(
                "DC Motor Control App\nVersion 1.0\n\nA modern Android app for controlling DC motors via Bluetooth.\n\nVisit GitHub Repository"
        );

        // Set a clickable span for the "Visit GitHub Repository" text
        message.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Open your GitHub repo in the browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HichemTab-tech/Control-DC-Motor-by-app"));
                widget.getContext().startActivity(browserIntent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.BLUE); // Set link color
                ds.setUnderlineText(true); // Underline the link
            }
        }, message.length() - "Visit GitHub Repository".length(), message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .create();

        // Required for clickable spans in a TextView
        dialog.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

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

        // Update the ViewPager adapter to show all tabs
        updateViewPagerForConnectionState();
    }

    /**
     * Called when connection to a device fails.
     */
    @Override
    public void onConnectionFailed() {
        isConnected = false;

        // Update the test fragment with the connection manager
        testFragment.setConnectionManager(connectionManager, false);

        // Update the ViewPager adapter to show only the main tab
        updateViewPagerForConnectionState();
    }

    /**
     * Called when disconnected from a device.
     */
    @Override
    public void onDisconnected() {
        isConnected = false;

        // Update the test fragment with the connection manager
        testFragment.setConnectionManager(connectionManager, false);

        // Update the ViewPager adapter to show only the main tab
        updateViewPagerForConnectionState();
    }

    /**
     * Update the ViewPager adapter based on the connection state.
     */
    private void updateViewPagerForConnectionState() {
        // Save the current position
        int currentPosition = viewPager.getCurrentItem();

        // Detach the mediator
        tabLayoutMediator.detach();

        // Notify the adapter that the data set has changed
        pagerAdapter.notifyDataSetChanged();

        // Reattach the mediator
        tabLayoutMediator.attach();

        // Set the current position (if it's valid)
        if (currentPosition < pagerAdapter.getItemCount()) {
            viewPager.setCurrentItem(currentPosition, false);
        } else {
            viewPager.setCurrentItem(0, false);
        }
    }
}
