package com.example.memegenerator;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LayerAdapter extends RecyclerView.Adapter<LayerAdapter.LayerViewHolder> {

    public interface Listener {
        void onToggleVisibility(@NonNull LayerRowItem item);
        void onDelete(@NonNull LayerRowItem item);
        void onOpacityChanged(@NonNull LayerRowItem item, float alpha);
        void onMove(int fromPosition, int toPosition);
    }

    private final Listener listener;
    private final List<LayerRowItem> items = new ArrayList<>();

    public LayerAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<LayerRowItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    public List<LayerRowItem> getItems() {
        return items;
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= items.size() || toPosition >= items.size()) {
            return;
        }
        Collections.swap(items, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        listener.onMove(fromPosition, toPosition);
    }

    @NonNull
    @Override
    public LayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layer_row, parent, false);
        return new LayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LayerViewHolder holder, int position) {
        LayerRowItem item = items.get(position);

        holder.layerName.setText(item.title);
        holder.layerOpacity.setProgress(Math.round(item.alpha * 100f));

        if (item.isImageLayer() || item.isDrawLayer()) {
            Bitmap thumb = item.thumbnail;
            if (thumb != null) {
                holder.layerThumb.setImageBitmap(thumb);
            } else {
                holder.layerThumb.setImageResource(item.isDrawLayer() ? R.drawable.ic_brush : R.drawable.ic_photo);
            }
        } else {
            holder.layerThumb.setImageResource(R.drawable.ic_text);
        }

        holder.btnLayerVisible.setImageResource(
                item.visible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off
        );

        holder.btnLayerVisible.setOnClickListener(v -> listener.onToggleVisibility(item));
        holder.btnLayerDelete.setOnClickListener(v -> listener.onDelete(item));

        holder.layerOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                listener.onOpacityChanged(item, progress / 100f);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LayerViewHolder extends RecyclerView.ViewHolder {
        ImageView layerThumb;
        TextView layerName;
        SeekBar layerOpacity;
        ImageButton btnLayerVisible;
        ImageButton btnLayerDelete;

        public LayerViewHolder(@NonNull View itemView) {
            super(itemView);
            layerThumb = itemView.findViewById(R.id.layerThumb);
            layerName = itemView.findViewById(R.id.layerName);
            layerOpacity = itemView.findViewById(R.id.layerOpacity);
            btnLayerVisible = itemView.findViewById(R.id.btnLayerVisible);
            btnLayerDelete = itemView.findViewById(R.id.btnLayerDelete);
        }
    }
}