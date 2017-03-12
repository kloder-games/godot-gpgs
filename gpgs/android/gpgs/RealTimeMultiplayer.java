package org.godotengine.godot.gpgs;

import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import org.godotengine.godot.GodotLib;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;

import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Invitations;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;

public class RealTimeMultiplayer implements RoomUpdateListener, RealTimeMessageReceivedListener, RoomStatusUpdateListener, OnInvitationReceivedListener {

    public final static int RC_SELECT_PLAYERS = 10000;
    public final static int RC_INVITATION_INBOX = 10001;
    public final static int RC_WAITING_ROOM = 10002;

    private Activity activity = null;
    private int instanceId = 0;
    private GoogleApiClient googleApiClient = null;

    private static final String TAG = "godot";

    // Room ID where the currently active game is taking place; null if we're not playing.
    String roomId = null;

    // The participants in the currently active game
    ArrayList<Participant> participants = null;

    // My participant ID in the currently active game
    String myId = null;

    // Id for invitation
    String incomingInvitationId = null;

    /**
     * Contructor
     * @param activity The main activity
     * @param googleApiClient The Google API Client Object
     * @param instanceId The instance of the game for callbacks
     */
    public RealTimeMultiplayer(Activity activity, GoogleApiClient googleApiClient, int instanceId) {
        this.googleApiClient = googleApiClient;
        this.activity = activity;
        this.instanceId = instanceId;
    }

    /**
     * Invite players with Intent
     * @param min Minimum players to play
     * @param max Maximum players to play
     */
    public void invitePlayers(final int min, final int max) {
        if (googleApiClient == null || !googleApiClient.isConnected()) return;
        activity.runOnUiThread(new Runnable() {
 			@Override
            public void run() {
                Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(googleApiClient, min, max);
                activity.startActivityForResult(intent, RC_SELECT_PLAYERS);
                Log.d(TAG, "GPGS: invitePlayers (" + min + ", " + max + ")");
            }
 		});
    }

    /**
     * Intent Activity result parser
     * @param request The request of the Intent
     * @param response The response of the Intent
     * @param intent The Intent
     */
    public void onActivityResult(int request, int response, Intent intent) {
        switch(request) {
            case RealTimeMultiplayer.RC_SELECT_PLAYERS:
                // Return if the user cancel
                if (response != Activity.RESULT_OK) return;

                // get the invitee list
                Bundle extras = intent.getExtras();
                final ArrayList<String> invitees = intent.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

                // Get auto-match criteria
                Bundle autoMatchCriteria = null;
                int minAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
                int maxAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

                if (minAutoMatchPlayers > 0) autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
                else autoMatchCriteria = null;

                // Create the room and specify a variant if appropriate
                RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
                roomConfigBuilder.addPlayersToInvite(invitees);
                if (autoMatchCriteria != null) roomConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
                RoomConfig roomConfig = roomConfigBuilder.build();
                Games.RealTimeMultiplayer.create(googleApiClient, roomConfig);

                // Prevent screen from sleeping during handshake
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;

            case RealTimeMultiplayer.RC_WAITING_ROOM:
                // we got the result from the "waiting room" UI.
                if (response == Activity.RESULT_OK) {
                    // ready to start playing
                    Log.d(TAG, "GPGS: Starting game (waiting room returned OK).");
                    GodotLib.calldeferred(instanceId, "_on_gpgs_rtm_start_game", new Object[] { });
                } else if (response == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player indicated that they want to leave the room
                    leaveRoom();
                } else if (response == Activity.RESULT_CANCELED) {
                    // Dialog was cancelled (user pressed back key, for instance). In our game,
                    // this means leaving the room too. In more elaborate games, this could mean
                    // something else (like minimizing the waiting room UI).
                    leaveRoom();
                }
                break;

            case RealTimeMultiplayer.RC_INVITATION_INBOX:
                // we got the result from the "select invitation" UI (invitation inbox). We're
                // ready to accept the selected invitation:
                handleInvitationInboxResult(response, intent);
                break;
        }
    }

    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {
        if (response != Activity.RESULT_OK) {
            Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
            GodotLib.calldeferred(instanceId, "_on_gpgs_rtm_invitation_cancelled", new Object[] { });
            return;
        }

        Log.d(TAG, "GPGS: Invitation inbox UI succeeded.");
        Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

        // accept invitation
        acceptInviteToRoom(inv.getInvitationId());
    }

    // Accept the given invitation.
    private void acceptInviteToRoom(String invId) {
        // accept the invitation
        Log.d(TAG, "GPGS: Accepting invitation: " + invId);
        RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
        roomConfigBuilder.setInvitationIdToAccept(invId)
                .setMessageReceivedListener(this)
                .setRoomStatusUpdateListener(this);
        GodotLib.calldeferred(instanceId, "_on_gpgs_rtm_invitation_accepted", new Object[] { });
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Games.RealTimeMultiplayer.join(googleApiClient, roomConfigBuilder.build());
    }

    // create a RoomConfigBuilder that's appropriate for your implementation
    private RoomConfig.Builder makeBasicRoomConfigBuilder() {
        return RoomConfig.builder(this)
        .setMessageReceivedListener(this)
        .setRoomStatusUpdateListener(this);
    }

