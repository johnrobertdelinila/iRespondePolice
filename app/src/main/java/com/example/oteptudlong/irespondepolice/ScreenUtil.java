package com.example.oteptudlong.irespondepolice;

import android.content.Context;

public class ScreenUtil {
    public static float dp2px(int dip, Context context) {
        float scale = context.getResources().getDisplayMetrics().density;
        return dip * scale + 0.5f;
    }
}
