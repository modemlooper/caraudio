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
        
        if (androidAutoController != null) {
            // Use the AndroidAutoController's MediaSession instead of creating our own
            Log.d(TAG, "Using AndroidAutoController's MediaSession");
            MediaSessionCompat.Token token = androidAutoController.getSessionToken();
            if (token != null) {
                setSessionToken(token);
                Log.d(TAG, "Set session token from AndroidAutoController");
            } else {
                Log.e(TAG, "AndroidAutoController session token is null");
            }
        } else {
            Log.w(TAG, "AndroidAutoController not available, creating fallback MediaSession");
            // Create a fallback MediaSessionCompat
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
        
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        
        if (MEDIA_ROOT_ID.equals(parentId)) {
            // Add some sample media items for Android Auto to display
            // These will show up as browseable content in Android Auto
            
            // Create a "Now Playing" item
            MediaBrowserCompat.MediaItem nowPlayingItem = new MediaBrowserCompat.MediaItem(
                new android.support.v4.media.MediaDescriptionCompat.Builder()
                    .setMediaId("now_playing")
                    .setTitle("Now Playing")
                    .setSubtitle("Current track")
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            );
            mediaItems.add(nowPlayingItem);
            
            // Create a "Recent Tracks" browseable folder
            MediaBrowserCompat.MediaItem recentTracksItem = new MediaBrowserCompat.MediaItem(
                new android.support.v4.media.MediaDescriptionCompat.Builder()
                    .setMediaId("recent_tracks")
                    .setTitle("Recent Tracks")
                    .setSubtitle("Your recently played music")
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            );
            mediaItems.add(recentTracksItem);
            
            // Create a "Favorites" browseable folder
            MediaBrowserCompat.MediaItem favoritesItem = new MediaBrowserCompat.MediaItem(
                new android.support.v4.media.MediaDescriptionCompat.Builder()
                    .setMediaId("favorites")
                    .setTitle("Favorites")
                    .setSubtitle("Your favorite music")
                    .build(),
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            );
            mediaItems.add(favoritesItem);
            
        } else if ("recent_tracks".equals(parentId)) {
            // Add some sample recent tracks
            for (int i = 1; i <= 3; i++) {
                MediaBrowserCompat.MediaItem trackItem = new MediaBrowserCompat.MediaItem(
                    new android.support.v4.media.MediaDescriptionCompat.Builder()
                        .setMediaId("recent_track_" + i)
                        .setTitle("Sample Track " + i)
                        .setSubtitle("Sample Artist " + i)
                        .setDescription("Sample Album " + i)
                        .build(),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                );
                mediaItems.add(trackItem);
            }
            
        } else if ("favorites".equals(parentId)) {
            // Add some sample favorite tracks
            for (int i = 1; i <= 2; i++) {
                MediaBrowserCompat.MediaItem trackItem = new MediaBrowserCompat.MediaItem(
                    new android.support.v4.media.MediaDescriptionCompat.Builder()
                        .setMediaId("favorite_track_" + i)
                        .setTitle("Favorite Song " + i)
                        .setSubtitle("Favorite Artist " + i)
                        .setDescription("Favorite Album " + i)
                        .build(),
                    MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                );
                mediaItems.add(trackItem);
            }
        }
        
        Log.d(TAG, "Returning " + mediaItems.size() + " media items for parentId: " + parentId);
        result.sendResult(mediaItems);
    }

    public static void setAndroidAutoController(AndroidAutoController controller) {
        staticAndroidAutoController = controller;
        Log.d(TAG, "AndroidAutoController set in MediaBrowserService (static)");
    }
    
    public void updateAndroidAutoController(AndroidAutoController controller) {
        this.androidAutoController = controller;
        if (controller != null) {
            MediaSessionCompat.Token token = controller.getSessionToken();
            if (token != null) {
                setSessionToken(token);
                Log.d(TAG, "Updated session token from AndroidAutoController");
            }
        }
    }

    private final class MySessionCallback extends MediaSessionCompat.Callback {
        // Note: Most callbacks are handled by AndroidAutoController's MediaSession
        // This callback class is only used when AndroidAutoController is not available
        
        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "MediaBrowserService: onPlayFromMediaId called with mediaId: " + mediaId);
            
            // Handle different media IDs with valid URLs
            String audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"; // Default sample
            String title = "Unknown Track";
            String artist = "Unknown Artist";
            String album = "Unknown Album";
            
            if (mediaId.equals("now_playing")) {
                title = "Now Playing";
                artist = "Current Artist";
                album = "Current Album";
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";
            } else if (mediaId.startsWith("recent_track_")) {
                int trackNum = Integer.parseInt(mediaId.replace("recent_track_", ""));
                title = "Sample Track " + trackNum;
                artist = "Sample Artist " + trackNum;
                album = "Sample Album " + trackNum;
                // Use valid SoundHelix URLs (they have songs 1-16 available)
                int songNum = ((trackNum - 1) % 16) + 1; // Cycle through 1-16
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-" + songNum + ".mp3";
            } else if (mediaId.startsWith("favorite_track_")) {
                int trackNum = Integer.parseInt(mediaId.replace("favorite_track_", ""));
                title = "Favorite Song " + trackNum;
                artist = "Favorite Artist " + trackNum;
                album = "Favorite Album " + trackNum;
                // Use different valid SoundHelix URLs for favorites
                int songNum = ((trackNum + 7) % 16) + 1; // Offset by 7, cycle through 1-16
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-" + songNum + ".mp3";
            }
            
            Log.d(TAG, "Selected track: " + title + " with URL: " + audioUrl);
            
            // Update the AndroidAutoController with the new track info
            if (androidAutoController != null) {
                Log.d(TAG, "Updating AndroidAutoController with: " + title + " (" + audioUrl + ")");
                Log.d(TAG, "AndroidAutoController isAndroidAutoConnected: " + androidAutoController.isAndroidAutoConnected());
                
                // Use the new method that notifies JavaScript about track selection
                androidAutoController.updateNowPlayingFromAndroidAuto(audioUrl, title, artist, album, null, 180000); // 3 minutes
                
                // Directly trigger playback through AndroidAutoController
                // State management is now handled by CarAudioStateListener
                Log.d(TAG, "Calling androidAutoController.handlePlayCommand()");
                androidAutoController.handlePlayCommand();
                Log.d(TAG, "androidAutoController.handlePlayCommand() completed");
            } else {
                Log.e(TAG, "AndroidAutoController is null, cannot play media");
                // Set error state only if we have our own mediaSession
                if (mediaSession != null && stateBuilder != null) {
                    mediaSession.setPlaybackState(stateBuilder
                        .setState(PlaybackStateCompat.STATE_ERROR, 0, 0.0f)
                        .build());
                }
            }
        }
    }
    
    // Method to get sample URL for testing
    private String getSampleAudioUrl() {
        return "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";
    }
}
