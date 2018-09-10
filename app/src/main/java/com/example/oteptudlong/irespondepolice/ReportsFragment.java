package com.example.oteptudlong.irespondepolice;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class ReportsFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {


    public ReportsFragment() {
        // Required empty public constructor
    }

    private Activity activity;
    private Context context;

    // Static
    private static final int LOCATION_REQUEST_CODE = 5000;

    private int animatedRow = -1;
    private int pendingCount = 0;

    // Map
    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private Location mLastLocation;

    // Widgets
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView message;

    // Database
    private DatabaseReference mCitizenReport = FirebaseDatabase.getInstance().getReference().child("Citizen Reports");
    private DatabaseReference mThisPolice;

    // Database Adapter
    private FirebaseRecyclerAdapter mFirebaseAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_reports, container, false);
        activity = getActivity();
        context = getContext();

        mThisPolice = FirebaseDatabase.getInstance().getReference().child("Police").child(HomeActivity.mAuth.getCurrentUser().getUid());
        progressBar = view.findViewById(R.id.progress_bar);
        message = view.findViewById(R.id.text_message);

        recyclerView = view.findViewById(R.id.recycler_view);
        LinearLayoutManager mLinearManager = new LinearLayoutManager(activity);
        mLinearManager.setReverseLayout(true);
        mLinearManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(mLinearManager);

        setUpLocation();

        return view;
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity, new String[]{
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
        int status = googleApiAvailability.isGooglePlayServicesAvailable(context);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 6000).show();
            } else {
                Toast.makeText(activity, "Device is not supported", Toast.LENGTH_SHORT).show();
                activity.finish();
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
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private void setLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (mLastLocation != null) {
            setFirebaseRecyclerView();
        }else {
            Toast.makeText(context, "Can't get your current location. Please turn on your" +
                    " location to fetch reports.", Toast.LENGTH_LONG).show();
        }
    }

    private void setFirebaseRecyclerView() {
        // Insert the FIREBASE TIMESTAMP
        mThisPolice.child("readTime").setValue(ServerValue.TIMESTAMP, (databaseError, databaseReference) -> {
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
//                        Toast.makeText(context, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
//                Toast.makeText(context, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setmFirebaseAdapter(Query query) {
        SnapshotParser<Report> parser = snapshot -> {
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
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount() <= 0) {
                    progressBar.setVisibility(View.GONE);
                    message.setText("No reports within 24 hours.");
                    message.setVisibility(View.VISIBLE);
                }else {
                    message.setVisibility(View.GONE);
                }
            }

            @Override
            protected void onBindViewHolder(@NonNull ReportHolder holder, int position, @NonNull Report model) {

//                if (position > animatedRow) {
//                    animatedRow = position;
//                    long animationDelay = 400L + holder.getAdapterPosition() * 25;
//
//                    holder.itemView.setAlpha(0);
//
//                    holder.itemView.animate()
//                            .alpha(1)
//                            .translationY(0)
//                            .setDuration(200)
//                            .setInterpolator(new LinearOutSlowInInterpolator())
//                            .setStartDelay(animationDelay)
//                            .start();
//                }


                // FIXME: Nawawala ung icon
//                if (model.getStatus().equals("pending")) {
//                    pendingCount++;
//                    reportPending.setBadgeCount(pendingCount);
//                }

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
                holder.distance.setText("Distance: " + distanceInKilometer);
                holder.cardStatus.setCardBackgroundColor(color);
                holder.status.setText(status);
                holder.incident.setText(model.getIncident() + "   " + dateTime);
                holder.responding.setText(msg);

                final Intent intent = new Intent(context, RespondActivity.class);
                intent.putExtra("report", model);
                holder.cardView.setOnClickListener(v -> startActivity(intent));

                progressBar.setVisibility(ProgressBar.GONE);
            }
        };

        recyclerView.setAdapter(mFirebaseAdapter);
        pendingCount = 0;
        mFirebaseAdapter.startListening();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        setLocation();
        setUpLocationRequest();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
    public void onStop() {
        super.onStop();
        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.stopListening();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.startListening();
        }
    }
}
