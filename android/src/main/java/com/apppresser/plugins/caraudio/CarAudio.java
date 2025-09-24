package com.apppresser.plugins.caraudio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.IOException;

public class CarAudio implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    public static final String TAG = "CarAudio";
    private Context context;
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private static final int PREPARE_TIMEOUT = 30000; // 30 seconds timeout
    
    // Callback interface for state changes
    public interface CarAudioStateListener {
        void onPreparing();
        void onPlaying();
        void onPaused();
        void onStopped();
        void onError(String errorMessage);
        void onBuffering();
    }
    
    private CarAudioStateListener stateListener;
    
    // Playback state
    private enum PlaybackState {
        IDLE, PREPARING, PREPARED, PLAYING, PAUSED, STOPPED, ERROR
    }
    
    private PlaybackState currentState = PlaybackState.IDLE;
    private String currentUrl;
    private String currentTitle;
    private String currentArtist;
    private String currentArtwork;

    public CarAudio(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.timeoutHandler = new Handler(Looper.getMainLooper());
        initializeMediaPlayer();
    }
    
    public void setStateListener(CarAudioStateListener listener) {
        this.stateListener = listener;
    }

    private void initializeMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        
        // Set audio attributes for media playback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build();
            mediaPlayer.setAudioAttributes(audioAttributes);
            Log.d(TAG, "Set AudioAttributes for API " + Build.VERSION.SDK_INT);
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            Log.d(TAG, "Set AudioStreamType STREAM_MUSIC for API " + Build.VERSION.SDK_INT);
        }
        
        // Set volume to maximum
        mediaPlayer.setVolume(1.0f, 1.0f);
        Log.d(TAG, "MediaPlayer volume set to maximum");
        
        // Log current audio settings
        logAudioSettings();
    }

    public void play(String url, String title, String artist, String artwork) {
        Log.d(TAG, "play called with url: " + url);
        
        if (url == null || url.trim().isEmpty()) {
            Log.e(TAG, "Invalid URL provided");
            return;
        }

        // Store current track info
        currentUrl = url;
        currentTitle = title;
        currentArtist = artist;
        currentArtwork = artwork;

        // Ensure system volume is audible
        ensureAudibleVolume();

        // Request audio focus
        if (!requestAudioFocus()) {
            Log.e(TAG, "Failed to gain audio focus");
            return;
        }

        try {
            // Reset and prepare MediaPlayer
            currentState = PlaybackState.PREPARING;
            mediaPlayer.reset();
            mediaPlayer.setDataSource(url);
            
            // Notify listener that we're preparing
            if (stateListener != null) {
                stateListener.onPreparing();
            }
            
            // Set up timeout for preparation
            setupPrepareTimeout();
            
            // Prepare asynchronously
            mediaPlayer.prepareAsync();
            
            Log.d(TAG, "Started preparing audio from: " + url);
            
        } catch (IOException e) {
            Log.e(TAG, "Error setting data source: " + e.getMessage());
            currentState = PlaybackState.ERROR;
            if (stateListener != null) {
                stateListener.onError("Error setting data source: " + e.getMessage());
            }
            abandonAudioFocus();
        }
    }

    public void pause() {
        Log.d(TAG, "pause called");
        
        if (currentState == PlaybackState.PLAYING && mediaPlayer != null) {
            mediaPlayer.pause();
            currentState = PlaybackState.PAUSED;
            if (stateListener != null) {
                stateListener.onPaused();
            }
            Log.d(TAG, "Audio paused");
        } else {
            Log.w(TAG, "Cannot pause - not currently playing");
        }
    }

    public void resume() {
        Log.d(TAG, "resume called");
        
        if (currentState == PlaybackState.PAUSED && mediaPlayer != null) {
            if (requestAudioFocus()) {
                mediaPlayer.start();
                currentState = PlaybackState.PLAYING;
                if (stateListener != null) {
                    stateListener.onPlaying();
                }
                Log.d(TAG, "Audio resumed");
            } else {
                Log.e(TAG, "Failed to gain audio focus for resume");
            }
        } else {
            Log.w(TAG, "Cannot resume - not currently paused");
        }
    }

    public void stop() {
        Log.d(TAG, "stop called");
        
        cancelPrepareTimeout();
        
        if (mediaPlayer != null && (currentState == PlaybackState.PLAYING || currentState == PlaybackState.PAUSED || currentState == PlaybackState.PREPARING)) {
            mediaPlayer.stop();
            currentState = PlaybackState.STOPPED;
            if (stateListener != null) {
                stateListener.onStopped();
            }
            Log.d(TAG, "Audio stopped");
        }
        
        abandonAudioFocus();
    }

    public void release() {
        Log.d(TAG, "Releasing CarAudio resources");
        
        cancelPrepareTimeout();
        abandonAudioFocus();
        
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        
        currentState = PlaybackState.IDLE;
    }

    // MediaPlayer.OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "MediaPlayer prepared successfully");
        cancelPrepareTimeout();
        
        currentState = PlaybackState.PREPARED;
        
        // Log audio details
        Log.d(TAG, "Audio duration: " + mp.getDuration() + "ms");
        Log.d(TAG, "MediaPlayer isPlaying before start: " + mp.isPlaying());
        
        // Ensure volume is set
        mp.setVolume(1.0f, 1.0f);
        
        // Log current audio settings before starting
        logAudioSettings();
        
        // Start playback
        mp.start();
        currentState = PlaybackState.PLAYING;
        
        // Notify listener that playback started
        if (stateListener != null) {
            stateListener.onPlaying();
        }
        
        Log.d(TAG, "Audio playback started");
        Log.d(TAG, "MediaPlayer isPlaying after start: " + mp.isPlaying());
        Log.d(TAG, "MediaPlayer current position: " + mp.getCurrentPosition());
    }

    // MediaPlayer.OnErrorListener
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error - what: " + what + ", extra: " + extra);
        cancelPrepareTimeout();
        
        currentState = PlaybackState.ERROR;
        if (stateListener != null) {
            stateListener.onError("MediaPlayer error - what: " + what + ", extra: " + extra);
        }
        abandonAudioFocus();
        
        // Return true to indicate we handled the error
        return true;
    }

    // MediaPlayer.OnCompletionListener
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "Audio playback completed");
        currentState = PlaybackState.STOPPED;
        if (stateListener != null) {
            stateListener.onStopped();
        }
        abandonAudioFocus();
    }

    // Audio Focus Management
    private boolean requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
                    
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build();
            }
            
            int result = audioManager.requestAudioFocus(audioFocusRequest);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
    }

    // AudioManager.OnAudioFocusChangeListener
    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "Audio focus changed: " + focusChange);
        
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback
                stop();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time: pause playback
                if (currentState == PlaybackState.PLAYING) {
                    pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus but can duck (lower volume)
                // For simplicity, we'll pause instead of ducking
                if (currentState == PlaybackState.PLAYING) {
                    pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                // Regained focus: resume playback if paused
                if (currentState == PlaybackState.PAUSED) {
                    resume();
                }
                break;
        }
    }

    // Timeout Management
    private void setupPrepareTimeout() {
        cancelPrepareTimeout();
        
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "MediaPlayer preparation timed out after " + PREPARE_TIMEOUT + "ms");
                if (currentState == PlaybackState.PREPARING) {
                    currentState = PlaybackState.ERROR;
                    if (stateListener != null) {
                        stateListener.onError("MediaPlayer preparation timed out after " + PREPARE_TIMEOUT + "ms");
                    }
                    if (mediaPlayer != null) {
                        mediaPlayer.reset();
                    }
                    abandonAudioFocus();
                }
            }
        };
        
        timeoutHandler.postDelayed(timeoutRunnable, PREPARE_TIMEOUT);
    }

    private void cancelPrepareTimeout() {
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    // Getters for current state
    public boolean isPlaying() {
        return currentState == PlaybackState.PLAYING;
    }

    public boolean isPaused() {
        return currentState == PlaybackState.PAUSED;
    }

    public boolean isStopped() {
        return currentState == PlaybackState.STOPPED || currentState == PlaybackState.IDLE;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    // Audio debugging methods
    private void logAudioSettings() {
        try {
            // Log system audio settings
            int musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            Log.d(TAG, "System music volume: " + musicVolume + "/" + maxMusicVolume);
            
            // Check if audio is muted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean isMusicMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
                Log.d(TAG, "Music stream muted: " + isMusicMuted);
            }
            
            // Check audio mode
            int audioMode = audioManager.getMode();
            String modeString = "UNKNOWN";
            switch (audioMode) {
                case AudioManager.MODE_NORMAL:
                    modeString = "NORMAL";
                    break;
                case AudioManager.MODE_RINGTONE:
                    modeString = "RINGTONE";
                    break;
                case AudioManager.MODE_IN_CALL:
                    modeString = "IN_CALL";
                    break;
                case AudioManager.MODE_IN_COMMUNICATION:
                    modeString = "IN_COMMUNICATION";
                    break;
            }
            Log.d(TAG, "Audio mode: " + modeString + " (" + audioMode + ")");
            
            // Check if speaker is on
            boolean isSpeakerOn = audioManager.isSpeakerphoneOn();
            Log.d(TAG, "Speakerphone on: " + isSpeakerOn);
            
            // Check if wired headset is connected
            boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
            Log.d(TAG, "Wired headset connected: " + isWiredHeadsetOn);
            
            // Check if Bluetooth A2DP is connected
            boolean isBluetoothA2dpOn = audioManager.isBluetoothA2dpOn();
            Log.d(TAG, "Bluetooth A2DP connected: " + isBluetoothA2dpOn);
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging audio settings: " + e.getMessage());
        }
    }

    // Method to set system volume to ensure audio is audible
    public void ensureAudibleVolume() {
        try {
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            
            // If volume is too low, set it to 70% of max
            if (currentVolume < maxVolume * 0.3) {
                int targetVolume = (int) (maxVolume * 0.7);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0);
                Log.d(TAG, "Increased system volume from " + currentVolume + " to " + targetVolume);
            }
            
            // Unmute if muted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
                    Log.d(TAG, "Unmuted music stream");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error ensuring audible volume: " + e.getMessage());
        }
    }
}
