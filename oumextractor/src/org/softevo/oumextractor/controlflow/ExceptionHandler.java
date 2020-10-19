package org.softevo.oumextractor.controlflow;

/**
 * This class is used to represent exception handlers.
 *
 * @author Andrzej Wasylkowski
 */
final class ExceptionHandler {

    /**
     * Fully qualified name of the exception class (or null for any class).
     */
    private final String exceptionType;

    /**
     * First node of the handler of this exception.
     */
    private final Node handler;

    /**
     * Creates new exception handler of given type with specified first node of
     * the handler.
     *
     * @param exceptionType Fully qualified name of the exception class (or null
     *                      for any class).
     * @param handler       First node of the handler of the exception.
     */
    ExceptionHandler(String exceptionType, Node handler) {
        this.exceptionType = exceptionType;
        this.handler = handler;
    }

    /**
     * Returns fully qualified name of the exception class (or null for any
     * class).
     *
     * @return Fully qualified name of the exception class or
     * <code>null</code>.
     */
    String getExceptionType() {
        return this.exceptionType;
    }

    /**
     * Returns first node of the handler of this exception.
     *
     * @return First node of the handler of this exception.
     */
    Node getHandler() {
        return handler;
    }
}
