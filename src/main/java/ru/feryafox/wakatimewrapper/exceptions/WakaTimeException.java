package ru.feryafox.wakatimewrapper.exceptions;

import java.io.IOException;

public class WakaTimeException extends IOException {
    public WakaTimeException(String message) {
        super(message);
    }

    public WakaTimeException(String message, Throwable cause) {
        super(message, cause);
    }
}