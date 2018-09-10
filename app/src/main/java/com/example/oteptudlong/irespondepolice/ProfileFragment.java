package com.example.oteptudlong.irespondepolice;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    public ProfileFragment() {
        // Required empty public constructor
    }

    private Activity activity;
    private Context context;

    private TextView textPoliceName, textPolicePosition;
    private Button btnLogout;

    private DatabaseReference policeRef = FirebaseDatabase.getInstance().getReference().child("Police");

    private FirebaseUser mUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        activity = getActivity();
        context = getContext();

        textPoliceName = view.findViewById(R.id.text_police_name);
        textPolicePosition = view.findViewById(R.id.text_police_position);
        btnLogout = view.findViewById(R.id.btn_logout);
        mUser = HomeActivity.mAuth.getCurrentUser();

        if (mUser != null && mUser.getDisplayName() != null) {
            textPoliceName.setText(mUser.getDisplayName());
        }

        if (HomeActivity.policePosition != null) {
            textPolicePosition.setText(HomeActivity.policePosition);
        }

        btnLogout.setOnClickListener(view1 -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setTitle("Sign out");
            dialog.setMessage("Are you sure to logout?");
            dialog.setPositiveButton("OK", (dialogInterface, i) -> {
                HomeActivity.mAuth.signOut();
                Intent intent = new Intent(activity, LoginActivity.class);
                activity.finish();
                startActivity(intent);
            });
            dialog.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());
            dialog.show();
        });

        return view;
    }

}
