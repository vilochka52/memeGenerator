package com.example.memegenerator.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memegenerator.MainActivity;
import com.example.memegenerator.R;
import com.example.memegenerator.data.Meme;
import com.example.memegenerator.data.MemeDatabase;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.google.android.material.appbar.MaterialToolbar;



public class HistoryActivity extends AppCompatActivity implements MemeAdapter.OnMemeClick {
    public static String formatDate(long ts) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(ts);
    }


    private RecyclerView recyclerView;
    private MemeAdapter adapter;
    private View emptyView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        MaterialToolbar tb = findViewById(R.id.historyTopBar);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Мои мемы");
        }



        recyclerView = findViewById(R.id.memeRecycler);
        emptyView = findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemeAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnCreateMeme).setOnClickListener(v -> {
            startActivity(new Intent(HistoryActivity.this, MainActivity.class));
        });

        loadMemes();
    }

    private void loadMemes() {
        AsyncTask.execute(() -> {
            List<Meme> items = MemeDatabase.getInstance(this).memeDao().getAllDesc();
            runOnUiThread(() -> {
                adapter.submit(items);
                toggleEmpty(items == null || items.isEmpty());
            });
        });
    }

    private void toggleEmpty(boolean isEmpty) {
        if (emptyView != null)
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMemes();
    }

    @Override
    public void onOpenImage(Meme meme) {
        Uri uri = Uri.parse(meme.imagePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Открыть мем"));
    }
}
