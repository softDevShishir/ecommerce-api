package com.shishir.ecommerce.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when request data fails validation, optionally carrying the
 * individual field-level error messages via {@link #getErrors()}.
 */
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = Collections.emptyList();
    }

    public ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errors = Collections.emptyList();
    }

    public List<String> getErrors() {
        return errors;
    }
}
