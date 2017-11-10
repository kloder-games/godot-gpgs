package org.godotengine.godot.gpgs;

import android.util.Log;
import android.app.Activity;
import com.google.android.gms.common.api.GoogleApiClient;
import android.os.Bundle;
import org.godotengine.godot.GodotLib;
import org.godotengine.godot.GodotGooglePlayGameServices;

import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;

import com.google.android.gms.plus.Plus;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.GamesActivityResultCodes;

import com.google.android.gms.common.ConnectionResult;


public class Client {

    private static final int STATUS_OTHER = 0;
    private static final int STATUS_CONNECTING = 1;
    private static final int STATUS_CONNECTED = 2;
    public static final int RC_SIGN_IN = 9001;
    public static final int REQUEST_RESOLVE_ERROR = 1001;

    private Activity activity = null;
    private int instance_id = 0;
    private GoogleApiClient googleApiClient = null;
    private GodotGooglePlayGameServices gpgs = null;

    private Boolean isResolvingConnectionFailure = false;

    private static final String TAG = "godot";

    public Client(final Activity activity, final GoogleApiClient googleApiClient, final int instance_id, GodotGooglePlayGameServices gpgs) {
        this.googleApiClient = googleApiClient;
        this.activity = activity;
        this.instance_id = instance_id;
        this.gpgs = gpgs;
        Log.d(TAG, "GPGS: Init");
        init();
    }

    public void init() {
        googleApiClient = new GoogleApiClient.Builder(activity).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                gpgs.setClient(googleApiClient);
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
        }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult result) {
                Log.i(TAG, "GPGS: onConnectionFailed result code: " + String.valueOf(result));
                if (isResolvingConnectionFailure) return; // Already resolving
                isResolvingConnectionFailure = true;
                if (!resolveConnectionFailure(result, REQUEST_RESOLVE_ERROR)) {
                    isResolvingConnectionFailure = false;
                }
            }
        })
        .addApi(Games.API).addScope(Games.SCOPE_GAMES)
        .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
        .addApi(Drive.API).addScope(Drive.SCOPE_APPFOLDER)
        .build();
        GodotLib.calldeferred(instance_id, "_on_google_play_game_services_initiated", new Object[] { });
    }

    public boolean resolveConnectionFailure(ConnectionResult result, int requestCode) {
        if (result.hasResolution()) {
            try {
                result.startResolutionForResult(activity, requestCode);
                return true;
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                googleApiClient.connect();
                return false;
            }
        } else {
            // not resolvable... so show an error message
            int errorCode = result.getErrorCode();

            if (errorCode == ConnectionResult.INTERNAL_ERROR) {
                googleApiClient.connect();
            }

            GodotLib.calldeferred(instance_id, "_on_google_play_game_services_connection_failed", new Object[] { });
            Log.i(TAG, "GPGS: onConnectionFailed error code: " + String.valueOf(errorCode));
            return false;
        }
    }

    public void onMainActivityResult(int requestCode, int responseCode, Intent intent) {
        if (responseCode == activity.RESULT_OK) {
            switch(requestCode) {
                case Client.RC_SIGN_IN:
                case Client.REQUEST_RESOLVE_ERROR:
                    isResolvingConnectionFailure = false;
                    if(!googleApiClient.isConnecting() && !googleApiClient.isConnected()) googleApiClient.connect();
                    break;
            }
        }
	}

    private void disconnect() {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        //Plus.AccountApi.clearDefaultAccount(googleApiClient);
		googleApiClient.disconnect();
        GodotLib.calldeferred(instance_id, "_on_google_play_game_services_disconnected", new Object[] { });
        Log.d(TAG, "GPGS: disconnected.");
    }

    public void signIn() {
        if (googleApiClient == null) return;
        activity.runOnUiThread(new Runnable() {
			@Override
            public void run() {
				if (!googleApiClient.isConnecting()) {
					googleApiClient.connect();
                    Log.d(TAG, "GPGS: signIn");
				}
			}
		});
	}

    public void signOut() {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
		activity.runOnUiThread(new Runnable() {
			@Override
            public void run() {
				disconnect();
                Log.d(TAG, "GPGS: signOut");
			}
		});
	}

    public int getStatus() {
        if (googleApiClient == null) return STATUS_OTHER;
		if (googleApiClient.isConnecting()) return STATUS_CONNECTING;
		if (googleApiClient.isConnected()) return STATUS_CONNECTED;
		return STATUS_OTHER;
	}

}
