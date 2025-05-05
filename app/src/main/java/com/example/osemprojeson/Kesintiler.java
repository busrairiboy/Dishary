package com.example.osemprojeson;

import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.*;

public class Kesintiler extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private LinearLayout layoutContainer;
    private Spinner spinnerPersonelTypeFilter, spinnerMonth, spinnerYear, spinnerTimeFilter;
    private TextView textViewTotalExpense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kesintiler);

        layoutContainer = findViewById(R.id.layoutContainer);
        textViewTotalExpense = findViewById(R.id.textViewTotalExpense);
        spinnerPersonelTypeFilter = findViewById(R.id.spinnerFilter);
        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerTimeFilter = findViewById(R.id.spinnerTimeFilter);
        databaseHelper = new DatabaseHelper(this);

        setupSpinners();
        applyFilters();
    }

    private void setupSpinners() {
        spinnerPersonelTypeFilter.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Arrays.asList("Tümü", "Personel", "Misafir")));

        spinnerTimeFilter.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, Arrays.asList("Aylık", "Yıllık")));

        List<String> months = Arrays.asList(
                "14 Ocak - 13 Şubat", "14 Şubat - 13 Mart", "14 Mart - 13 Nisan", "14 Nisan - 13 Mayıs",
                "14 Mayıs - 13 Haziran", "14 Haziran - 13 Temmuz", "14 Temmuz - 13 Ağustos", "14 Ağustos - 13 Eylül",
                "14 Eylül - 13 Ekim", "14 Ekim - 13 Kasım", "14 Kasım - 13 Aralık", "14 Aralık - 13 Ocak"
        );
        spinnerMonth.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months));

        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = 0; i <= 5; i++) years.add(String.valueOf(currentYear - i));
        spinnerYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years));

        Calendar now = Calendar.getInstance();
        spinnerMonth.setSelection(now.get(Calendar.MONTH));
        spinnerYear.setSelection(0);
        spinnerTimeFilter.setSelection(0);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerPersonelTypeFilter.setOnItemSelectedListener(listener);
        spinnerMonth.setOnItemSelectedListener(listener);
        spinnerYear.setOnItemSelectedListener(listener);
        spinnerTimeFilter.setOnItemSelectedListener(listener);
    }

    private void applyFilters() {
        layoutContainer.removeAllViews();

        int selectedMonth = spinnerMonth.getSelectedItemPosition();
        int selectedYear = Integer.parseInt(spinnerYear.getSelectedItem().toString());
        String personTypeFilter = spinnerPersonelTypeFilter.getSelectedItem().toString();
        String timeFilter = spinnerTimeFilter.getSelectedItem().toString();

        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();

        if (timeFilter.equals("Yıllık")) {
            start.set(selectedYear, Calendar.JANUARY, 1);
            end.set(selectedYear, Calendar.DECEMBER, 31);
        } else {
            start.set(selectedYear, selectedMonth, 14);
            end.set(selectedYear, selectedMonth, 14);
            end.add(Calendar.MONTH, 1);
            end.add(Calendar.DAY_OF_MONTH, -1);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startDate = sdf.format(start.getTime());
        String endDate = sdf.format(end.getTime());

        Calendar now = Calendar.getInstance();
        boolean isPastOrCurrent = !start.after(now);

        double grandTotal = 0.0;

        Cursor cursor = databaseHelper.getAllPersons();
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String surname = cursor.getString(cursor.getColumnIndexOrThrow("surname"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));

                if (!personTypeFilter.equals("Tümü") && !type.equalsIgnoreCase(personTypeFilter)) continue;

                if (databaseHelper.hasMealDataBetweenDates(id, startDate, endDate)) {
                    double total = databaseHelper.getManualOrMealTotal(id, startDate, endDate);
                    grandTotal += total;
                    addExpenseCard(name + " " + surname, type, total);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        textViewTotalExpense.setText("Toplam Gider: " + String.format(Locale.getDefault(), "%.2f ₺", grandTotal));
    }

    private void addExpenseCard(String fullName, String type, double total) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 24, 32, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        card.setLayoutParams(params);

        card.setBackgroundResource(R.drawable.card_background);

        TextView nameView = new TextView(this);
        nameView.setText(fullName);
        nameView.setTextSize(18);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView typeView = new TextView(this);
        typeView.setText("Tip: " + (type.equalsIgnoreCase("misafir") ? "Misafir" : "Personel"));
        typeView.setTextSize(15);

        TextView amountView = new TextView(this);
        amountView.setTextSize(16);
        amountView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        amountView.setPadding(0, 8, 0, 0);
        amountView.setText("Harcamasi: " + String.format(Locale.getDefault(), "%.2f ₺", total));

        card.addView(nameView);
        card.addView(typeView);
        card.addView(amountView);
        layoutContainer.addView(card);
    }
}