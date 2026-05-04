package com.spendlens.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.spendlens.app.R;
import com.spendlens.app.adapters.CategoryAdapter;
import com.spendlens.app.entities.Category;
import com.spendlens.app.utils.CategoryDisplayUtils;
import com.spendlens.app.viewmodels.CategoryViewModel;

public class CategoryPickerBottomSheet extends BottomSheetDialogFragment
        implements CategoryAdapter.OnCategoryClickListener {

    public interface OnCategorySelectedListener {
        void onCategorySelected(Category category, String iconEmoji);
    }

    private OnCategorySelectedListener listener;
    private CategoryViewModel viewModel;

    public void setOnCategorySelected(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_category, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.rvCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        CategoryAdapter adapter = new CategoryAdapter(this);
        recyclerView.setAdapter(adapter);
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), adapter::submitList);
        return view;
    }

    @Override
    public void onCategoryClick(Category category) {
        if (listener != null) {
            listener.onCategorySelected(
                    category,
                    CategoryDisplayUtils.getEmojiForIconName(category.iconName)
            );
        }
        dismiss();
    }
}
