package com.example.osemprojeson;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class PredefinedMeal extends AppCompatActivity {

    EditText editTextMealName, editTextMealPrice;
    Button buttonAddMeal;
    ListView listViewMeals;

    DatabaseHelper databaseHelper;
    ArrayList<String> mealList = new ArrayList<>();
    ArrayList<Integer> mealIds = new ArrayList<>();
    ArrayAdapter<String> mealAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predefined_meal);

        editTextMealName = findViewById(R.id.editTextMealName);
        editTextMealPrice = findViewById(R.id.editTextMealPrice);
        buttonAddMeal = findViewById(R.id.buttonAddMeal);
        listViewMeals = findViewById(R.id.listViewPredefinedMeals);

        databaseHelper = new DatabaseHelper(this);

        buttonAddMeal.setOnClickListener(v -> addPredefinedMeal());
        listViewMeals.setOnItemLongClickListener((parent, view, position, id) -> {
            showUpdateDeleteDialog(position);
            return true;
        });

        loadPredefinedMeals();
    }

    private void addPredefinedMeal() {
        String name = editTextMealName.getText().toString().trim();
        String priceText = editTextMealPrice.getText().toString().trim();

        if (name.isEmpty() || priceText.isEmpty()) {
            Toast.makeText(this, "Tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceText);
        boolean success = databaseHelper.addPredefinedMeal(name, price);

        if (success) {
            Toast.makeText(this, "Yemek eklendi.", Toast.LENGTH_SHORT).show();
            editTextMealName.setText("");
            editTextMealPrice.setText("");
            loadPredefinedMeals();
        } else {
            Toast.makeText(this, "Ekleme başarısız.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPredefinedMeals() {
        mealList.clear();
        mealIds.clear();

        Cursor cursor = databaseHelper.getAllPredefinedMeals();
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("meal_name"));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("meal_price"));

                mealList.add(name + " - " + price + " ₺");
                mealIds.add(id);
            } while (cursor.moveToNext());
        }
        cursor.close();

        mealAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mealList);
        listViewMeals.setAdapter(mealAdapter);
    }

    private void showUpdateDeleteDialog(int position) {
        String selectedMeal = mealList.get(position);
        int mealId = mealIds.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("İşlem Seçin");
        builder.setItems(new String[]{"Güncelle", "Sil"}, (dialog, which) -> {
            if (which == 0) {
                showUpdateDialog(mealId);
            } else {
                databaseHelper.deletePredefinedMeal(mealId);
                Toast.makeText(this, "Silindi", Toast.LENGTH_SHORT).show();
                loadPredefinedMeals();
            }
        });
        builder.show();
    }

    private void showUpdateDialog(int mealId) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_meal, null);
        EditText editName = dialogView.findViewById(R.id.editUpdateMealName);
        EditText editPrice = dialogView.findViewById(R.id.editUpdateMealPrice);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Yemek Güncelle");
        builder.setView(dialogView);

        builder.setPositiveButton("Kaydet", (dialog, which) -> {
            String newName = editName.getText().toString().trim();
            String newPriceText = editPrice.getText().toString().trim();
            if (!newName.isEmpty() && !newPriceText.isEmpty()) {
                double newPrice = Double.parseDouble(newPriceText);
                boolean success = databaseHelper.updatePredefinedMeal(mealId, newName, newPrice);
                if (success) {
                    Toast.makeText(this, "Güncellendi", Toast.LENGTH_SHORT).show();
                    loadPredefinedMeals();
                } else {
                    Toast.makeText(this, "Hata oluştu", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("İptal", null);
        builder.show();
    }
}
