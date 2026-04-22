package com.example.memegenerator.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.memegenerator.data.Project;
import com.example.memegenerator.data.ProjectDatabase;
import com.example.memegenerator.databinding.ActivityHistoryBinding;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private ProjectAdapter adapter;
    private final List<Project> projects = new ArrayList<>();

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

            ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin =
                    dp(24) + navBars.bottom;
            fab.requestLayout();

            return insets;
        });
    }

    private void setupToolbar() {

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.projects_title));
        }

    }

    private void setupList() {
        adapter = new ProjectAdapter(projects, new ProjectAdapter.Listener() {
            @Override
            public void onOpen(Project item) {
                openPreview(item);
            }

            @Override
            public void onEdit(Project item) {
                editProject(item);
            }

            @Override
            public void onDelete(Project item) {
                deleteProject(item);
            }

            @Override
            public void onRename(Project item) {
                renameProject(item);
            }
        });

        binding.imageRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.imageRecycler.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabCreate.setOnClickListener(v ->
                AsyncTask.execute(() -> {
                    int count = ProjectDatabase.getInstance(getApplicationContext())
                            .projectDao()
                            .getCount();

                    String nextName = getString(R.string.new_project_numbered, count + 1);

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
            List<Project> items = ProjectDatabase.getInstance(getApplicationContext())
                    .projectDao()
                    .getAllDesc();

            runOnUiThread(() -> {
                projects.clear();
                projects.addAll(items);
                adapter.notifyDataSetChanged();

                binding.emptyView.setVisibility(projects.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void openPreview(@NonNull Project item) {
        String path = item.previewImagePath;
        if (path == null || path.trim().isEmpty()) {
            Toast.makeText(this, R.string.preview_open_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(path), "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.image_open_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void editProject(@NonNull Project item) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("edit_project_id", item.id);
        intent.putExtra("edit_original_image_path", item.originalImagePath);
        intent.putExtra("edit_overlay_image_path", item.previewImagePath);
        intent.putExtra("edit_text_items_json", item.textItemsJson);
        intent.putExtra("edit_project_name", item.projectName);
        startActivity(intent);
    }

    private void deleteProject(@NonNull Project item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_project_title)
                .setMessage(R.string.delete_project_message)
                .setPositiveButton(R.string.action_delete, (d, w) ->
                        AsyncTask.execute(() -> {
                            ProjectDatabase.getInstance(getApplicationContext())
                                    .projectDao()
                                    .delete(item);

                            runOnUiThread(this::loadProjects);
                        })
                )
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void renameProject(@NonNull Project item) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(item.projectName != null ? item.projectName : "");
        input.setSelection(input.getText().length());

        int pad = dp(16);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle(R.string.rename_project)
                .setView(input)
                .setPositiveButton(R.string.save_action, (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, R.string.name_empty_error, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AsyncTask.execute(() -> {
                        ProjectDatabase.getInstance(getApplicationContext())
                                .projectDao()
                                .updateProjectName(item.id, newName);

                        runOnUiThread(this::loadProjects);
                    });
                })
                .setNegativeButton(R.string.action_cancel, null)
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