package com.example.memegenerator;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memegenerator.data.Project;
import com.example.memegenerator.data.ProjectDatabase;
import com.example.memegenerator.databinding.ActivityMainBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private EditorViewModel viewModel;
    private ActivityResultLauncher<String> pickImageLauncher;

    private long editingProjectId = -1L;
    private boolean isEditingExistingProject = false;

    private boolean isCropMode = false;
    private boolean isDrawMode = false;

    @Nullable
    private String currentOriginalImagePath = null;

    private String currentProjectName = "";

    private final Stack<Bitmap> undoImageStack = new Stack<>();
    private final Stack<Bitmap> redoImageStack = new Stack<>();

    private static final int[] BRUSH_COLORS = new int[]{
            0xFFFFFFFF,
            0xFF000000,
            0xFFFF3B30,
            0xFFFF9500,
            0xFFFFD60A,
            0xFF34C759,
            0xFF0A84FF,
            0xFF5E5CE6,
            0xFFFF2D55
    };

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
                    undoImageStack.clear();
                    redoImageStack.clear();
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
        binding.btnBack.setOnClickListener(v -> {
            if (isCropMode) {
                exitCropMode(false);
            } else if (isDrawMode) {
                exitDrawMode();
            } else {
                finish();
            }
        });

        binding.btnExportTop.setOnClickListener(v -> {
            if (isCropMode) {
                applyCropMode();
            } else {
                saveImage();
            }
        });

        binding.btnUndo.setOnClickListener(v -> {
            if (isCropMode) return;

            boolean imageUndone = undoImageChange();
            if (!imageUndone) {
                viewModel.undo();
            }
        });

        binding.btnRedo.setOnClickListener(v -> {
            if (isCropMode) return;

            boolean imageRedone = redoImageChange();
            if (!imageRedone) {
                viewModel.redo();
            }
        });

        binding.btnLayersTop.setOnClickListener(v -> {
            if (isCropMode || isDrawMode) return;
            showLayersSheet();
        });

        binding.toolPickCard.setOnClickListener(v -> {
            if (isCropMode || isDrawMode) return;
            pickImageLauncher.launch("image/*");
        });

        binding.btnPick.setOnClickListener(v -> {
            if (isCropMode || isDrawMode) return;
            pickImageLauncher.launch("image/*");
        });

        binding.toolCropCard.setOnClickListener(v -> {
            if (isDrawMode) return;
            onCropClicked();
        });

        binding.btnCrop.setOnClickListener(v -> {
            if (isDrawMode) return;
            onCropClicked();
        });

        binding.toolTextCard.setOnClickListener(v -> {
            if (isCropMode || isDrawMode) return;
            addTextLayer();
        });

        binding.btnAddText.setOnClickListener(v -> {
            if (isCropMode || isDrawMode) return;
            addTextLayer();
        });

        binding.toolDrawCard.setOnClickListener(v -> onDrawClicked());
        binding.btnDraw.setOnClickListener(v -> onDrawClicked());

        binding.toolFiltersCard.setOnClickListener(v -> {
            if (isCropMode || isDrawMode) return;
            Toast.makeText(this, "Фильтры будут подключены следующим шагом", Toast.LENGTH_SHORT).show();
        });

        binding.btnFilters.setOnClickListener(v -> {
            if (isCropMode || isDrawMode) return;
            Toast.makeText(this, "Фильтры будут подключены следующим шагом", Toast.LENGTH_SHORT).show();
        });
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
        BottomSheetDialog dialog = new BottomSheetDialog(
                this,
                com.google.android.material.R.style.Theme_Design_BottomSheetDialog
        );

        View sheet = LayoutInflater.from(this).inflate(R.layout.sheet_layers, null, false);
        dialog.setContentView(sheet);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setDimAmount(0.55f);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        RecyclerView recycler = sheet.findViewById(R.id.layersRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        final LayerAdapter[] adapterRef = new LayerAdapter[1];

        LayerAdapter adapter = new LayerAdapter(new LayerAdapter.Listener() {
            @Override
            public void onToggleVisibility(@NonNull LayerRowItem item) {
                if (item.isImageLayer()) {
                    binding.imageView.setBaseImageVisible(!binding.imageView.isBaseImageVisible());
                } else if (item.isDrawLayer()) {
                    binding.imageView.setDrawLayerVisible(!binding.imageView.isDrawLayerVisible());
                } else {
                    viewModel.setItemVisible(item.textIndex, !item.visible);
                }

                if (adapterRef[0] != null) {
                    refreshLayersAdapter(adapterRef[0]);
                }
            }

            @Override
            public void onDelete(@NonNull LayerRowItem item) {
                if (item.isImageLayer()) {
                    binding.imageView.clearBaseImage();
                    updateEmptyState();
                } else if (item.isDrawLayer()) {
                    binding.imageView.clearDrawLayer();
                } else {
                    viewModel.deleteItem(item.textIndex);
                }

                if (adapterRef[0] != null) {
                    refreshLayersAdapter(adapterRef[0]);
                }
            }

            @Override
            public void onOpacityChanged(@NonNull LayerRowItem item, float alpha) {
                if (item.isImageLayer()) {
                    binding.imageView.setBaseImageAlpha(alpha);
                } else if (item.isDrawLayer()) {
                    binding.imageView.setDrawLayerAlpha(alpha);
                } else {
                    viewModel.setItemAlpha(item.textIndex, alpha);
                }
            }

            @Override
            public void onMove(int fromPosition, int toPosition) {
                if (adapterRef[0] == null) return;

                List<LayerRowItem> rows = adapterRef[0].getItems();
                if (fromPosition < 0 || toPosition < 0 || fromPosition >= rows.size() || toPosition >= rows.size()) {
                    return;
                }

                LayerRowItem fromItem = rows.get(fromPosition);
                LayerRowItem toItem = rows.get(toPosition);

                if (fromItem.isTextLayer() && toItem.isTextLayer()) {
                    viewModel.swapItems(fromItem.textIndex, toItem.textIndex);
                    refreshLayersAdapter(adapterRef[0]);
                }
            }
        });

        adapterRef[0] = adapter;
        recycler.setAdapter(adapter);

        ItemTouchHelper touchHelper =
                new ItemTouchHelper(
                        new ItemTouchHelper.SimpleCallback(
                                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                                0
                        ) {
                            @Override
                            public boolean onMove(@NonNull RecyclerView recyclerView,
                                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                                  @NonNull RecyclerView.ViewHolder target) {
                                int from = viewHolder.getBindingAdapterPosition();
                                int to = target.getBindingAdapterPosition();
                                adapter.onItemMove(from, to);
                                return true;
                            }

                            @Override
                            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                            }

                            @Override
                            public boolean isLongPressDragEnabled() {
                                return true;
                            }
                        });

        touchHelper.attachToRecyclerView(recycler);

        refreshLayersAdapter(adapter);
        dialog.show();
    }

    private void refreshLayersAdapter(@NonNull LayerAdapter adapter) {
        List<LayerRowItem> rows = new ArrayList<>();

        if (binding.imageView.hasImage()) {
            Bitmap thumb = binding.imageView.getBaseThumbnail(dp(56));
            rows.add(new LayerRowItem(
                    LayerRowItem.TYPE_IMAGE,
                    -1,
                    "Фон",
                    binding.imageView.isBaseImageVisible(),
                    binding.imageView.getBaseImageAlpha(),
                    thumb
            ));
        }

        if (binding.imageView.hasDrawLayer()) {
            rows.add(new LayerRowItem(
                    LayerRowItem.TYPE_DRAW,
                    -1,
                    "Рисование",
                    binding.imageView.isDrawLayerVisible(),
                    binding.imageView.getDrawLayerAlpha(),
                    null
            ));
        }

        List<TextItem> textItems = viewModel.getTextItems().getValue();
        if (textItems != null) {
            for (int i = 0; i < textItems.size(); i++) {
                TextItem item = textItems.get(i);
                String title = item.text == null || item.text.trim().isEmpty()
                        ? "Текст"
                        : item.text.trim();

                if (title.length() > 18) {
                    title = title.substring(0, 18) + "...";
                }

                rows.add(new LayerRowItem(
                        LayerRowItem.TYPE_TEXT,
                        i,
                        title,
                        item.visible,
                        item.alpha,
                        null
                ));
            }
        }

        adapter.submitList(rows);
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
                            item.boxWidth,
                            item.visible,
                            item.alpha
                    );

                    viewModel.updateItem(index, updated);
                })
                .setNeutralButton(R.string.action_delete, (d, which) -> viewModel.removeItem(index))
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.show();
    }

    private void onCropClicked() {
        if (!binding.imageView.hasImage()) {
            Toast.makeText(this, R.string.pick_first, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isDrawMode) return;

        if (!isCropMode) {
            enterCropMode();
        } else {
            applyCropMode();
        }
    }

    private void enterCropMode() {
        isCropMode = true;
        binding.imageView.startCropMode();

        binding.btnExportTop.setText("Применить");
        Toast.makeText(this, "Режим обрезки включён", Toast.LENGTH_SHORT).show();
    }

    private void exitCropMode(boolean keepChanges) {
        isCropMode = false;

        if (!keepChanges) {
            binding.imageView.cancelCropMode();
        }

        binding.btnExportTop.setText(getString(R.string.export_action));
    }

    private void applyCropMode() {
        pushImageStateToUndo();

        boolean success = binding.imageView.applyCropAndReplaceBase();
        if (!success) {
            if (!undoImageStack.isEmpty()) {
                undoImageStack.pop();
            }
            Toast.makeText(this, "Не удалось применить обрезку", Toast.LENGTH_SHORT).show();
            return;
        }

        updateEmptyState();
        exitCropMode(true);
        Toast.makeText(this, "Обрезка применена", Toast.LENGTH_SHORT).show();
    }

    private void onDrawClicked() {
        if (!binding.imageView.hasImage()) {
            Toast.makeText(this, R.string.pick_first, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCropMode) return;

        if (!isDrawMode) {
            enterDrawMode();
        } else {
            showBrushSettingsSheet();
        }
    }

    private void enterDrawMode() {
        isDrawMode = true;
        binding.imageView.setDrawMode(true);
        binding.btnExportTop.setText(getString(R.string.export_action));
        Toast.makeText(this, "Режим рисования включён", Toast.LENGTH_SHORT).show();
        showBrushSettingsSheet();
    }

    private void exitDrawMode() {
        isDrawMode = false;
        binding.imageView.setDrawMode(false);
        Toast.makeText(this, "Режим рисования выключен", Toast.LENGTH_SHORT).show();
    }

    private void showBrushSettingsSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = LayoutInflater.from(this).inflate(R.layout.sheet_brush_settings, null, false);
        dialog.setContentView(sheet);

        MaterialSwitch eraserSwitch = sheet.findViewById(R.id.switchEraser);
        SeekBar sizeSeek = sheet.findViewById(R.id.seekBrushSize);
        TextView sizeValue = sheet.findViewById(R.id.brushSizeValue);
        View colorRow = sheet.findViewById(R.id.colorRow);
        MaterialButton btnCloseDraw = sheet.findViewById(R.id.btnCloseDraw);
        MaterialButton btnClearDraw = sheet.findViewById(R.id.btnClearDraw);

        eraserSwitch.setChecked(binding.imageView.isEraserMode());

        int sizeDp = Math.round(binding.imageView.getBrushSizeDp());
        sizeDp = Math.max(4, Math.min(sizeDp, 72));
        sizeSeek.setProgress(sizeDp);
        sizeValue.setText(sizeDp + " dp");

        eraserSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.imageView.setEraserMode(isChecked)
        );

        sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int safe = Math.max(4, progress);
                binding.imageView.setBrushSizeDp(safe);
                sizeValue.setText(safe + " dp");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        if (colorRow instanceof LinearLayout) {
            LinearLayout colorsWrap = (LinearLayout) colorRow;
            colorsWrap.removeAllViews();

            for (int color : BRUSH_COLORS) {
                View swatch = new View(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(28), dp(28));
                lp.rightMargin = dp(10);
                swatch.setLayoutParams(lp);
                swatch.setBackground(makeCircleDrawable(color));
                swatch.setOnClickListener(v -> {
                    binding.imageView.setBrushColor(color);
                    binding.imageView.setEraserMode(false);
                    eraserSwitch.setChecked(false);
                });
                colorsWrap.addView(swatch);
            }
        }

        btnClearDraw.setOnClickListener(v -> {
            binding.imageView.clearDrawLayer();
            Toast.makeText(this, "Слой рисования очищен", Toast.LENGTH_SHORT).show();
        });

        btnCloseDraw.setOnClickListener(v -> {
            dialog.dismiss();
            exitDrawMode();
        });

        dialog.show();
    }

    private android.graphics.drawable.Drawable makeCircleDrawable(int color) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        d.setColor(color);
        d.setStroke(dp(1), 0x33FFFFFF);
        return d;
    }

    private void pushImageStateToUndo() {
        Bitmap snapshot = binding.imageView.getBaseBitmapCopy();
        if (snapshot != null) {
            undoImageStack.push(snapshot);
            redoImageStack.clear();
        }
    }

    private boolean undoImageChange() {
        if (undoImageStack.isEmpty()) return false;

        Bitmap current = binding.imageView.getBaseBitmapCopy();
        if (current != null) {
            redoImageStack.push(current);
        }

        Bitmap previous = undoImageStack.pop();
        binding.imageView.replaceBaseBitmap(previous);
        updateEmptyState();
        return true;
    }

    private boolean redoImageChange() {
        if (redoImageStack.isEmpty()) return false;

        Bitmap current = binding.imageView.getBaseBitmapCopy();
        if (current != null) {
            undoImageStack.push(current);
        }

        Bitmap next = redoImageStack.pop();
        binding.imageView.replaceBaseBitmap(next);
        updateEmptyState();
        return true;
    }
}