    // Called when we get an invitation to play a game. We react by showing that to the user.
    @Override
    public void onInvitationReceived(Invitation invitation) {
        // We got an invitation to play a game! So, store it in
        // mIncomingInvitationId
        // and show the popup on the screen.
        incomingInvitationId = invitation.getInvitationId();
        Log.e(TAG, "GPGS: Invitation received " + incomingInvitationId);
        GodotLib.calldeferred(instanceId, "_on_gpgs_rtm_invitation_received", new Object[] { });
    }

    @Override
    public void onInvitationRemoved(String invitationId) {
        if (incomingInvitationId.equals(invitationId) && incomingInvitationId != null) {
            Log.e(TAG, "GPGS: Invitation removed " + incomingInvitationId);
            incomingInvitationId = null;
            GodotLib.calldeferred(instanceId, "_on_gpgs_rtm_invitation_removed", new Object[] { });
        }
      
    }

    public void showInvitationInbox() {
        Intent intent = Games.Invitations.getInvitationInboxIntent(googleApiClient);
        activity.startActivityForResult(intent, RC_INVITATION_INBOX);
    }

    @Override
    public void onRoomConnected(int statusCode, Room room) {
        Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }
        updateRoom(room);
    }

    // Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
            showGameError();
            return;
        }

        // save room ID so we can leave cleanly before the game starts.
        roomId = room.getRoomId();

        // show the waiting room UI
        showWaitingRoom(room);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
            showGameError();
            return;
        }

        // show the waiting room UI
        showWaitingRoom(room);
    }

    // Called when we've successfully left the room (this happens a result of voluntarily leaving
    // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        // we have left the room; return to main screen.
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        GodotLib.calldeferred(instanceId, "_on_gpgs_rtm_left_room", new Object[] { });
    }

    // Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
        roomId = null;
        GodotLib.calldeferred(instanceId, "_on_gpgs_rtm_disconnected_from_room", new Object[] { });
    }

    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedToRoom.");

        //get participants and my ID:
        participants = room.getParticipants();
        myId = room.getParticipantId(Games.Players.getCurrentPlayerId(googleApiClient));

         // save room ID if its not initialized in onRoomCreated() so we can leave cleanly before the game starts.
         if (roomId == null) roomId = room.getRoomId();

        // print out the list of participants (for debug purposes)
        Log.d(TAG, "Room ID: " + roomId);
        Log.d(TAG, "My ID " + myId);
        Log.d(TAG, "<< CONNECTED TO ROOM>>");
    }

    // Show error message about game being cancelled and return to main screen.
    void showGameError() {
        GodotLib.calldeferred(instanceId, "_on_gpgs_rtm_show_game_error", new Object[] { });
    }

    // Called when we receive a real-time message from the network.
    // Messages in our game are made up of 2 bytes: the first one is 'F' or 'U'
    // indicating
    // whether it's a final or interim score. The second byte is the score.
    // There is also the
    // 'S' message, which indicates that the game should start.
    @Override
    public void onRealTimeMessageReceived(RealTimeMessage rtm) {
        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        Log.d(TAG, "Message received: " + (char) buf[0] + "/" + (int) buf[1]);
        String msg = new String(buf);
        GodotLib.calldeferred(instanceId, "_on_gpgs_rtm_message_received", new Object[] { sender, msg });
    }

    void updateRoom(Room room) {
        if (room != null) participants = room.getParticipants();
        if (participants != null) {
            // Nothing
        }
    }

    // Show the waiting room UI to track the progress of other players as they enter the
    // room and get connected.
    void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Integer.MAX_VALUE;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(googleApiClient, room, MIN_PLAYERS);

        // show waiting room UI
        activity.startActivityForResult(i, RC_WAITING_ROOM);
    }

    // Leave the room.
    public void leaveRoom() {
        Log.d(TAG, "GPGS: Leaving room.");
        if (roomId != null) {
            Games.RealTimeMultiplayer.leave(googleApiClient, this, roomId);
            //activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            roomId = null;
        }
        GodotLib.calldeferred(instanceId, "_on_gpgs_rtm_leaved_room", new Object[] { });
        Log.d(TAG, "GPGS: Leaving room 5.");
    }

    // We treat most of the room update callbacks in the same way: we update our list of
    // participants and update the display. In a real game we would also have to check if that
    // change requires some action like removing the corresponding player avatar from the screen,
    // etc.
    @Override
    public void onPeerDeclined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onP2PDisconnected(String participant) {
    }

    @Override
    public void onP2PConnected(String participant) {
    }

    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        updateRoom(room);
    }

    @Override
    public void onRoomConnecting(Room room) {
        updateRoom(room);
    }

    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        updateRoom(room);
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        updateRoom(room);
    }



    public void sendReliableMessage(String msg, String participantId) {
        byte[] message = msg.getBytes();
        Games.RealTimeMultiplayer.sendReliableMessage(googleApiClient, null, message, roomId, participantId);
    }

    public void sendBroadcastMessage(String msg) {
        for (Participant participant: participants) {
            String participantId = participant.getParticipantId();
            if (!participantId.equals(myId)) {
                sendReliableMessage(msg, participantId);
            }
        }
    }

}
