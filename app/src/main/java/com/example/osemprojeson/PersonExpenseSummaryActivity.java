package com.example.osemprojeson;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class PersonExpenseSummaryActivity extends AppCompatActivity {

    SearchView searchViewPerson;
    Spinner spinnerPerson;
    TextView textViewTitle, textViewName, textViewType, textViewMonthlyExpense, textViewYearlyExpense;
    Button buttonEditExpenses;

    DatabaseHelper databaseHelper;

    ArrayList<Integer> personIds = new ArrayList<>();
    ArrayList<String> personNames = new ArrayList<>();
    ArrayList<Integer> filteredIds = new ArrayList<>();
    ArrayList<String> filteredNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(this);
        databaseHelper.ensureManualExpenseColumnsExist(); // sütunları garanti et
        setContentView(R.layout.activity_person_expense_summary);

        // View tanımları
        searchViewPerson = findViewById(R.id.searchViewPerson);
        spinnerPerson = findViewById(R.id.spinnerPersonSummary);
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewName = findViewById(R.id.textViewName);
        textViewType = findViewById(R.id.textViewType);
        textViewMonthlyExpense = findViewById(R.id.textViewMonthlyExpense);
        textViewYearlyExpense = findViewById(R.id.textViewYearlyExpense);
        buttonEditExpenses = findViewById(R.id.buttonEditExpenses);

        // SearchView ayarı
        searchViewPerson.setIconifiedByDefault(false);
        searchViewPerson.clearFocus();
        searchViewPerson.requestFocusFromTouch();

        loadPersons();

        spinnerPerson.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (filteredIds.size() > position) {
                    updateExpenseSummary(filteredIds.get(position), filteredNames.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        searchViewPerson.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { return false; }
            @Override public boolean onQueryTextChange(String newText) {
                filterSpinner(newText);
                return true;
            }
        });

        buttonEditExpenses.setOnClickListener(v -> {
            int pos = spinnerPerson.getSelectedItemPosition();
            if (pos >= 0 && filteredIds.size() > pos) {
                int selectedId = filteredIds.get(pos);
                String selectedName = filteredNames.get(pos);
                showEditExpenseDialog(selectedId, selectedName);
            } else {
                Toast.makeText(this, "Lütfen bir kişi seçin", Toast.LENGTH_SHORT).show();
            }
        });
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

        filterSpinner("");
    }

    private void filterSpinner(String query) {
        filteredIds.clear();
        filteredNames.clear();

        for (int i = 0; i < personNames.size(); i++) {
            String fullName = personNames.get(i);
            if (fullName.toLowerCase().contains(query.toLowerCase())) {
                filteredNames.add(fullName);
                filteredIds.add(personIds.get(i));
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filteredNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPerson.setAdapter(adapter);
    }

    private void updateExpenseSummary(int personId, String fullName) {
        textViewName.setText("Ad Soyad: " + fullName);

        Cursor cursor = databaseHelper.getPersonById(personId);
        if (cursor.moveToFirst()) {
            String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
            textViewType.setText("Tip: " + type);
        }
        cursor.close();

        Calendar calendar = Calendar.getInstance();
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month = String.format(Locale.getDefault(), "%02d", (calendar.get(Calendar.MONTH) + 1));
        String startMonth = year + "-" + month + "-01";
        String endMonth = year + "-" + month + "-" + calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // ✨ Manuel varsa onu göster, yoksa yemekleri hesapla
        double monthly = databaseHelper.getManualOrMealTotal(personId, startMonth, endMonth);
        double yearly = databaseHelper.getManualOrMealTotal(personId, year + "-01-01", year + "-12-31");

        textViewMonthlyExpense.setText("Aylık Gider: " + String.format(Locale.getDefault(), "%.2f ₺", monthly));
        textViewYearlyExpense.setText("Yıllık Gider: " + String.format(Locale.getDefault(), "%.2f ₺", yearly));
    }

    private void showEditExpenseDialog(int personId, String fullName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Harcamaları Düzenle");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_expense, null);
        EditText editMonthly = dialogView.findViewById(R.id.editTextMonthlyExpense);
        EditText editYearly = dialogView.findViewById(R.id.editTextYearlyExpense);
        builder.setView(dialogView);

        builder.setPositiveButton("Kaydet", (dialog, which) -> {
            try {
                double manualMonthly = Double.parseDouble(editMonthly.getText().toString());
                double manualYearly = Double.parseDouble(editYearly.getText().toString());

                databaseHelper.updateManualExpenses(personId, manualMonthly, manualYearly);
                updateExpenseSummary(personId, fullName);
                Toast.makeText(this, "Harcamalar güncellendi", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Lütfen geçerli sayılar girin", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("İptal", null);
        builder.show();
    }
}
