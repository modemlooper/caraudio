import Foundation
import Capacitor

@objc(AudioNowPlayingPlugin)
public class AudioNowPlayingPlugin: CAPPlugin {
    @objc func updateNowPlaying(_ call: CAPPluginCall) {
        let title = call.getString("title")
        let artist = call.getString("artist")
        let artworkUrl = call.getString("artworkUrl")

        AudioManager.shared.updateNowPlaying(title: title, artist: artist, artworkURL: artworkUrl)
        call.resolve()
    }
}

@objc(AudioNowPlayingPluginJS)
public class AudioNowPlayingPluginJS: NSObject, CAPBridgedPlugin {
    public let jsName: String = "AudioNowPlaying"
    public let pluginId: String = "AudioNowPlayingPlugin"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "updateNowPlaying", returnType: CAPPluginReturnPromise)
    ]
}
