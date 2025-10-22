package com.example.memegenerator.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memegenerator.R;
import com.example.memegenerator.data.Meme;

import java.util.ArrayList;
import java.util.List;

public class MemeAdapter extends RecyclerView.Adapter<MemeAdapter.VH> {

    public interface OnMemeClick {
        void onOpenImage(Meme meme);
    }

    private final List<Meme> data = new ArrayList<>();
    private final OnMemeClick listener;

    public MemeAdapter(List<Meme> initial, OnMemeClick listener) {
        if (initial != null) data.addAll(initial);
        this.listener = listener;
    }

    public void submit(List<Meme> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meme, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Meme m = data.get(position);
        h.top.setText(m.topText == null ? "" : m.topText);
        h.bottom.setText(m.bottomText == null ? "" : m.bottomText);
        h.date.setText(HistoryActivity.formatDate(m.createdAt));

        // Простейшая загрузка превью из файла
        h.thumb.setImageURI(android.net.Uri.parse(m.imagePath)); // вместо file://


        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpenImage(m);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView thumb;
        TextView top;
        TextView bottom;
        TextView date;

        VH(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);
            top = itemView.findViewById(R.id.topText);
            bottom = itemView.findViewById(R.id.bottomText);
            date = itemView.findViewById(R.id.dateText);
        }
    }
}
