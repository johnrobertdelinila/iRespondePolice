package com.example.oteptudlong.irespondepolice;

import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class CoordinatorActivity extends AppCompatActivity {

    private LinearLayout bottomSheetLayout;
    private BottomSheetBehavior bottomSheetBehavior;
    private ImageView share;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coordinator);

        bottomSheetLayout = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        share = findViewById(R.id.image_share);

        // helo

        // state hidden
//        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        //state Collapsed
//            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        //state Expanded
//        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        //stete dragging
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_DRAGGING);

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

    }
}
