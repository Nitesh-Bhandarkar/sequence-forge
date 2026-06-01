package io.sequenceforge.common.exception;

public class PlaceholderValueMissingException extends RuntimeException {
    public PlaceholderValueMissingException(String placeholderName) {
        super("Required placeholder value missing: {" + placeholderName + "}");
    }
}
