package com.apppresser.plugins.caraudio;

import com.getcapacitor.Logger;

public class CarAudio {

    public String echo(String value) {
        Logger.info("Echo", value);
        return value;
    }
}
