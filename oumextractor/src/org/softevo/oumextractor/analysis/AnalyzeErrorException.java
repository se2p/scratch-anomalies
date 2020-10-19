package org.softevo.oumextractor.analysis;

/**
 * This exception is to be thrown whenever some method can't be analyzed for
 * a reason that does not make other methods unanalyzable.
 *
 * @author Andrzej Wasylkowski
 */
@SuppressWarnings("serial")
public class AnalyzeErrorException extends Exception {

    /**
     * Creates new exception with given message.
     *
     * @param message Message to be bound with the exception created.
     */
    public AnalyzeErrorException(String message) {
        super(message);
    }

    /**
     * Creates new exception as stemming from given exception.
     *
     * @param e Exception causing this exception to be thrown.
     */
    public AnalyzeErrorException(Throwable e) {
        super(e);
    }
}
