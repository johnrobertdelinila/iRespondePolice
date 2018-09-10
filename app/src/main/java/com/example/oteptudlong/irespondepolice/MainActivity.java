package com.example.oteptudlong.irespondepolice;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    private EditText edit_idNumber, edit_password;
    private Button btn_login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        btn_login.setOnClickListener(v -> {
            String idNumber = edit_idNumber.getText().toString().trim();
            String password = edit_password.getText().toString().trim();

            if (TextUtils.isEmpty(idNumber)) {
                return;
            }
            if (TextUtils.isEmpty(password)) {
                return;
            }

            // authentication of police

        });

    }

    private void init() {
        edit_idNumber = findViewById(R.id.edit_id);
        edit_password = findViewById(R.id.edit_password);
        btn_login = findViewById(R.id.btn_login);
    }
}
