package com.example.osemprojeson;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "meals.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_PREDEFINED_MEALS = "predefined_meals";
    public static final String TABLE_PERSON = "Person";
    public static final String TABLE_MEAL = "Meal";
    public static final String TABLE_GENERAL_EXPENSE = "GeneralExpense";

    public static final String COL_ID = "id";
    public static final String COL_PREDEFINED_MEAL_ID = "id";
    public static final String COL_PREDEFINED_MEAL_NAME = "name";
    public static final String COL_PREDEFINED_MEAL_PRICE = "price";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PREDEFINED_MEALS + " (" +
                COL_PREDEFINED_MEAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PREDEFINED_MEAL_NAME + " TEXT, " +
                COL_PREDEFINED_MEAL_PRICE + " REAL)");

        db.execSQL("CREATE TABLE " + TABLE_PERSON + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "surname TEXT, " +
                "type TEXT, " +
                "manual_monthly REAL DEFAULT 0, " +
                "manual_yearly REAL DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_MEAL + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "person_id INTEGER, " +
                "meal_name TEXT, " +
                "meal_price REAL, " +
                "meal_quantity INTEGER, " +
                "meal_date TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_GENERAL_EXPENSE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "expense_name TEXT, " +
                "expense_amount REAL, " +
                "expense_date TEXT)");

        // onCreate içinde yeni tablo
        db.execSQL("CREATE TABLE IF NOT EXISTS MonthlyExpenseLog (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "person_id INTEGER, " +
                "year TEXT, " +
                "month TEXT, " +
                "amount REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PREDEFINED_MEALS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERSON);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEAL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GENERAL_EXPENSE);
        onCreate(db);
    }

    public void ensureManualExpenseColumnsExist() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.rawQuery("SELECT manual_monthly FROM " + TABLE_PERSON + " LIMIT 1", null).close();
        } catch (Exception e) {
            db.execSQL("ALTER TABLE " + TABLE_PERSON + " ADD COLUMN manual_monthly REAL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_PERSON + " ADD COLUMN manual_yearly REAL DEFAULT 0");
        }
    }


    public boolean addPredefinedMeal(String name, double price) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PREDEFINED_MEAL_NAME, name);
        values.put(COL_PREDEFINED_MEAL_PRICE, price);
        long result = db.insert(TABLE_PREDEFINED_MEALS, null, values);
        db.close();
        return result != -1;
    }

    public Cursor getAllPredefinedMeals() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_PREDEFINED_MEALS,
                new String[]{COL_PREDEFINED_MEAL_ID, COL_PREDEFINED_MEAL_NAME, COL_PREDEFINED_MEAL_PRICE},
                null, null, null, null, COL_PREDEFINED_MEAL_NAME + " ASC");
    }

    public void saveMonthlyLogIfNotExists(int personId, String year, String month, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM MonthlyExpenseLog WHERE person_id = ? AND year = ? AND month = ?",
                new String[]{String.valueOf(personId), year, month});
        if (!cursor.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put("person_id", personId);
            values.put("year", year);
            values.put("month", month);
            values.put("amount", amount);
            db.insert("MonthlyExpenseLog", null, values);
        }
        cursor.close();
    }


    public boolean updatePredefinedMeal(int id, String name, double price) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PREDEFINED_MEAL_NAME, name);
        values.put(COL_PREDEFINED_MEAL_PRICE, price);
        int result = db.update(TABLE_PREDEFINED_MEALS, values, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result > 0;
    }

    public boolean deletePredefinedMeal(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_PREDEFINED_MEALS, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result > 0;
    }

    public boolean addPerson(String name, String surname, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("surname", surname);
        values.put("type", type);
        long result = db.insert(TABLE_PERSON, null, values);
        return result != -1;
    }

    public Cursor getAllPersons() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_PERSON, null);
    }

    public Cursor getPersonById(int personId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_PERSON + " WHERE id = ?", new String[]{String.valueOf(personId)});
    }

    public boolean updatePerson(int personId, String newName, String newSurname, String newType) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", newName);
        values.put("surname", newSurname);
        values.put("type", newType);
        int result = db.update(TABLE_PERSON, values, "id=?", new String[]{String.valueOf(personId)});
        return result > 0;
    }

    public boolean deletePerson(int personId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_PERSON, "id=?", new String[]{String.valueOf(personId)});
        return result > 0;
    }

    public boolean addMeal(int personId, String mealName, double mealPrice, int quantity, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("person_id", personId);
        values.put("meal_name", mealName);
        values.put("meal_price", mealPrice);
        values.put("meal_quantity", quantity);
        values.put("meal_date", date);
        long result = db.insert(TABLE_MEAL, null, values);
        return result != -1;
    }

    public double getTotalExpensesForPerson(int personId, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(meal_price * meal_quantity) FROM " + TABLE_MEAL +
                        " WHERE person_id = ? AND meal_date BETWEEN ? AND ?",
                new String[]{String.valueOf(personId), startDate, endDate});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public double getManualOrMealTotal(int personId, String startDate, String endDate) {
        boolean isFullYear = startDate.endsWith("01-01") && endDate.endsWith("12-31");
        Cursor cursor = getPersonById(personId);

        if (cursor.moveToFirst()) {
            double manual = isFullYear
                    ? cursor.getDouble(cursor.getColumnIndexOrThrow("manual_yearly"))
                    : cursor.getDouble(cursor.getColumnIndexOrThrow("manual_monthly"));
            if (manual > 0) {
                cursor.close();
                return manual;
            }
        }
        cursor.close();


        return getMealTotalBetweenDates(personId, startDate, endDate);
    }
    public double getMealTotalBetweenDates(int personId, String startDate, String endDate) {
        double total = 0.0;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(meal_price * meal_quantity) AS total FROM " + TABLE_MEAL +
                        " WHERE person_id = ? AND meal_date BETWEEN ? AND ?",
                new String[]{String.valueOf(personId), startDate, endDate});

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
        }
        cursor.close();

        return total;
    }
    public boolean hasMealDataBetweenDates(int personId, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM Meal WHERE person_id = ? AND meal_date BETWEEN ? AND ? LIMIT 1",
                new String[]{String.valueOf(personId), startDate, endDate}
        );
        boolean hasData = cursor.moveToFirst();
        cursor.close();
        return hasData;
    }



    public void updateManualExpenses(int personId, double manualMonthly, double manualYearly) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("manual_monthly", manualMonthly);
        values.put("manual_yearly", manualYearly);
        db.update(TABLE_PERSON, values, "id=?", new String[]{String.valueOf(personId)});
    }

    public double[] getManualExpenses(int personId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT manual_monthly, manual_yearly FROM Person WHERE id = ?",
                new String[]{String.valueOf(personId)});
        double[] result = {0, 0};
        if (cursor.moveToFirst()) {
            result[0] = cursor.getDouble(0);
            result[1] = cursor.getDouble(1);
        }
        cursor.close();
        return result;
    }

    public boolean addGeneralExpense(String name, double amount, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("expense_name", name);
        values.put("expense_amount", amount);
        values.put("expense_date", date);
        long result = db.insert(TABLE_GENERAL_EXPENSE, null, values);
        return result != -1;
    }

    public Cursor getAllGeneralExpenses() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_GENERAL_EXPENSE + " ORDER BY expense_date DESC", null);
    }

    public double getTotalExpensesForPeriod(String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT SUM(expense_amount) FROM " + TABLE_GENERAL_EXPENSE +
                        " WHERE expense_date BETWEEN ? AND ?",
                new String[]{startDate, endDate});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }
}