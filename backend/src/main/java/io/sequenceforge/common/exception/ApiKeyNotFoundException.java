package io.sequenceforge.common.exception;

import java.util.UUID;

public class ApiKeyNotFoundException extends RuntimeException {
    public ApiKeyNotFoundException(UUID keyId) {
        super("API key not found: " + keyId);
    }
}
