package com.apppresser.plugins.caraudio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * AndroidAutoController handles MediaSession integration for Android Auto
 * while delegating actual audio playback to CarAudio
 */
public class AndroidAutoController implements CarAudio.CarAudioStateListener {
    
    public static final String TAG = "AndroidAutoController";
    private Context context;
    private CarAudio carAudio;
    private MediaSessionCompat mediaSession;
    private AndroidAutoControllerListener listener;
    
    // Current track info
    private String currentUrl;
    private String currentTitle;
    private String currentArtist;
    private String currentAlbum;
    private String currentArtworkUrl;
    private long currentDuration;
    
    public interface AndroidAutoControllerListener {
        void onPlay();
        void onPause();
        void onStop();
        void onSkipToNext();
        void onSkipToPrevious();
        void onSeekTo(long position);
        void onTrackSelected(String url, String title, String artist, String album, String artworkUrl, long duration);
    }
    
    public AndroidAutoController(Context context, CarAudio carAudio) {
        this.context = context;
        this.carAudio = carAudio;
        // Set this controller as the state listener for CarAudio
        this.carAudio.setStateListener(this);
        initializeMediaSession();
    }
    
    public void setListener(AndroidAutoControllerListener listener) {
        this.listener = listener;
    }
    
    public AndroidAutoControllerListener getListener() {
        return this.listener;
    }
    
