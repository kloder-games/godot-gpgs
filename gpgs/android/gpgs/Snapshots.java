package org.godotengine.godot.gpgs;

import android.util.Log;
import android.app.Activity;
import com.google.android.gms.common.api.GoogleApiClient;
import org.godotengine.godot.GodotLib;

import android.os.AsyncTask;
import java.io.IOException;
import android.graphics.Bitmap;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;

import com.google.android.gms.common.api.PendingResult;

public class Snapshots {

    private static final int RC_SAVED_GAMES = 9009;

    private Activity activity = null;
    private int instance_id = 0;
    private GoogleApiClient googleApiClient = null;
    private byte[] saveGameData;

    private static final String TAG = "godot";

    public Snapshots(Activity activity, GoogleApiClient googleApiClient, int instance_id) {
        this.googleApiClient = googleApiClient;
        this.activity = activity;
        this.instance_id = instance_id;
    }

    public void saveSnapshot(final String snapshotName, final String data, final String description) {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        Log.i(TAG, "GPGS: Saving snapshot: '" + snapshotName + "'");
        saveGameData = data.getBytes(); // Prepare the data for save data
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                // Open the snapshot, creating if necessary
                com.google.android.gms.games.snapshot.Snapshots.OpenSnapshotResult open = Games.Snapshots.open(googleApiClient, snapshotName, true).await();
                Snapshot snapshot = null;
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
                        com.google.android.gms.games.snapshot.Snapshots.OpenSnapshotResult resolveResult = Games.Snapshots.resolveConflict(googleApiClient, open.getConflictId(), resolvedSnapshot).await();
                        // Only try one time and move on..
                        open = Games.Snapshots.open(googleApiClient, snapshotName, true).await();
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
                    snapshot = open.getSnapshot(); // Everythings allrights
                }
                // Write the new data to the snapshot
                snapshot.getSnapshotContents().writeBytes(saveGameData);
                // Change metadata
                SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                        .setDescription(description)
                        .build();
                com.google.android.gms.games.snapshot.Snapshots.CommitSnapshotResult commit = Games.Snapshots.commitAndClose(googleApiClient, snapshot, metadataChange).await();
                if (!commit.getStatus().isSuccess()) {
                    Log.w(TAG, "GPGS: Failed to commit Snapshot.");
                    return false;
                }
                // No failures
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) Log.d(TAG, "GPGS: Snapshot saved successfully.");
                else Log.w(TAG, "GPGS: Snapshot saved error.");
            }
        };
        task.execute();
    }

    public void loadFromSnapshot(final String snapshotName) {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        // Display a progress dialog
        // Handled by the game
        
        Log.i(TAG, "GPGS: Loading snapshot: '" + snapshotName + "'");
        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                // Open the saved game using its name.
                com.google.android.gms.games.snapshot.Snapshots.OpenSnapshotResult result = Games.Snapshots.open(googleApiClient, snapshotName, true).await();
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
