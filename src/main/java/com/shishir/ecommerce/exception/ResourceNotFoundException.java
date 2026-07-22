package com.shishir.ecommerce.exception;

/**
 * Thrown when a requested resource (user, product, order, etc.) does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
