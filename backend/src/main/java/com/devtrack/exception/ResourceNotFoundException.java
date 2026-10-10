package com.devtrack.exception;

public final class ResourceNotFoundException extends DevTrackException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s with identifier '%s' was not found", resourceName, identifier));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
