package com.example.osemprojeson;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button buttonAddPerson, buttonMealSelection, buttonPersonExpenses, buttonGeneralExpenses,buttonnewmeal,buttonkesintiler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonAddPerson = findViewById(R.id.buttonAddPerson);
        buttonMealSelection = findViewById(R.id.buttonMealSelection);
        buttonPersonExpenses = findViewById(R.id.buttonPersonExpenses);
        buttonGeneralExpenses = findViewById(R.id.buttonGeneralExpenses);
        buttonnewmeal = findViewById(R.id.buttonnewmeal);
        buttonkesintiler = findViewById(R.id.buttonkesintiler);

        buttonAddPerson.setOnClickListener(v -> startActivity(new Intent(this, AddPersonActivity.class)));

        // Güncellenen kısım: Artık NewMealActivity'e yönlendiriyor
        buttonMealSelection.setOnClickListener(v -> startActivity(new Intent(this, MealSelectionActivity.class)));

        buttonPersonExpenses.setOnClickListener(v -> startActivity(new Intent(this, PersonExpenseSummaryActivity.class)));
        buttonGeneralExpenses.setOnClickListener(v -> startActivity(new Intent(this, GeneralExpenseActivity.class)));
        buttonnewmeal.setOnClickListener(v -> startActivity(new Intent(this, NewMealActivity.class)));
        buttonkesintiler.setOnClickListener(v -> startActivity(new Intent(this,Kesintiler.class)));
    }
}
