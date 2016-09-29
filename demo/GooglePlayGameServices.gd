extends Node

var gpgs = null

func _ready():
	if Globals.has_singleton("GodotGooglePlayGameServices"):
		gpgs = Globals.get_singleton("GodotGooglePlayGameServices")

# Login/Logout
# ------------------------------------------------------------------------------

func signIn():
	if gpgs != null:
		gpgs.init(get_instance_ID())
		gpgs.signIn()

func signOut():
	if gpgs != null:
		gpgs.signOut()

func _on_google_play_game_services_connected():
    # Thing to do after GPGS init
	pass

func _on_google_play_game_services_disconnected():
	pass

func _on_google_play_game_services_suspended_network_lost():
	pass

func _on_google_play_game_services_suspended_service_disconnected():
    pass

func _on_google_play_game_services_suspended_unknown():
	pass

# Achievements
# ------------------------------------------------------------------------------

func unlockAchy(id):
	if gpgs != null:
		gpgs.unlockAchy(id)

func showAchyList():
	if gpgs != null:
		gpgs.showAchyList()

# Leaderboards
# ------------------------------------------------------------------------------

func leaderSubmit(leaderboard, value):
	if gpgs != null:
		gpgs.leaderSubmit(leaderboard, value)

func showLeaderList(leaderboard):
	if gpgs != null:
		gpgs.showLeaderList(leaderboard)
