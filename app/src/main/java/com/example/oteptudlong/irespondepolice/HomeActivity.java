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

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class HomeActivity extends AppCompatActivity {

    public RecyclerView firebaseRecycler;
    public FirebaseRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // get the reports using firebase recyclerview

        firebaseRecycler = findViewById(R.id.firebaseRecyclerview);
        firebaseRecycler.setHasFixedSize(true);
        firebaseRecycler.setLayoutManager(new LinearLayoutManager(this));

        Query query = FirebaseDatabase.getInstance().getReference()
                .child("Citizen Report");

        FirebaseRecyclerOptions<Report> options = new FirebaseRecyclerOptions.Builder<Report>()
                .setQuery(query, Report.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<Report, ReportHolder>(options) {

            @Override
            public ReportHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_report, parent, false);
                return new ReportHolder(view);
            }

            @Override
            protected void onBindViewHolder(ReportHolder holder, int position, Report model) {

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


        firebaseRecycler.setAdapter(adapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}
