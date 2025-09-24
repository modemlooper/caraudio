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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarAudioMediaBrowserService extends MediaBrowserServiceCompat {
    private static final String TAG = "CarAudioMediaBrowser";
    private static final String MEDIA_ROOT_ID = "media_root_id";
    
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private AndroidAutoController androidAutoController;
    
    // Static reference to allow setting the controller from the plugin
    private static AndroidAutoController staticAndroidAutoController;
    
    // Static data structures for dynamic MediaItems
    private static final Map<String, List<MediaItemData>> mediaItemsMap = new HashMap<>();
    private static final Map<String, String> mediaItemUrls = new HashMap<>();
    
    // Data class to hold MediaItem information
    public static class MediaItemData {
        public String mediaId;
        public String title;
        public String subtitle;
        public String description;
        public boolean isBrowsable;
        public String url;
        public String artwork;
        
        public MediaItemData(String mediaId, String title, String subtitle, String description, boolean isBrowsable, String url, String artwork) {
            this.mediaId = mediaId;
            this.title = title;
            this.subtitle = subtitle;
            this.description = description;
            this.isBrowsable = isBrowsable;
            this.url = url;
            this.artwork = artwork;
        }
    }
    
    // Static methods for managing MediaItems from JavaScript
    public static void clearMediaItems() {
        synchronized (mediaItemsMap) {
            int previousSize = mediaItemsMap.size();
            mediaItemsMap.clear();
            mediaItemUrls.clear();
            Log.d(TAG, "Cleared all media items (was " + previousSize + " parents)");
            Log.d(TAG, "MediaItems map is now empty: " + mediaItemsMap.isEmpty());
        }
    }
    
    public static void addBrowsableItem(String parentId, String mediaId, String title, String subtitle) {
        Log.d(TAG, "addBrowsableItem called - parentId: " + parentId + ", mediaId: " + mediaId + ", title: " + title);
        synchronized (mediaItemsMap) {
            List<MediaItemData> items = mediaItemsMap.get(parentId);
            if (items == null) {
                items = new ArrayList<>();
                mediaItemsMap.put(parentId, items);
                Log.d(TAG, "Created new items list for parent: " + parentId);
            }
            items.add(new MediaItemData(mediaId, title, subtitle, "", true, "", ""));
            Log.d(TAG, "Added browsable item: " + title + " to parent: " + parentId + " (total items: " + items.size() + ")");
            Log.d(TAG, "Total parents in map: " + mediaItemsMap.size());
            
            // Log all current parents
            Log.d(TAG, "Current parents in map:");
            for (String key : mediaItemsMap.keySet()) {
                Log.d(TAG, "  - " + key + " (" + mediaItemsMap.get(key).size() + " items)");
            }
        }
    }
    
    public static void addPlayableItem(String parentId, String mediaId, String title, String subtitle, String description, String url, String artwork) {
        Log.d(TAG, "addPlayableItem called - parentId: " + parentId + ", mediaId: " + mediaId + ", title: " + title + ", url: " + url);
        synchronized (mediaItemsMap) {
            List<MediaItemData> items = mediaItemsMap.get(parentId);
            if (items == null) {
                items = new ArrayList<>();
                mediaItemsMap.put(parentId, items);
                Log.d(TAG, "Created new items list for parent: " + parentId);
            }
            items.add(new MediaItemData(mediaId, title, subtitle, description, false, url, artwork));
            
            // Store URL mapping for playback
            if (url != null && !url.isEmpty()) {
                mediaItemUrls.put(mediaId, url);
                Log.d(TAG, "Stored URL mapping: " + mediaId + " -> " + url);
            }
            
            Log.d(TAG, "Added playable item: " + title + " to parent: " + parentId + " (total items: " + items.size() + ")");
            Log.d(TAG, "Total parents in map: " + mediaItemsMap.size());
            
            // Log all current parents
            Log.d(TAG, "Current parents in map:");
            for (String key : mediaItemsMap.keySet()) {
                Log.d(TAG, "  - " + key + " (" + mediaItemsMap.get(key).size() + " items)");
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MediaBrowserService created");
        
        // Debug: Log current media items
        synchronized (mediaItemsMap) {
            Log.d(TAG, "Current media items map size: " + mediaItemsMap.size());
            for (String parentId : mediaItemsMap.keySet()) {
                List<MediaItemData> items = mediaItemsMap.get(parentId);
                Log.d(TAG, "Parent ID: " + parentId + " has " + (items != null ? items.size() : 0) + " items");
                if (items != null) {
                    for (MediaItemData item : items) {
                        Log.d(TAG, "  - " + item.title + " (browsable: " + item.isBrowsable + ")");
                    }
                }
            }
        }
        
        // Use the static controller if available
        this.androidAutoController = staticAndroidAutoController;
        
        if (androidAutoController != null) {
            // Use the AndroidAutoController's MediaSession instead of creating our own
            Log.d(TAG, "Using AndroidAutoController's MediaSession");
            MediaSessionCompat.Token token = androidAutoController.getSessionToken();
            if (token != null) {
                setSessionToken(token);
                Log.d(TAG, "Set session token from AndroidAutoController - SUCCESS");
                Log.d(TAG, "AndroidAutoController is connected: " + androidAutoController.isAndroidAutoConnected());
            } else {
                Log.e(TAG, "AndroidAutoController session token is null - this will cause issues!");
            }
        } else {
            Log.w(TAG, "AndroidAutoController not available, creating fallback MediaSession");
            Log.w(TAG, "This means track selection from Android Auto may not work properly!");
            
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
            Log.d(TAG, "Fallback MediaSession created and token set");
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
        
        synchronized (mediaItemsMap) {
            // Log current state of media items map
            Log.d(TAG, "Current mediaItemsMap size: " + mediaItemsMap.size());
            for (String key : mediaItemsMap.keySet()) {
                List<MediaItemData> items = mediaItemsMap.get(key);
                Log.d(TAG, "  Key: " + key + " has " + (items != null ? items.size() : 0) + " items");
            }
            
            List<MediaItemData> items = mediaItemsMap.get(parentId);
            
            if (items != null && !items.isEmpty()) {
                Log.d(TAG, "Found " + items.size() + " dynamic items for parentId: " + parentId);
                // Use dynamic items from JavaScript
                for (MediaItemData itemData : items) {
                    Log.d(TAG, "Adding item: " + itemData.title + " (browsable: " + itemData.isBrowsable + ")");
                    
                    android.support.v4.media.MediaDescriptionCompat.Builder descBuilder = 
                        new android.support.v4.media.MediaDescriptionCompat.Builder()
                            .setMediaId(itemData.mediaId)
                            .setTitle(itemData.title)
                            .setSubtitle(itemData.subtitle);
                    
                    if (itemData.description != null && !itemData.description.isEmpty()) {
                        descBuilder.setDescription(itemData.description);
                    }
                    
                    MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                        descBuilder.build(),
                        itemData.isBrowsable ? MediaBrowserCompat.MediaItem.FLAG_BROWSABLE : MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                    );
                    
                    mediaItems.add(mediaItem);
                }
            } else {
                Log.d(TAG, "No items found for parentId: " + parentId);
            }
        }
        
        Log.d(TAG, "Returning " + mediaItems.size() + " media items for parentId: " + parentId);
        for (MediaBrowserCompat.MediaItem item : mediaItems) {
            Log.d(TAG, "  - " + item.getDescription().getTitle() + " (ID: " + item.getDescription().getMediaId() + ")");
        }
        
        result.sendResult(mediaItems);
    }

    public static void setAndroidAutoController(AndroidAutoController controller) {
        staticAndroidAutoController = controller;
        Log.d(TAG, "AndroidAutoController set in MediaBrowserService (static)");
    }
    
    // Static method to handle track selection from AndroidAutoController
    public static void handleTrackSelectionFromController(String mediaId) {
        Log.d(TAG, "handleTrackSelectionFromController called with mediaId: " + mediaId);
        
        // Get URL from dynamic mapping first
        String audioUrl = mediaItemUrls.get(mediaId);
        String title = "Unknown Track";
        String artist = "Unknown Artist";
        String album = "Unknown Album";
        
        // Find the item data for metadata
        synchronized (mediaItemsMap) {
            for (List<MediaItemData> items : mediaItemsMap.values()) {
                for (MediaItemData item : items) {
                    if (mediaId.equals(item.mediaId)) {
                        title = item.title;
                        artist = item.subtitle; // Using subtitle as artist
                        album = item.description; // Using description as album
                        if (audioUrl == null || audioUrl.isEmpty()) {
                            audioUrl = item.url;
                        }
                        break;
                    }
                }
            }
        }
        
        // Check if we found the track data
        if (audioUrl == null || audioUrl.isEmpty()) {
            Log.e(TAG, "No URL found for mediaId: " + mediaId + " - track not found in media items");
            return; // Don't try to play if we don't have the proper data
        } else {
            Log.d(TAG, "Found URL for mediaId " + mediaId + ": " + audioUrl);
        }
        
        Log.d(TAG, "Track info - Title: " + title + ", Artist: " + artist + ", Album: " + album + ", URL: " + audioUrl);
        
        // Update the AndroidAutoController with the new track info and start playback
        if (staticAndroidAutoController != null) {
            Log.d(TAG, "Updating AndroidAutoController with track info and starting playback");
            
            // Use the method that notifies JavaScript about track selection
            staticAndroidAutoController.updateNowPlayingFromAndroidAuto(audioUrl, title, artist, album, null, 180000);
            
            // Start playback
            staticAndroidAutoController.handlePlayCommand();
            
            Log.d(TAG, "Track selection and playback initiated");
        } else {
            Log.e(TAG, "staticAndroidAutoController is null, cannot handle track selection");
        }
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
            
            // Get URL from dynamic mapping first
            String audioUrl = mediaItemUrls.get(mediaId);
            String title = "Unknown Track";
            String artist = "Unknown Artist";
            String album = "Unknown Album";
            
            // Find the item data for metadata
            synchronized (mediaItemsMap) {
                for (List<MediaItemData> items : mediaItemsMap.values()) {
                    for (MediaItemData item : items) {
                        if (mediaId.equals(item.mediaId)) {
                            title = item.title;
                            artist = item.subtitle; // Using subtitle as artist
                            album = item.description; // Using description as album
                            if (audioUrl == null || audioUrl.isEmpty()) {
                                audioUrl = item.url;
                            }
                            break;
                        }
                    }
                }
            }
            
            // Check if we found the track data
            if (audioUrl == null || audioUrl.isEmpty()) {
                Log.e(TAG, "No URL found for mediaId: " + mediaId + " - cannot play track");
                // Set error state if we have our own mediaSession
                if (mediaSession != null && stateBuilder != null) {
                    mediaSession.setPlaybackState(stateBuilder
                        .setState(PlaybackStateCompat.STATE_ERROR, 0, 0.0f)
                        .build());
                }
                return;
            } else {
                Log.d(TAG, "Found URL for mediaId " + mediaId + ": " + audioUrl);
            }
            
            Log.d(TAG, "Selected track: " + title + " with URL: " + audioUrl);
            Log.d(TAG, "Track metadata - Title: " + title + ", Artist: " + artist + ", Album: " + album);
            
            // Update the AndroidAutoController with the new track info
            if (androidAutoController != null) {
                Log.d(TAG, "Updating AndroidAutoController with: " + title + " (" + audioUrl + ")");
                
                // Use the new method that notifies JavaScript about track selection
                androidAutoController.updateNowPlayingFromAndroidAuto(audioUrl, title, artist, album, null, 180000);
                
                // Directly trigger playback through AndroidAutoController
                Log.d(TAG, "Calling androidAutoController.handlePlayCommand()");
                androidAutoController.handlePlayCommand();
                Log.d(TAG, "androidAutoController.handlePlayCommand() completed");
                
                // Log the current state after attempting to play
                Log.d(TAG, "Post-play state check - AndroidAutoController ready: " + androidAutoController.isAndroidAutoConnected());
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
