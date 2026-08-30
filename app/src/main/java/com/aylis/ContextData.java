

package com.aylis;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public class ContextData {

    private Activity activity;

    public ContextData(Activity activity) {
        this.activity = activity;
    }

    public ContextData(View view) {
        activity = (Activity)view.getContext();
    }

    public FragmentManager getFragmentManager() {
        if (activity instanceof AppCompatActivity) {
            return ((AppCompatActivity) activity).getSupportFragmentManager();
        }
        return null;
    }

    public LayoutInflater getLayoutInflater() {
        return activity.getLayoutInflater();
    }

    public Context getContext() {
        return activity;
    }
}
