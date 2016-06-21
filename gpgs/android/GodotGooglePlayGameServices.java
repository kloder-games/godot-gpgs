package org.godotengine.godot;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Log;
import android.view.View;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.games.Games;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.common.ConnectionResult;
import android.content.IntentSender.SendIntentException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

public class GodotGooglePlayGameServices extends Godot.SingletonBase
{

    private static final int                RC_SAVED_GAMES = 9002;
    private static final int                RC_SIGN_IN = 9001;
    private static final int                REQUEST_ACHIEVEMENTS = 9002;

    private int device_id;
    private Activity activity;
    private GoogleApiClient googleApiClient;
    private Boolean requestSignIn = false;
    private Boolean intentInProgress = false;
    private Boolean googlePlayConndected = false;

    /**
     * Singleton
     */
    static public Godot.SingletonBase initialize(Activity activity)
    {
        return new GodotGoogleGamePlayServices(activity);
    }

    /**
     * Initialization
     */
    public void init() {
        device_id = this.getDeviceId();
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                googleApiClient = new GoogleApiClient.Builder(activity).addConnectionCallbacks(new ConnectionCallbacks()
                {
                    @Override
                    public void onConnected(Bundle bundle) {
                        googlePlayConndected = true;
                        Log.d("godot", "GPGS: Connected");
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d("godot", "GPGS: Suspended");
                    }
                }).addOnConnectionFailedListener(new OnConnectionFailedListener()
                {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.w("godot", "Connection failed: " + String.valueOf(result));

                        if(!intentInProgress && result.hasResolution()) {
                            try {
                                intentInProgress = true;
                                activity.startIntentSenderForResult(result.getResolution().getIntentSender(), RC_SIGN_IN, null, 0, 0, 0);
                            } catch (SendIntentException ex) {
                                intentInProgress = false;
                                googleApiClient.connect();
                            }
                        }
                    }
                })
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Drive.API).addScope(Drive.SCOPE_APPFOLDER)
                .build();
            }
        });
    }

    /**
     * Sign In method
     */
    public void signIn() {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run() {
                googleApiClient.connect();
            }
        });
    }

    /**
     * Sign Out method
     */
    public void signOut() {
        disconnect();
    }

    /**
     * Internal disconnect method
     */
    private void disconnect() {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run() {
                if(googleApiClient.isConnected()) {
                    Games.signOut(googleApiClient);
                    googleApiClient.disconnect();
                    googlePlayConndected = false;
                }
                Log.d("godot", "GPGS: disconnect");
            }
        });
    }

    /**
     * Check if the user is connected
     */
    public void isLoggedIn() {
        GodotLib.calldeferred(device_id, "is_user_logged_in", new Object[]
        {
            (boolean) googlePlayConndected
        });
    }

    /**
     * Increment Achivement
     * @param String achievementId Achivement to increment
     * @param int incrementAmount The amount for increment
     */
    public void incrementAchy(final String achievementId, final int incrementAmount) {
        if(googlePlayConndected) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Games.Achievements.increment(googleApiClient, achievementId, incrementAmount);
                }
            });
        } else {
            Log.w("godot", "GPGS: incrementAchy - Not connected.");
        }
    }

    /**
     * Unlock Achivement
     * @param String achievementId Achivement to unlock
     */
    public void unlockAchy(final String achievementId) {
        if(googlePlayConndected) {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run() {
                    Games.Achievements.unlock(googleApiClient, achievementId);
                }
            });
        } else {
            Log.w("godot", "GPGS: unlockAchy - Not connected.");
        }
    }

    /**
     * Show Achivements List
     */
    public void showAchyList() {
        if(googlePlayConndected) {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run() {
                    activity.startActivityForResult(Games.Achievements.getAchievementsIntent(googleApiClient), REQUEST_ACHIEVEMENTS);
                }
            });
        } else {
            Log.w("godot", "GPGS: showAchyList - Not connected.");
        }
    }

    protected void onMainActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RC_SIGN_IN) {
            intentInProgress = false;

            if(!googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
        }
    }

    /**
	 * Generate MD5 for the deviceID
	 * @param String s The string to generate de MD5
	 * @return String The MD5 generated
	 */
	private String md5(final String s)
	{
		try {
			// Create MD5 Hash
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i=0; i<messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2) h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();
		} catch(NoSuchAlgorithmException e) {
			//Logger.logStackTrace(TAG,e);
		}
		return "";
	}

    /**
	 * Get the Device ID
	 * @return String Device ID
	 */
	private String getDeviceId()
	{
		String android_id = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
		String deviceId = md5(android_id).toUpperCase();
		return deviceId;
	}

    /**
     * Constructor
     * @param Activity Main activity
     */
    public GodotGooglePlayGameServices(Activity activity) {
        this.activity = activity;
        registerClass("GodotGooglePlayGameServices", new String[] {
            "init", "signIn", "signOut", "unlockAchy", "incrementAchy", "showAchyList", "isLoggedIn"
        });
    }
}
