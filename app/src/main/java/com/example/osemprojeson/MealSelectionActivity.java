package com.example.osemprojeson;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MealSelectionActivity extends AppCompatActivity {

    SearchView searchViewPerson;
    SearchView searchViewMeal;
    Spinner spinnerPerson;
    Button buttonSaveSelectedMeals;
    Button buttonSelectPerson;
    ListView listViewMeals;
    TextView textViewTotalPrice;
    LinearLayout layoutPersonSelection;
    LinearLayout layoutMealSelection;
    TextView textViewSelectedPerson;

    DatabaseHelper databaseHelper;
    ArrayList<Integer> personIds = new ArrayList<>();
    ArrayList<String> personNames = new ArrayList<>();
    ArrayList<String> filteredNames = new ArrayList<>();
    ArrayList<Integer> filteredIds = new ArrayList<>();

    ArrayList<MealItem> mealItemList = new ArrayList<>();
    ArrayList<MealItem> displayedMealList = new ArrayList<>();
    ArrayAdapter<String> personAdapter;
    MealAdapter mealAdapter;

    int selectedPersonId = -1;
    String selectedPersonName = "";
    double totalPrice = 0.0;
    DecimalFormat decimalFormat = new DecimalFormat("0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_selection);

        searchViewPerson = findViewById(R.id.searchViewPerson);
        searchViewMeal = findViewById(R.id.searchViewMeal);
        spinnerPerson = findViewById(R.id.spinnerPerson);
        buttonSaveSelectedMeals = findViewById(R.id.buttonSaveSelectedMeals);
        buttonSelectPerson = findViewById(R.id.buttonSelectPerson);
        listViewMeals = findViewById(R.id.listViewMeals);
        textViewTotalPrice = findViewById(R.id.textViewTotalPrice);
        layoutPersonSelection = findViewById(R.id.layoutPersonSelection);
        layoutMealSelection = findViewById(R.id.layoutMealSelection);
        textViewSelectedPerson = findViewById(R.id.textViewSelectedPerson);

        Button buttonAddPredefinedMeal = findViewById(R.id.buttonAddPredefinedMeal);
        buttonAddPredefinedMeal.setOnClickListener(v -> {
            Intent intent = new Intent(MealSelectionActivity.this, NewMealActivity.class);
            startActivityForResult(intent, 101);
        });

        databaseHelper = new DatabaseHelper(this);

        mealAdapter = new MealAdapter(this, displayedMealList);
        listViewMeals.setAdapter(mealAdapter);

        loadPersons();
        setupSearchView();
        setupMealSearchView();

        spinnerPerson.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && filteredIds.size() > position) {
                    selectedPersonId = filteredIds.get(position);
                    selectedPersonName = filteredNames.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedPersonId = -1;
                selectedPersonName = "";
            }
        });

        buttonSelectPerson.setOnClickListener(v -> {
            if (spinnerPerson.getSelectedItemPosition() == -1 || filteredIds.isEmpty()) {
                Toast.makeText(this, "Lütfen bir kişi seçin", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedPersonId = filteredIds.get(spinnerPerson.getSelectedItemPosition());
            selectedPersonName = filteredNames.get(spinnerPerson.getSelectedItemPosition());

            layoutPersonSelection.setVisibility(View.GONE);
            layoutMealSelection.setVisibility(View.VISIBLE);
            textViewSelectedPerson.setText("Seçilen Kişi: " + selectedPersonName);

            loadPredefinedMeals();
            updateTotalPrice();
        });

        buttonSaveSelectedMeals.setOnClickListener(v -> {
            if (selectedPersonId == -1) {
                Toast.makeText(this, "Kişi seçilmedi.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean anySelected = false;
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            for (MealItem item : mealItemList) {
                if (item.isSelected && item.quantity > 0) {
                    anySelected = true;
                    databaseHelper.addMeal(selectedPersonId, item.name, item.price, item.quantity, date);
                }
            }

            if (!anySelected) {
                Toast.makeText(this, "Lütfen en az bir yemek seçin.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Seçilen yemekler kaydedildi.", Toast.LENGTH_SHORT).show();
            resetSelections();
        });

        layoutMealSelection.setVisibility(View.GONE);
    }

    private void resetSelections() {
        for (MealItem item : mealItemList) {
            item.isSelected = false;
            item.quantity = 1;
        }
        totalPrice = 0.0;
        updateTotalPrice();
        layoutMealSelection.setVisibility(View.GONE);
        layoutPersonSelection.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            loadPredefinedMeals();
            updateTotalPrice();
        }
    }

    private void loadPersons() {
        personIds.clear();
        personNames.clear();
        Cursor cursor = databaseHelper.getAllPersons();
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String surname = cursor.getString(cursor.getColumnIndexOrThrow("surname"));
                personIds.add(id);
                personNames.add(name + " " + surname);
            } while (cursor.moveToNext());
        }
        cursor.close();

        filteredNames.clear();
        filteredIds.clear();
        filteredNames.addAll(personNames);
        filteredIds.addAll(personIds);
        personAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filteredNames);
        personAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPerson.setAdapter(personAdapter);
    }

    private void setupSearchView() {
        searchViewPerson.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filteredNames.clear();
                filteredIds.clear();
                for (int i = 0; i < personNames.size(); i++) {
                    if (personNames.get(i).toLowerCase().contains(newText.toLowerCase())) {
                        filteredNames.add(personNames.get(i));
                        filteredIds.add(personIds.get(i));
                    }
                }

                // Spinner uyumlu değil, yeni adapter oluşturmak gerek
                personAdapter = new ArrayAdapter<>(MealSelectionActivity.this, android.R.layout.simple_spinner_item, filteredNames);
                personAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerPerson.setAdapter(personAdapter);

                return true;
            }
        });
    }

    private void setupMealSearchView() {
        searchViewMeal.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                displayedMealList.clear();
                for (MealItem item : mealItemList) {
                    if (item.name.toLowerCase().contains(newText.toLowerCase())) {
                        displayedMealList.add(item);
                    }
                }
                mealAdapter.notifyDataSetChanged();
                return true;
            }
        });
    }

    private void loadPredefinedMeals() {
        mealItemList.clear();
        displayedMealList.clear();
        Cursor cursor = databaseHelper.getAllPredefinedMeals();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PREDEFINED_MEAL_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PREDEFINED_MEAL_NAME));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PREDEFINED_MEAL_PRICE));
                MealItem item = new MealItem(id, name, price);
                mealItemList.add(item);
                displayedMealList.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }
        mealAdapter.notifyDataSetChanged();
    }

    private void updateTotalPrice() {
        totalPrice = 0.0;
        for (MealItem item : mealItemList) {
            if (item.isSelected) {
                totalPrice += (item.price * item.quantity);
            }
        }
        textViewTotalPrice.setText("Toplam: " + decimalFormat.format(totalPrice) + "₺");
    }

    static class MealItem {
        int id;
        String name;
        double price;
        boolean isSelected = false;
        int quantity = 1;

        MealItem(int id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    }

    class MealAdapter extends ArrayAdapter<MealItem> {
        public MealAdapter(android.content.Context context, List<MealItem> meals) {
            super(context, 0, meals);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MealItem item = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.meal_list_item, parent, false);
            }

            CheckBox checkBoxMeal = convertView.findViewById(R.id.checkBoxMeal);
            TextView textViewMealInfo = convertView.findViewById(R.id.textViewMealInfo);
            LinearLayout layoutQuantity = convertView.findViewById(R.id.layoutQuantity);
            Button buttonDecrease = convertView.findViewById(R.id.buttonDecrease);
            Button buttonIncrease = convertView.findViewById(R.id.buttonIncrease);
            TextView textViewQuantity = convertView.findViewById(R.id.textViewQuantity);

            textViewMealInfo.setText(item.name + " - " + decimalFormat.format(item.price) + "₺");
            checkBoxMeal.setChecked(item.isSelected);
            textViewQuantity.setText(String.valueOf(item.quantity));
            layoutQuantity.setVisibility(item.isSelected ? View.VISIBLE : View.GONE);

            checkBoxMeal.setOnClickListener(v -> {
                item.isSelected = checkBoxMeal.isChecked();
                layoutQuantity.setVisibility(item.isSelected ? View.VISIBLE : View.GONE);
                updateTotalPrice();
            });

            buttonDecrease.setOnClickListener(v -> {
                if (item.quantity > 1) {
                    item.quantity--;
                    textViewQuantity.setText(String.valueOf(item.quantity));
                    updateTotalPrice();
                }
            });

            buttonIncrease.setOnClickListener(v -> {
                item.quantity++;
                textViewQuantity.setText(String.valueOf(item.quantity));
                updateTotalPrice();
            });

            return convertView;
        }
    }
}
