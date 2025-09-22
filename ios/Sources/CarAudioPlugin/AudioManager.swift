import Foundation
import AVFoundation
import MediaPlayer
import UIKit

@objc public class AudioManager: NSObject {
    
    static let shared = AudioManager()
    private var player: AVPlayer?
    
    // Current metadata for Now Playing
    private var currentTitle: String?
    private var currentArtist: String?
    // Can be an asset name or a URL string
    private var currentArtwork: String?
    
    // Add observers for playback state
    private var timeObserver: Any?
    private var playerItemObserver: NSKeyValueObservation?

    override init() {
        super.init()
        // Configure the audio session once when the manager is initialized
        let audioSession = AVAudioSession.sharedInstance()
        do {
            try audioSession.setCategory(.playback, mode: .default, options: [])
            // NEW: Setup remote controls on initialization
            setupRemoteTransportControls()
            NotificationCenter.default.addObserver(self,
                                                   selector: #selector(handleInterruption(_:)),
                                                   name: AVAudioSession.interruptionNotification,
                                                   object: nil)
            NotificationCenter.default.addObserver(self,
                                                   selector: #selector(handleRouteChange(_:)),
                                                   name: AVAudioSession.routeChangeNotification,
                                                   object: nil)
        } catch {
            print("Failed to set audio session category: \(error)")
        }
    }
    
    @objc public func stop() {
        player?.pause()
        updatePlaybackRate(0.0)
        if let observer = timeObserver {
            player?.removeTimeObserver(observer)
            timeObserver = nil
        }
        playerItemObserver?.invalidate()
        playerItemObserver = nil
        player = nil
        
        // Clear stored metadata
        currentTitle = nil
        currentArtist = nil
        currentArtwork = nil

        // Clear Now Playing
        MPNowPlayingInfoCenter.default().nowPlayingInfo = [:]
        if #available(iOS 13.0, *) {
            MPNowPlayingInfoCenter.default().playbackState = .stopped
        }
        
