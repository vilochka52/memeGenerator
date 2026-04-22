package com.example.memegenerator;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.memegenerator.databinding.ActivityWelcomeBinding;
import com.example.memegenerator.ui.HistoryActivity;

public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.root, (v, insets) -> {
            androidx.core.graphics.Insets statusBars =
                    insets.getInsets(WindowInsetsCompat.Type.statusBars());
            androidx.core.graphics.Insets navBars =
                    insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            binding.root.setPadding(
                    binding.root.getPaddingLeft(),
                    24 + statusBars.top,
                    binding.root.getPaddingRight(),
                    24 + navBars.bottom
            );

            return insets;
        });

        binding.btnStart.setOnClickListener(v -> openProjects());
    }

    private void openProjects() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
        finish();
    }
}