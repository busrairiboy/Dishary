package com.example.osemprojeson;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.*;
import android.view.View;
import android.view.Gravity;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.*;

public class GeneralExpenseActivity extends AppCompatActivity {

    EditText editTextExpenseName, editTextExpenseAmount;
    Button buttonSaveExpense;
    LinearLayout linearLayoutExpensesContainer;
    TextView textViewMonthlyExpense, textViewYearlyExpense;

    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TEST", "GeneralExpenseActivity açıldı");
        setContentView(R.layout.activity_general_expense);

        // View bileşenlerini bağla
        editTextExpenseName = findViewById(R.id.editTextExpenseName);
        editTextExpenseAmount = findViewById(R.id.editTextExpenseAmount);
        buttonSaveExpense = findViewById(R.id.buttonSaveExpense);
        linearLayoutExpensesContainer = findViewById(R.id.linearLayoutExpensesContainer);
        textViewMonthlyExpense = findViewById(R.id.textViewMonthlyExpense);
        textViewYearlyExpense = findViewById(R.id.textViewYearlyExpense);

        databaseHelper = new DatabaseHelper(this);

        // Kaydet butonu dinleyicisi
        buttonSaveExpense.setOnClickListener(v -> addExpense());

        // Başlangıçta yükle
        loadExpenses();
        updateExpenseSummary();
    }

    private void addExpense() {
        String name = editTextExpenseName.getText().toString().trim();
        String amountText = editTextExpenseAmount.getText().toString().trim();

        if (name.isEmpty() || amountText.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Geçerli bir tutar girin.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        boolean success = databaseHelper.addGeneralExpense(name, amount, currentDate);

        if (success) {
            Toast.makeText(this, "Gider kaydedildi.", Toast.LENGTH_SHORT).show();
            editTextExpenseName.setText("");
            editTextExpenseAmount.setText("");
            loadExpenses();
            updateExpenseSummary();
        } else {
            Toast.makeText(this, "Gider eklenemedi.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadExpenses() {
        linearLayoutExpensesContainer.removeAllViews(); // Önceki giderleri temizle
        Cursor cursor = databaseHelper.getAllGeneralExpenses();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("expense_name"));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("expense_amount"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("expense_date"));

                addExpenseCard(name, amount, date);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private void addExpenseCard(String name, double amount, String date) {
        // Kart görünümü oluştur
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(36, 24, 36, 24);
        card.setBackground(ContextCompat.getDrawable(this, R.drawable.card_background));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) card.getLayoutParams()).setMargins(0, 0, 0, 24);

        // Gider Adı
        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextSize(16f);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setTextColor(Color.DKGRAY);

        // Tutar ve Tarih
        TextView detailView = new TextView(this);
        detailView.setText("₺" + String.format(Locale.getDefault(), "%.2f", amount) + " • " + date);
        detailView.setTextSize(14f);
        detailView.setTextColor(Color.GRAY);

        card.addView(nameView);
        card.addView(detailView);

        linearLayoutExpensesContainer.addView(card);
    }

    private void updateExpenseSummary() {
        double monthlyExpense = calculateTotalExpenseForPeriod("monthly");
        textViewMonthlyExpense.setText("Aylık Gider: " + String.format(Locale.getDefault(), "%.2f", monthlyExpense) + "₺");

        double yearlyExpense = calculateTotalExpenseForPeriod("yearly");
        textViewYearlyExpense.setText("Yıllık Gider: " + String.format(Locale.getDefault(), "%.2f", yearlyExpense) + "₺");
    }

    private double calculateTotalExpenseForPeriod(String period) {
        String startDate = "";
        String endDate = "";

        Calendar calendar = Calendar.getInstance();

        if (period.equals("monthly")) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        } else if (period.equals("yearly")) {
            calendar.set(Calendar.DAY_OF_YEAR, 1);
            startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

            calendar.set(Calendar.MONTH, Calendar.DECEMBER);
            calendar.set(Calendar.DAY_OF_MONTH, 31);
            endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        }

        return databaseHelper.getTotalExpensesForPeriod(startDate, endDate);
    }
}
