package com.example.memegenerator;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.memegenerator.data.Project;
import com.example.memegenerator.data.ProjectDatabase;
import com.example.memegenerator.databinding.ActivityMainBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private EditorViewModel viewModel;
    private ActivityResultLauncher<String> pickImageLauncher;

    private long editingProjectId = -1L;
    private boolean isEditingExistingProject = false;

    @Nullable
    private String currentOriginalImagePath = null;

    private String currentProjectName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupInsets();
        setupViewModel();
        setupImagePicker();
        setupButtons();
        handleEditIntent();
        updateEmptyState();

        String newProjectName = getIntent().getStringExtra("new_project_name");
        if (newProjectName != null && !newProjectName.trim().isEmpty()) {
            currentProjectName = newProjectName;
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void updateEmptyState() {
        if (binding == null) return;

        boolean hasImage = binding.imageView != null && binding.imageView.hasImage();
        binding.emptyState.setVisibility(hasImage ? View.GONE : View.VISIBLE);
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, (v, insets) -> {
            androidx.core.graphics.Insets statusBars =
                    insets.getInsets(WindowInsetsCompat.Type.statusBars());
            androidx.core.graphics.Insets navBars =
                    insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            binding.topBar.setPadding(
                    binding.topBar.getPaddingLeft(),
                    statusBars.top + dp(12),
                    binding.topBar.getPaddingRight(),
                    binding.topBar.getPaddingBottom()
            );

            binding.root.post(() -> {
                android.view.ViewGroup.MarginLayoutParams topActionsLp =
                        (android.view.ViewGroup.MarginLayoutParams) binding.topActionsRow.getLayoutParams();

                topActionsLp.topMargin = binding.topBar.getHeight() + dp(12);
                binding.topActionsRow.setLayoutParams(topActionsLp);

                binding.root.post(() -> {
                    android.view.ViewGroup.MarginLayoutParams canvasLp =
                            (android.view.ViewGroup.MarginLayoutParams) binding.canvasCard.getLayoutParams();

                    canvasLp.topMargin = binding.topBar.getHeight()
                            + binding.topActionsRow.getHeight()
                            + dp(24);

                    canvasLp.bottomMargin = binding.toolsScroll.getHeight() + dp(32) + navBars.bottom;
                    binding.canvasCard.setLayoutParams(canvasLp);
                });
            });

            return insets;
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(EditorViewModel.class);

        viewModel.getTextItems().observe(this, items -> binding.imageView.setTextItems(items));

        binding.imageView.setOnTextEditRequestListener(this::showEditDialog);

        binding.imageView.setOnTextMoveFinishedListener((index, oldItem, newItem) ->
                viewModel.updateItem(index, newItem)
        );
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;

                    Uri localUri = copyPickedImageToAppStorage(uri);
                    if (localUri == null) {
                        Toast.makeText(this, R.string.image_store_error, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int targetW = Math.max(binding.imageView.getWidth(), 1080);
                    int targetH = Math.max(binding.imageView.getHeight(), 1920);

                    Bitmap bmp = ImageLoader.loadBitmapFromUri(this, localUri, targetW, targetH);
                    if (bmp == null) {
                        Toast.makeText(this, R.string.image_load_error, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentOriginalImagePath = localUri.toString();
                    binding.imageView.addImageBitmap(bmp);
                    updateEmptyState();
                }
        );
    }

    @Nullable
    private Uri copyPickedImageToAppStorage(@NonNull Uri sourceUri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) return null;

            java.io.File dir = new java.io.File(getFilesDir(), "source_images");
            if (!dir.exists() && !dir.mkdirs()) {
                inputStream.close();
                return null;
            }

            String fileName = "src_" + System.currentTimeMillis() + ".jpg";
            java.io.File outFile = new java.io.File(dir, fileName);

            java.io.OutputStream outputStream = new java.io.FileOutputStream(outFile);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return Uri.fromFile(outFile);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnExportTop.setOnClickListener(v -> saveImage());

        binding.btnUndo.setOnClickListener(v -> viewModel.undo());
        binding.btnRedo.setOnClickListener(v -> viewModel.redo());
        binding.btnLayersTop.setOnClickListener(v -> showLayersSheet());

        binding.toolPickCard.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        binding.btnPick.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.toolCropCard.setOnClickListener(v ->
                Toast.makeText(this, "Обрезка будет подключена следующим шагом", Toast.LENGTH_SHORT).show()
        );
        binding.btnCrop.setOnClickListener(v ->
                Toast.makeText(this, "Обрезка будет подключена следующим шагом", Toast.LENGTH_SHORT).show()
        );

        binding.toolTextCard.setOnClickListener(v -> addTextLayer());
        binding.btnAddText.setOnClickListener(v -> addTextLayer());

        binding.toolDrawCard.setOnClickListener(v ->
                Toast.makeText(this, "Рисование будет подключена следующим шагом", Toast.LENGTH_SHORT).show()
        );
        binding.btnDraw.setOnClickListener(v ->
                Toast.makeText(this, "Рисование будет подключена следующим шагом", Toast.LENGTH_SHORT).show()
        );

        binding.toolFiltersCard.setOnClickListener(v ->
                Toast.makeText(this, "Фильтры будут подключены следующим шагом", Toast.LENGTH_SHORT).show()
        );
        binding.btnFilters.setOnClickListener(v ->
                Toast.makeText(this, "Фильтры будут подключены следующим шагом", Toast.LENGTH_SHORT).show()
        );
    }

    private void addTextLayer() {
        if (!binding.imageView.hasImage()) {
            Toast.makeText(this, R.string.pick_first, Toast.LENGTH_SHORT).show();
            return;
        }

        float centerX = binding.imageView.getWidth() / 2f;
        float centerY = binding.imageView.getHeight() / 2f;
        viewModel.addTextCentered(getString(R.string.placeholder_text), 28f, centerX, centerY);
    }

    private void showLayersSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.sheet_layers, null, false);
        dialog.setContentView(sheet);

        TextView summary = sheet.findViewById(R.id.layersSummary);
        LinearLayout container = sheet.findViewById(R.id.layersContainer);

        List<TextItem> items = viewModel.getTextItems().getValue();
        final List<TextItem> textItems = items != null ? items : new ArrayList<>();

        boolean hasImage = binding.imageView.hasImage();
        String summaryText = "Фон: " + (hasImage ? "есть" : "нет") + " • Текстов: " + textItems.size();
        summary.setText(summaryText);

        container.removeAllViews();

        if (hasImage) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_layer_row, container, false);

            TextView layerName = row.findViewById(R.id.layerName);
            TextView layerMeta = row.findViewById(R.id.layerMeta);
            android.widget.ImageButton btnVisible = row.findViewById(R.id.btnLayerVisible);
            android.widget.ImageButton btnUp = row.findViewById(R.id.btnMoveUp);
            android.widget.ImageButton btnDown = row.findViewById(R.id.btnMoveDown);
            android.widget.SeekBar opacity = row.findViewById(R.id.layerOpacity);

            layerName.setText("Фон");
            layerMeta.setText("Прозрачность: " + Math.round(binding.imageView.getBaseImageAlpha() * 100) + "%");

            boolean visible = binding.imageView.isBaseImageVisible();
            btnVisible.setImageResource(visible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);

            btnVisible.setOnClickListener(v -> {
                binding.imageView.setBaseImageVisible(!binding.imageView.isBaseImageVisible());
                dialog.dismiss();
                showLayersSheet();
            });

            btnUp.setVisibility(View.INVISIBLE);
            btnDown.setVisibility(View.INVISIBLE);

            opacity.setProgress(Math.round(binding.imageView.getBaseImageAlpha() * 100));
            opacity.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    float alpha = progress / 100f;
                    binding.imageView.setBaseImageAlpha(alpha);
                    layerMeta.setText("Прозрачность: " + progress + "%");
                }

                @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
            });

            container.addView(row);
        }

        for (int i = textItems.size() - 1; i >= 0; i--) {
            final int index = i;
            final TextItem item = textItems.get(i);

            View row = LayoutInflater.from(this).inflate(R.layout.item_layer_row, container, false);

            TextView layerName = row.findViewById(R.id.layerName);
            TextView layerMeta = row.findViewById(R.id.layerMeta);
            android.widget.ImageButton btnVisible = row.findViewById(R.id.btnLayerVisible);
            android.widget.ImageButton btnUp = row.findViewById(R.id.btnMoveUp);
            android.widget.ImageButton btnDown = row.findViewById(R.id.btnMoveDown);
            android.widget.SeekBar opacity = row.findViewById(R.id.layerOpacity);

            String title = item.text != null && !item.text.trim().isEmpty() ? item.text : "Текст";
            if (title.length() > 18) {
                title = title.substring(0, 18) + "...";
            }

            layerName.setText("Текст: " + title);
            layerMeta.setText("Прозрачность: " + Math.round(item.alpha * 100) + "%");

            btnVisible.setImageResource(item.visible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
            btnVisible.setOnClickListener(v -> {
                viewModel.setItemVisible(index, !item.visible);
                dialog.dismiss();
                showLayersSheet();
            });

            btnUp.setOnClickListener(v -> {
                viewModel.moveItemUp(index);
                dialog.dismiss();
                showLayersSheet();
            });

            btnDown.setOnClickListener(v -> {
                viewModel.moveItemDown(index);
                dialog.dismiss();
                showLayersSheet();
            });

            opacity.setProgress(Math.round(item.alpha * 100));
            opacity.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    float alpha = progress / 100f;
                    viewModel.setItemAlpha(index, alpha);
                    layerMeta.setText("Прозрачность: " + progress + "%");
                }

                @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
            });

            container.addView(row);
        }

        if (!hasImage && textItems.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Слоёв пока нет");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setTextSize(14f);
            empty.setPadding(0, dp(8), 0, dp(8));
            container.addView(empty);
        }

        dialog.show();
    }

    private void handleEditIntent() {
        String editOriginalImagePath = getIntent().getStringExtra("edit_original_image_path");
        String editPreviewImagePath = getIntent().getStringExtra("edit_preview_image_path");
        String editTextItemsJson = getIntent().getStringExtra("edit_text_items_json");
        String editProjectName = getIntent().getStringExtra("edit_project_name");
        long projectId = getIntent().getLongExtra("edit_project_id", -1L);

        if ((editOriginalImagePath == null || editOriginalImagePath.trim().isEmpty())
                && (editPreviewImagePath == null || editPreviewImagePath.trim().isEmpty())) {
            return;
        }

        editingProjectId = projectId;
        isEditingExistingProject = projectId != -1L;
        currentProjectName = editProjectName != null ? editProjectName : "";

        String pathToOpen = editOriginalImagePath;
        if (pathToOpen == null || pathToOpen.trim().isEmpty()) {
            pathToOpen = editPreviewImagePath;
        }

        loadProjectForEdit(Uri.parse(pathToOpen), editTextItemsJson);
    }

    private void loadProjectForEdit(@NonNull Uri uri, @Nullable String textItemsJson) {
        binding.imageView.post(() -> {
            int targetW = Math.max(binding.imageView.getWidth(), 1080);
            int targetH = Math.max(binding.imageView.getHeight(), 1920);

            Bitmap bmp = null;

            try {
                bmp = ImageLoader.loadBitmapFromUri(this, uri, targetW, targetH);
            } catch (Exception ignored) {
            }

            if (bmp == null) {
                String previewPath = getIntent().getStringExtra("edit_overlay_image_path");
                if (previewPath != null && !previewPath.trim().isEmpty()) {
                    try {
                        bmp = ImageLoader.loadBitmapFromUri(this, Uri.parse(previewPath), targetW, targetH);
                    } catch (Exception ignored) {
                    }
                }
            }

            if (bmp == null) {
                Toast.makeText(this, R.string.project_open_error, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            binding.imageView.addImageBitmap(bmp);
            updateEmptyState();

            List<TextItem> restored = parseTextItemsJson(textItemsJson);
            viewModel.setItems(restored);
        });
    }

    @NonNull
    private List<TextItem> parseTextItemsJson(@Nullable String json) {
        List<TextItem> result = new ArrayList<>();

        if (json == null || json.trim().isEmpty()) {
            return result;
        }

        try {
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);

                String text = o.optString("text", getString(R.string.default_text));
                float textSizeSp = (float) o.optDouble("textSizeSp", 28f);
                float x = (float) o.optDouble("x", 0f);
                float y = (float) o.optDouble("y", 0f);
                int typefaceStyle = o.optInt("typefaceStyle", 0);
                int color = o.optInt("color", 0xFFFFFFFF);
                int align = o.optInt("align", TextItem.ALIGN_CENTER);
                float boxWidth = (float) o.optDouble("boxWidth", 0f);

                boolean visible = o.optBoolean("visible", true);
                float alpha = (float) o.optDouble("alpha", 1f);

                result.add(new TextItem(
                        text,
                        textSizeSp,
                        x,
                        y,
                        typefaceStyle,
                        color,
                        align,
                        boxWidth,
                        visible,
                        alpha
                ));
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    private void saveImage() {
        if (!binding.imageView.hasImage()) {
            Toast.makeText(this, R.string.pick_first, Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap previewBitmap = binding.imageView.exportToBitmapAtOriginal();
        Bitmap originalOnlyBitmap = binding.imageView.exportBaseImageAtOriginal();

        if (previewBitmap == null || originalOnlyBitmap == null) {
            Toast.makeText(this, R.string.image_prepare_error, Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String projectNameToSave = currentProjectName;
                if (projectNameToSave == null || projectNameToSave.trim().isEmpty()) {
                    projectNameToSave = getString(R.string.new_project);
                }

                Uri originalSaved = ImageRepository.saveBitmapToGallery(
                        this,
                        originalOnlyBitmap,
                        "original_" + System.currentTimeMillis() + ".png"
                );

                Uri previewSaved = ImageRepository.saveBitmapToGallery(
                        this,
                        previewBitmap,
                        "project_" + System.currentTimeMillis() + ".png"
                );

                if (originalSaved == null || previewSaved == null) {
                    runOnUiThread(() ->
                            Toast.makeText(this, R.string.save_error, Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String textItemsJson = viewModel.exportToJson();

                ProjectDatabase db = ProjectDatabase.getInstance(getApplicationContext());

                if (isEditingExistingProject && editingProjectId != -1L) {
                    Project updated = new Project(
                            projectNameToSave,
                            originalSaved.toString(),
                            previewSaved.toString(),
                            textItemsJson,
                            System.currentTimeMillis()
                    );
                    updated.id = editingProjectId;
                    db.projectDao().update(updated);
                } else {
                    Project project = new Project(
                            projectNameToSave,
                            originalSaved.toString(),
                            previewSaved.toString(),
                            textItemsJson,
                            System.currentTimeMillis()
                    );
                    long newId = db.projectDao().insert(project);
                    editingProjectId = newId;
                    isEditingExistingProject = true;
                }

                currentProjectName = projectNameToSave;

                runOnUiThread(() ->
                        Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.save_project_error, Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void showEditDialog(int index, @NonNull TextItem item) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        int pad = dp(16);
        container.setPadding(pad, pad, pad, pad);

        EditText textInput = new EditText(this);
        textInput.setHint(R.string.enter_text_hint);
        textInput.setText(item.text);
        textInput.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        textInput.setHintTextColor(0x99FFFFFF);
        textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        container.addView(textInput);

        EditText sizeInput = new EditText(this);
        sizeInput.setHint(R.string.text_size_hint);
        sizeInput.setText(String.valueOf(item.textSizeSp));
        sizeInput.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        sizeInput.setHintTextColor(0x99FFFFFF);
        sizeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(sizeInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_text_title)
                .setView(container)
                .setPositiveButton(R.string.save_action, (d, which) -> {
                    String newText = textInput.getText().toString().trim();
                    if (newText.isEmpty()) newText = getString(R.string.default_text);

                    float newSize;
                    try {
                        newSize = Float.parseFloat(sizeInput.getText().toString().trim());
                    } catch (Exception e) {
                        newSize = item.textSizeSp;
                    }

                    TextItem updated = new TextItem(
                            newText,
                            newSize,
                            item.x,
                            item.y,
                            item.typefaceStyle,
                            item.color,
                            item.align,
                            item.boxWidth
                    );

                    viewModel.updateItem(index, updated);
                })
                .setNeutralButton(R.string.action_delete, (d, which) -> viewModel.removeItem(index))
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.show();
    }
}