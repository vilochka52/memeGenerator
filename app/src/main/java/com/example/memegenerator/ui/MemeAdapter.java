package com.example.memegenerator.ui;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memegenerator.R;
import com.example.memegenerator.data.Meme;

import java.util.ArrayList;
import java.util.List;

public class MemeAdapter extends RecyclerView.Adapter<MemeAdapter.VH> {

    public interface OnProjectActionListener {
        void onOpenImage(Meme item);

        void onEditClick(Meme item);

        void onDeleteClick(Meme item);
    }

    private final List<Meme> data = new ArrayList<>();
    private final OnProjectActionListener listener;

    public MemeAdapter(List<Meme> initial, OnProjectActionListener listener) {
        if (initial != null) {
            data.addAll(initial);
        }
        this.listener = listener;
    }

    public void submit(List<Meme> items) {
        data.clear();
        if (items != null) {
            data.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meme, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Meme item = data.get(position);

        String title = item.topText != null ? item.topText.trim() : "";
        String subtitle = item.bottomText != null ? item.bottomText.trim() : "";

        if (title.isEmpty() && subtitle.isEmpty()) {
            holder.top.setText("Проект без названия");
            holder.bottom.setText("Без добавленного текста");
            holder.bottom.setVisibility(View.VISIBLE);
        } else if (title.isEmpty()) {
            holder.top.setText("Проект");
            holder.bottom.setText(subtitle);
            holder.bottom.setVisibility(View.VISIBLE);
        } else {
            holder.top.setText(title);
            if (subtitle.isEmpty()) {
                holder.bottom.setVisibility(View.GONE);
            } else {
                holder.bottom.setText(subtitle);
                holder.bottom.setVisibility(View.VISIBLE);
            }
        }
        holder.date.setText(HistoryActivity.formatDate(item.createdAt));
        holder.thumb.setImageURI(Uri.parse(item.imagePath));

        holder.itemView.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (listener != null) {
                listener.onOpenImage(item);
            }
        });

        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(120).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    break;
            }
            return false;
        });

        holder.deleteButton.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
            if (listener != null) {
                listener.onDeleteClick(item);
            }
        });

        holder.editButton.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
            if (listener != null) {
                listener.onEditClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView thumb;
        ImageButton editButton;
        ImageButton deleteButton;
        TextView top;
        TextView bottom;
        TextView date;

        VH(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            top = itemView.findViewById(R.id.topText);
            bottom = itemView.findViewById(R.id.bottomText);
            date = itemView.findViewById(R.id.dateText);
        }
    }
}