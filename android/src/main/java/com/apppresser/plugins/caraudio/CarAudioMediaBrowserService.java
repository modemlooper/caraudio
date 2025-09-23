package com.apppresser.plugins.caraudio;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import java.util.ArrayList;
import java.util.List;

public class CarAudioMediaBrowserService extends MediaBrowserServiceCompat {
    private static final String TAG = "CarAudioMediaBrowser";
    private static final String MEDIA_ROOT_ID = "media_root_id";
    
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private AndroidAutoController androidAutoController;
    
    // Static reference to allow setting the controller from the plugin
    private static AndroidAutoController staticAndroidAutoController;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MediaBrowserService created");
        
        // Use the static controller if available
        this.androidAutoController = staticAndroidAutoController;
        
        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(this, TAG);
        
        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        
        // Create a PlaybackStateCompat.Builder
        stateBuilder = new PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_STOP |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SEEK_TO);
        
        mediaSession.setPlaybackState(stateBuilder.build());
        
        // MySessionCallback has methods that handle callbacks from a media controller
        mediaSession.setCallback(new MySessionCallback());
        
        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSession.getSessionToken());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaSession != null) {
            mediaSession.release();
        }
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.d(TAG, "onGetRoot called with clientPackageName: " + clientPackageName);
        // Return a root ID that clients can use with onLoadChildren() to retrieve
        // the content hierarchy.
        return new BrowserRoot(MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "onLoadChildren called with parentId: " + parentId);
        
        // Return an empty list for now
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        result.sendResult(mediaItems);
    }

    public static void setAndroidAutoController(AndroidAutoController controller) {
        staticAndroidAutoController = controller;
        Log.d(TAG, "AndroidAutoController set in MediaBrowserService (static)");
    }

    private final class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Log.d(TAG, "MediaBrowserService: onPlay called");
            if (androidAutoController != null) {
                // Delegate to AndroidAutoController's handlePlayCommand
                androidAutoController.handlePlayCommand();
            }
            
            // Update playback state
            mediaSession.setPlaybackState(stateBuilder
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                .build());
        }
        
        @Override
        public void onPause() {
            Log.d(TAG, "MediaBrowserService: onPause called");
            if (androidAutoController != null) {
                // Delegate to AndroidAutoController's handlePauseCommand
                androidAutoController.handlePauseCommand();
            }
            
            // Update playback state
            mediaSession.setPlaybackState(stateBuilder
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 0.0f)
                .build());
        }
        
        @Override
        public void onStop() {
            Log.d(TAG, "MediaBrowserService: stop called");
            if (androidAutoController != null) {
                // Delegate to AndroidAutoController's handleStopCommand
                androidAutoController.handleStopCommand();
            }
            
            // Update playback state
            mediaSession.setPlaybackState(stateBuilder
                .setState(PlaybackStateCompat.STATE_STOPPED, 0, 0.0f)
                .build());
        }
        
        @Override
        public void onSkipToNext() {
            Log.d(TAG, "MediaBrowserService: onSkipToNext called");
            if (androidAutoController != null) {
                // Trigger the listener callback
                AndroidAutoController.AndroidAutoControllerListener listener = androidAutoController.getListener();
                if (listener != null) {
                    listener.onSkipToNext();
                }
            }
        }
        
        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "MediaBrowserService: onSkipToPrevious called");
            if (androidAutoController != null) {
                // Trigger the listener callback
                AndroidAutoController.AndroidAutoControllerListener listener = androidAutoController.getListener();
                if (listener != null) {
                    listener.onSkipToPrevious();
                }
            }
        }
        
        @Override
        public void onSeekTo(long position) {
            Log.d(TAG, "MediaBrowserService: onSeekTo called with position: " + position);
            if (androidAutoController != null) {
                // Trigger the listener callback
                AndroidAutoController.AndroidAutoControllerListener listener = androidAutoController.getListener();
                if (listener != null) {
                    listener.onSeekTo(position);
                }
            }
        }
    }
}
