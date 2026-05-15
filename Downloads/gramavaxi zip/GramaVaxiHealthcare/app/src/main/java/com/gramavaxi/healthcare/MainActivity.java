package com.gramavaxi.healthcare;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private TextView tvTotal, tvVaccinated, tvSick;
    private EditText etSearch;
    private ListView listView;
    private SharedPreferences sharedPreferences;
    private ArrayList<AnimalRecord> animalRecords;
    private ArrayList<AnimalRecord> filteredRecords;
    private ActivityResultLauncher<Intent> qrScanLauncher;
    private boolean isSearchMode = false;

    private static class AnimalRecord {
        String id, animalId, ownerName, village, breed, age, gender;
        String vaccineType, vaccinationDate, nextDueDate;
        String healthStatus, treatment, medicine, vetName, notes;
        boolean isPregnant;

        AnimalRecord(String id, String animalId, String ownerName, String village, String breed, String age, String gender,
                     String vaccineType, String vaccinationDate, String nextDueDate, String healthStatus,
                     String treatment, String medicine, String vetName, String notes, boolean isPregnant) {
            this.id = id;
            this.animalId = animalId;
            this.ownerName = ownerName;
            this.village = village;
            this.breed = breed;
            this.age = age;
            this.gender = gender;
            this.vaccineType = vaccineType;
            this.vaccinationDate = vaccinationDate;
            this.nextDueDate = nextDueDate;
            this.healthStatus = healthStatus;
            this.treatment = treatment;
            this.medicine = medicine;
            this.vetName = vetName;
            this.notes = notes;
            this.isPregnant = isPregnant;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTotal = findViewById(R.id.tvTotal);
        tvVaccinated = findViewById(R.id.tvVaccinated);
        tvSick = findViewById(R.id.tvSick);

        Button btnScan = findViewById(R.id.btnScan);
        Button btnView = findViewById(R.id.btnView);
        Button btnReports = findViewById(R.id.btnReports);
        Button btnDeleteAll = findViewById(R.id.btnDeleteAll);

        sharedPreferences = getSharedPreferences("GramVaxiHealth", Context.MODE_PRIVATE);
        animalRecords = new ArrayList<>();
        filteredRecords = new ArrayList<>();
        loadData();
        updateDashboard();

        qrScanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String scannedData = result.getData().getStringExtra("SCAN_RESULT");
                        if (scannedData == null) scannedData = result.getData().getStringExtra("TEXT");
                        if (scannedData != null && !scannedData.isEmpty()) {
                            showHealthRecordDialog(scannedData);
                        }
                    }
                });

        btnScan.setOnClickListener(v -> startScanner());
        btnView.setOnClickListener(v -> showRecordsList());
        btnReports.setOnClickListener(v -> showReportsMenu());
        btnDeleteAll.setOnClickListener(v -> confirmDeleteAll());
    }

    private void startScanner() {
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            qrScanLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Please install QR Scanner", Toast.LENGTH_SHORT).show();
        }
    }

    private void showHealthRecordDialog(String animalId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🐄 Add Health Record");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        TextView tvId = new TextView(this);
        tvId.setText("Animal ID: " + animalId);
        tvId.setTextSize(16);
        tvId.setTextColor(0xFF2E7D32);
        layout.addView(tvId);

        EditText etOwner = new EditText(this);
        etOwner.setHint("👤 Owner Name");
        layout.addView(etOwner);

        EditText etVillage = new EditText(this);
        etVillage.setHint("📍 Village Name");
        layout.addView(etVillage);

        EditText etBreed = new EditText(this);
        etBreed.setHint("🐂 Breed");
        layout.addView(etBreed);

        EditText etAge = new EditText(this);
        etAge.setHint("📅 Age (years)");
        layout.addView(etAge);

        TextView tvGender = new TextView(this);
        tvGender.setText("⚥ Gender:");
        layout.addView(tvGender);
        RadioGroup rgGender = new RadioGroup(this);
        rgGender.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbMale = new RadioButton(this);
        rbMale.setText("Male");
        RadioButton rbFemale = new RadioButton(this);
        rbFemale.setText("Female");
        rgGender.addView(rbMale);
        rgGender.addView(rbFemale);
        layout.addView(rgGender);

        CheckBox cbPregnant = new CheckBox(this);
        cbPregnant.setText("🤰 Is Pregnant?");
        layout.addView(cbPregnant);

        rgGender.setOnCheckedChangeListener((group, checkedId) -> {
            cbPregnant.setVisibility(checkedId == rbFemale.getId() ? View.VISIBLE : View.GONE);
        });

        TextView tvHealth = new TextView(this);
        tvHealth.setText("🩺 Health Status:");
        layout.addView(tvHealth);
        Spinner spinnerHealth = new Spinner(this);
        String[] healthStatus = {"Healthy", "Sick", "Injured", "Under Treatment", "Recovering"};
        ArrayAdapter<String> healthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, healthStatus);
        spinnerHealth.setAdapter(healthAdapter);
        layout.addView(spinnerHealth);

        TextView tvVaccine = new TextView(this);
        tvVaccine.setText("💉 Vaccine Type:");
        layout.addView(tvVaccine);
        Spinner spinnerVaccine = new Spinner(this);
        String[] vaccines = {"FMD", "Hemorrhagic Septicemia", "Black Quarter", "Anthrax", "Brucellosis", "LSD"};
        ArrayAdapter<String> vaccineAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vaccines);
        spinnerVaccine.setAdapter(vaccineAdapter);
        layout.addView(spinnerVaccine);

        TextView tvDate = new TextView(this);
        tvDate.setText("📅 Vaccination Date:");
        layout.addView(tvDate);
        Button btnDate = new Button(this);
        btnDate.setText("Select Date");
        layout.addView(btnDate);
        TextView tvSelectedDate = new TextView(this);
        tvSelectedDate.setText("");
        layout.addView(tvSelectedDate);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        btnDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day);
                tvSelectedDate.setText(dateFormat.format(cal.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dpd.show();
        });

        EditText etTreatment = new EditText(this);
        etTreatment.setHint("💊 Treatment Given");
        layout.addView(etTreatment);

        EditText etMedicine = new EditText(this);
        etMedicine.setHint("💊 Medicine Name");
        layout.addView(etMedicine);

        EditText etVet = new EditText(this);
        etVet.setHint("👨‍⚕️ Veterinarian Name");
        layout.addView(etVet);

        EditText etNotes = new EditText(this);
        etNotes.setHint("📝 Additional Notes");
        layout.addView(etNotes);

        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("Save Health Record", (dialog, which) -> {
            String owner = etOwner.getText().toString().trim();
            String village = etVillage.getText().toString().trim();
            String breed = etBreed.getText().toString().trim();
            String age = etAge.getText().toString().trim();
            String gender = rgGender.getCheckedRadioButtonId() == rbFemale.getId() ? "Female" : "Male";
            boolean pregnant = cbPregnant.isChecked();
            String health = spinnerHealth.getSelectedItem().toString();
            String vaccine = spinnerVaccine.getSelectedItem().toString();
            String vDate = tvSelectedDate.getText().toString();
            String treatment = etTreatment.getText().toString().trim();
            String medicine = etMedicine.getText().toString().trim();
            String vet = etVet.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            if (vDate.isEmpty()) vDate = dateFormat.format(calendar.getTime());
            if (owner.isEmpty()) owner = "Unknown";
            if (village.isEmpty()) village = "Unknown";

            Calendar nextCal = Calendar.getInstance();
            try {
                Date d = dateFormat.parse(vDate);
                if (d != null) nextCal.setTime(d);
            } catch(Exception e) {}
            nextCal.add(Calendar.MONTH, 6);
            String nextDue = dateFormat.format(nextCal.getTime());

            String id = String.valueOf(System.currentTimeMillis());
            AnimalRecord record = new AnimalRecord(id, animalId, owner, village, breed, age, gender,
                    vaccine, vDate, nextDue, health, treatment, medicine, vet, notes, pregnant);

            animalRecords.add(0, record);
            saveData();
            updateDashboard();
            Toast.makeText(MainActivity.this, "✓ Health record saved for " + animalId, Toast.LENGTH_LONG).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadData() {
        try {
            String json = sharedPreferences.getString("records", "");
            if (!json.isEmpty()) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    AnimalRecord r = new AnimalRecord(
                            obj.getString("id"), obj.getString("animalId"), obj.getString("owner"),
                            obj.getString("village"), obj.getString("breed"), obj.getString("age"),
                            obj.getString("gender"), obj.getString("vaccine"), obj.getString("vDate"),
                            obj.getString("nextDue"), obj.getString("health"), obj.getString("treatment"),
                            obj.getString("medicine"), obj.getString("vet"), obj.getString("notes"), obj.optBoolean("pregnant")
                    );
                    animalRecords.add(r);
                }
            }
        } catch (Exception e) {}
    }

    private void saveData() {
        try {
            JSONArray arr = new JSONArray();
            for (AnimalRecord r : animalRecords) {
                JSONObject obj = new JSONObject();
                obj.put("id", r.id);
                obj.put("animalId", r.animalId);
                obj.put("owner", r.ownerName);
                obj.put("village", r.village);
                obj.put("breed", r.breed);
                obj.put("age", r.age);
                obj.put("gender", r.gender);
                obj.put("vaccine", r.vaccineType);
                obj.put("vDate", r.vaccinationDate);
                obj.put("nextDue", r.nextDueDate);
                obj.put("health", r.healthStatus);
                obj.put("treatment", r.treatment);
                obj.put("medicine", r.medicine);
                obj.put("vet", r.vetName);
                obj.put("notes", r.notes);
                obj.put("pregnant", r.isPregnant);
                arr.put(obj);
            }
            sharedPreferences.edit().putString("records", arr.toString()).apply();
        } catch (Exception e) {}
    }

    private void updateDashboard() {
        int total = animalRecords.size();
        int vaccinated = animalRecords.size();
        int sick = 0;
        for (AnimalRecord r : animalRecords) {
            if (r.healthStatus.equalsIgnoreCase("Sick")) sick++;
        }
        tvTotal.setText(String.valueOf(total));
        tvVaccinated.setText(String.valueOf(vaccinated));
        tvSick.setText(String.valueOf(sick));
    }

    private void showRecordsList() {
        setContentView(R.layout.activity_records);

        listView = findViewById(R.id.listView);
        etSearch = findViewById(R.id.etSearch);
        Button btnSearch = findViewById(R.id.btnSearch);
        Button btnBack = findViewById(R.id.btnBack);

        updateRecordList("");

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim().toLowerCase();
            updateRecordList(query);
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            AnimalRecord record = isSearchMode ? filteredRecords.get(position) : animalRecords.get(position);
            showRecordDetail(record);
        });

        btnBack.setOnClickListener(v -> recreate());
    }

    private void updateRecordList(String query) {
        if (query.isEmpty()) {
            isSearchMode = false;
            displayRecords(animalRecords);
        } else {
            isSearchMode = true;
            filteredRecords.clear();
            for (AnimalRecord r : animalRecords) {
                if (r.animalId.toLowerCase().contains(query) ||
                        r.ownerName.toLowerCase().contains(query) ||
                        r.village.toLowerCase().contains(query)) {
                    filteredRecords.add(r);
                }
            }
            displayRecords(filteredRecords);
        }
    }

    private void displayRecords(ArrayList<AnimalRecord> records) {
        ArrayList<String> displayList = new ArrayList<>();
        for (AnimalRecord r : records) {
            String healthIcon = r.healthStatus.equalsIgnoreCase("Healthy") ? "✅" :
                    (r.healthStatus.equalsIgnoreCase("Sick") ? "⚠️" : "🩺");
            displayList.add(healthIcon + " " + r.animalId + "\n👤 " + r.ownerName + "\n📍 " + r.village);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);
    }

    private void showRecordDetail(AnimalRecord r) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🐄 Complete Health Record");
        builder.setMessage("ID: " + r.animalId +
                "\n👤 Owner: " + r.ownerName +
                "\n📍 Village: " + r.village +
                "\n🐂 Breed: " + r.breed +
                "\n📅 Age: " + r.age +
                "\n⚥ Gender: " + r.gender +
                (r.isPregnant ? "\n🤰 Pregnant: Yes" : "") +
                "\n🩺 Health: " + r.healthStatus +
                "\n💉 Vaccine: " + r.vaccineType +
                "\n📅 Vaccinated: " + r.vaccinationDate +
                "\n⏰ Next Due: " + r.nextDueDate +
                (r.treatment.isEmpty() ? "" : "\n💊 Treatment: " + r.treatment) +
                (r.medicine.isEmpty() ? "" : "\n💊 Medicine: " + r.medicine) +
                (r.vetName.isEmpty() ? "" : "\n👨‍⚕️ Vet: " + r.vetName) +
                (r.notes.isEmpty() ? "" : "\n📝 Notes: " + r.notes));
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showReportsMenu() {
        String[] options = {"📊 Health Summary", "📧 Share Records", "💉 Vaccination Report"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Health Reports");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) showHealthSummary();
            else if (which == 1) shareRecords();
            else if (which == 2) showVaccinationReport();
        });
        builder.show();
    }

    private void showHealthSummary() {
        int total = animalRecords.size();
        int healthy = 0, sick = 0, pregnant = 0;
        for (AnimalRecord r : animalRecords) {
            if (r.healthStatus.equalsIgnoreCase("Healthy")) healthy++;
            if (r.healthStatus.equalsIgnoreCase("Sick")) sick++;
            if (r.isPregnant) pregnant++;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📊 Health Summary");
        builder.setMessage("Total Animals: " + total +
                "\n✅ Healthy: " + healthy +
                "\n⚠️ Sick: " + sick +
                "\n🤰 Pregnant: " + pregnant);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void shareRecords() {
        StringBuilder sb = new StringBuilder();
        sb.append("🐄 GRAM VAXI HEALTH REPORT\n");
        sb.append("========================\n\n");
        for (AnimalRecord r : animalRecords) {
            sb.append("ID: ").append(r.animalId);
            sb.append(" | Owner: ").append(r.ownerName);
            sb.append(" | Village: ").append(r.village);
            sb.append(" | Health: ").append(r.healthStatus);
            sb.append(" | Vaccine: ").append(r.vaccineType);
            sb.append(" | Date: ").append(r.vaccinationDate).append("\n");
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Gram Vaxi Health Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Report"));
    }

    private void showVaccinationReport() {
        int fmd = 0, hs = 0, bq = 0, anthrax = 0, brucellosis = 0, lsd = 0;
        for (AnimalRecord r : animalRecords) {
            switch(r.vaccineType) {
                case "FMD": fmd++; break;
                case "Hemorrhagic Septicemia": hs++; break;
                case "Black Quarter": bq++; break;
                case "Anthrax": anthrax++; break;
                case "Brucellosis": brucellosis++; break;
                case "LSD": lsd++; break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("💉 Vaccination Report");
        builder.setMessage("FMD: " + fmd +
                "\nHemorrhagic Septicemia: " + hs +
                "\nBlack Quarter: " + bq +
                "\nAnthrax: " + anthrax +
                "\nBrucellosis: " + brucellosis +
                "\nLSD: " + lsd);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Records")
                .setMessage("Delete all health records?")
                .setPositiveButton("Yes", (d, w) -> {
                    animalRecords.clear();
                    saveData();
                    updateDashboard();
                    Toast.makeText(this, "All records deleted", Toast.LENGTH_SHORT).show();
                    recreate();
                })
                .setNegativeButton("No", null)
                .show();
    }
}