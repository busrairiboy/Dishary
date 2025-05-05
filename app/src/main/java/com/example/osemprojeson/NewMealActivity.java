package com.example.osemprojeson;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.database.Cursor;

import java.text.DecimalFormat;

public class NewMealActivity extends AppCompatActivity {

    private EditText editTextNewMealName, editTextNewMealPrice;
    private Button buttonSaveNewMeal;
    private LinearLayout mealListContainer;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_meal);

        initializeViews();
        databaseHelper = new DatabaseHelper(this);
        loadMeals();

        buttonSaveNewMeal.setOnClickListener(v -> saveMeal());
    }

    private void initializeViews() {
        editTextNewMealName = findViewById(R.id.editTextNewMealName);
        editTextNewMealPrice = findViewById(R.id.editTextNewMealPrice);
        buttonSaveNewMeal = findViewById(R.id.buttonSaveNewMeal);
        mealListContainer = findViewById(R.id.mealListContainer);
    }

    private void saveMeal() {
        String name = editTextNewMealName.getText().toString().trim();
        String priceText = editTextNewMealPrice.getText().toString().trim();

        if (name.isEmpty() || priceText.isEmpty()) {
            Toast.makeText(this, "Tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            boolean added = databaseHelper.addPredefinedMeal(name, price);

            if (added) {
                Toast.makeText(this, "Yemek eklendi.", Toast.LENGTH_SHORT).show();
                clearInputFields();
                setResult(Activity.RESULT_OK);
                loadMeals();
            } else {
                Toast.makeText(this, "Yemek eklenemedi.", Toast.LENGTH_SHORT).show();
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Geçerli bir fiyat girin.", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearInputFields() {
        editTextNewMealName.setText("");
        editTextNewMealPrice.setText("");
    }

    private void loadMeals() {
        mealListContainer.removeAllViews();
        Cursor cursor = databaseHelper.getAllPredefinedMeals();
        DecimalFormat df = new DecimalFormat("0.00");

        if (cursor.moveToFirst()) {
            do {
                int mealId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PREDEFINED_MEAL_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PREDEFINED_MEAL_NAME));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PREDEFINED_MEAL_PRICE));
                addMealItemToList(mealId, name, price, df);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void addMealItemToList(int mealId, String name, double price, DecimalFormat df) {
        CardView mealItemView = (CardView) LayoutInflater.from(this).inflate(
                R.layout.item_meal, mealListContainer, false);

        TextView textViewMealName = mealItemView.findViewById(R.id.textViewMealName);
        TextView textViewMealPrice = mealItemView.findViewById(R.id.textViewMealPrice);
        ImageButton buttonEdit = mealItemView.findViewById(R.id.buttonEdit);
        ImageButton buttonDelete = mealItemView.findViewById(R.id.buttonDelete);

        textViewMealName.setText(name);
        textViewMealPrice.setText(df.format(price) + "₺");

        buttonEdit.setOnClickListener(v -> showEditDialog(mealId, name, price));
        buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog(mealId));

        mealListContainer.addView(mealItemView);
    }

    private void showEditDialog(int mealId, String currentName, double currentPrice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_meal, null);
        builder.setView(dialogView);

        EditText editTextName = dialogView.findViewById(R.id.editTextEditMealName);
        EditText editTextPrice = dialogView.findViewById(R.id.editTextEditMealPrice);

        editTextName.setText(currentName);
        editTextPrice.setText(String.valueOf(currentPrice));

        builder.setTitle("Yemeği Düzenle")
                .setPositiveButton("Kaydet", (dialog, which) -> {
                    String newName = editTextName.getText().toString().trim();
                    String newPriceText = editTextPrice.getText().toString().trim();

                    if (newName.isEmpty() || newPriceText.isEmpty()) {
                        Toast.makeText(this, "Tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double newPrice = Double.parseDouble(newPriceText);
                        boolean updated = databaseHelper.updatePredefinedMeal(mealId, newName, newPrice);
                        if (updated) {
                            Toast.makeText(this, "Yemek güncellendi.", Toast.LENGTH_SHORT).show();
                            loadMeals();
                        } else {
                            Toast.makeText(this, "Yemek güncellenemedi.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Geçerli bir fiyat girin.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("İptal", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showDeleteConfirmationDialog(int mealId) {
        new AlertDialog.Builder(this)
                .setTitle("Yemeği Sil")
                .setMessage("Bu yemeği silmek istediğinizden emin misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    databaseHelper.deletePredefinedMeal(mealId);
                    Toast.makeText(this, "Yemek silindi.", Toast.LENGTH_SHORT).show();
                    loadMeals();
                })
                .setNegativeButton("Hayır", null)
                .show();
    }
}