package com.devtrack.exception;

public abstract class DevTrackException extends RuntimeException {

    public DevTrackException(String message) {
        super(message);
    }

    public DevTrackException(String message, Throwable cause) {
        super(message, cause);
    }
}
