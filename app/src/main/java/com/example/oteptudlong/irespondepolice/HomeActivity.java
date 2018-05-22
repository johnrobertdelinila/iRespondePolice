package com.example.oteptudlong.irespondepolice;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView firebaseRecycler;
    private FirebaseRecyclerAdapter mFirebaseAdapter;
    private DatabaseReference mCitizenReport = FirebaseDatabase.getInstance().getReference().child("Citizen Reports");
    private LinearLayoutManager mLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        SnapshotParser<Report> parser = new SnapshotParser<Report>() {
            @NonNull
            @Override
            public Report parseSnapshot(@NonNull DataSnapshot snapshot) {
                Report report = snapshot.getValue(Report.class);
                if (report != null) {
                    report.setKey(snapshot.getKey());
                }
                return report;
            }
        };

        FirebaseRecyclerOptions<Report> options = new FirebaseRecyclerOptions.Builder<Report>()
                .setQuery(mCitizenReport, parser)
                .build();
        // TODO: Show Resolved Reports in UI
        // TODO: Add Progress Dialog
        // Initialize RecyclerView
        firebaseRecycler = findViewById(R.id.firebaseRecyclerview);
        mLinearLayout = new LinearLayoutManager(this);
        firebaseRecycler.setLayoutManager(mLinearLayout);

        mFirebaseAdapter = new FirebaseRecyclerAdapter<Report, ReportHolder>(options) {
            @NonNull
            @Override
            public ReportHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_report, parent, false);
                return new ReportHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ReportHolder holder, int position, @NonNull Report model) {

                holder.textView.setText(model.getIncident());
                final Report report = model;
                holder.textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(HomeActivity.this, RespondActivity.class);
                        intent.putExtra("report", report);
                        startActivity(intent);
                    }
                });

            }
        };

        firebaseRecycler.setAdapter(mFirebaseAdapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if the user is signed in.
        // TODO: Add code to check if the user is signed in.
    }

    @Override
    protected void onPause() {
        mFirebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFirebaseAdapter.stopListening();
    }
}
