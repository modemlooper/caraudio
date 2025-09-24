package com.apppresser.plugins.caraudio;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import android.util.Log;
import com.getcapacitor.JSArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "CarAudio")
public class CarAudioPlugin extends Plugin implements AndroidAutoController.AndroidAutoControllerListener {

    private CarAudio implementation;
    private AndroidAutoController androidAutoController;
    private boolean androidAutoEnabled = false;

    @Override
    public void load() {
        super.load();
        // Your existing CarAudio initialization
        implementation = new CarAudio(getContext());
        
        // Add Android Auto controller
        androidAutoController = new AndroidAutoController(getContext(), implementation);
        androidAutoController.setListener(this);
        
        // Register the AndroidAutoController with the MediaBrowserService
        CarAudioMediaBrowserService.setAndroidAutoController(androidAutoController);
        
        // Auto-populate media items for Android Auto
        setupDefaultMediaItems();
        
        Log.d("CarAudioPlugin", "CarAudio plugin loaded with Android Auto support");
    }
    
    private void setupDefaultMediaItems() {
        Log.d("CarAudioPlugin", "Setting up default media items for Android Auto");
        
        // Clear any existing items first
        CarAudioMediaBrowserService.clearMediaItems();
        
        // Add test browsable folders
        CarAudioMediaBrowserService.addBrowsableItem("media_root_id", "test_playlists", "Test Playlists", "Debug playlists");
        CarAudioMediaBrowserService.addBrowsableItem("media_root_id", "test_albums", "Test Albums", "Debug albums");
        
        // Add playable items to playlists
        CarAudioMediaBrowserService.addPlayableItem("test_playlists", "test_track_1", "Debug Song 1", "Debug Artist", "Debug Album", 
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "");
        CarAudioMediaBrowserService.addPlayableItem("test_playlists", "test_track_2", "Debug Song 2", "Debug Artist 2", "Debug Album 2", 
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "");
        
        // Add items to albums
        CarAudioMediaBrowserService.addPlayableItem("test_albums", "album_track_1", "Album Track 1", "Album Artist", "Test Album", 
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "");
        
        Log.d("CarAudioPlugin", "Default media items setup completed");
    }
    
    // Add this new method to enable/disable Android Auto
    @PluginMethod
    public void enableAndroidAuto(PluginCall call) {
        androidAutoEnabled = call.getBoolean("enabled", true);
        
        JSObject result = new JSObject();
        result.put("enabled", androidAutoEnabled);
        result.put("connected", androidAutoController.isAndroidAutoConnected());
        
        call.resolve(result);
    }
    
    // Method to update Android Auto with current track info (called from JavaScript)
    @PluginMethod
    public void updateAndroidAutoNowPlaying(PluginCall call) {
        if (!androidAutoEnabled) {
            call.resolve(new JSObject().put("success", false).put("message", "Android Auto not enabled"));
            return;
        }
        
        String url = call.getString("url");
        String title = call.getString("title", "");
        String artist = call.getString("artist", "");
        String album = call.getString("album", "");
        String artwork = call.getString("artwork", "");
        Long duration = call.getLong("duration", 0L);
        
        androidAutoController.updateNowPlaying(url, title, artist, album, artwork, duration);
        
        JSObject result = new JSObject();
        result.put("success", true);
        call.resolve(result);
    }
    
    // Modify your existing play method to include Android Auto updates
    @PluginMethod
    public void play(PluginCall call) {
        String url = call.getString("url");
        String title = call.getString("title", "");
        String artist = call.getString("artist", "");
        String album = call.getString("album", "");
        String artwork = call.getString("artwork", "");
        Long duration = call.getLong("duration", 0L);
        
        // Enable Android Auto by default
        androidAutoEnabled = true;
        
        // Your existing CarAudio play logic
        implementation.play(url, title, artist, artwork);
        
        // Add Android Auto integration
        if (androidAutoEnabled) {
            androidAutoController.updateNowPlaying(url, title, artist, album, artwork, duration);
            androidAutoController.notifyBuffering(0);
            
            // Notify playing after a short delay (when CarAudio is ready)
            new android.os.Handler().postDelayed(() -> {
                if (implementation.isPlaying()) {
                    androidAutoController.notifyPlaying(0);
                }
            }, 1000);
        }
        
        // Your existing response
        JSObject result = new JSObject();
        result.put("success", true);
        call.resolve(result);
    }
    
    // Modify your existing pause method
    @PluginMethod
    public void pause(PluginCall call) {
        implementation.pause();
        
        if (androidAutoEnabled) {
            androidAutoController.notifyPaused(0);
        }
        
        JSObject result = new JSObject();
        result.put("success", true);
        call.resolve(result);
    }
    
    // Modify your existing resume method
    @PluginMethod
    public void resume(PluginCall call) {
        implementation.resume();
        
        if (androidAutoEnabled) {
            androidAutoController.notifyPlaying(0);
        }
        
        JSObject result = new JSObject();
        result.put("success", true);
        call.resolve(result);
    }
    
    // Modify your existing stop method
    @PluginMethod
    public void stop(PluginCall call) {
        implementation.stop();
        
        if (androidAutoEnabled) {
            androidAutoController.notifyStopped();
        }
        
        JSObject result = new JSObject();
        result.put("success", true);
        call.resolve(result);
    }
    
    // Implement AndroidAutoController.AndroidAutoControllerListener
    @Override
    public void onPlay() {
        // Notify JavaScript that Android Auto requested play
        JSObject data = new JSObject();
        data.put("action", "play");
        notifyListeners("androidAutoCommand", data);
    }
    
