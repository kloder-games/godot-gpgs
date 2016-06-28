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
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.LeaderboardScore;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;

public class GodotGooglePlayGameServices extends Godot.SingletonBase
{

    private static final int REQUEST_RESOLVE_ERROR = 1001;
	private static final int REQUEST_LEADERBOARD = 1002;
    private static final int REQUEST_ACHIEVEMENTS = 9002;

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
                        GodotLib.calldeferred(instance_id, "on_google_play_game_services_connected", new Object[] { });
                        Log.d("godot", "GPGS: Connected");
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d("godot", "GPGS: Suspended->" + cause);
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
                            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), activity, 0).show();
                        }
                    }
                }) // .setShowConnectingPopup(false)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

                isResolvingError = false;
				client.connect();

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
                    disconnect();
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
		if (client.isConnecting()) return 1;
		if (client.isConnected()) return 2;
		return 0;
	}

    /* Achievements Methods
     * ********************************************************************** */

    /**
     * Increment Achivement
     * @param String achievementId Achivement to increment
     * @param int incrementAmount The amount for increment
     */
    public void incrementAchy(final String id, final int increment) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (client != null && client.isConnected()) {
                    Games.Achievements.increment(client, id, increment);
                }
            }
        });
    }

    /**
     * Unlock Achivement
     * @param String achievementId Achivement to unlock
     */
    public void unlockAchy(final String id) {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run() {
                if (client != null && client.isConnected()) {
                    Games.Achievements.unlock(client, id);
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
 					Games.Leaderboards.submitScore(client, id, score);
                    Log.d("godot", "GPGS: leaderSubmit");
 				}
 			}
 		});
 	}

    /**
     * Show leader board
     */
    public void showLeaderList(final String id)
    {
        activity.runOnUiThread(new Runnable()
 		{
 			@Override public void run()
 			{
 				if (client != null && client.isConnected()) {
 					activity.startActivityForResult(Games.Leaderboards.getLeaderboardIntent(client, id), REQUEST_LEADERBOARD);
                    Log.d("godot", "GPGS: leaderShow");
 				}
 			}
 		});
    }

    public void getLeaderboardValue(final String id)
    {
        activity.runOnUiThread(new Runnable()
 		{
 			@Override public void run()
 			{
 				if (client != null && client.isConnected()) {
                    Games.Leaderboards.loadCurrentPlayerLeaderboardScore(client, id, LeaderboardVariant.TIME_SPAN_DAILY, LeaderboardVariant.COLLECTION_PUBLIC).setResultCallback(new ResultCallback<LoadPlayerScoreResult>()
                    {
                        @Override
                        public void onResult(LoadPlayerScoreResult result) {
                            if (result.getStatus().getStatusCode() == GamesStatusCodes.STATUS_OK) {
                                LeaderboardScore score = result.getScore();
                                int scoreValue = (int) score.getRawScore();
                                Log.d("godot", "GPGS: Leaderboard values is " + scoreValue);
                                GodotLib.calldeferred(instance_id, "_on_leaderboard_get_value", new Object[]{ scoreValue });
                            } else {
                                Log.d("godot", "GPGS: Leaderboard connection error");
                                GodotLib.calldeferred(instance_id, "_on_leaderboard_get_value_error", new Object[]{ });
                            }
                        }
                    });
                    Log.d("godot", "GPGS: Leaderboard get score");
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
