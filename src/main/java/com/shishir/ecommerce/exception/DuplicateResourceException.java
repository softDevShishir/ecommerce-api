package com.shishir.ecommerce.exception;

/**
 * Thrown when an operation would create a resource that already exists
 * (e.g. registering with an email that's already taken).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
