import { CarAudio } from 'caraudio';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    CarAudio.echo({ value: inputValue })
}
