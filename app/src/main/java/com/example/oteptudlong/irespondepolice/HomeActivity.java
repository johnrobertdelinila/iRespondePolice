package com.example.oteptudlong.irespondepolice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarTab;
import com.roughike.bottombar.OnTabSelectListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class HomeActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private RecyclerView firebaseRecycler;
    private FirebaseRecyclerAdapter mFirebaseAdapter;
    private DatabaseReference mCitizenReport = FirebaseDatabase.getInstance().getReference().child("Citizen Reports");
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String uid;
    private DatabaseReference mThisPolice;
    private LinearLayoutManager mLinearLayout;
    private ProgressBar progressBar;
    private BottomBar bottomBar;
    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private Location mLastLocation;
    private BottomBarTab reportPending;
    private int pendingCount = 0;
    private int animatedRow = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (mAuth.getCurrentUser() == null) {
            // If the user is deleted
            goLoginPage();
            return;
        }else {
            mAuth.getCurrentUser().getIdToken(true)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            String disabledMessage = "The user account has been disabled by an administrator.";
                            String errMessage =  e.getMessage();
                            if (disabledMessage.equals(errMessage)) {
                                // OR ADD DELETED IF THE ACCOUNT HAS BEEN DELETED
                                // If the user is disabled
                                goLoginPage();
                                Toast.makeText(HomeActivity.this, errMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

        setUpLocation();

        HomeActivity.this.setTitle("Dashboard");

        uid = mAuth.getCurrentUser().getUid();
        mThisPolice = FirebaseDatabase.getInstance().getReference().child("Police").child(uid);

        bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(int tabId) {
                if (tabId == R.id.tab_reports) {
                    bottomBar.refreshDrawableState();
                }else if (tabId == R.id.tab_profile) {
                    bottomBar.refreshDrawableState();
                }
            }
        });

        reportPending = bottomBar.getTabWithId(R.id.tab_reports);

        // TODO: Add logout
        // Initialize RecyclerView
        firebaseRecycler = findViewById(R.id.firebaseRecyclerview);
        mLinearLayout = new LinearLayoutManager(this);
        firebaseRecycler.setLayoutManager(mLinearLayout);
        progressBar = findViewById(R.id.progresBar);

    }

    private void setFirebaseRecyclerView() {
        // Insert the FIREBASE TIMESTAMP
        mThisPolice.child("readTime").setValue(ServerValue.TIMESTAMP, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                // Retrieve the Firebase TIMESTAMP
                if (databaseError == null) {
                    databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            // only report from the last 24 hours can be read.
                            // regardless of the report status; ex. pending or resolved
                            Long currentTime = dataSnapshot.getValue(Long.class);
                            int day = 86400000; // 1 day to milliseconds
                            if (currentTime != null) {
                                Query query = mCitizenReport.orderByChild("timestamp").startAt((currentTime - day));
                                setmFirebaseAdapter(query);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(HomeActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(HomeActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(HomeActivity.this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 5000);
        } else {
            if (googlePlayServicesAvailable()) {
                buildGoogleApiClient();
                setUpLocationRequest();
            }
        }
    }

    private boolean googlePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(HomeActivity.this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(HomeActivity.this, status, 6000).show();
            } else {
                Toast.makeText(this, "Device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        } else {
            return true;
        }
    }

    private void setUpLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(500);
    }

    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(HomeActivity.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private void setmFirebaseAdapter(Query query) {
        SnapshotParser<Report> parser = new SnapshotParser<Report>() {
            @NonNull
            @Override
            public Report parseSnapshot(DataSnapshot snapshot) {
                Report report = snapshot.getValue(Report.class);
                if (report != null) {
                    report.setKey(snapshot.getKey());
                    if (snapshot.hasChild("policeRespondee")) {
                        report.setNumOfRespondee(snapshot.child("policeRespondee").getChildrenCount());
                    }else {
                        report.setNumOfRespondee(0);
                    }
                }
                return report;
            }
        };

        FirebaseRecyclerOptions<Report> options = new FirebaseRecyclerOptions.Builder<Report>()
                .setQuery(query, parser)
                .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<Report, ReportHolder>(options) {
            @NonNull
            @Override
            public ReportHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_report, parent, false);
                return new ReportHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ReportHolder holder, int position, @NonNull Report model) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(ProgressBar.INVISIBLE);
                    }
                }, 190);

                if (position > animatedRow) {
                    animatedRow = position;
                    long animationDelay = 1000L + holder.getAdapterPosition() * 25;

                    holder.itemView.setAlpha(0);
                    holder.itemView.setTranslationY(ScreenUtil.dp2px(16, holder.itemView.getContext()));

                    holder.itemView.animate()
                            .alpha(1)
                            .translationY(0)
                            .setDuration(200)
                            .setInterpolator(new LinearOutSlowInInterpolator())
                            .setStartDelay(animationDelay)
                            .start();
                }



                if (model.getStatus().equals("pending")) {
                    pendingCount++;
                    reportPending.setBadgeCount(pendingCount);
                }

                SimpleDateFormat sfd = new SimpleDateFormat("E h:mm a", Locale.getDefault());
                String dateTime = sfd.format(new Date((Long) model.getTimestamp()));

                Location reportLocation = new Location("");
                reportLocation.setLatitude(model.getLocation_latlng().get("latitude"));
                reportLocation.setLongitude(model.getLocation_latlng().get("longtitude"));
                float distance = (float) (mLastLocation.distanceTo(reportLocation) * 0.001);
                DecimalFormat f = new DecimalFormat("##.00");

                String distanceInKilometer;

                if (distance == 0) {
                    distanceInKilometer = "0 km";
                }else {
                    distanceInKilometer = f.format(distance) + " km";
                }

                Log.e("DISTANCE", distanceInKilometer);

                String msg;
                if (model.getNumOfRespondee() == 0) {
                    msg = "Waiting for police to respond.";
                }else {
                    if (model.getNumOfRespondee() == 1) {
                        msg = String.valueOf(model.getNumOfRespondee()) + " is currently responding.";
                    }else {
                        msg = String.valueOf(model.getNumOfRespondee()) + " are currently responding.";
                    }
                }

                String status = "UNRESOLVE";
                int color = getResources().getColor(R.color.cardStatusPending);

                if (model.getStatus().equals("resolved")) {
                    status = "RESOLVED";
                    color = getResources().getColor(R.color.cardStatusResolved);
                    if (model.getNumOfRespondee() == 1) {
                        msg = String.valueOf(model.getNumOfRespondee()) + " police responded.";
                    }else {
                        msg = String.valueOf(model.getNumOfRespondee()) + " police responded.";
                    }
                }

                Log.e("REPORT DATE", dateTime);
                Log.e("Number of respondee", msg);
                holder.distance.setText(distanceInKilometer);
                holder.cardStatus.setCardBackgroundColor(color);
                holder.status.setText(status);
                holder.incident.setText(model.getIncident() + "   " + dateTime);
                holder.responding.setText(msg);

                final Intent intent = new Intent(HomeActivity.this, RespondActivity.class);
                intent.putExtra("report", model);
                holder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(intent);
                    }
                });
            }
        };

        firebaseRecycler.setAdapter(mFirebaseAdapter);
        pendingCount = 0;
        mFirebaseAdapter.startListening();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mFirebaseAdapter != null) {
            pendingCount = 0;
            mFirebaseAdapter.startListening();
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            finish();
            startActivity(intent);
        }
    }

//    @Override
//    protected void onPause() {
//        if (mFirebaseAdapter != null) {
//            mFirebaseAdapter.stopListening();
//        }
//        super.onPause();
//    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFirebaseAdapter != null) {
            pendingCount = 0;
            mFirebaseAdapter.startListening();
        }
        if (progressBar != null) {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.stopListening();
        }
    }

    private void goLoginPage() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        finish();
        startActivity(intent);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        setLocation();
        setUpLocationRequest();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 5000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (googlePlayServicesAvailable()) {
                    buildGoogleApiClient();
                    setLocation();
                    setUpLocationRequest();
                }
            }else {
                Log.d("HELLO", "FAILED");
            }
        }
    }

    private void setLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        setFirebaseRecyclerView();
    }

}
