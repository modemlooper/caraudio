import { CarAudio } from 'caraudio';

// Update status display
const updateStatus = (status) => {
    const statusElement = document.getElementById("status");
    if (statusElement) {
        statusElement.textContent = `Status: ${status.status}`;
        if (status.url) {
            statusElement.textContent += ` | URL: ${status.url}`;
        }
        if (status.title) {
            statusElement.textContent += ` | Title: ${status.title}`;
        }
        if (status.artist) {
            statusElement.textContent += ` | Artist: ${status.artist}`;
        }
    }
};

// Show error messages
const showError = (error) => {
    const errorElement = document.getElementById("error");
    if (errorElement) {
        errorElement.textContent = `Error: ${error}`;
        errorElement.style.display = 'block';
        setTimeout(() => {
            errorElement.style.display = 'none';
        }, 5000);
    }
    console.error('CarAudio Error:', error);
};

window.testPlay = async () => {
    try {
        const inputValue = document.getElementById("playInput").value;
        if (!inputValue) {
            showError("Please enter a valid URL");
            return;
        }

        const status = await CarAudio.play({
            url: inputValue,
            title: "Alert to the magic in the world",
            artist: "Junius Johnson",
            artwork: "https://marshillaudio.org/wp-content/uploads/2023/07/Friday-Feature-square.jpg"
        });

        updateStatus(status);
        console.log('Play started:', status);
    } catch (error) {
        showError(error);
    }
};

window.testPause = async () => {
    try {
        const status = await CarAudio.pause();
        updateStatus(status);
        console.log('Paused:', status);
    } catch (error) {
        showError(error);
    }
};

window.testResume = async () => {
    try {
        const status = await CarAudio.resume();
        updateStatus(status);
        console.log('Resumed:', status);
    } catch (error) {
        showError(error);
    }
};

window.testStop = async () => {
    try {
        const status = await CarAudio.stop();
        updateStatus(status);
        console.log('Stopped:', status);
    } catch (error) {
        showError(error);
    }
};

window.getStatus = async () => {
    try {
        const status = await CarAudio.getStatus();
        updateStatus(status);
        console.log('Current status:', status);
    } catch (error) {
        showError(error);
    }
};

window.ensureVolume = async () => {
    try {
        const result = await CarAudio.ensureAudibleVolume();
        console.log('Volume ensured:', result);

        // Show success message
        const statusElement = document.getElementById("status");
        if (statusElement) {
            const originalText = statusElement.textContent;
            statusElement.textContent = "✅ Volume checked and adjusted if needed";
            statusElement.style.backgroundColor = "#e8f5e8";
            setTimeout(() => {
                statusElement.style.backgroundColor = "#e8f5e8";
                getStatus(); // Refresh status
            }, 2000);
        }
    } catch (error) {
        showError(error);
    }
};

// Android Auto debug functions
window.testAndroidAutoSetup = async () => {
    try {
        console.log('Testing Android Auto setup...');
        
        // Check Android Auto status
        const autoStatus = await CarAudio.enableAndroidAuto({ enabled: true });
        console.log('Android Auto status:', autoStatus);
        
        // Update status display
        const statusElement = document.getElementById("status");
        if (statusElement) {
            statusElement.textContent = `Android Auto - Enabled: ${autoStatus.enabled}, Connected: ${autoStatus.connected}`;
            statusElement.style.backgroundColor = autoStatus.enabled ? "#e8f5e8" : "#ffebee";
        }
        
    } catch (error) {
        showError('Android Auto setup failed: ' + error);
    }
};

window.addTestMediaItems = async () => {
    try {
        console.log('Refreshing test media items...');
        
        // Clear existing items first
        await CarAudio.clearMediaItems();
        
        // Add test browsable folders
        await CarAudio.addBrowsableItem({
            parentId: "media_root_id",
            mediaId: "test_playlists",
            title: "Test Playlists",
            subtitle: "Debug playlists"
        });
        
        await CarAudio.addBrowsableItem({
            parentId: "media_root_id", 
            mediaId: "test_albums",
            title: "Test Albums",
            subtitle: "Debug albums"
        });
        
        // Add playable items to playlists
        await CarAudio.addPlayableItem({
            parentId: "test_playlists",
            mediaId: "test_track_1",
            title: "Debug Song 1",
            subtitle: "Debug Artist",
            description: "Debug Album",
            url: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            artwork: ""
        });
        
        await CarAudio.addPlayableItem({
            parentId: "test_playlists",
            mediaId: "test_track_2", 
            title: "Debug Song 2",
            subtitle: "Debug Artist 2",
            description: "Debug Album 2",
            url: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            artwork: ""
        });
        
        // Add items to albums
        await CarAudio.addPlayableItem({
            parentId: "test_albums",
            mediaId: "album_track_1",
            title: "Album Track 1", 
            subtitle: "Album Artist",
            description: "Test Album",
            url: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            artwork: ""
        });

        await CarAudio.addPlayableItem({
            parentId: "test_albums",
            mediaId: "album_track_2",
            title: "Ryan Track 1", 
            subtitle: "Album Artist",
            description: "Test Album",
            url: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            artwork: ""
        });
        
        console.log('Test media items refreshed successfully!');
        
        const statusElement = document.getElementById("status");
        if (statusElement) {
            statusElement.textContent = "✅ Test media items refreshed! Check Android Auto.";
            statusElement.style.backgroundColor = "#e8f5e8";
        }
        
    } catch (error) {
        showError('Failed to refresh test media items: ' + error);
    }
};

window.clearAllMediaItems = async () => {
    try {
        console.log('Clearing all media items...');
        
        const result = await CarAudio.clearMediaItems();
        console.log('Clear result:', result);
        
        const statusElement = document.getElementById("status");
        if (statusElement) {
            statusElement.textContent = "🗑️ All media items cleared.";
            statusElement.style.backgroundColor = "#fff3e0";
        }
        
    } catch (error) {
        showError('Failed to clear media items: ' + error);
    }
};

// Auto-update status every 2 seconds
setInterval(async () => {
    try {
        const status = await CarAudio.getStatus();
        updateStatus(status);
    } catch (error) {
        // Silently fail for auto-updates
    }
}, 2000);