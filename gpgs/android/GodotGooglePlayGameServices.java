package org.godotengine.godot;

import android.util.Log;
import android.app.Activity;
import android.content.Intent;
import com.google.android.gms.common.api.GoogleApiClient;

import org.godotengine.godot.gpgs.Client;
import org.godotengine.godot.gpgs.Network;
import org.godotengine.godot.gpgs.Achievements;
import org.godotengine.godot.gpgs.Leaderboards;
import org.godotengine.godot.gpgs.Snapshots;
import org.godotengine.godot.gpgs.RealTimeMultiplayer;

public class GodotGooglePlayGameServices extends Godot.SingletonBase {

    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private Activity activity = null;
    private int instance_id = 0;
    private GoogleApiClient googleApiClient = null;

    private Client client;
    private Network network;
    private Leaderboards leaderboards;
    private Snapshots snapshots;
    private Achievements achievements;
    private RealTimeMultiplayer realTimeMultiplayer;

    private static final String TAG = "godot";

    /**
     * Singleton
     * @param Activity Main activity
     */
     static public Godot.SingletonBase initialize(Activity activity) {
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
            "isOnline", "isWifiConnected", "isMobileConnected",
            "saveSnapshot", "loadFromSnapshot",
            "unlockAchy", "incrementAchy", "showAchyList",
            "leaderSubmit", "showLeaderList", "getLeaderboardValue",
            "invitePlayers", "showInvitationInbox", "sendReliableMessage", "sendBroadcastMessage", "leaveRoom"
        });
    }

    /* Connection Methods
     * ********************************************************************** */

    /**
     * Initialization Method
     * @param int instance_id The instance of the application (In Godot: get_instance())
     */
    public void init(final int instance_id) {
        this.instance_id = instance_id;
        client = new Client(activity, googleApiClient, instance_id, this);
        network = new Network(activity);
    }

    public void setClient(GoogleApiClient googleApiClient) {
        this.googleApiClient = googleApiClient;
        leaderboards = new Leaderboards(activity, googleApiClient, instance_id);
        snapshots = new Snapshots(activity, googleApiClient, instance_id);
        achievements = new Achievements(activity, googleApiClient, instance_id);
        realTimeMultiplayer = new RealTimeMultiplayer(activity, googleApiClient, instance_id);
    }

    @Override
    protected void onMainActivityResult(int request, int response, Intent intent) {
        switch(request) {
            case Client.RC_SIGN_IN:
            case Client.REQUEST_RESOLVE_ERROR:
                if (client != null)
                    client.onMainActivityResult(request, response, intent);
                break;
            case RealTimeMultiplayer.RC_SELECT_PLAYERS:
            case RealTimeMultiplayer.RC_INVITATION_INBOX:
            case RealTimeMultiplayer.RC_WAITING_ROOM:
                if (realTimeMultiplayer != null)
                    realTimeMultiplayer.onActivityResult(request, response, intent);
                break;
        }
    }

    /**
     * Sign In method
     */
    public void signIn() {
        if (client == null) return;
        client.signIn();
    }

    /**
     * Sign Out method
     */
    public void signOut() {
        if (client == null) return;
        client.signOut();
    }

    /**
     * Get the client status
     * @return int Return 1 for Conecting..., 2 for Connected, 0 in any other case
     */
    public int getStatus() {
        if (client == null) return 0;
        return client.getStatus();
    }

    /* Network Methods
     * ********************************************************************** */
    
    /**
     * Check network connection
     * @return boolean
     */
    public boolean isOnline() {
        if (network == null) return false;
        return network.isOnline();
    }

    /**
     * Check network connection
     * @return boolean
     */
    public boolean isWifiConnected() {
        if (network == null) return false;
        return network.isWifiConnected();
    }

    /**
     * Check network connection
     * @return boolean
     */
    public boolean isMobileConnected() {
        if (network == null) return false;
        return network.isMobileConnected();
    }

    /* Snapshots Methods
     * ********************************************************************** */

    /**
     * Save snapshot
     * @param String snapshotName The name of the snapshot
     * @param String data The data to save (JSON string or something)
     * @param String description The description for the snapshot
     */
    public void saveSnapshot(final String snapshotName, final String data, final String description) {
        if (snapshots == null) return;
        snapshots.saveSnapshot(snapshotName, data, description);
    }

    /**
     * Load Snapshot
     * @param String snapshotName The name of the snapshot
     */
    public void loadFromSnapshot(final String snapshotName) {
        if (snapshots == null) return;
        snapshots.loadFromSnapshot(snapshotName);
    }

    /* Achievements Methods
     * ********************************************************************** */

    /**
     * Increment Achivement
     * @param String id Achivement to increment
     * @param int amount The amount for increment
     */
    public void incrementAchy(final String id, final int increment) {
        if (achievements == null) return;
        achievements.incrementAchy(id, increment);
    }

    /**
     * Unlock Achivement
     * @param String id Achivement to unlock
     */
    public void unlockAchy(final String id) {
        if (achievements == null) return;
        achievements.unlockAchy(id);
    }

    /**
     * Show Achivements List
     */
    public void showAchyList() {
        if (achievements == null) return;
        achievements.showAchyList();
    }

    /* Leaderboards Methods
     * ********************************************************************** */

    /**
     * Upload score to a leaderboard
     * @param String id Id of the leaderboard
     * @param int score Score to upload to the leaderboard
     */
    public void leaderSubmit(final String id, final int score) {
        if (leaderboards == null) return;
        leaderboards.leaderSubmit(id, score);
    }

    /**
     * Show leader board
     * @param String id Id of the leaderboard
     */
    public void showLeaderList(final String id) {
        if (leaderboards == null) return;
        leaderboards.showLeaderList(id);
    }

    /**
     * Get a leaderboard value (in a callback)
     * @param String id Id of the leaderboard
     */
    public void getLeaderboardValue(final String id) {
        if (leaderboards == null) return;
        leaderboards.getLeaderboardValue(id);
    }

    /* Real Time Multiplayer Methods
     * ********************************************************************** */
    public void invitePlayers(final int min, final int max) {
        if (realTimeMultiplayer == null) return;
        realTimeMultiplayer.invitePlayers(min, max);
    }

    public void showInvitationInbox() {
        if (realTimeMultiplayer == null) return;
        realTimeMultiplayer.showInvitationInbox();
    }

    public void sendReliableMessage(String msg, String participantId) {
        if (realTimeMultiplayer == null) return;
        realTimeMultiplayer.sendReliableMessage(msg, participantId);
    }

    public void sendBroadcastMessage(String msg) {
        if (realTimeMultiplayer == null) return;
        realTimeMultiplayer.sendBroadcastMessage(msg);
    }

    public void leaveRoom() {
        if (realTimeMultiplayer == null) return;
        realTimeMultiplayer.leaveRoom();
    }

}
