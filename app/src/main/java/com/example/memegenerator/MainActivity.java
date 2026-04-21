package com.example.memegenerator;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import com.example.memegenerator.data.Meme;
import com.example.memegenerator.data.MemeDatabase;
import com.example.memegenerator.databinding.ActivityMainBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MemeViewModel viewModel;
    private ActivityResultLauncher<String> pickImageLauncher;

    private long editingMemeId = -1L;
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

        setupToolbar();
        setupInsets();
        setupViewModel();
        setupImagePicker();
        setupButtons();
        handleEditIntent();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.topBar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        binding.topBar.setNavigationOnClickListener(v -> finish());

        binding.topBar.setNavigationIconTint(
                ContextCompat.getColor(this, R.color.text_primary)
        );

        binding.topBar.setTitleTextColor(
                ContextCompat.getColor(this, R.color.text_primary)
        );
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, (v, insets) -> {
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

            binding.bottomBar.setPadding(
                    binding.bottomBar.getPaddingLeft(),
                    binding.bottomBar.getPaddingTop(),
                    binding.bottomBar.getPaddingRight(),
                    navBars.bottom
            );

            int bottomInset = binding.bottomBar.getMeasuredHeight() + navBars.bottom;

            binding.imageView.setPadding(
                    binding.imageView.getPaddingLeft(),
                    binding.imageView.getPaddingTop(),
                    binding.imageView.getPaddingRight(),
                    bottomInset
            );

            binding.imageView.setContentBottomInsetPx(bottomInset);

            return insets;
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MemeViewModel.class);

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
                        Toast.makeText(this, "Не удалось сохранить фото в память приложения", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int targetW = Math.max(binding.imageView.getWidth(), 1080);
                    int targetH = Math.max(binding.imageView.getHeight(), 1920);

                    Bitmap bmp = ImageLoader.loadBitmapFromUri(this, localUri, targetW, targetH);
                    if (bmp == null) {
                        Toast.makeText(this, "Не удалось загрузить фото", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    currentOriginalImagePath = localUri.toString();
                    binding.imageView.addImageBitmap(bmp);
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
        binding.btnPick.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.btnAddText.setOnClickListener(v -> {
            float centerX = binding.imageView.getWidth() / 2f;
            float centerY = binding.imageView.getHeight() / 2f;
            viewModel.addTextCentered("Ваш текст", 28f, centerX, centerY);
        });

        binding.btnUndo.setOnClickListener(v -> viewModel.undo());
        binding.btnRedo.setOnClickListener(v -> viewModel.redo());
    }

    private void handleEditIntent() {
        String editOriginalImagePath = getIntent().getStringExtra("edit_original_image_path");
        String editPreviewImagePath = getIntent().getStringExtra("edit_preview_image_path");
        String editTextItemsJson = getIntent().getStringExtra("edit_text_items_json");
        String editProjectName = getIntent().getStringExtra("edit_project_name");
        long memeId = getIntent().getLongExtra("edit_meme_id", -1L);

        if ((editOriginalImagePath == null || editOriginalImagePath.trim().isEmpty())
                && (editPreviewImagePath == null || editPreviewImagePath.trim().isEmpty())) {
            return;
        }

        editingMemeId = memeId;
        isEditingExistingProject = memeId != -1L;
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
                Toast.makeText(this, "Не удалось открыть проект", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            binding.imageView.addImageBitmap(bmp);

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

                String text = o.optString("text", "Текст");
                float textSizeSp = (float) o.optDouble("textSizeSp", 28f);
                float x = (float) o.optDouble("x", 0f);
                float y = (float) o.optDouble("y", 0f);
                int typefaceStyle = o.optInt("typefaceStyle", 0);
                int color = o.optInt("color", 0xFFFFFFFF);
                int align = o.optInt("align", TextItem.ALIGN_CENTER);
                float boxWidth = (float) o.optDouble("boxWidth", 0f);

                result.add(new TextItem(
                        text,
                        textSizeSp,
                        x,
                        y,
                        typefaceStyle,
                        color,
                        align,
                        boxWidth
                ));
            }
        } catch (Exception ignored) {
        }

        return result;
    }

    @NonNull
    private String buildTextItemsJson() {
        JSONArray array = new JSONArray();

        List<TextItem> items = viewModel.getTextItems().getValue();
        if (items == null) return array.toString();

        try {
            for (TextItem item : items) {
                JSONObject o = new JSONObject();
                o.put("text", item.text);
                o.put("textSizeSp", item.textSizeSp);
                o.put("x", item.x);
                o.put("y", item.y);
                o.put("typefaceStyle", item.typefaceStyle);
                o.put("color", item.color);
                o.put("align", item.align);
                o.put("boxWidth", item.boxWidth);
                array.put(o);
            }
        } catch (Exception ignored) {
        }

        return array.toString();
    }

    private void saveImage() {
        if (!binding.imageView.hasImage()) {
            Toast.makeText(this, "Сначала выберите фото", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap previewBitmap = binding.imageView.exportToBitmapAtOriginal();
        Bitmap originalOnlyBitmap = binding.imageView.exportBaseImageAtOriginal();

        if (previewBitmap == null || originalOnlyBitmap == null) {
            Toast.makeText(this, "Ошибка подготовки изображения", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String projectNameToSave = currentProjectName;
                if (projectNameToSave == null || projectNameToSave.trim().isEmpty()) {
                    projectNameToSave = "Новый проект";
                }

                Uri originalSaved = MemeRepository.saveBitmapToGallery(
                        this,
                        originalOnlyBitmap,
                        "original_" + System.currentTimeMillis() + ".png"
                );

                Uri previewSaved = MemeRepository.saveBitmapToGallery(
                        this,
                        previewBitmap,
                        "project_" + System.currentTimeMillis() + ".png"
                );

                if (originalSaved == null || previewSaved == null) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String textItemsJson = viewModel.exportToJson();

                MemeDatabase db = MemeDatabase.getInstance(getApplicationContext());

                if (isEditingExistingProject && editingMemeId != -1L) {
                    Meme updated = new Meme(
                            projectNameToSave,
                            originalSaved.toString(),
                            previewSaved.toString(),
                            textItemsJson,
                            System.currentTimeMillis()
                    );
                    updated.id = editingMemeId;
                    db.memeDao().update(updated);
                } else {
                    Meme meme = new Meme(
                            projectNameToSave,
                            originalSaved.toString(),
                            previewSaved.toString(),
                            textItemsJson,
                            System.currentTimeMillis()
                    );
                    long newId = db.memeDao().insert(meme);
                    editingMemeId = newId;
                    isEditingExistingProject = true;
                }

                currentProjectName = projectNameToSave;

                runOnUiThread(() ->
                        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка сохранения проекта", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void showEditDialog(int index, @NonNull TextItem item) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText textInput = new EditText(this);
        textInput.setHint("Введите текст");
        textInput.setText(item.text);
        textInput.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        textInput.setHintTextColor(0x99FFFFFF);
        textInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        container.addView(textInput);

        EditText sizeInput = new EditText(this);
        sizeInput.setHint("Размер текста");
        sizeInput.setText(String.valueOf(item.textSizeSp));
        sizeInput.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        sizeInput.setHintTextColor(0x99FFFFFF);
        sizeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        container.addView(sizeInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Редактировать текст")
                .setView(container)
                .setPositiveButton("Сохранить", (d, which) -> {
                    String newText = textInput.getText().toString().trim();
                    if (newText.isEmpty()) newText = "Текст";

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
                .setNeutralButton("Удалить", (d, which) -> viewModel.removeItem(index))
                .setNegativeButton("Отмена", null)
                .create();

        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.clear();

        android.view.MenuItem saveItem = menu.add(
                android.view.Menu.NONE,
                R.id.action_save,
                android.view.Menu.NONE,
                "Сохранить"
        );

        saveItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
        saveItem.setIcon(android.R.drawable.ic_menu_save);

        if (saveItem.getIcon() != null) {
            saveItem.getIcon().setTint(
                    ContextCompat.getColor(this, R.color.text_primary)
            );
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        if (item.getItemId() == R.id.action_save) {
            saveImage();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}