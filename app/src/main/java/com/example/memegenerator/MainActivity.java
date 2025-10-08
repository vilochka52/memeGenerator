package com.example.memegenerator;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private MemeView memeView;
    private EditText editText;
    private Button btnAddText, btnSelectImage;
    private Spinner fontSpinner;
    private ActivityResultLauncher<String> imagePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        memeView = findViewById(R.id.memeView);
        editText = findViewById(R.id.editText);
        btnAddText = findViewById(R.id.btnAddText);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        fontSpinner = findViewById(R.id.fontSpinner);

        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) memeView.setImageUri(uri);
        });

        btnSelectImage.setOnClickListener(v -> imagePicker.launch("image/*"));

        btnAddText.setOnClickListener(v -> {
            String text = editText.getText().toString().trim();
            if (!text.isEmpty()) memeView.addTextBlock(text);
        });

        ArrayAdapter<CharSequence> fontAdapter = ArrayAdapter.createFromResource(this,
                R.array.fonts_array, android.R.layout.simple_spinner_item);
        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontSpinner.setAdapter(fontAdapter);

        fontSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int pos, long id) {
                memeView.setFontByIndex(pos);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
}
