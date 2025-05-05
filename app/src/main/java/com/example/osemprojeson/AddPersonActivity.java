package com.example.osemprojeson;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AddPersonActivity extends AppCompatActivity {

    EditText editTextName, editTextSurname;
    Spinner spinnerType;
    Button buttonSavePerson;
    LinearLayout linearLayoutPersons;
    SearchView searchViewPersons;

    DatabaseHelper databaseHelper;
    ArrayList<PersonItem> personList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_person);

        editTextName = findViewById(R.id.editTextName);
        editTextSurname = findViewById(R.id.editTextSurname);
        spinnerType = findViewById(R.id.spinnerType);
        buttonSavePerson = findViewById(R.id.buttonSavePerson);
        linearLayoutPersons = findViewById(R.id.linearLayoutPersons);
        searchViewPersons = findViewById(R.id.searchViewPersons);

        databaseHelper = new DatabaseHelper(this);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Misafir", "Personel"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(spinnerAdapter);

        buttonSavePerson.setOnClickListener(v -> addPerson());

        setupSearchView();
        loadPersons();
    }

    private void addPerson() {
        String name = editTextName.getText().toString().trim();
        String surname = editTextSurname.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();

        if (name.isEmpty() || surname.isEmpty()) {
            Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = databaseHelper.addPerson(name, surname, type);
        if (success) {
            Toast.makeText(this, "Kişi başarıyla eklendi.", Toast.LENGTH_SHORT).show();
            loadPersons();
            editTextName.setText("");
            editTextSurname.setText("");
        } else {
            Toast.makeText(this, "Kayıt başarısız!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPersons() {
        personList.clear();
        linearLayoutPersons.removeAllViews();

        Cursor cursor = databaseHelper.getAllPersons();
        if (cursor.moveToFirst()) {
            do {
                personList.add(new PersonItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("surname")),
                        cursor.getString(cursor.getColumnIndexOrThrow("type"))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();

        Collections.sort(personList, Comparator.comparing(p -> p.name.toLowerCase()));
        displayFilteredPersons(personList);
    }

    private void displayFilteredPersons(ArrayList<PersonItem> list) {
        linearLayoutPersons.removeAllViews();
        for (PersonItem person : list) {
            View personView = LayoutInflater.from(this).inflate(R.layout.person_card_item, linearLayoutPersons, false);

            TextView textViewPersonName = personView.findViewById(R.id.textViewPersonName);
            TextView textViewPersonType = personView.findViewById(R.id.textViewPersonType);
            ImageView imageViewEdit = personView.findViewById(R.id.imageViewEdit);
            ImageView imageViewDelete = personView.findViewById(R.id.imageViewDelete);
            CardView cardViewPerson = personView.findViewById(R.id.cardViewPerson);

            textViewPersonName.setText(person.name + " " + person.surname);
            textViewPersonType.setText(person.type);

            cardViewPerson.setCardBackgroundColor(ContextCompat.getColor(this,
                    "Personel".equals(person.type) ? R.color.staffColor : R.color.guestColor));

            imageViewEdit.setOnClickListener(v -> updatePersonDialog(person.id));
            imageViewDelete.setOnClickListener(v -> confirmDeletePerson(person));

            linearLayoutPersons.addView(personView);
        }
    }

    private void setupSearchView() {
        searchViewPersons.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPersons(newText);
                return true;
            }
        });
    }

    private void filterPersons(String text) {
        if (text.isEmpty()) {
            displayFilteredPersons(personList);
            return;
        }
        ArrayList<PersonItem> filtered = new ArrayList<>();
        String search = text.toLowerCase();
        for (PersonItem person : personList) {
            String fullName = (person.name + " " + person.surname).toLowerCase();
            if (fullName.contains(search) || person.type.toLowerCase().contains(search)) {
                filtered.add(person);
            }
        }
        displayFilteredPersons(filtered);
    }

    private void updatePersonDialog(int personId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Kişiyi Düzenle");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        EditText editName = new EditText(this);
        editName.setHint("Yeni Ad");
        layout.addView(editName);

        EditText editSurname = new EditText(this);
        editSurname.setHint("Yeni Soyad");
        layout.addView(editSurname);

        Spinner spinnerNewType = new Spinner(this);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Misafir", "Personel"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNewType.setAdapter(spinnerAdapter);
        layout.addView(spinnerNewType);

        Cursor cursor = databaseHelper.getPersonById(personId);
        if (cursor.moveToFirst()) {
            editName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            editSurname.setText(cursor.getString(cursor.getColumnIndexOrThrow("surname")));
            spinnerNewType.setSelection("Personel".equals(cursor.getString(cursor.getColumnIndexOrThrow("type"))) ? 1 : 0);
        }
        cursor.close();

        builder.setView(layout);
        builder.setPositiveButton("Güncelle", (dialog, which) -> {
            String newName = editName.getText().toString().trim();
            String newSurname = editSurname.getText().toString().trim();
            String newType = spinnerNewType.getSelectedItem().toString();
            if (!newName.isEmpty() && !newSurname.isEmpty()) {
                if (databaseHelper.updatePerson(personId, newName, newSurname, newType)) {
                    Toast.makeText(this, "Kişi güncellendi.", Toast.LENGTH_SHORT).show();
                    loadPersons();
                } else {
                    Toast.makeText(this, "Güncelleme başarısız.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }

    private void confirmDeletePerson(PersonItem person) {
        new AlertDialog.Builder(this)
                .setTitle("Kişiyi Sil")
                .setMessage(person.name + " " + person.surname + " kişisini silmek istiyor musunuz?")
                .setPositiveButton("Evet", (dialog, which) -> deletePerson(person.id))
                .setNegativeButton("Hayır", null)
                .show();
    }

    private void deletePerson(int personId) {
        if (databaseHelper.deletePerson(personId)) {
            Toast.makeText(this, "Kişi silindi.", Toast.LENGTH_SHORT).show();
            loadPersons();
        } else {
            Toast.makeText(this, "Silme başarısız.", Toast.LENGTH_SHORT).show();
        }
    }

    private static class PersonItem {
        int id;
        String name, surname, type;

        PersonItem(int id, String name, String surname, String type) {
            this.id = id;
            this.name = name;
            this.surname = surname;
            this.type = type;
        }
    }
}
