package com.shishir.ecommerce.exception;

/**
 * Thrown when a caller is not authenticated or lacks permission for the
 * requested operation.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
