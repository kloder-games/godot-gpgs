# GooglePlayGameServices
This is the Google Play Game Services module for Godot Engine (https://github.com/okamstudio/godot)
- Android only
- Login and Logout
- Achievements
- Leaderboards
- Snapshots
- Real Time Multiplayer

## How to use
Drop the "gpgs" directory inside the "modules" directory on the Godot source.

In android/AndroidManifestChunk.xml modify:
```xml
<meta-data android:name="com.google.android.gms.games.APP_ID"
  android:value="\ 012345678901" />
```
Replace your APP_ID value, it must begin with "\ ".

```xml
  <meta-data android:name="com.google.android.gms.version"
    android:value="@integer/google_play_services_version" />
```
If your other module had this meta-data delete it (It can only be one).

Recompile.

In Example project goto Export->Target->Android:

	Options:
		Custom Package:
			- place your apk from build
		Permissions on:
			- Access Network State
			- Internet


## Configuring your game
To enable the module on Android, add the path to the module to the "modules" property on the [android] section of your engine.cfg file. It should look like this:

	[android]
	modules="org/godotengine/godot/GodotGooglePlayGameServices"

If you have more separete by comma.

## Common errors
If the plugin doesn't works:
* Check this page: https://developers.google.com/games/services/android/troubleshooting
* Check if the Google Play Console > Game Services > Linked Apps has your App linked. If your App has two keystores (one for develop and one for release) check that you have two entries in this screen one for each (debug and release), this gave me a good headache.
* Check if you have the Drive API in the linked Google API Console.
* In other case, please leave an Issue for fix it. Thanks!

## API Reference

The following methods are available:
```python
# Init GooglePlayGameServices
# @param int instance_id The instance id from Godot (get_instance_ID())
init(instance_id)

# Connected callback
_on_google_play_game_services_connected()

# Network Lost callback
_on_google_play_game_services_suspended_network_lost()

# Service disconnected callback
_on_google_play_game_services_suspended_service_disconnected()

# Disconected by unknown causes callback
_on_google_play_game_services_suspended_unknown()

# On disconected method callback
_on_google_play_game_services_disconnected()

# Sign In method
signIn()

# Sign Out method
signOut()

# Get connection status
# @return int Connection status: 1 - Connecting..., 2 - Conected, 0 - Other states
getStatus()

# Achievements
# --------------------

# Unlock Achivement
# @param @param String id Achivement ID to unlock
unlockAchy(id)

# Increment Achivement
# @param String id Achivement ID to increment
# @param int amount The amount for increment
incrementAchy(id, amount)

# Show Achivements List
showAchyList()

# Leaderboards
# --------------------

# Upload score to a leaderboard
# @param String id Id of the leaderboard
# @param int score Score to upload to the leaderboard
leaderSubmit(String id, int score)

# On submitted leaderboard score OK
# @param String id Id of the leaderboard (sometimes you maybe want more than one at the same time)
_on_google_play_game_services_leaderboard_submitted_ok(id)

# On submitted leaderboard score Error
# @param String id Id of the leaderboard (sometimes you maybe want more than one at the same time)
_on_leaderboard_submit_error(id)

# Show leader board
# @param String id Id of the leaderboard
showLeaderList(String id)

# Get the value of one leaderboard (returned by a callback)
# @param String id Id of the leaderboard
getLeaderboardValue(id)

# Callback when the score is ok
# @param int score Score returned
# @param String id Id of the leaderboard (sometimes you maybe want more than one at the same time)
_on_leaderboard_get_value(score, id)

# Callback when the score is on error
# @param String id Id of the leaderboard (sometimes you maybe want more than one at the same time)
_on_leaderboard_get_value_error(id)

# Snapshots
# ---------

# Save snapshots
# @param String snapshotName The name of the snapshots
# @param String data The data to save serialized on string
# @param String description A description for the snapshot
saveSnapshot(snapshotName, data, description)

# Load snapshot
# @param String The name of the snapshots
loadFromSnapshot(snapshotName)

# Callback when the snapshot is loaded
# @param The data from the snapshot
_on_google_play_game_services_snapshot_loaded(data)

# Real Time Multiplayer
# ---------------------

# Show invite players screen
# @param int min The minimum number of players
# @param int max The maximum number of players
invitePlayers(min, max)

# Show Invitation box
showInvitationInbox()

# Send reliable message
# @param msg The message to send
# @param participantId The id of the receipt participant
sendReliableMessage(String msg, String participantId)

# Send broadcast message over reliable format
# @param String msg The message to send over broadcast
sendBroadcastMessage(String msg)

# Leave the room
leaveRoom()

# On start the game (All participants are in)
_on_gpgs_rtm_start_game()

# On received message
# @param String participantId The id of the participant message
# @param String msg The message
_on_gpgs_rtm_message_received(participantId, msg)

# On disconnected from room
_on_gpgs_rtm_disconnected_from_room()
```

## Demo
You can find an example on "demo/GooglePlayGameServices.gd" file.

## References
* https://github.com/Mavhod/GodotGPS
* https://github.com/teamblubee/bbGGPS
* https://developers.google.com/games/services/android/realtimeMultiplayer

## License
MIT license