        UIApplication.shared.endReceivingRemoteControlEvents()
        do {
            try AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
        } catch {
            print("Failed to deactivate audio session: \(error)")
        }
    }

    @objc public func play(url: String, title: String, artist: String, artwork: String) -> Bool {
        // Stop any existing player before creating a new one.
        stop()

        guard let url = URL(string: url) else {
            print("Invalid URL")
            return false
        }
        
        UIApplication.shared.beginReceivingRemoteControlEvents()
        // Activate the audio session
        do {
            try AVAudioSession.sharedInstance().setActive(true, options: [.notifyOthersOnDeactivation])
        } catch {
            print("Failed to activate audio session: \(error)")
            return false
        }
        
        let playerItem = AVPlayerItem(url: url)
        player = AVPlayer(playerItem: playerItem)
        
        // Store metadata for Now Playing
        self.currentTitle = title
        self.currentArtist = artist
        self.currentArtwork = artwork

        // Observe the player item's status to set up Now Playing info when it's ready.
        playerItemObserver = playerItem.observe(\.status, options: [.new, .initial]) { [weak self] item, _ in
            guard let self = self else { return }
            if item.status == .readyToPlay {
                self.setupNowPlaying()
                self.player?.play()
                self.updatePlaybackRate(1.0)
            }
        }
        
        return true
    }
    
    @objc public func pause() {
        player?.pause()
        updatePlaybackRate(0.0)
    }
    
    @objc public func resume() {
        do {
            try AVAudioSession.sharedInstance().setActive(true, options: [.notifyOthersOnDeactivation])
        } catch {
            print("Failed to activate audio session: \(error)")
            return
        }
        player?.play()
        updatePlaybackRate(1.0)
    }

    func setupRemoteTransportControls() {
        let commandCenter = MPRemoteCommandCenter.shared()

        // Play
        commandCenter.playCommand.isEnabled = true
        commandCenter.playCommand.addTarget { [unowned self] _ in
            do {
                try AVAudioSession.sharedInstance().setActive(true, options: [.notifyOthersOnDeactivation])
            } catch {
                print("Failed to activate session on play: \(error)")
                return .commandFailed
            }
            self.player?.play()
            DispatchQueue.main.async {
                self.updatePlaybackRate(1.0)
            }
            return .success
        }

        // Pause
        commandCenter.pauseCommand.isEnabled = true
        commandCenter.pauseCommand.addTarget { [unowned self] _ in
            self.player?.pause()
            DispatchQueue.main.async {
                self.updatePlaybackRate(0.0)
            }
            return .success
        }

        // Toggle Play/Pause
        commandCenter.togglePlayPauseCommand.isEnabled = true
        commandCenter.togglePlayPauseCommand.addTarget { [unowned self] _ in
            if self.player?.rate == 0 {
                do {
                    try AVAudioSession.sharedInstance().setActive(true, options: [.notifyOthersOnDeactivation])
                } catch {
                    print("Failed to activate session on toggle: \(error)")
                    return .commandFailed
                }
                self.player?.play()
                DispatchQueue.main.async { self.updatePlaybackRate(1.0) }
            } else {
                self.player?.pause()
                DispatchQueue.main.async { self.updatePlaybackRate(0.0) }
            }
            return .success
        }

        // Stop
        commandCenter.stopCommand.isEnabled = true
        commandCenter.stopCommand.addTarget { [unowned self] _ in
            self.stop()
            return .success
        }

        // Scrubbing (seek)
        commandCenter.changePlaybackPositionCommand.isEnabled = true
        commandCenter.changePlaybackPositionCommand.addTarget { [unowned self] event in
            guard let event = event as? MPChangePlaybackPositionCommandEvent else { return .commandFailed }
            let time = CMTime(seconds: event.positionTime, preferredTimescale: 600)
            self.player?.seek(to: time) { _ in
                DispatchQueue.main.async {
                    self.updateNowPlayingPlaybackTime()
                }
            }
            return .success
        }
    }

    func setupNowPlaying() {
        var nowPlayingInfo: [String: Any] = [:]

        // Populate from stored metadata
        if let title = currentTitle { nowPlayingInfo[MPMediaItemPropertyTitle] = title }
        if let artist = currentArtist { nowPlayingInfo[MPMediaItemPropertyArtist] = artist }

        // Artwork: try local asset name first, then fall back to URL fetch
        if let artworkRef = currentArtwork, !artworkRef.isEmpty {
            if let image = UIImage(named: artworkRef) {
                nowPlayingInfo[MPMediaItemPropertyArtwork] =
                    MPMediaItemArtwork(boundsSize: image.size) { _ in image }
            } else if let url = URL(string: artworkRef) {
                // We'll set initial info now, and fetch artwork asynchronously below
                URLSession.shared.dataTask(with: url) { data, _, _ in
                    guard let data = data, let image = UIImage(data: data) else { return }
                    DispatchQueue.main.async {
                        var current = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
                        current[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
                        MPNowPlayingInfoCenter.default().nowPlayingInfo = current
                    }
                }.resume()
            }
        }

        guard let player = player, let currentItem = player.currentItem else {
            return
        }

        // If duration is known, set it; otherwise mark as live stream
        let duration = currentItem.duration.seconds
        if duration.isFinite && duration > 0 {
            nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = duration
        } else {
            nowPlayingInfo[MPNowPlayingInfoPropertyIsLiveStream] = true
        }

        // Initial timing + rate
        nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentItem.currentTime().seconds
        nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = player.rate

        MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo

        if #available(iOS 13.0, *) {
            MPNowPlayingInfoCenter.default().playbackState = player.rate == 0 ? .paused : .playing
        }

        // Periodic updates
        if let observer = timeObserver {
            player.removeTimeObserver(observer)
        }
        timeObserver = player.addPeriodicTimeObserver(forInterval: CMTime(seconds: 1, preferredTimescale: 1), queue: .main) { [weak self] _ in
            self?.updateNowPlayingPlaybackTime()
        }
    }
    
    // Function to update the elapsed time
    func updateNowPlayingPlaybackTime() {
        DispatchQueue.main.async {
            if var nowPlayingInfo = MPNowPlayingInfoCenter.default().nowPlayingInfo {
                nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = self.player?.currentTime().seconds
                MPNowPlayingInfoCenter.default().nowPlayingInfo = nowPlayingInfo
            }
        }
    }

    func updatePlaybackRate(_ rate: Float) {
        DispatchQueue.main.async {
            if var info = MPNowPlayingInfoCenter.default().nowPlayingInfo {
                info[MPNowPlayingInfoPropertyPlaybackRate] = rate
                MPNowPlayingInfoCenter.default().nowPlayingInfo = info
            }
            if #available(iOS 13.0, *) {
                MPNowPlayingInfoCenter.default().playbackState = rate == 0 ? .paused : .playing
            }
        }
    }
    
    @objc public func updateNowPlaying(title: String?, artist: String?, artworkURL: String?) {
        DispatchQueue.main.async {
            if let title = title { self.currentTitle = title }
            if let artist = artist { self.currentArtist = artist }
            if let artworkURL = artworkURL { self.currentArtwork = artworkURL }
            
            var info = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
            if let title = title {
                info[MPMediaItemPropertyTitle] = title
            }
            if let artist = artist {
                info[MPMediaItemPropertyArtist] = artist
            }
            // Apply the partial updates immediately
            MPNowPlayingInfoCenter.default().nowPlayingInfo = info

            // If we have an artwork URL, fetch and apply it asynchronously
            if let artworkURL = artworkURL, let url = URL(string: artworkURL) {
                URLSession.shared.dataTask(with: url) { data, _, _ in
                    guard let data = data, let image = UIImage(data: data) else { return }
                    DispatchQueue.main.async {
                        var current = MPNowPlayingInfoCenter.default().nowPlayingInfo ?? [:]
                        current[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(boundsSize: image.size) { _ in image }
                        MPNowPlayingInfoCenter.default().nowPlayingInfo = current
                    }
                }.resume()
            }
        }
    }
    
    @objc private func handleInterruption(_ note: Notification) {
        guard let info = note.userInfo,
              let typeRaw = info[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: typeRaw) else { return }

        switch type {
        case .began:
            player?.pause()
            updatePlaybackRate(0.0)
        case .ended:
            let optionsRaw = info[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
            let options = AVAudioSession.InterruptionOptions(rawValue: optionsRaw)
            if options.contains(.shouldResume) {
                try? AVAudioSession.sharedInstance().setActive(true)
                player?.play()
                updatePlaybackRate(1.0)
            }
        @unknown default:
            break
        }
    }

    @objc private func handleRouteChange(_ note: Notification) {
        // Inspect route change if needed (e.g., pause on oldDeviceUnavailable)
    }
}
