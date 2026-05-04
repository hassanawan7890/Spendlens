package com.spendlens.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spendlens.app.R;
import com.spendlens.app.entities.Category;
import com.spendlens.app.utils.CategoryDisplayUtils;

import java.util.Objects;

public class CategoryAdapter extends ListAdapter<Category, CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final OnCategoryClickListener listener;

    public CategoryAdapter(OnCategoryClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Category> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Category>() {
                @Override
                public boolean areItemsTheSame(@NonNull Category a, @NonNull Category b) {
                    return a.categoryId == b.categoryId;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Category a, @NonNull Category b) {
                    return Objects.equals(a.categoryName, b.categoryName)
                            && Objects.equals(a.iconName, b.iconName)
                            && a.spendingLimit == b.spendingLimit
                            && a.isDefault == b.isDefault;
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = getItem(position);
        holder.icon.setText(CategoryDisplayUtils.getEmojiForIconName(category.iconName));
        holder.name.setText(category.categoryName);
        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView icon;
        TextView name;

        ViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.tvCatIcon);
            name = view.findViewById(R.id.tvCatName);
        }
    }
}
