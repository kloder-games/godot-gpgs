package org.godotengine.godot.gpgs;

import android.util.Log;
import android.app.Activity;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

public class Network {

    private Activity activity = null;
    private ConnectivityManager connectivityManager = null;

    private static final String TAG = "godot";

    public Network(final Activity activity) {
        this.activity = activity;

        connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        Log.d(TAG, "GPGS: Network init");
    }

    private boolean isConnected(int type) {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(type);

        return networkInfo.isConnected();
    }

    public boolean isOnline() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());
    }

    public boolean isWifiConnected() {
        return isConnected(ConnectivityManager.TYPE_WIFI);
    }

    public boolean isMobileConnected() {
        return isConnected(ConnectivityManager.TYPE_MOBILE);
    }
}
