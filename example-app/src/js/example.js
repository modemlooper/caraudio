import { CarAudio } from 'caraudio';

window.testPlay = () => {
    const inputValue = document.getElementById("playInput").value;
    CarAudio.play({ url: inputValue, title: "Alert to the magic in the world", artist: "Junius Johnson", artwork: "https://marshillaudio.org/wp-content/uploads/2023/07/Friday-Feature-square.jpg" })
}

window.testPause = () => {
    CarAudio.pause()
}

window.testResume = () => {
    CarAudio.resume()
}