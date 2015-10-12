package com.melody.cool.myocr.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
//import com.google.android.gms.analytics.GoogleAnalytics;

public class BaseActivity  extends ActionBarActivity {

    public Context mContext;


    @Override
    protected void onStart() {
        super.onStart();
        //GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mContext = this;


    }
}