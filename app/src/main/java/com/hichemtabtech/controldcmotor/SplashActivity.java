package com.hichemtabtech.controldcmotor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.hichemtabtech.controldcmotor.databinding.ActivitySplashBinding;

/**
 * Splash screen activity that displays the app logo for a few seconds
 * before transitioning to the main activity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DISPLAY_TIME = 2000; // 2 seconds
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set full screen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // Use view binding
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Delay for SPLASH_DISPLAY_TIME and then start MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        }, SPLASH_DISPLAY_TIME);
    }
}