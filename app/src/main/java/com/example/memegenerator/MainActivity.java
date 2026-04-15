package com.example.memegenerator;

import static com.example.memegenerator.ImageLoader.loadBitmapFromUri;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
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
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.memegenerator.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @Nullable
    private AlertDialog editDialog;

    private ActivityMainBinding binding;
    private MemeViewModel viewModel;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupWindow();
        setupViewModel();
        setupPickImage();
        setupButtons();
        setupInsets();

        binding.titleText.setText("Редактор");
        binding.titleText.setTextSize(20f);

        handleEditIntent();

    }
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);

        android.view.MenuItem saveItem = menu.findItem(R.id.action_save);
        if (saveItem != null && saveItem.getIcon() != null) {
            saveItem.getIcon().setTint(
                    ContextCompat.getColor(this, R.color.text_primary)
            );
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveCurrentImage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.topBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        binding.topBar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        binding.topBar.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        binding.topBar.setTitleTextColor(ContextCompat.getColor(this, R.color.text_primary));
        binding.topBar.setNavigationIconTint(ContextCompat.getColor(this, R.color.text_primary));
    }

    private void setupWindow() {
        new WindowInsetsControllerCompat(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(true);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MemeViewModel.class);
        viewModel.getTextItems().observe(this, items -> binding.imageView.setTextItems(items));

        binding.imageView.setOnTextEditRequestListener(this::showEditDialog);
        binding.imageView.setOnTextMovedListener((index, item) -> viewModel.updateItem(index, item));
    }

    private void setupPickImage() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onImagePicked
        );
    }

    private void setupButtons() {
        binding.btnPick.setOnClickListener(v -> ensurePhotoPermissionThenPick());

        binding.btnAddText.setOnClickListener(v -> {
            float x = Math.max(40f, binding.imageView.getWidth() * 0.5f);
            float y = Math.max(80f, binding.imageView.getHeight() * 0.35f);
            viewModel.addTextCentered("Ваш текст", 32f, x, y);
            Toast.makeText(this, "Двойной тап по тексту — редактировать", Toast.LENGTH_SHORT).show();
        });

        binding.bottomAppBar.post(() ->
                binding.imageView.setContentBottomInsetPx(binding.bottomAppBar.getHeight())
        );
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            int status = WindowInsetsCompat.Type.statusBars();
            int nav = WindowInsetsCompat.Type.navigationBars();

            androidx.core.graphics.Insets sb = insets.getInsets(status);
            androidx.core.graphics.Insets nb = insets.getInsets(nav);

            binding.appbar.setPadding(
                    binding.appbar.getPaddingLeft(),
                    sb.top,
                    binding.appbar.getPaddingRight(),
                    binding.appbar.getPaddingBottom()
            );

            binding.bottomAppBar.setPadding(
                    binding.bottomAppBar.getPaddingLeft(),
                    binding.bottomAppBar.getPaddingTop(),
                    binding.bottomAppBar.getPaddingRight(),
                    nb.bottom
            );

            binding.imageView.setPadding(
                    binding.imageView.getPaddingLeft(),
                    binding.imageView.getPaddingTop(),
                    binding.imageView.getPaddingRight(),
                    nb.bottom + binding.bottomAppBar.getHeight()
            );

            return insets;
        });
    }

    private void handleEditIntent() {
        String editImagePath = getIntent().getStringExtra("edit_image_path");
        String editTopText = getIntent().getStringExtra("edit_top_text");
        String editBottomText = getIntent().getStringExtra("edit_bottom_text");

        if (editImagePath == null || editImagePath.trim().isEmpty()) {
            return;
        }

        Uri uri = Uri.parse(editImagePath);
        loadProjectFromHistory(uri, editTopText, editBottomText);
    }

    private void loadProjectFromHistory(@NonNull Uri uri, @Nullable String topText, @Nullable String bottomText) {
        int tw = binding.imageView.getWidth();
        int th = binding.imageView.getHeight();

        if (tw <= 0 || th <= 0) {
            var dm = getResources().getDisplayMetrics();
            tw = dm.widthPixels;
            th = dm.heightPixels;
        }

        Bitmap bmp = loadBitmapFromUri(this, uri, tw, th);
        if (bmp == null) {
            Toast.makeText(this, "Не удалось открыть проект", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.imageView.addImageBitmap(bmp);

        binding.imageView.post(() -> {
            float centerX = Math.max(40f, binding.imageView.getWidth() * 0.5f);

            if (topText != null && !topText.trim().isEmpty()) {
                float topY = Math.max(80f, binding.imageView.getHeight() * 0.25f);
                viewModel.addTextCentered(topText, 32f, centerX, topY);
            }

            if (bottomText != null && !bottomText.trim().isEmpty()) {
                float bottomY = Math.max(80f, binding.imageView.getHeight() * 0.75f);
                viewModel.addTextCentered(bottomText, 32f, centerX, bottomY);
            }
        });
    }

    private void onImagePicked(@Nullable Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "Изображение не выбрано", Toast.LENGTH_SHORT).show();
            return;
        }

        int tw = binding.imageView.getWidth();
        int th = binding.imageView.getHeight();
        if (tw <= 0 || th <= 0) {
            var dm = getResources().getDisplayMetrics();
            tw = dm.widthPixels;
            th = dm.heightPixels;
        }

        Bitmap bmp = loadBitmapFromUri(this, uri, tw, th);
        if (bmp == null) {
            Toast.makeText(this, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.imageView.addImageBitmap(bmp);
    }

    private void saveCurrentImage() {
        if (!binding.imageView.hasImage()) {
            Toast.makeText(this, "Сначала выберите фото", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap out = binding.imageView.exportToBitmapAtOriginal();
        new Thread(() -> {
            Uri saved = MemeRepository.saveBitmapToGallery(
                    this, out, "snapforge_" + System.currentTimeMillis() + ".png"
            );

            runOnUiThread(() -> {
                if (saved != null) {
                    Toast.makeText(this, "Сохранено в Галерею", Toast.LENGTH_SHORT).show();
                    String[] tb = pickTopBottomTexts();
                    saveProjectToHistory(saved, tb[0], tb[1]);
                } else {
                    Toast.makeText(this, "Не удалось сохранить", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @NonNull
    private String[] pickTopBottomTexts() {
        var live = viewModel.getTextItems().getValue();
        String top = "";
        String bottom = "";

        if (live != null && !live.isEmpty()) {
            top = live.get(0).text != null ? live.get(0).text : "";
            if (live.size() > 1) {
                bottom = live.get(1).text != null ? live.get(1).text : "";
            }
        }

        return new String[]{top, bottom};
    }

    private void showEditDialog(int index, @NonNull TextItem item) {
        if (editDialog != null && editDialog.isShowing()) return;

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);

        EditText textEt = new EditText(this);
        textEt.setHint("Текст");
        textEt.setText(item.text);
        textEt.setSingleLine(false);
        textEt.setMinLines(2);
        textEt.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );
        container.addView(textEt);

        EditText sizeEt = new EditText(this);
        sizeEt.setHint("Размер, sp");
        sizeEt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        sizeEt.setText(String.valueOf(item.textSizeSp));
        container.addView(sizeEt);

        LinearLayout alignRow = new LinearLayout(this);
        alignRow.setOrientation(LinearLayout.HORIZONTAL);
        alignRow.setPadding(0, pad / 2, 0, 0);

        android.widget.Button leftBtn = new android.widget.Button(this);
        leftBtn.setText("←");
        android.widget.Button centerBtn = new android.widget.Button(this);
        centerBtn.setText("↔");
        android.widget.Button rightBtn = new android.widget.Button(this);
        rightBtn.setText("→");

        int btnPad = (int) (8 * getResources().getDisplayMetrics().density);
        leftBtn.setPadding(btnPad, btnPad, btnPad, btnPad);
        centerBtn.setPadding(btnPad, btnPad, btnPad, btnPad);
        rightBtn.setPadding(btnPad, btnPad, btnPad, btnPad);

        alignRow.addView(leftBtn);
        alignRow.addView(centerBtn);
        alignRow.addView(rightBtn);
        container.addView(alignRow);

        final int[] selectedAlign = {item.align};
        Runnable refreshAlignUI = () -> {
            leftBtn.setEnabled(selectedAlign[0] != TextItem.ALIGN_LEFT);
            centerBtn.setEnabled(selectedAlign[0] != TextItem.ALIGN_CENTER);
            rightBtn.setEnabled(selectedAlign[0] != TextItem.ALIGN_RIGHT);
        };

        View.OnClickListener alignClick = v -> {
            if (v == leftBtn) selectedAlign[0] = TextItem.ALIGN_LEFT;
            else if (v == centerBtn) selectedAlign[0] = TextItem.ALIGN_CENTER;
            else if (v == rightBtn) selectedAlign[0] = TextItem.ALIGN_RIGHT;
            refreshAlignUI.run();
        };

        leftBtn.setOnClickListener(alignClick);
        centerBtn.setOnClickListener(alignClick);
        rightBtn.setOnClickListener(alignClick);
        refreshAlignUI.run();

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Редактировать текст")
                .setView(container)
                .setPositiveButton("OK", (d, w) -> {
                    String newText = textEt.getText().toString();
                    float newSize;
                    try {
                        newSize = Float.parseFloat(sizeEt.getText().toString());
                    } catch (Exception ex) {
                        newSize = item.textSizeSp;
                    }

                    TextItem updated = new TextItem(
                            newText,
                            Math.max(8f, newSize),
                            item.x,
                            item.y,
                            item.typefaceStyle,
                            item.color,
                            selectedAlign[0]
                    );
                    viewModel.updateItem(index, updated);
                })
                .setNeutralButton("Удалить", (d, w) -> viewModel.removeItem(index))
                .setNegativeButton("Отмена", null);

        editDialog = b.create();
        editDialog.setOnDismissListener(d -> editDialog = null);
        editDialog.show();
    }

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted;

                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    Boolean ok = result.getOrDefault(android.Manifest.permission.READ_MEDIA_IMAGES, false);
                    granted = ok != null && ok;
                } else if (android.os.Build.VERSION.SDK_INT >= 23) {
                    Boolean ok = result.getOrDefault(android.Manifest.permission.READ_EXTERNAL_STORAGE, false);
                    granted = ok != null && ok;
                } else {
                    granted = true;
                }

                if (granted) {
                    pickImageLauncher.launch("image/*");
                } else {
                    Toast.makeText(this, "Нет разрешения на чтение изображений", Toast.LENGTH_SHORT).show();
                }
            });

    private void ensurePhotoPermissionThenPick() {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            pickImageLauncher.launch("image/*");
            return;
        }

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*");
            } else {
                permissionLauncher.launch(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES});
            }
        } else {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*");
            } else {
                permissionLauncher.launch(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE});
            }
        }
    }

    private void saveProjectToHistory(@NonNull Uri savedUri, @NonNull String topText, @NonNull String bottomText) {
        new Thread(() -> {
            com.example.memegenerator.data.Meme item =
                    new com.example.memegenerator.data.Meme(
                            savedUri.toString(),
                            topText,
                            bottomText,
                            System.currentTimeMillis()
                    );

            com.example.memegenerator.data.MemeDatabase
                    .getInstance(getApplicationContext())
                    .memeDao()
                    .insert(item);
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}