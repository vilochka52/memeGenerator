package com.example.memegenerator.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.memegenerator.MainActivity;
import com.example.memegenerator.R;
import com.example.memegenerator.data.Meme;
import com.example.memegenerator.data.MemeDatabase;
import com.example.memegenerator.databinding.ActivityHistoryBinding;

import java.util.ArrayList;
import java.util.List;
import android.view.ViewGroup;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private MemeAdapter adapter;
    private final List<Meme> memes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupInsets();
        setupToolbar();
        setupList();
        setupFab();
        loadProjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets statusBars =
                    insets.getInsets(WindowInsetsCompat.Type.statusBars());
            androidx.core.graphics.Insets navBars =
                    insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            binding.appbar.setPadding(
                    binding.appbar.getPaddingLeft(),
                    statusBars.top,
                    binding.appbar.getPaddingRight(),
                    binding.appbar.getPaddingBottom()
            );

            binding.imageRecycler.setPadding(
                    binding.imageRecycler.getPaddingLeft(),
                    binding.imageRecycler.getPaddingTop(),
                    binding.imageRecycler.getPaddingRight(),
                    navBars.bottom + dp(90)
            );

            View fab = binding.fabCreate;
            fab.setPadding(
                    fab.getPaddingLeft(),
                    fab.getPaddingTop(),
                    fab.getPaddingRight(),
                    fab.getPaddingBottom()
            );
            ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin = dp(24) + navBars.bottom;
            fab.requestLayout();

            return insets;
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.historyTopBar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Мои проекты");
        }

        binding.historyTopBar.setNavigationOnClickListener(v -> finish());
        binding.historyTopBar.setNavigationIconTint(
                ContextCompat.getColor(this, R.color.on_surface)
        );
        binding.historyTopBar.setTitleTextColor(
                ContextCompat.getColor(this, R.color.on_surface)
        );
    }

    private void setupList() {
        adapter = new MemeAdapter(memes, new MemeAdapter.Listener() {
            @Override
            public void onOpen(Meme item) {
                openPreview(item);
            }

            @Override
            public void onEdit(Meme item) {
                editProject(item);
            }

            @Override
            public void onDelete(Meme item) {
                deleteProject(item);
            }

            @Override
            public void onRename(Meme item) {
                renameProject(item);
            }
        });

        binding.imageRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.imageRecycler.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabCreate.setOnClickListener(v ->
                AsyncTask.execute(() -> {
                    int count = MemeDatabase.getInstance(getApplicationContext())
                            .memeDao()
                            .getCount();

                    String nextName = "Новый проект " + (count + 1);

                    runOnUiThread(() -> {
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("new_project_name", nextName);
                        startActivity(intent);
                    });
                })
        );
    }

    private void loadProjects() {
        AsyncTask.execute(() -> {
            List<Meme> items = MemeDatabase.getInstance(getApplicationContext())
                    .memeDao()
                    .getAllDesc();

            runOnUiThread(() -> {
                memes.clear();
                memes.addAll(items);
                adapter.notifyDataSetChanged();

                binding.emptyView.setVisibility(memes.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void openPreview(@NonNull Meme item) {
        String path = item.previewImagePath;
        if (path == null || path.trim().isEmpty()) {
            Toast.makeText(this, "Не удалось открыть превью", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(path), "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть изображение", Toast.LENGTH_SHORT).show();
        }
    }

    private void editProject(@NonNull Meme item) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("edit_meme_id", item.id);
        intent.putExtra("edit_original_image_path", item.originalImagePath);
        intent.putExtra("edit_overlay_image_path", item.previewImagePath);
        intent.putExtra("edit_text_items_json", item.textItemsJson);
        startActivity(intent);
    }

    private void deleteProject(@NonNull Meme item) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить проект?")
                .setMessage("Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (d, w) ->
                        AsyncTask.execute(() -> {
                            MemeDatabase.getInstance(getApplicationContext())
                                    .memeDao()
                                    .delete(item);

                            runOnUiThread(this::loadProjects);
                        })
                )
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void renameProject(@NonNull Meme item) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(item.projectName != null ? item.projectName : "");
        input.setSelection(input.getText().length());

        int pad = dp(16);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Название проекта")
                .setView(input)
                .setPositiveButton("Сохранить", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AsyncTask.execute(() -> {
                        MemeDatabase.getInstance(getApplicationContext())
                                .memeDao()
                                .updateProjectName(item.id, newName);

                        runOnUiThread(this::loadProjects);
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

}