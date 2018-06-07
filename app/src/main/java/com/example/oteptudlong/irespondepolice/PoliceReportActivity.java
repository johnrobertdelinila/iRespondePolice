package com.example.oteptudlong.irespondepolice;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmax.dialog.SpotsDialog;

public class PoliceReportActivity extends AppCompatActivity {

    private EditText case_no, incident, detail_of_event, actions_taken, witness;
    private Button btn_submit;
    private DatabaseReference mPoliceReport = FirebaseDatabase.getInstance().getReference().child("Police Reports");
    private DatabaseReference mCitizenReport = FirebaseDatabase.getInstance().getReference().child("Citizen Reports");
    public String citizen_report_id;
    public Double latitude, longtitude;
    private SpotsDialog loadingDialog;
    private String policeUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private String str_incident = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_police_report);

        init();

        // TODO: Make title spinner

        Log.e("KEY", citizen_report_id);

        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str_case_no = case_no.getText().toString().trim();
                str_incident = incident.getText().toString().trim();
                String str_detail = detail_of_event.getText().toString().trim();
                String str_action = actions_taken.getText().toString().trim();
                String str_witness = witness.getText().toString().trim();

                if (TextUtils.isEmpty(str_case_no)) {
                    return;
                }
                if (TextUtils.isEmpty(str_incident)) {
                    return;
                }
                if (TextUtils.isEmpty(str_detail)) {
                    return;
                }
                if (TextUtils.isEmpty(str_action)) {
                    return;
                }
                if (TextUtils.isEmpty(str_witness)) {
                    return;
                }

                loadingDialog.show();
                insertReport(str_action, str_case_no, str_witness, str_detail, str_incident);

            }
        });

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

        mPoliceReport.push().setValue(policeReport, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError == null) {
                    insertPoliceReportID(databaseReference.getKey());
                }else {
                    loadingDialog.dismiss();
                    Toast.makeText(PoliceReportActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void insertPoliceReportID(String policeReportID) {
        Map<String, Object> policeReport = new HashMap<>();
        policeReport.put(policeUid, policeReportID);
        mCitizenReport.child(citizen_report_id).child("policeReports").updateChildren(policeReport)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        updateReportStatus();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingDialog.dismiss();
                        Toast.makeText(PoliceReportActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateReportStatus() {
        HashMap<String, Object> status = new HashMap<>();
        status.put("status", "resolved");
        status.put("title", str_incident);
        mCitizenReport.child(citizen_report_id).updateChildren(status)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        loadingDialog.dismiss();
                        Toast.makeText(PoliceReportActivity.this, "Report has been successfully inserted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingDialog.dismiss();
                        Toast.makeText(PoliceReportActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
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

        citizen_report_id = getIntent().getExtras().getString("key");
        latitude = getIntent().getExtras().getDouble("latitude");
        longtitude = getIntent().getExtras().getDouble("longtitude");

        loadingDialog = new SpotsDialog(this);
    }
}
