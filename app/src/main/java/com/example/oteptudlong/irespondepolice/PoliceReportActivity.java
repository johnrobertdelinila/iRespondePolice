package com.example.oteptudlong.irespondepolice;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmax.dialog.SpotsDialog;

public class PoliceReportActivity extends AppCompatActivity {

    private TextInputEditText case_no, incident, detail_of_event, actions_taken, witness;
    private TextInputLayout layout_case_no, layout_incident, layout_detail_of_event, layout_actions_taken, layout_witness;
    private Button btn_submit;
    private DatabaseReference mPoliceReport = FirebaseDatabase.getInstance().getReference().child("Police Reports");
    private DatabaseReference mCitizenReport = FirebaseDatabase.getInstance().getReference().child("Citizen Reports");
    public String citizen_report_id;
    public Double latitude, longtitude;
    private android.app.AlertDialog loadingDialog;
    private String policeUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private String str_incident = "";

    private List<String> titles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_police_report);

        init();
        setUpToolbar();

        // TODO: Make title spinner
        Log.e("KEY", citizen_report_id);

        titles = new ArrayList<>();
        titles.add("Rape");
        titles.add("Fire");
        titles.add("Near Miss");
        titles.add("Road Accident");
        titles.add("Theft");
        titles.add("Property Damage");
        titles.add("Others");

        btn_submit.setOnClickListener(v -> {
            String str_case_no = case_no.getText().toString().trim();
            str_incident = incident.getText().toString().trim();
            String str_detail = detail_of_event.getText().toString().trim();
            String str_action = actions_taken.getText().toString().trim();
            String str_witness = witness.getText().toString().trim();

            if (TextUtils.isEmpty(str_case_no)) {
                layout_case_no.setError("Case id is required.");
                return;
            }else {
                layout_case_no.setError(null);
            }
            if (TextUtils.isEmpty(str_incident)) {
                layout_incident.setError("Incident must not be empty.");
                return;
            }else {
                layout_incident.setError(null);
            }
            if (!checkIfInTitles(str_incident)) {
                layout_incident.setError("Please enter a valid incident name");
                return;
            }else {
                layout_incident.setError(null);
            }
            if (TextUtils.isEmpty(str_detail)) {
                layout_detail_of_event.setError("Detail of event is required.");
                return;
            }else {
                layout_detail_of_event.setError(null);
            }
            if (TextUtils.isEmpty(str_action)) {
                layout_actions_taken.setError("Action taken must not be empty.");
                return;
            }else {
                layout_actions_taken.setError(null);
            }
            if (TextUtils.isEmpty(str_witness)) {
                layout_witness.setError("Witness input must not be empty.");
                return;
            }else {
                layout_witness.setError(null);
            }

            loadingDialog.show();
            insertReport(str_action, str_case_no, str_witness, str_detail, str_incident);

        });

    }

    private boolean checkIfInTitles(String customTitle) {
        boolean nadaliBa = false;
        for (String title: titles) {
            if (title.equalsIgnoreCase(customTitle)) {
                nadaliBa = true;
                break;
            }
        }
        return nadaliBa;
    }

    private void insertReport(String str_action, String str_case_no, String str_witness, String str_detail, String str_incident) {
        String time = DateFormat.getTimeInstance().format(new Date());
        String date = DateFormat.getDateInstance().format(new Date());

        PoliceReport policeReport = new PoliceReport();
        policeReport.setActions_taken(str_action);
        policeReport.setCase_no(str_case_no);
        policeReport.setTime(time);
        policeReport.setDate(date);
        policeReport.setLatitude(latitude);
        policeReport.setLongtitude(longtitude);
        policeReport.setCitizen_report_id(citizen_report_id);
        policeReport.setWitness(str_witness);
        policeReport.setDetail_of_event(str_detail);
        policeReport.setPolice_id(policeUid);
        policeReport.setIncident(str_incident);

        mPoliceReport.push().setValue(policeReport, (databaseError, databaseReference) -> {
            if (databaseError == null) {
                insertPoliceReportID(databaseReference.getKey());
            }else {
                loadingDialog.dismiss();
                Toast.makeText(PoliceReportActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void insertPoliceReportID(String policeReportID) {
        Map<String, Object> policeReport = new HashMap<>();
        policeReport.put(policeUid, policeReportID);
        mCitizenReport.child(citizen_report_id).child("policeReports").updateChildren(policeReport)
                .addOnSuccessListener(aVoid -> updateReportStatus())
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(PoliceReportActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateReportStatus() {
        HashMap<String, Object> status = new HashMap<>();
        status.put("status", "resolved");
        status.put("title", str_incident);
        mCitizenReport.child(citizen_report_id).updateChildren(status)
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.dismiss();
                    Toast.makeText(PoliceReportActivity.this, "Report has been successfully wrote.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(PoliceReportActivity.this, HomeActivity.class);
                    finish();
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(PoliceReportActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void init() {
        PoliceReportActivity.this.setTitle("Police Report");

        case_no = findViewById(R.id.edit_case_no);
        incident = findViewById(R.id.edit_incident);
        detail_of_event = findViewById(R.id.edit_detail);
        actions_taken = findViewById(R.id.edit_actions);
        witness = findViewById(R.id.edit_witness);
        btn_submit = findViewById(R.id.btn_submit);

        layout_case_no = findViewById(R.id.textInput_case_no);
        layout_incident = findViewById(R.id.textInput_incident);
        layout_detail_of_event = findViewById(R.id.textInput_detail);
        layout_actions_taken = findViewById(R.id.textInput_actions);
        layout_witness = findViewById(R.id.textInput_witness);

        citizen_report_id = getIntent().getExtras().getString("key");
        latitude = getIntent().getExtras().getDouble("latitude");
        longtitude = getIntent().getExtras().getDouble("longtitude");

        loadingDialog = new SpotsDialog.Builder()
                .setContext(this)
                .build();
    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
    }

}
