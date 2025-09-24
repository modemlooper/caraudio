package com.apppresser.plugins.caraudio;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import android.util.Log;

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
        
        Log.d("CarAudioPlugin", "CarAudio plugin loaded with Android Auto support");
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
