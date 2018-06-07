package com.example.oteptudlong.irespondepolice;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import dmax.dialog.SpotsDialog;

public class LoginActivity extends AppCompatActivity {

    private Button btn_login;
    private TextInputEditText edit_username, edit_password;
    private TextInputLayout textInput_email, textInput_password;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private DatabaseReference mPolice = FirebaseDatabase.getInstance().getReference().child("Police");
    private SpotsDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }

        init();

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authenticatePolice();
            }
        });

    }

    private void authenticatePolice() {
        String email = edit_username.getText().toString().trim();
        String password = edit_password.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            textInput_email.setError("Email is required.");
            return;
        }
        textInput_email.setError(null);
        if (TextUtils.isEmpty(password)) {
            textInput_password.setError("Password is empty.");
            return;
        }
        textInput_password.setError(null);
        loadingDialog.show();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        mPolice.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String uid = mAuth.getCurrentUser().getUid();
                                if (dataSnapshot.hasChild(uid)) {
                                    // Go to dashboard page
                                    loadingDialog.dismiss();
                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    finish();
                                    startActivity(intent);
                                }else {
                                    // Logout user
                                    loadingDialog.dismiss();
                                    Toast.makeText(LoginActivity.this, "The password is invalid or the user does not have a password.", Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                loadingDialog.dismiss();
                                Toast.makeText(LoginActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingDialog.dismiss();
                        Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void init() {
        getSupportActionBar().hide();

        btn_login = findViewById(R.id.btn_login);
        edit_username = findViewById(R.id.edit_email);
        edit_password = findViewById(R.id.edit_password);
        textInput_email = findViewById(R.id.textInput_email);
        textInput_password = findViewById(R.id.textInput_password);
        loadingDialog = new SpotsDialog(this);
    }

}
