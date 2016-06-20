package com.android.godot;

import android.util.Log;
import android.view.View;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.games.Games;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.common.ConnectionResult;
import android.content.IntentSender.SendIntentException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;


public class GodotGoogleGamePlayServices extends Godot.SingletonBase {

    private static final int                RC_SAVED_GAMES = 9002;
    private static final int                RC_SIGN_IN = 9001;
    private static final int                REQUEST_ACHIEVEMENTS = 9002;

    private int                             m_device_id;
    private Activity                        m_activity;
    private GoogleApiClient                 m_GoogleApiClient;
    private Boolean                         m_requestSignIn = false;
    private Boolean                         m_intentInProgress = false;
    private Boolean                         m_googlePlayConndected = false;


    static public Godot.SingletonBase initialize(Activity p_activity) { return new GodotGoogleGamePlayServices(p_activity); }

    public GodotGoogleGamePlayServices(Activity p_activity) {
        m_activity = p_activity;
          registerClass("bbGGPS", new String[]{"init_GGPGS","sign_in","unlock_achy","increment_achy","show_achy_list","is_logged_in","sign_out"});
    }

    protected void onMainDestroy() {

        disconnect();
    }




    public void init_GGPGS(int device_id) {
        m_device_id = device_id;
        m_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_GoogleApiClient = new GoogleApiClient.Builder(m_activity)
                .addConnectionCallbacks(new ConnectionCallbacks(){
                    @Override
                    public void onConnected(Bundle m_bundle) {
                        Log.d("--------- godot ----------", "connectioncallbacks on connected ");
                        m_googlePlayConndected = true;
                        Log.d("--------- godot ----------", "calling godot above ");
                    }
                    @Override
                    public void onConnectionSuspended(int m_cause) {
                        Log.w("--------- godot ----------", "connectioncallbacks onConnectionSuspended int cause "+String.valueOf(m_cause));
                    }
                })
                .addOnConnectionFailedListener(new OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult m_result) {
                        Log.w("--------- godot ----------", "onConnectionFailed result code: "+String.valueOf(m_result));

                        if(!m_intentInProgress && m_result.hasResolution()) {
                            try {
                                m_intentInProgress = true;
                                m_activity.startIntentSenderForResult(m_result.getResolution().getIntentSender(), RC_SIGN_IN, null, 0, 0, 0);
                            } catch (SendIntentException ex) {
                                m_intentInProgress = false;
                                m_GoogleApiClient.connect();
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

    public void sign_in() {
        m_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_GoogleApiClient.connect();
            }
        });
    }

    public void sign_out() {
        disconnect();
    }

    public void disconnect() {
        m_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(m_GoogleApiClient.isConnected()) {
                    Games.signOut(m_GoogleApiClient);
                    m_GoogleApiClient.disconnect();
                    m_googlePlayConndected = false;
                }
                Log.d("--------- godot ----------", "disconnecting from google game play services");
            }
        });
    }

    public void is_logged_in() {
        GodotLib.calldeferred(m_device_id, "is_user_logged_in", new Object[]{(boolean)m_googlePlayConndected});
    }

    public void increment_achy(final String achievement_id, final int increment_amount) {
        if(m_googlePlayConndected) {
            m_activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Games.Achievements.increment(m_GoogleApiClient, achievement_id, increment_amount);
                }
            });
        } else {
            Log.w("--------- godot ----------", "trying to make Google Play Game Services calls before connected, try calling signIn first");
            return;
        }
    }

    public void unlock_achy(final String achievement_id) {
        if(m_googlePlayConndected) {
            m_activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Games.Achievements.unlock(m_GoogleApiClient, achievement_id);
                }
            });
        } else {
            Log.w("--------- godot ----------", "trying to make Google Play Game Services calls before connected, try calling signIn first");
            return;
        }
    }


    public void show_achy_list() {
        if(m_googlePlayConndected) {
            m_activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    m_activity.startActivityForResult(Games.Achievements.getAchievementsIntent(m_GoogleApiClient), REQUEST_ACHIEVEMENTS);
                }
            });
        } else {
            Log.w("--------- godot ----------", "trying to make Google Play Game Services calls before connected, try calling signIn first");
            return;
        }
    }

    protected void onMainActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RC_SIGN_IN) {
            m_intentInProgress = false;

            if(!m_GoogleApiClient.isConnecting()) {
                m_GoogleApiClient.connect();
            }
        }
    }
}
