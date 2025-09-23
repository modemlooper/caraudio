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

// Auto-update status every 2 seconds
setInterval(async () => {
    try {
        const status = await CarAudio.getStatus();
        updateStatus(status);
    } catch (error) {
        // Silently fail for auto-updates
    }
}, 2000);