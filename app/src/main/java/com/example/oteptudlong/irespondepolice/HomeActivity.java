package com.example.oteptudlong.irespondepolice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarTab;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity implements NavigationHost {

    // Widgets
    private BottomBar bottomBar;
    private TextView textTitle;
    private NestedScrollView nestedScrollView;
    private BottomBarTab reportPending;

    // Auth
    public static FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Database
    private DatabaseReference policeRef = FirebaseDatabase.getInstance().getReference().child("Police");

    // Data
    private String uid;
    public static String policePosition;


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
                    .addOnFailureListener(e -> {
                        String disabledMessage = "The user account has been disabled by an administrator.";
                        String errMessage =  e.getMessage();
                        if (disabledMessage.equals(errMessage)) {
                            // OR ADD DELETED IF THE ACCOUNT HAS BEEN DELETED
                            // If the user is disabled
                            goLoginPage();
                            Toast.makeText(HomeActivity.this, errMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        }

        textTitle = findViewById(R.id.textview_title_page);
        nestedScrollView = findViewById(R.id.nestedScrollView);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new ReportsFragment())
                    .commit();
        }

        if (mAuth.getCurrentUser() != null) {
            policeRef.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild("position") && dataSnapshot.child("position").getValue(String.class) != null) {
                        policePosition = dataSnapshot.child("position").getValue(String.class);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        HomeActivity.this.setTitle("Dashboard");

        uid = mAuth.getCurrentUser().getUid();

        bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setOnTabSelectListener(tabId -> {
            if (tabId == R.id.tab_reports) {
                if (!textTitle.getText().equals(getString(R.string.iresponde_report_tab))) {
                    textTitle.setText(R.string.iresponde_report_tab);
//                    textTitle.setTextColor(getResources().getColor(R.color.colorProfileTab));
                    navigateTo(new ReportsFragment(), false);
                }
            }else if (tabId == R.id.tab_profile) {
                if (!textTitle.getText().equals(getString(R.string.iresponde_profile_tab))) {
                    textTitle.setText(R.string.iresponde_profile_tab);
//                    textTitle.setTextColor(getResources().getColor(R.color.colorAccent));
                    navigateTo(new ProfileFragment(), false);
                }
            }
            bottomBar.refreshDrawableState();
        });

        reportPending = bottomBar.getTabWithId(R.id.tab_reports);

        // TODO: Add logout

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            finish();
            startActivity(intent);
        }
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().getIdToken(true)
                    .addOnFailureListener(e -> {
                        String disabledMessage = "The user account has been disabled by an administrator.";
                        String errMessage =  e.getMessage();
                        if (disabledMessage.equals(errMessage)) {
                            // OR ADD DELETED IF THE ACCOUNT HAS BEEN DELETED
                            // If the user is disabled
                            goLoginPage();
                            Toast.makeText(HomeActivity.this, errMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void goLoginPage() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        finish();
        startActivity(intent);
    }

    @Override
    public void navigateTo(Fragment fragment, boolean addToBackstack) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();

        if (addToBackstack) {
            transaction.addToBackStack(null);
        }
        transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        transaction.replace(R.id.container, fragment);
        transaction.commit();
    }
}
