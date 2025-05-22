package com.hichemtabtech.controldcmotor;

import static com.hichemtabtech.controldcmotor.fragments.SettingsFragment.openGitHub;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.hichemtabtech.controldcmotor.databinding.ActivitySplashBinding;

/**
 * Splash screen activity that displays the app logo for a few seconds
 * before transitioning to the main activity.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DISPLAY_TIME = 2000; // 2 seconds

    private final long remainingTime = SPLASH_DISPLAY_TIME;
    private boolean isPaused = false; // To track whether the timer is paused
    private Handler handler;
    private Runnable runnable;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set full screen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // Use view binding
        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize the handler and runnable
        handler = new Handler(Looper.getMainLooper());
        runnable = () -> {
            Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        };

        // Post the delay
        handler.postDelayed(runnable, SPLASH_DISPLAY_TIME);

        // Add touch listener to pause/resume the timeout
        binding.getRoot().setOnTouchListener((View v, MotionEvent event) -> switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                // Pause the timer
                if (!isPaused) {
                    handler.removeCallbacks(runnable); // Stop the current timer
                    isPaused = true;
                }
                yield true;
            }
            case MotionEvent.ACTION_UP -> {
                // Resume the timer
                if (isPaused) {
                    handler.postDelayed(runnable, remainingTime); // Restart the remaining delay
                    isPaused = false;
                }
                yield true;
            }
            default -> false;
        });


        // Set up click listeners
        binding.tvPoweredBy.setOnClickListener(v -> openGitHub(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up to avoid memory leaks
        handler.removeCallbacks(runnable);
    }

}