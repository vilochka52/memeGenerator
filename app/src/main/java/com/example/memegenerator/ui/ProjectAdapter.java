package com.example.memegenerator.ui;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.memegenerator.R;
import com.example.memegenerator.data.Project;
import com.example.memegenerator.databinding.ItemProjectBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onOpen(Project item);
        void onEdit(Project item);
        void onDelete(Project item);
        void onRename(Project item);
    }

    private final List<ProjectListItem> items;
    private final Listener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public ProjectAdapter(List<ProjectListItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ProjectListItem.TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_project_header, parent, false);
            return new HeaderViewHolder(view);
        }

        ItemProjectBinding binding = ItemProjectBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ProjectViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ProjectListItem item = items.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).headerText.setText(item.headerTitle);
            return;
        }

        Project project = item.project;
        ProjectViewHolder vh = (ProjectViewHolder) holder;

        String name = project.projectName != null && !project.projectName.trim().isEmpty()
                ? project.projectName
                : holder.itemView.getContext().getString(R.string.new_project);

        String date = dateFormat.format(new Date(project.createdAt));

        vh.binding.topText.setText(name);
        vh.binding.bottomText.setText(R.string.rename_hint);
        vh.binding.dateText.setText(date);

        if (project.previewImagePath != null && !project.previewImagePath.trim().isEmpty()) {
            vh.binding.thumb.setImageURI(Uri.parse(project.previewImagePath));
        } else {
            vh.binding.thumb.setImageDrawable(null);
        }

        vh.binding.historyCard.setOnClickListener(v -> listener.onOpen(project));
        vh.binding.editButton.setOnClickListener(v -> listener.onEdit(project));
        vh.binding.deleteButton.setOnClickListener(v -> listener.onDelete(project));
        vh.binding.topText.setOnClickListener(v -> listener.onRename(project));
        vh.binding.bottomText.setOnClickListener(v -> listener.onRename(project));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView headerText;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
        }
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        final ItemProjectBinding binding;

        ProjectViewHolder(ItemProjectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}