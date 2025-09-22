import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CarAudioPlugin)
public class CarAudioPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "CarAudioPlugin"
    public let jsName = "CarAudio"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "play", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stop", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "pause", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "resume", returnType: CAPPluginReturnPromise)
    ]

    override public func load() {
        // This will initialize the AudioManager and setup remote transport controls
        _ = AudioManager.shared
    }

    @objc func stop(_ call: CAPPluginCall) {
        AudioManager.shared.stop()
        call.resolve(["status": "stopped"])
    }

    @objc func play(_ call: CAPPluginCall) {
        guard let url = call.getString("url") else {
            call.reject("URL is required")
            return
        }
        
        let title = call.getString("title") ?? "Uknown Title"
        let artist = call.getString("artist") ?? "Umknown Artist"
        let artwork = call.getString("artwork") ?? ""

        let success = AudioManager.shared.play(url: url, title: title, artist: artist, artwork: artwork)
        if success {
            AudioManager.shared.updateNowPlaying(title: title, artist: artist, artworkURL: artwork)
            call.resolve(["status": "playing"])
        } else {
            call.reject("Failed to play audio")
        }
    }
    
    @objc func pause(_ call: CAPPluginCall) {
        AudioManager.shared.pause()
        call.resolve(["status": "paused"])
    }
    
    @objc func resume(_ call: CAPPluginCall) {
        AudioManager.shared.resume()
        call.resolve(["status": "resumed"])
    }
}