    @Override
    public void onPause() {
        JSObject data = new JSObject();
        data.put("action", "pause");
        notifyListeners("androidAutoCommand", data);
    }
    
    @Override
    public void onStop() {
        JSObject data = new JSObject();
        data.put("action", "stop");
        notifyListeners("androidAutoCommand", data);
    }
    
    @Override
    public void onSkipToNext() {
        JSObject data = new JSObject();
        data.put("action", "skipToNext");
        notifyListeners("androidAutoCommand", data);
    }
    
    @Override
    public void onSkipToPrevious() {
        JSObject data = new JSObject();
        data.put("action", "skipToPrevious");
        notifyListeners("androidAutoCommand", data);
    }
    
    @Override
    public void onSeekTo(long position) {
        JSObject data = new JSObject();
        data.put("action", "seekTo");
        data.put("position", position);
        notifyListeners("androidAutoCommand", data);
    }
    
    @Override
    public void onTrackSelected(String url, String title, String artist, String album, String artworkUrl, long duration) {
        JSObject data = new JSObject();
        data.put("action", "trackSelected");
        data.put("url", url);
        data.put("title", title);
        data.put("artist", artist);
        data.put("album", album);
        data.put("artwork", artworkUrl);
        data.put("duration", duration);
        notifyListeners("androidAutoCommand", data);
    }
    
    // Method to clear/reset MediaItems from JavaScript
    @PluginMethod
    public void clearMediaItems(PluginCall call) {
        Log.d("CarAudioPlugin", "clearMediaItems called from JavaScript");
        CarAudioMediaBrowserService.clearMediaItems();
        
        JSObject result = new JSObject();
        result.put("success", true);
        call.resolve(result);
    }
    
    // Method to manually refresh Android Auto UI
    @PluginMethod
    public void refreshAndroidAutoUI(PluginCall call) {
        Log.d("CarAudioPlugin", "refreshAndroidAutoUI called from JavaScript");
        CarAudioMediaBrowserService.refreshAndroidAutoUI();
        
        JSObject result = new JSObject();
        result.put("success", true);
        call.resolve(result);
    }
    
    // Method to add browsable folder from JavaScript
    @PluginMethod
    public void addBrowsableItem(PluginCall call) {
        String mediaId = call.getString("mediaId");
        String title = call.getString("title");
        String subtitle = call.getString("subtitle", "");
        String parentId = call.getString("parentId", "media_root_id");
        
        Log.d("CarAudioPlugin", "addBrowsableItem called from JavaScript - mediaId: " + mediaId + ", title: " + title + ", parentId: " + parentId);
        
        if (mediaId == null || title == null) {
            call.reject("mediaId and title are required");
            return;
        }
        
        CarAudioMediaBrowserService.addBrowsableItem(parentId, mediaId, title, subtitle);
        
        JSObject result = new JSObject();
        result.put("success", true);
        call.resolve(result);
    }
    
    // Method to add playable track from JavaScript
    @PluginMethod
    public void addPlayableItem(PluginCall call) {
        String mediaId = call.getString("mediaId");
        String title = call.getString("title");
        String subtitle = call.getString("subtitle", "");
        String description = call.getString("description", "");
        String parentId = call.getString("parentId", "media_root_id");
        String url = call.getString("url", "");
        String artwork = call.getString("artwork", "");
        
        Log.d("CarAudioPlugin", "addPlayableItem called from JavaScript - mediaId: " + mediaId + ", title: " + title + ", parentId: " + parentId + ", url: " + url);
        
        if (mediaId == null || title == null) {
            call.reject("mediaId and title are required");
            return;
        }
        
        CarAudioMediaBrowserService.addPlayableItem(parentId, mediaId, title, subtitle, description, url, artwork);
        
        JSObject result = new JSObject();
        result.put("success", true);
        call.resolve(result);
    }
    
    // Method to add multiple items at once from JavaScript
    @PluginMethod
    public void setMediaItems(PluginCall call) {
        try {
            JSArray items = call.getArray("items");
            if (items == null) {
                call.reject("items array is required");
                return;
            }
            
            // Clear existing items first
            CarAudioMediaBrowserService.clearMediaItems();
            
            // Add each item
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String type = item.getString("type"); // "browsable" or "playable"
                String mediaId = item.getString("mediaId");
                String title = item.getString("title");
                String subtitle = item.optString("subtitle", "");
                String description = item.optString("description", "");
                String parentId = item.optString("parentId", "media_root_id");
                
                if ("browsable".equals(type)) {
                    CarAudioMediaBrowserService.addBrowsableItem(parentId, mediaId, title, subtitle);
                } else if ("playable".equals(type)) {
                    String url = item.optString("url", "");
                    String artwork = item.optString("artwork", "");
                    CarAudioMediaBrowserService.addPlayableItem(parentId, mediaId, title, subtitle, description, url, artwork);
                }
            }
            
            // Refresh Android Auto UI to show the new items immediately
            CarAudioMediaBrowserService.refreshAndroidAutoUI();
            
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("itemsAdded", items.length());
            call.resolve(result);
            
        } catch (JSONException e) {
            call.reject("Error parsing items array: " + e.getMessage());
        }
    }
    
    @Override
    protected void handleOnDestroy() {
        if (implementation != null) {
            implementation.release();
        }
        
        if (androidAutoController != null) {
            androidAutoController.release();
        }
        
        super.handleOnDestroy();
    }
}
