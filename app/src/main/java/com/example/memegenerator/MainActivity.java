package com.example.memegenerator;
import android.view.View;
import android.widget.Button;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.memegenerator.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    @androidx.annotation.Nullable
    private AlertDialog editDialog;


    private ActivityMainBinding binding;
    private MemeViewModel viewModel;
    private ActivityResultLauncher<String> pickImageLauncher;

    // ⚠️ НИКАКИХ инициализаций, которые требуют Context, вне onCreate() !
    public MainActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MemeViewModel.class);
        viewModel.getTextItems().observe(this, items -> binding.memeView.setTextItems(items));

        // двойной тап по тексту -> диалог
        binding.memeView.setOnTextEditRequestListener(this::showEditDialog);
        // по завершению перетаскивания — сохранить координаты в VM (исправляет "телепорт")
        binding.memeView.setOnTextMovedListener((index, item) -> viewModel.updateItem(index, item));

        // Пикер фото
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onImagePicked
        );

        binding.btnPick.setOnClickListener(v -> ensurePhotoPermissionThenPick());

        binding.btnAddText.setOnClickListener(v -> {
            float x = Math.max(40f, binding.memeView.getWidth() * 0.5f);
            float y = Math.max(80f,  binding.memeView.getHeight() * 0.35f);
            viewModel.addTextCentered("Ваш текст", 32f, x, y);
            Toast.makeText(this, "Двойной тап по тексту — редактировать", Toast.LENGTH_SHORT).show();
        });

        binding.btnSave.setOnClickListener(v -> saveCurrentMeme());
    }

    private void onImagePicked(Uri uri) {
        if (uri == null) {
            Toast.makeText(this, "Изображение не выбрано", Toast.LENGTH_SHORT).show();
            return;
        }

        // Размер для семплинга: берём размеры канвы (если она ещё 0 — подставим экран)
        int tw = binding.memeView.getWidth();
        int th = binding.memeView.getHeight();
        if (tw <= 0 || th <= 0) {
            var dm = getResources().getDisplayMetrics();
            tw = dm.widthPixels;
            th = dm.heightPixels;
        }

        Bitmap bmp = ImageLoader.loadBitmapFromUri(this, uri, tw, th);
        if (bmp == null) {
            Toast.makeText(this, "Не удалось загрузить изображение", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.memeView.addImageBitmap(bmp);
    }

    private void saveCurrentMeme() {
        Bitmap out = binding.memeView.exportToBitmapAtOriginal();
        new Thread(() -> {
            Uri saved = MemeRepository.saveBitmapToGallery(
                    this, out, "meme_" + System.currentTimeMillis() + ".png");
            runOnUiThread(() -> {
                if (saved != null) Toast.makeText(this, "Сохранено в Галерею", Toast.LENGTH_SHORT).show();
                else Toast.makeText(this, "Не удалось сохранить", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void showEditDialog(int index, @NonNull TextItem item) {
        // Если уже открыт диалог редактирования – просто игнорируем повторный дабл-тап,
        // чтобы не пытаться добавить тот же view ещё раз.
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

        // --- блок выравнивания, если уже добавлял align ---
        // Можно убрать этот блок, если он у тебя уже есть в текущей версии showEditDialog
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

        final int[] selectedAlign = { item.align };
        Runnable refreshAlignUI = () -> {
            leftBtn.setEnabled(selectedAlign[0] != TextItem.ALIGN_LEFT);
            centerBtn.setEnabled(selectedAlign[0] != TextItem.ALIGN_CENTER);
            rightBtn.setEnabled(selectedAlign[0] != TextItem.ALIGN_RIGHT);
        };
        android.view.View.OnClickListener alignClick = v -> {
            if (v == leftBtn) selectedAlign[0] = TextItem.ALIGN_LEFT;
            else if (v == centerBtn) selectedAlign[0] = TextItem.ALIGN_CENTER;
            else if (v == rightBtn) selectedAlign[0] = TextItem.ALIGN_RIGHT;
            refreshAlignUI.run();
        };
        leftBtn.setOnClickListener(alignClick);
        centerBtn.setOnClickListener(alignClick);
        rightBtn.setOnClickListener(alignClick);
        refreshAlignUI.run();
        // --- конец блока выравнивания ---

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Редактировать текст")
                .setView(container)
                .setPositiveButton("OK", (d, w) -> {
                    String newText = textEt.getText().toString();
                    float newSize;
                    try { newSize = Float.parseFloat(sizeEt.getText().toString()); }
                    catch (Exception ex) { newSize = item.textSizeSp; }

                    TextItem updated = new TextItem(
                            newText,
                            Math.max(8f, newSize),
                            item.x, item.y,
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
                boolean granted = false;
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    Boolean ok = result.getOrDefault(android.Manifest.permission.READ_MEDIA_IMAGES, false);
                    granted = ok != null && ok;
                } else if (android.os.Build.VERSION.SDK_INT >= 23) {
                    Boolean ok = result.getOrDefault(android.Manifest.permission.READ_EXTERNAL_STORAGE, false);
                    granted = ok != null && ok;
                } else {
                    granted = true; // на <23 уже выдано при установке
                }
                if (granted) {
                    pickImageLauncher.launch("image/*");
                } else {
                    Toast.makeText(this, "Нет разрешения на чтение изображений", Toast.LENGTH_SHORT).show();
                }
            });

    private void ensurePhotoPermissionThenPick() {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            // старые Android — разрешение уже есть
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
}
