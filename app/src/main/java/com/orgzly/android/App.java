package com.orgzly.android;

import android.app.Application;
import android.content.Context;

import androidx.preference.PreferenceManager;

public class App extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getAppContext() {
        return context;
    }

    public static void setDefaultPreferences(Context context, boolean readAgain) {
        if (readAgain) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().apply();
        }
    }
}
