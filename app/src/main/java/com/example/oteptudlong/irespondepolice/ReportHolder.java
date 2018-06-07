package com.example.oteptudlong.irespondepolice;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.github.aakira.compoundicontextview.CompoundIconTextView;

public class ReportHolder extends RecyclerView.ViewHolder {

    TextView status, incident, responding;
    CardView cardView, cardStatus;
    CompoundIconTextView distance;

    public ReportHolder(View itemView) {
        super(itemView);

        distance = itemView.findViewById(R.id.textView);
        cardView = itemView.findViewById(R.id.cardView);
        status = itemView.findViewById(R.id.status);
        cardStatus = itemView.findViewById(R.id.card_status);
        incident = itemView.findViewById(R.id.incidentRespond);
        responding = itemView.findViewById(R.id.responding);
    }

}
