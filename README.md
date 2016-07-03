GooglePlayGameServices
=====
This is the Google Play Game Services module for Godot Engine (https://github.com/okamstudio/godot)
- Android only
- Log in and log out
- Achievements
- Leaderboards

How to use
----------
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

Configuring your game
---------------------

To enable the module on Android, add the path to the module to the "modules" property on the [android] section of your engine.cfg file. It should look like this:

	[android]
	modules="org/godotengine/godot/GodotGooglePlayGameServices"

If you have more separete by comma.

API Reference
-------------

The following methods are available:
```python
# Init GooglePlayGameServices
# @param int instance_id The instance id from Godot (get_instance_ID())
init(instance_id)

# Connected callback
func _on_google_play_game_services_connected()

# Network Lost callback
func _on_google_play_game_services_suspended_network_lost()

# Service disconnected callback
func _on_google_play_game_services_suspended_service_disconnected()

# Disconected by unknown causes callback
func _on_google_play_game_services_suspended_unknown()

# On disconected method callback
func _on_google_play_game_services_disconnected()

# Sign In method
signIn()

# Sign Out method
signOut()

# Get connection status
# @return int Connection status: 1 - Connecting..., 2 - Conected, 0 - Other states
getStatus()

# Achievements Methods
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

# Leaderboards Methods
# --------------------

# Upload score to a leaderboard
# @param String id Id of the leaderboard
# @param int score Score to upload to the leaderboard
leaderSubmit(String id, int score)

# On submitted leaderboard score OK
# @param String id Id of the leaderboard (sometimes you maybe want more than one at the same time)
func _on_google_play_game_services_leaderboard_submitted_ok(id)

# On submitted leaderboard score Error
# @param String id Id of the leaderboard (sometimes you maybe want more than one at the same time)
func _on_leaderboard_submit_error(id)

# Show leader board
# @param String id Id of the leaderboard
showLeaderList(String id)

# Get the value of one leaderboard (returned by a callback)
# @param String id Id of the leaderboard
getLeaderboardValue(id)

# Callback when the score is ok
# @param int score Score returned
# @param String id Id of the leaderboard (sometimes you maybe want more than one at the same time)
func _on_leaderboard_get_value(score, id)

# Callback when the score is on error
# @param String id Id of the leaderboard (sometimes you maybe want more than one at the same time)
func _on_leaderboard_get_value_error(id)
```

References
-------------
Based on the work of:
* https://github.com/Mavhod/GodotGPS
* https://github.com/teamblubee/bbGGPS


License
-------------
MIT license
