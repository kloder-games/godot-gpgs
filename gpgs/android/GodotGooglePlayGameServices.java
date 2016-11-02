package org.godotengine.godot;

import android.util.Log;
import android.os.Bundle;
import android.os.AsyncTask; // Added for snapshots
import java.io.IOException; // Added for snapshots
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap; // Added for snapshots

//import com.google.android.gms.plus.Plus;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.leaderboard.Leaderboards.LoadPlayerScoreResult;
import com.google.android.gms.games.leaderboard.Leaderboards.SubmitScoreResult;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.LeaderboardScore;

// Added for Snapshots
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.Snapshots;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.PendingResult; // Added for snapshots
import com.google.android.gms.common.api.Status;

public class GodotGooglePlayGameServices extends Godot.SingletonBase
{

    private static final int REQUEST_RESOLVE_ERROR = 1001;
	private static final int REQUEST_LEADERBOARD = 9102;
    private static final int REQUEST_ACHIEVEMENTS = 9002;
    private static final int RC_SIGN_IN = 9001;
    private static final int RC_SAVED_GAMES = 9009;

    private static final int STATUS_OTHER = 0;
    private static final int STATUS_CONNECTING = 1;
    private static final int STATUS_CONNECTED = 2;

    private static final String TAG = "godot";

    private Activity activity = null;
    private int instance_id = 0;

    private GoogleApiClient client = null;
    private boolean isResolvingError = false;

    private Boolean googlePlayConndected = false;

    private byte[] saveGameData;


    /* Savegame
     * ********************************************************************** */

    public void saveSnapshot(final String snapshotName, final String data, final String description)
    {

        if (client != null && client.isConnected()) {

            Log.i(TAG, "GPGS: Saving snapshot: '" + snapshotName + "'");

            // Prepare the data for save
            // data
            saveGameData = data.getBytes();

            Log.i(TAG, "GPGS: Test 1");

            AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>()
            {
                @Override
                protected Boolean doInBackground(Void... params) {

                    Log.i(TAG, "GPGS: Test 2");

                    // Open the snapshot, creating if necessary
                    Snapshots.OpenSnapshotResult open = Games.Snapshots.open(client, snapshotName, true).await();

                    Snapshot snapshot = null;

                    Log.i(TAG, "GPGS: Test 3");

                    if (!open.getStatus().isSuccess()) {

                        Log.w(TAG, "GPGS: Snapshots conflicts maybe...");

                        int status = open.getStatus().getStatusCode();
                        Log.i(TAG, "GPGS: Save Result status: " + status);

                        if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE) {
                            snapshot = open.getSnapshot();
                        } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {
                            snapshot = open.getSnapshot();
                            Snapshot conflictSnapshot = open.getConflictingSnapshot();

                            // Resolve between conflicts by selecting the newest of the conflicting snapshots.
                            Snapshot resolvedSnapshot = snapshot;
                            if (snapshot.getMetadata().getLastModifiedTimestamp() < conflictSnapshot.getMetadata().getLastModifiedTimestamp()) {
                                resolvedSnapshot = conflictSnapshot;
                            }

                            Snapshots.OpenSnapshotResult resolveResult = Games.Snapshots.resolveConflict(client, open.getConflictId(), resolvedSnapshot).await();

                            // Only try one time and move on..

                            open = Games.Snapshots.open(client, snapshotName, true).await();
                            if (!open.getStatus().isSuccess()) {
                                if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE) {
                                    snapshot = open.getSnapshot();
                                } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {
                                    Log.e(TAG, "GPGS: Could not resolve snapshot conflicts");
                                    return false;
                                }
                            } else {
                                snapshot = open.getSnapshot();
                            }

                        } else {

                            Log.e(TAG, "GPGS: Not a conflict, unknown error.");
                            return false;
                        }

                    } else {
                        // Everythings allrights
                        snapshot = open.getSnapshot();
                    }

                    Log.i(TAG, "GPGS: Test 4");

                    // Write the new data to the snapshot
                    snapshot.getSnapshotContents().writeBytes(saveGameData);

                    Log.i(TAG, "GPGS: Test 5");

                    // Change metadata
                    SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                            .setDescription(description)
                            .build();

                    Log.i(TAG, "GPGS: Test 6");

                    Snapshots.CommitSnapshotResult commit = Games.Snapshots.commitAndClose(client, snapshot, metadataChange).await();

                    Log.i(TAG, "GPGS: Test 6");

                    if (!commit.getStatus().isSuccess()) {
                        Log.w(TAG, "GPGS: Failed to commit Snapshot.");
                        return false;
                    }

                    Log.i(TAG, "GPGS: Test 7");

                    // No failures
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (result) {
                        Log.d(TAG, "GPGS: Snapshot saved successfully.");
                    } else {
                        Log.w(TAG, "GPGS: Snapshot saved error.");
                    }
                }
            };

