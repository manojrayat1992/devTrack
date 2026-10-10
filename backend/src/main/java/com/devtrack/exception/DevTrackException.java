package com.devtrack.exception;

public abstract sealed class DevTrackException extends RuntimeException
    permits ResourceNotFoundException,
            TaskOwnershipException,
            InvalidStatusTransitionException {

    public DevTrackException(String message) {
        super(message);
    }

    public DevTrackException(String message, Throwable cause) {
        super(message, cause);
    }
}
