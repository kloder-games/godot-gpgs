def can_build(plat):
    return plat == 'android'

def configure(env):
    if env['platform'] == 'android':
        env.android_module_file("GodotGoogleGamePlayServices.java")
        env.android_module_manifest("AndroidManifestChunk.xml")
