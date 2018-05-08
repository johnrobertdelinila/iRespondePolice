package com.example.oteptudlong.irespondepolice;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class ReportHolder extends RecyclerView.ViewHolder {

    TextView textView;

    public ReportHolder(View itemView) {
        super(itemView);

        textView = itemView.findViewById(R.id.textView);

    }

}
