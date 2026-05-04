package com.spendlens.app.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.spendlens.app.R;
import com.spendlens.app.adapters.CategoryAdapter;
import com.spendlens.app.databinding.ActivityManageCategoriesBinding;
import com.spendlens.app.entities.Category;
import com.spendlens.app.viewmodels.CategoryViewModel;

public class ManageCategoriesActivity extends AppCompatActivity {

    private ActivityManageCategoriesBinding binding;
    private CategoryViewModel categoryVm;
    private CategoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageCategoriesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        categoryVm = new ViewModelProvider(this).get(CategoryViewModel.class);

        adapter = new CategoryAdapter(cat -> showCategoryOptions(cat));
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCategories.setAdapter(adapter);

        categoryVm.getAllCategories().observe(this, cats -> {
            if (cats != null) adapter.submitList(cats);
        });

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void showAddCategoryDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        TextInputLayout layout = view.findViewById(R.id.layoutCategoryName);
        TextInputEditText edit  = view.findViewById(R.id.editCategoryName);

        new AlertDialog.Builder(this)
                .setTitle("New Category")
                .setView(view)
                .setPositiveButton("Add", (d, w) -> {
                    String name = edit.getText() != null ? edit.getText().toString().trim() : "";
                    if (name.isEmpty()) { Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show(); return; }
                    Category cat = new Category(name, "ic_others", 0.0, false);
                    categoryVm.insert(cat);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCategoryOptions(Category cat) {
        String[] options = cat.isDefault
                ? new String[]{"View"}
                : new String[]{"Rename", "Delete"};

        new AlertDialog.Builder(this)
                .setTitle(cat.categoryName)
                .setItems(options, (d, which) -> {
                    if (!cat.isDefault) {
                        if (which == 1) confirmDelete(cat);
                        else showRenameDialog(cat);
                    }
                }).show();
    }

    private void showRenameDialog(Category cat) {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        TextInputEditText edit = view.findViewById(R.id.editCategoryName);
        edit.setText(cat.categoryName);

        new AlertDialog.Builder(this)
                .setTitle("Rename Category")
                .setView(view)
                .setPositiveButton("Save", (d, w) -> {
                    String name = edit.getText() != null ? edit.getText().toString().trim() : "";
                    if (!name.isEmpty()) { cat.categoryName = name; categoryVm.update(cat); }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void confirmDelete(Category cat) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + cat.categoryName + "?")
                .setMessage("Expenses in this category will be moved to Others.")
                .setPositiveButton("Delete", (d, w) -> categoryVm.delete(cat))
                .setNegativeButton("Cancel", null).show();
    }
}