            task.execute();
        }
    }

    public void loadFromSnapshot(final String snapshotName) {
        // Display a progress dialog
        // Handled by the game

        if (client != null && client.isConnected()) {

            Log.i(TAG, "GPGS: Loading snapshot: '" + snapshotName + "'");

            AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
                @Override
                protected Integer doInBackground(Void... params) {
                    // Open the saved game using its name.
                    Snapshots.OpenSnapshotResult result = Games.Snapshots.open(client, snapshotName, true).await();

                    // Check the result of the open operation
                    if (result.getStatus().isSuccess()) {
                        Snapshot snapshot = result.getSnapshot();

                        // Read the byte content of the saved game.
                        try {
                            saveGameData = snapshot.getSnapshotContents().readFully();
                            Log.d(TAG, "GPGS: Snapshot loaded successfully.");
                        } catch (IOException e) {
                            Log.e(TAG, "GPGS: Error while reading Snapshot.", e);
                        }
                    } else{
                        Log.e(TAG, "GPGS: Error while loading: " + result.getStatus().getStatusCode());
                    }

                    return result.getStatus().getStatusCode();
                }

                @Override
                protected void onPostExecute(Integer status) {
                    if (status == GamesStatusCodes.STATUS_OK) {
                        Log.d(TAG, "GPGS: Snapshot ready to use...");
                        String returnData = new String(saveGameData);
                        GodotLib.calldeferred(instance_id, "_on_google_play_game_services_snapshot_loaded", new Object[] { returnData });
                    } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_NOT_FOUND) {
                        Log.w(TAG, "GPGS: Snapshot not found.");
                        //GodotLib.calldeferred(instance_id, "_on_google_play_game_services_snapshot_not_found", new Object[] { });
                    } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CREATION_FAILED) {
                        Log.w(TAG, "GPGS: Snapshot creation failed.");
                    } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {
                        Log.w(TAG, "GPGS: STATUS_SNAPSHOT_CREATION_FAILED.");
                    } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE) {
                        Log.w(TAG, "GPGS: STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE.");
                    } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_FOLDER_UNAVAILABLE) {
                        Log.w(TAG, "GPGS: STATUS_SNAPSHOT_FOLDER_UNAVAILABLE.");
                    } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT_MISSING) {
                        Log.w(TAG, "GPGS: STATUS_SNAPSHOT_CONFLICT_MISSING.");
                    } else if (status == GamesStatusCodes.STATUS_NETWORK_ERROR_NO_DATA) {
                        Log.w(TAG, "GPGS: STATUS_NETWORK_ERROR_NO_DATA.");
                    } else if (status == GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED) {
                        Log.w(TAG, "GPGS: STATUS_CLIENT_RECONNECT_REQUIRED.");
                    } else if (status == GamesStatusCodes.STATUS_LICENSE_CHECK_FAILED) {
                        Log.w(TAG, "GPGS: STATUS_LICENSE_CHECK_FAILED.");
                    } else if (status == GamesStatusCodes.STATUS_INTERNAL_ERROR) {
                        Log.w(TAG, "GPGS: STATUS_INTERNAL_ERROR.");
                    } else {
                        Log.w(TAG, "GPGS: Unknown error.");
                    }

                    // Dismiss progress dialog and reflect the changes in the UI.
                    // ...
                }
            };

            task.execute();
        }
    }

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
                        Log.d(TAG, "GPGS: onConnected");
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        if (cause == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                            GodotLib.calldeferred(instance_id, "_on_google_play_game_services_suspended_network_lost", new Object[] { });
                            Log.d(TAG, "GPGS: onConnectionSuspended -> Network Lost");
                        } else if (cause == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                            GodotLib.calldeferred(instance_id, "_on_google_play_game_services_suspended_service_disconnected", new Object[] { });
                            Log.d(TAG, "GPGS: onConnectionSuspended -> Service Disconnected");
                        } else {
                            GodotLib.calldeferred(instance_id, "_on_google_play_game_services_suspended_unknown", new Object[] { });
                            Log.d(TAG, "GPGS: onConnectionSuspended -> Unknown");
                        }
                    }
                }).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
                {
                    @Override
                    public void onConnected(Bundle connectionHint)
                    {
                        // Nothing to do
                    }

                    @Override
                    public void onConnectionSuspended(int i)
                    {
                        client.connect();
                    }

                }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener()
                {
                    @Override
                    public void onConnectionFailed(ConnectionResult result)
                    {
                        if (isResolvingError) {
                            // already resolving
                            Log.d(TAG, "GPGS: onConnectionFailed (already resolving) ->" + result.toString());

                            if (!GodotGooglePlayGameServices.resolveConnectionFailure(activity, client, result, REQUEST_RESOLVE_ERROR)) {
                                Log.d(TAG, "GPGS: onConnectionFailed (imposible to resolve)->" + result.toString());
                                isResolvingError = false;
                            }

                            return;
                        }

                        Log.d(TAG, "GPGS: onConnectionFailed (try to resolve)->" + result.toString());
                        isResolvingError = true;

                        // Attempt to resolve the connection failure using BaseGameUtils.
                        // The R.string.signin_other_error value should reference a generic
                        // error string in your strings.xml file, such as "There was
                        // an issue with sign-in, please try again later."
                        if (!GodotGooglePlayGameServices.resolveConnectionFailure(activity, client, result, REQUEST_RESOLVE_ERROR)) {
                            Log.d(TAG, "GPGS: onConnectionFailed (imposible to resolve)->" + result.toString());
                            isResolvingError = false;

                        }
                    }
                })
                //.addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .addApi(Drive.API).addScope(Drive.SCOPE_APPFOLDER)
                .build();

                isResolvingError = false;
				//client.connect();

                Log.d(TAG, "GPGS: Init");
            }
        });
    }

    public static boolean resolveConnectionFailure(Activity activity, GoogleApiClient client, ConnectionResult result, int requestCode) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(activity, requestCode);
                return true;
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                client.connect();
                return false;
            }
        } else {
            Log.d(TAG, "GPGS: onConnectionFailed (hard)->" + result.toString());
            return false;
        }
    }

    /**
     * Internal disconnect method
     */
    private void disconnect() {
        //Plus.AccountApi.clearDefaultAccount(client);
        if (client != null && client.isConnected()) {
    		client.disconnect();
            GodotLib.calldeferred(instance_id, "_on_google_play_game_services_disconnected", new Object[] { });
            Log.d(TAG, "GPGS: disconnected.");
        }
    }

    @Override
    protected void onMainActivityResult(int requestCode, int responseCode, Intent intent)
	{
		switch(requestCode) {
            case REQUEST_RESOLVE_ERROR:
                if (responseCode != Activity.RESULT_OK) {
				    Log.d(TAG, "GPGS: onMainActivityResult, REQUEST_RESOLVE_ERROR = " + responseCode);
                }
                isResolvingError = true;
                if (!client.isConnecting() && !client.isConnected()) {
                    client.connect();
                }
                break;
            case REQUEST_LEADERBOARD:
                Log.d(TAG, "GPGS: onMainActivityResult, REQUEST_LEADERBOARD = " + responseCode);
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
                    Log.d(TAG, "GPGS: signIn");
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
                    Log.d(TAG, "GPGS: signOut");
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
                    Log.d(TAG, "GPGS: incrementAchy '" + id + "' by " + increment + ".");
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
                    Log.d(TAG, "GPGS: unlockAchy '" + id + "'.");
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
                    Log.d(TAG, "GPGS: showAchyList.");
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
                                Log.d(TAG, "GPGS: leaderSubmit OK");
                                GodotLib.calldeferred(instance_id, "_on_google_play_game_services_leaderboard_submitted_ok", new Object[] { id });
                            } else if (status.getStatusCode() == GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED) {
                                Log.d(TAG, "GPGS: leaderSubmit reconnect required -> reconnecting...");
                                client.reconnect();
                            } else {
                                Log.d(TAG, "GPGS: leaderSubmit connection error -> " + status.getStatusMessage());
                                GodotLib.calldeferred(instance_id, "_on_leaderboard_submit_error", new Object[]{ id });
                            }
                        }
                    });
                    Log.d(TAG, "GPGS: leaderSubmit '" + id + "' by " + score + ".");
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
                    Log.d(TAG, "GPGS: showLeaderList.");
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
                                    Log.d(TAG, "GPGS: Leaderboard values is " + score.getDisplayScore());
                                    GodotLib.calldeferred(instance_id, "_on_leaderboard_get_value", new Object[]{ scoreValue, id });
                                } else {
                                    Log.d(TAG, "GPGS: getLeaderboardValue STATUS_OK but is NULL -> Request again...");
                                    getLeaderboardValue(id);
                                }
                            } else if (status.getStatusCode() == GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED) {
                                Log.d(TAG, "GPGS: getLeaderboardValue reconnect required -> reconnecting...");
                                client.reconnect();
                            } else {
                                Log.d(TAG, "GPGS: getLeaderboardValue connection error -> " + status.getStatusMessage());
                                GodotLib.calldeferred(instance_id, "_on_leaderboard_get_value_error", new Object[]{ id });
                            }
                        }
                    });
                    Log.d(TAG, "GPGS: getLeaderboardValue '" + id + "'.");
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
            "saveSnapshot", "loadFromSnapshot",
            "unlockAchy", "incrementAchy", "showAchyList",
            "leaderSubmit", "showLeaderList", "getLeaderboardValue"
        });
    }
}
