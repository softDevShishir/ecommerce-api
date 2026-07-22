package com.shishir.ecommerce.exception;

/**
 * Thrown when the caller-supplied request data is malformed or otherwise invalid.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
