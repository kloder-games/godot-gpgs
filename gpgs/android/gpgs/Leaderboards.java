package org.godotengine.godot.gpgs;

import android.util.Log;
import android.app.Activity;
import com.google.android.gms.common.api.GoogleApiClient;
import org.godotengine.godot.GodotLib;

import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.leaderboard.Leaderboards.LoadPlayerScoreResult;
import com.google.android.gms.games.leaderboard.Leaderboards.SubmitScoreResult;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.LeaderboardScore;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class Leaderboards {

    private static final int REQUEST_LEADERBOARD = 9102;

    private Activity activity = null;
    private int instance_id = 0;
    private GoogleApiClient googleApiClient = null;

    private static final String TAG = "godot";

    public Leaderboards(Activity activity, GoogleApiClient googleApiClient, int instance_id) {
        this.googleApiClient = googleApiClient;
        this.activity = activity;
        this.instance_id = instance_id;
    }

    public void leaderSubmit(final String id, final int score) {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
 		activity.runOnUiThread(new Runnable() {
 			@Override
            public void run() {
				Games.Leaderboards.submitScoreImmediate(googleApiClient, id, score).setResultCallback(new ResultCallback<SubmitScoreResult>() {
                    @Override
                    public void onResult(SubmitScoreResult result) {
                        Status status = result.getStatus();
                        if (status.getStatusCode() == GamesStatusCodes.STATUS_OK) {
                            Log.d(TAG, "GPGS: leaderSubmit OK");
                            GodotLib.calldeferred(instance_id, "_on_google_play_game_services_leaderboard_submitted_ok", new Object[] { id });
                        } else if (status.getStatusCode() == GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED) {
                            Log.d(TAG, "GPGS: leaderSubmit reconnect required -> reconnecting...");
                            googleApiClient.reconnect();
                        } else {
                            Log.d(TAG, "GPGS: leaderSubmit connection error -> " + status.getStatusMessage());
                            GodotLib.calldeferred(instance_id, "_on_leaderboard_submit_error", new Object[]{ id });
                        }
                    }
                });
                Log.d(TAG, "GPGS: leaderSubmit '" + id + "' by " + score + ".");
 			}
 		});
 	}

    public void showLeaderList(final String id) {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        activity.runOnUiThread(new Runnable() {
 			@Override
            public void run() {
 				if (googleApiClient != null && googleApiClient.isConnected()) {
 					activity.startActivityForResult(Games.Leaderboards.getLeaderboardIntent(googleApiClient, id, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC), REQUEST_LEADERBOARD);
                    Log.d(TAG, "GPGS: showLeaderList.");
 				}
 			}
 		});
    }

    public void getLeaderboardValue(final String id) {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        activity.runOnUiThread(new Runnable() {
 			@Override
            public void run() {
                Games.Leaderboards.loadCurrentPlayerLeaderboardScore(googleApiClient, id, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC).setResultCallback(new ResultCallback<LoadPlayerScoreResult>() {
                    @Override
                    public void onResult(LoadPlayerScoreResult result) {
                        Status status = result.getStatus();
                        if (status.getStatusCode() == GamesStatusCodes.STATUS_OK) {
                            LeaderboardScore score = result.getScore();
                            if (score != null) {
                                int scoreValue = (int) score.getRawScore();
                                Log.d(TAG, "GPGS: Leaderboard values is " + score.getDisplayScore());
                                GodotLib.calldeferred(instance_id, "_on_leaderboard_get_value", new Object[]{ scoreValue, id });
                            } else {
                                Log.d(TAG, "GPGS: getLeaderboardValue STATUS_OK but is NULL -> Request again...");
                                getLeaderboardValue(id);
                            }
                        } else if (status.getStatusCode() == GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED) {
                            Log.d(TAG, "GPGS: getLeaderboardValue reconnect required -> reconnecting...");
                            googleApiClient.reconnect();
                        } else {
                            Log.d(TAG, "GPGS: getLeaderboardValue connection error -> " + status.getStatusMessage());
                            GodotLib.calldeferred(instance_id, "_on_leaderboard_get_value_error", new Object[]{ id });
                        }
                    }
                });
                Log.d(TAG, "GPGS: getLeaderboardValue '" + id + "'.");
 			}
 		});
    }

}