    private void initializeMediaSession() {
        // Create MediaSession
        mediaSession = new MediaSessionCompat(context, "CarAudioMediaSession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                              MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        
        // Set callback for media button events
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                Log.d(TAG, "Android Auto requested PLAY");
                handlePlayCommand();
            }
            
            @Override
            public void onPause() {
                Log.d(TAG, "Android Auto requested PAUSE");
                handlePauseCommand();
            }
            
            @Override
            public void onStop() {
                Log.d(TAG, "Android Auto requested STOP");
                handleStopCommand();
            }
            
            @Override
            public void onSkipToNext() {
                Log.d(TAG, "Android Auto requested SKIP TO NEXT");
                if (listener != null) {
                    listener.onSkipToNext();
                }
            }
            
            @Override
            public void onSkipToPrevious() {
                Log.d(TAG, "Android Auto requested SKIP TO PREVIOUS");
                if (listener != null) {
                    listener.onSkipToPrevious();
                }
            }
            
            @Override
            public void onSeekTo(long position) {
                Log.d(TAG, "Android Auto requested SEEK TO: " + position);
                if (listener != null) {
                    listener.onSeekTo(position);
                }
            }
        });
        
        // Set initial playback state
        updatePlaybackState(PlaybackStateCompat.STATE_NONE, 0, 0.0f);
        
        // Activate the session
        mediaSession.setActive(true);
        
        Log.d(TAG, "MediaSession initialized and activated");
    }
    
    /**
     * Update the now playing metadata for Android Auto
     */
    public void updateNowPlaying(String url, String title, String artist, String album, String artworkUrl, long durationMs) {
        Log.d(TAG, "Updating now playing: " + title + " by " + artist + " (URL: " + url + ")");
        
        this.currentUrl = url;
        this.currentTitle = title;
        this.currentArtist = artist;
        this.currentAlbum = album;
        this.currentArtworkUrl = artworkUrl;
        this.currentDuration = durationMs;
        
        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title != null ? title : "")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist != null ? artist : "")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album != null ? album : "")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs);
        
        mediaSession.setMetadata(metadataBuilder.build());
        
        // Load artwork asynchronously if provided
        if (artworkUrl != null && !artworkUrl.trim().isEmpty()) {
            loadArtworkAsync(artworkUrl, metadataBuilder);
        }
    }
    
    /**
     * Update now playing and notify JavaScript about track selection from Android Auto
     */
    public void updateNowPlayingFromAndroidAuto(String url, String title, String artist, String album, String artworkUrl, long durationMs) {
        Log.d(TAG, "Track selected from Android Auto: " + title + " by " + artist + " (URL: " + url + ")");
        
        // Update the metadata
        updateNowPlaying(url, title, artist, album, artworkUrl, durationMs);
        
        // Notify JavaScript about the track selection
        if (listener != null) {
            listener.onTrackSelected(url, title, artist, album, artworkUrl, durationMs);
        }
    }
    
    /**
     * Update the playback state for Android Auto
     */
    public void updatePlaybackState(int state, long position) {
        updatePlaybackState(state, position, 1.0f);
    }
    
    private void updatePlaybackState(int state, long position, float playbackSpeed) {
        // Define available actions based on state
        long actions = PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                      PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                      PlaybackStateCompat.ACTION_SEEK_TO;
        
        switch (state) {
            case PlaybackStateCompat.STATE_PLAYING:
                actions |= PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_STOP;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
                actions |= PlaybackStateCompat.ACTION_PLAY;
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                actions |= PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_STOP;
                break;
        }
        
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, position, playbackSpeed)
                .build();
                
        mediaSession.setPlaybackState(playbackState);
        
        Log.d(TAG, "Updated playback state: " + getStateString(state) + " at position " + position);
    }
    
    /**
     * Call this when your CarAudio starts playing
     */
    public void notifyPlaying(long position) {
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, position);
    }
    
    /**
     * Call this when your CarAudio is paused
     */
    public void notifyPaused(long position) {
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED, position);
    }
    
    /**
     * Call this when your CarAudio is stopped
     */
    public void notifyStopped() {
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED, 0);
    }
    
    /**
     * Call this when your CarAudio is buffering/preparing
     */
    public void notifyBuffering(long position) {
        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING, position);
    }
    
    /**
     * Call this when there's an error
     */
    public void notifyError(String errorMessage) {
        PlaybackStateCompat errorState = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_ERROR, 0, 0.0f)
                .setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, errorMessage)
                .build();
        mediaSession.setPlaybackState(errorState);
        
        Log.e(TAG, "Updated playback state to ERROR: " + errorMessage);
    }
    
    public void handlePlayCommand() {
        Log.d(TAG, "handlePlayCommand() called");
        Log.d(TAG, "CarAudio state - isPlaying: " + carAudio.isPlaying() + ", isPaused: " + carAudio.isPaused() + ", isStopped: " + carAudio.isStopped());
        Log.d(TAG, "Current URL: " + currentUrl);
        
        if (carAudio.isPaused()) {
            // Resume existing audio - state will be updated via CarAudioStateListener
            Log.d(TAG, "Resuming paused audio");
            carAudio.resume();
            // Notify JavaScript about the resume action
            if (listener != null) {
                listener.onPlay();
            }
        } else if (carAudio.isPlaying()) {
            // Already playing, just update state
            Log.d(TAG, "Already playing, updating state");
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
            // Notify JavaScript about the play action
            if (listener != null) {
                listener.onPlay();
            }
        } else {
            // Not currently playing anything
            if (currentUrl != null && !currentUrl.isEmpty()) {
                // We have a URL (from Android Auto or JavaScript), use it - state will be updated via CarAudioStateListener
                Log.d(TAG, "Starting playback with URL: " + currentUrl);
                carAudio.play(currentUrl, currentTitle, currentArtist, currentArtworkUrl);
                
                // Always notify JavaScript when starting playback, so it can update its UI
                if (listener != null) {
                    Log.d(TAG, "Notifying JavaScript listener about play action");
                    listener.onPlay();
                }
            } else {
                // No URL available, notify listener to request one from JavaScript
                Log.d(TAG, "No URL available, requesting from JavaScript");
                if (listener != null) {
                    listener.onPlay();
                }
                
                // Fallback to sample URL only if JavaScript doesn't respond
                new android.os.Handler().postDelayed(() -> {
                    if (!carAudio.isPlaying() && (currentUrl == null || currentUrl.isEmpty())) {
                        String fallbackUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";
                        Log.d(TAG, "JavaScript didn't provide URL, using fallback: " + fallbackUrl);
                        carAudio.play(fallbackUrl, "Sample Track", "Sample Artist", null);
                        updateNowPlaying(fallbackUrl, "Sample Track", "Sample Artist", "Sample Album", null, 180000);
                    }
                }, 1000); // Give JavaScript 1 second to respond
            }
        }
    }
    
    public void handlePauseCommand() {
        if (carAudio.isPlaying()) {
            // Pause audio - state will be updated via CarAudioStateListener
            carAudio.pause();
        }
        
        if (listener != null) {
            listener.onPause();
        }
    }
    
    public void handleStopCommand() {
        if (carAudio.isPlaying() || carAudio.isPaused()) {
            // Stop audio - state will be updated via CarAudioStateListener
            carAudio.stop();
        }
        
        if (listener != null) {
            listener.onStop();
        }
    }
    
    /**
     * Load artwork asynchronously and update metadata
     */
    private void loadArtworkAsync(String artworkUrl, MediaMetadataCompat.Builder metadataBuilder) {
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... urls) {
                try {
                    URL url = new URL(urls[0]);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(10000);
                    connection.connect();
                    
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    connection.disconnect();
                    
                    return bitmap;
                } catch (Exception e) {
                    Log.w(TAG, "Failed to load artwork: " + e.getMessage());
                    return null;
                }
            }
            
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    MediaMetadataCompat metadata = metadataBuilder
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                            .build();
                    mediaSession.setMetadata(metadata);
                    Log.d(TAG, "Artwork loaded and updated");
                }
            }
        }.execute(artworkUrl);
    }
    
    /**
     * Get the MediaSession token for use by MediaBrowserService
     */
    public MediaSessionCompat.Token getSessionToken() {
        return mediaSession != null ? mediaSession.getSessionToken() : null;
    }
    
    /**
     * Check if Android Auto is connected
     */
    public boolean isAndroidAutoConnected() {
        // You can implement additional logic here to detect Android Auto connection
        // For now, we'll assume if the MediaSession is active, we're ready for Android Auto
        return mediaSession != null && mediaSession.isActive();
    }
    
    /**
     * Release resources
     */
    public void release() {
        Log.d(TAG, "Releasing AndroidAutoController resources");
        
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
    }
    
    // Helper method to convert state int to string for logging
    private String getStateString(int state) {
        switch (state) {
            case PlaybackStateCompat.STATE_NONE:
                return "NONE";
            case PlaybackStateCompat.STATE_STOPPED:
                return "STOPPED";
            case PlaybackStateCompat.STATE_PAUSED:
                return "PAUSED";
            case PlaybackStateCompat.STATE_PLAYING:
                return "PLAYING";
            case PlaybackStateCompat.STATE_BUFFERING:
                return "BUFFERING";
            case PlaybackStateCompat.STATE_ERROR:
                return "ERROR";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }
    
    // CarAudio.CarAudioStateListener implementation
    @Override
    public void onPreparing() {
        Log.d(TAG, "CarAudio is preparing - setting buffering state");
        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING, 0);
    }
    
    @Override
    public void onPlaying() {
        Log.d(TAG, "CarAudio started playing - setting playing state");
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
    }
    
    @Override
    public void onPaused() {
        Log.d(TAG, "CarAudio paused - setting paused state");
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED, 0);
    }
    
    @Override
    public void onStopped() {
        Log.d(TAG, "CarAudio stopped - setting stopped state");
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED, 0);
    }
    
    @Override
    public void onError(String errorMessage) {
        Log.e(TAG, "CarAudio error - setting error state: " + errorMessage);
        notifyError(errorMessage);
    }
    
    @Override
    public void onBuffering() {
        Log.d(TAG, "CarAudio buffering - setting buffering state");
        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING, 0);
    }
}