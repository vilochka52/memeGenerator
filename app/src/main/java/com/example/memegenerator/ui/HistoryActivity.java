package com.example.memegenerator.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memegenerator.MainActivity;
import com.example.memegenerator.R;
import com.example.memegenerator.data.Meme;
import com.example.memegenerator.data.MemeDatabase;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_history);

        MaterialToolbar tb = findViewById(R.id.historyTopBar);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Мои мемы");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appbar), (v, insets) -> {
            int t = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, t, 0, 0);
            return insets;
        });

        recyclerView = findViewById(R.id.memeRecycler);
        emptyView = findViewById(R.id.emptyView);
        adapter = new MemeAdapter(new ArrayList<>(), this);

        int span = getResources().getDisplayMetrics().widthPixels >= 720 ? 2 : 1;
        recyclerView.setLayoutManager(new GridLayoutManager(this, span));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);


        ExtendedFloatingActionButton fab = findViewById(R.id.fabCreate);
        fab.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 8 && fab.isExtended()) fab.shrink();
                else if (dy < -8 && !fab.isExtended()) fab.extend();
            }
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
        if (emptyView != null) emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
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
