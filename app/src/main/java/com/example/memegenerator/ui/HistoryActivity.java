package com.example.memegenerator.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.memegenerator.R;
import com.example.memegenerator.data.Project;
import com.example.memegenerator.data.ProjectDatabase;
import com.example.memegenerator.databinding.ActivityHistoryBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.example.memegenerator.MainActivity;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private ProjectAdapter adapter;

    private final List<Project> allProjects = new ArrayList<>();
    private final List<ProjectListItem> visibleItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupInsets();
        setupList();
        setupFab();
        setupSearchAndFilters();
        animateEntrance();
        loadProjects();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }

    private void animateEntrance() {
        binding.headerBlock.setAlpha(0f);
        binding.headerBlock.setTranslationY(24f);
        binding.searchLayout.setAlpha(0f);
        binding.searchLayout.setTranslationY(24f);
        binding.chipsScroll.setAlpha(0f);
        binding.chipsScroll.setTranslationY(24f);
        binding.fabCreate.setAlpha(0f);
        binding.fabCreate.setTranslationY(40f);

        binding.headerBlock.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260)
                .start();

        binding.searchLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260)
                .setStartDelay(70)
                .start();

        binding.chipsScroll.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260)
                .setStartDelay(120)
                .start();

        binding.fabCreate.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(280)
                .setStartDelay(180)
                .start();
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, (v, insets) -> {
            androidx.core.graphics.Insets statusBars =
                    insets.getInsets(WindowInsetsCompat.Type.statusBars());
            androidx.core.graphics.Insets navBars =
                    insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            binding.headerBlock.setPadding(
                    binding.headerBlock.getPaddingLeft(),
                    statusBars.top + dp(8),
                    binding.headerBlock.getPaddingRight(),
                    binding.headerBlock.getPaddingBottom()
            );

            binding.imageRecycler.setPadding(
                    binding.imageRecycler.getPaddingLeft(),
                    binding.imageRecycler.getPaddingTop(),
                    binding.imageRecycler.getPaddingRight(),
                    navBars.bottom + dp(110)
            );

            View fab = binding.fabCreate;
            ((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin =
                    dp(24) + navBars.bottom;
            fab.requestLayout();

            return insets;
        });
    }

    private void setupList() {
        adapter = new ProjectAdapter(visibleItems, new ProjectAdapter.Listener() {
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
        binding.imageRecycler.setLayoutAnimation(
                AnimationUtils.loadLayoutAnimation(this, R.anim.layout_fall_down)
        );
    }

    private void setupFab() {
        binding.fabCreate.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(70)
                    .withEndAction(() -> v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(90)
                            .start())
                    .start();

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
            });
        });
    }

    private void setupSearchAndFilters() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilters());
    }

    private void loadProjects() {
        AsyncTask.execute(() -> {
            List<Project> items = ProjectDatabase.getInstance(getApplicationContext())
                    .projectDao()
                    .getAllDesc();

            runOnUiThread(() -> {
                allProjects.clear();
                allProjects.addAll(items);
                applyFilters();
            });
        });
    }

    private void applyFilters() {
        String query = "";
        if (binding.searchInput.getText() != null) {
            query = binding.searchInput.getText().toString().trim().toLowerCase(Locale.getDefault());
        }

        long now = System.currentTimeMillis();
        long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;

        List<Project> filtered = new ArrayList<>();

        for (Project project : allProjects) {
            boolean matchesSearch = query.isEmpty()
                    || (project.projectName != null
                    && project.projectName.toLowerCase(Locale.getDefault()).contains(query));

            if (!matchesSearch) continue;

            int checkedId = binding.filterChips.getCheckedChipId();
            boolean matchesChip = true;

            if (checkedId == R.id.chipRecent) {
                matchesChip = project.createdAt >= (now - sevenDaysMs);
            } else if (checkedId == R.id.chipSaved) {
                matchesChip = project.createdAt < (now - sevenDaysMs);
            }

            if (matchesChip) {
                filtered.add(project);
            }
        }

        buildSectionedList(filtered);
    }

    private void buildSectionedList(List<Project> projects) {
        visibleItems.clear();

        List<Project> today = new ArrayList<>();
        List<Project> recent = new ArrayList<>();
        List<Project> older = new ArrayList<>();

        long startOfToday = getStartOfToday();
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);

        for (Project project : projects) {
            if (project.createdAt >= startOfToday) {
                today.add(project);
            } else if (project.createdAt >= sevenDaysAgo) {
                recent.add(project);
            } else {
                older.add(project);
            }
        }

        if (!today.isEmpty()) {
            visibleItems.add(ProjectListItem.header(getString(R.string.section_today)));
            for (Project project : today) {
                visibleItems.add(ProjectListItem.project(project));
            }
        }

        if (!recent.isEmpty()) {
            visibleItems.add(ProjectListItem.header(getString(R.string.section_recent)));
            for (Project project : recent) {
                visibleItems.add(ProjectListItem.project(project));
            }
        }

        if (!older.isEmpty()) {
            visibleItems.add(ProjectListItem.header(getString(R.string.section_older)));
            for (Project project : older) {
                visibleItems.add(ProjectListItem.project(project));
            }
        }

        adapter.notifyDataSetChanged();
        binding.imageRecycler.scheduleLayoutAnimation();
        binding.emptyView.setVisibility(visibleItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private long getStartOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
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
}