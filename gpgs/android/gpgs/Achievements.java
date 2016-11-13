package org.godotengine.godot.gpgs;

import android.util.Log;
import android.app.Activity;
import com.google.android.gms.common.api.GoogleApiClient;
import org.godotengine.godot.GodotLib;

import com.google.android.gms.games.Games;

public class Achievements {

    private static final int REQUEST_ACHIEVEMENTS = 9002;

    private Activity activity = null;
    private int instance_id = 0;
    private GoogleApiClient googleApiClient = null;

    private static final String TAG = "godot";

    public Achievements(Activity activity, GoogleApiClient googleApiClient, int instance_id) {
        this.googleApiClient = googleApiClient;
        this.activity = activity;
        this.instance_id = instance_id;
    }

    public void incrementAchy(final String id, final int increment) {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Games.Achievements.increment(googleApiClient, id, increment);
                Log.d(TAG, "GPGS: incrementAchy '" + id + "' by " + increment + ".");
            }
        });
    }

    public void unlockAchy(final String id) {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run() {
                Games.Achievements.unlock(googleApiClient, id);
                Log.d(TAG, "GPGS: unlockAchy '" + id + "'.");
            }
        });
    }

    public void showAchyList() {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run() {
                activity.startActivityForResult(Games.Achievements.getAchievementsIntent(googleApiClient), REQUEST_ACHIEVEMENTS);
                Log.d(TAG, "GPGS: showAchyList.");
            }
        });
    }

}
