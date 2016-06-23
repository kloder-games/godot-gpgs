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
```
<meta-data android:name="com.google.android.gms.games.APP_ID"
  android:value="\ 012345678901" />
```
Replace your APP_ID value, it must begin with "\ ".

```
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
```
void init() // Already signIn

void signIn()
void signOut()

int getStatus()
	// Return:
	// 1 - Connecting...
	// 2 - Conected
	// 0 - Other states

void unlockAchy(String id)
	// id: Id of the achievement to unlock
void incrementAchy(String id, int amount)
	// id: Id of the achievement to unlock
	// amount: Amount to increment
void showAchyList

void leaderSubmit(String id, int score)
	// id: Leaderboard ID
	// score: The score to upload
void showLeaderList(String id)
	// id: Leaderboard ID
```

References
-------------
Based on the work of:
* https://github.com/Mavhod/GodotGPS
* https://github.com/teamblubee/bbGGPS


License
-------------
MIT license
