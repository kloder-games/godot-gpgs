package org.godotengine.godot;

import android.util.Log;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;

import com.google.android.gms.plus.Plus;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.leaderboard.Leaderboards.LoadPlayerScoreResult;
import com.google.android.gms.games.leaderboard.Leaderboards.SubmitScoreResult;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.LeaderboardScore;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class GodotGooglePlayGameServices extends Godot.SingletonBase
{

    private static final int REQUEST_RESOLVE_ERROR = 1001;
	private static final int REQUEST_LEADERBOARD = 9102;
    private static final int REQUEST_ACHIEVEMENTS = 9002;

    private static final int STATUS_OTHER = 0;
    private static final int STATUS_CONNECTING = 1;
    private static final int STATUS_CONNECTED = 2;


    private Activity activity = null;
    private int instance_id = 0;

    private GoogleApiClient client = null;
    private boolean isResolvingError = false;

    private Boolean googlePlayConndected = false;

    /* Connection Methods
     * ********************************************************************** */

    /**
     * Initialization
     */
    public void init(final int instance_id) {
        this.instance_id = instance_id;

        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                client = new GoogleApiClient.Builder(activity).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
                {
                    @Override
                    public void onConnected(Bundle bundle) {
                        GodotLib.calldeferred(instance_id, "_on_google_play_game_services_connected", new Object[] { });
                        Log.d("godot", "GPGS: onConnected");
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        if (cause == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                            GodotLib.calldeferred(instance_id, "_on_google_play_game_services_suspended_network_lost", new Object[] { });
                            Log.d("godot", "GPGS: onConnectionSuspended -> Network Lost");
                        } else if (cause == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                            GodotLib.calldeferred(instance_id, "_on_google_play_game_services_suspended_service_disconnected", new Object[] { });
                            Log.d("godot", "GPGS: onConnectionSuspended -> Service Disconnected");
                        } else {
                            GodotLib.calldeferred(instance_id, "_on_google_play_game_services_suspended_unknown", new Object[] { });
                            Log.d("godot", "GPGS: onConnectionSuspended -> Unknown");
                        }
                    }
                }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener()
                {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        if (isResolvingError) {
                            Log.d("godot", "GPGS: onConnectionFailed->" + result.toString());
							return;
						} else if (result.hasResolution()) {
                            try {
                                isResolvingError = true;
                                result.startResolutionForResult(activity, REQUEST_RESOLVE_ERROR);
                            } catch (SendIntentException e) {
                                Log.d("godot", "GPGS: onConnectionFailed, try again");
								client.connect();
                            }
                        } else {
                            Log.d("godot", "GPGS: onConnectionFailed->" + result.toString());
                            isResolvingError = true;
                        }
                    }
                })
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

                isResolvingError = false;
				//client.connect();

                Log.d("godot", "GPGS: Init");
            }
        });
    }

    /**
     * Internal disconnect method
     */
    private void disconnect() {
        Plus.AccountApi.clearDefaultAccount(client);
		client.disconnect();
        GodotLib.calldeferred(instance_id, "_on_google_play_game_services_disconnected", new Object[] { });
        Log.d("godot", "GPGS: disconnected.");
    }

    @Override
    protected void onMainActivityResult(int requestCode, int responseCode, Intent intent)
	{
		switch(requestCode) {
            case REQUEST_RESOLVE_ERROR:
                if (responseCode != Activity.RESULT_OK) {
				    Log.d("godot", "GPGS: onMainActivityResult, REQUEST_RESOLVE_ERROR = " + responseCode);
                }
                isResolvingError = true;
                if (!client.isConnecting() && !client.isConnected()) {
                    client.connect();
                }
                break;
            case REQUEST_LEADERBOARD:
                Log.d("godot", "GPGS: onMainActivityResult, REQUEST_LEADERBOARD = " + responseCode);
                if(responseCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
                    client.reconnect();
                    //disconnect();
                }
                break;
        }
	}

    /**
     * Sign In method
     */
    public void signIn()
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override
            public void run()
			{
				if (!client.isConnecting()) {
					isResolvingError = false;
					client.connect();
                    Log.d("godot", "GPGS: signIn");
				}
			}
		});
	}

    /**
     * Sign Out method
     */
	public void signOut()
	{
		activity.runOnUiThread(new Runnable()
		{
			@Override
            public void run()
			{
				if (client != null && client.isConnected()) {
					disconnect();
                    Log.d("godot", "GPGS: signOut");
				}
			}
		});
	}

    /**
     * Get the client status
     * @return int Return 1 for Conecting..., 2 for Connected, 0 in any other case
     */
    public int getStatus()
	{
		if (client.isConnecting()) return STATUS_CONNECTING;
		if (client.isConnected()) return STATUS_CONNECTED;
		return STATUS_OTHER;
	}

    /* Achievements Methods
     * ********************************************************************** */

    /**
     * Increment Achivement
     * @param String id Achivement to increment
     * @param int amount The amount for increment
     */
    public void incrementAchy(final String id, final int increment) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (client != null && client.isConnected()) {
                    Games.Achievements.increment(client, id, increment);
                    Log.d("godot", "GPGS: incrementAchy '" + id + "' by " + increment + ".");
                }
            }
        });
    }

    /**
     * Unlock Achivement
     * @param String id Achivement to unlock
     */
    public void unlockAchy(final String id) {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run() {
                if (client != null && client.isConnected()) {
                    Games.Achievements.unlock(client, id);
                    Log.d("godot", "GPGS: unlockAchy '" + id + "'.");
                }
            }
        });
    }

    /**
     * Show Achivements List
     */
    public void showAchyList() {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run() {
                if (client != null && client.isConnected()) {
                    activity.startActivityForResult(Games.Achievements.getAchievementsIntent(client), REQUEST_ACHIEVEMENTS);
                    Log.d("godot", "GPGS: showAchyList.");
                }
            }
        });
    }

    /* Leaderboards Methods
     * ********************************************************************** */

    /**
     * Upload score to a leaderboard
     * @param String id Id of the leaderboard
     * @param int score Score to upload to the leaderboard
     */
    public void leaderSubmit(final String id, final int score)
 	{
 		activity.runOnUiThread(new Runnable()
 		{
 			@Override public void run()
 			{
 				if (client != null && client.isConnected()) {
 					Games.Leaderboards.submitScoreImmediate(client, id, score).setResultCallback(new ResultCallback<SubmitScoreResult>()
                    {
                        @Override
                        public void onResult(SubmitScoreResult result) {
                            Status status = result.getStatus();
                            if (status.getStatusCode() == GamesStatusCodes.STATUS_OK) {
                                Log.d("godot", "GPGS: leaderSubmit OK");
                                GodotLib.calldeferred(instance_id, "_on_google_play_game_services_leaderboard_submitted_ok", new Object[] { id });
                            } else if (status.getStatusCode() == GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED) {
                                Log.d("godot", "GPGS: leaderSubmit reconnect required -> reconnecting...");
                                client.reconnect();
                            } else {
                                Log.d("godot", "GPGS: leaderSubmit connection error -> " + status.getStatusMessage());
                                GodotLib.calldeferred(instance_id, "_on_leaderboard_submit_error", new Object[]{ id });
                            }
                        }
                    });
                    Log.d("godot", "GPGS: leaderSubmit '" + id + "' by " + score + ".");
 				}
 			}
 		});
 	}

    /**
     * Show leader board
     * @param String id Id of the leaderboard
     */
    public void showLeaderList(final String id)
    {
        activity.runOnUiThread(new Runnable()
 		{
 			@Override public void run()
 			{
 				if (client != null && client.isConnected()) {
 					activity.startActivityForResult(Games.Leaderboards.getLeaderboardIntent(client, id, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC), REQUEST_LEADERBOARD);
                    Log.d("godot", "GPGS: showLeaderList.");
 				}
 			}
 		});
    }

    /**
     * Get a leaderboard value (in a callback)
     * @param String id Id of the leaderboard
     */
    public void getLeaderboardValue(final String id)
    {
        activity.runOnUiThread(new Runnable()
 		{
 			@Override public void run()
 			{
 				if (client != null && client.isConnected()) {
                    Games.Leaderboards.loadCurrentPlayerLeaderboardScore(client, id, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC).setResultCallback(new ResultCallback<LoadPlayerScoreResult>()
                    {
                        @Override
                        public void onResult(LoadPlayerScoreResult result) {
                            Status status = result.getStatus();
                            if (status.getStatusCode() == GamesStatusCodes.STATUS_OK) {
                                LeaderboardScore score = result.getScore();
                                if (score != null) {
                                    int scoreValue = (int) score.getRawScore();
                                    Log.d("godot", "GPGS: Leaderboard values is " + score.getDisplayScore());
                                    GodotLib.calldeferred(instance_id, "_on_leaderboard_get_value", new Object[]{ scoreValue, id });
                                } else {
                                    Log.d("godot", "GPGS: getLeaderboardValue STATUS_OK but is NULL -> Request again...");
                                    getLeaderboardValue(id);
                                }
                            } else if (status.getStatusCode() == GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED) {
                                Log.d("godot", "GPGS: getLeaderboardValue reconnect required -> reconnecting...");
                                client.reconnect();
                            } else {
                                Log.d("godot", "GPGS: getLeaderboardValue connection error -> " + status.getStatusMessage());
                                GodotLib.calldeferred(instance_id, "_on_leaderboard_get_value_error", new Object[]{ id });
                            }
                        }
                    });
                    Log.d("godot", "GPGS: getLeaderboardValue '" + id + "'.");
 				}
 			}
 		});
    }

    /* Godot Methods
     * ********************************************************************** */

     /**
      * Singleton
      */
     static public Godot.SingletonBase initialize(Activity activity)
     {
         return new GodotGooglePlayGameServices(activity);
     }

    /**
     * Constructor
     * @param Activity Main activity
     */
    public GodotGooglePlayGameServices(Activity activity) {
        this.activity = activity;
        registerClass("GodotGooglePlayGameServices", new String[] {
            "init", "signIn", "signOut", "getStatus",
            "unlockAchy", "incrementAchy", "showAchyList",
            "leaderSubmit", "showLeaderList", "getLeaderboardValue"
        });
    }
}
