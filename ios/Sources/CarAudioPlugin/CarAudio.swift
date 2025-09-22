import Foundation

@objc public class CarAudio: NSObject {
    @objc public func play(url: String, title: String, artist: String, artwork: String ) -> Bool {
        return AudioManager.shared.play(url: url, title: title, artist: artist, artwork: artwork)
    }
    
    @objc public func stop() {
        AudioManager.shared.stop()
    }
    
    @objc public func pause() {
        AudioManager.shared.pause()
    }
    
    @objc public func resume() {
        AudioManager.shared.resume()
    }
}
