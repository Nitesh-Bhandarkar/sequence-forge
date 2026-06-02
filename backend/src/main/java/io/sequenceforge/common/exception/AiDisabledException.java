package io.sequenceforge.common.exception;

public class AiDisabledException extends RuntimeException {
    public AiDisabledException() {
        super("AI assistant is disabled on this instance");
    }
}
