package de.ovgu.skunk.detection.input;

/**
 * Exception to be thrown when {@link FunctionSignatureParseException} encounters strange syntax.
 * <p>
 * Created by wfenske on 07.03.17.
 */
public class FunctionSignatureParseException extends Exception {
    public FunctionSignatureParseException() {
    }

    public FunctionSignatureParseException(String message) {
        super(message);
    }

    public FunctionSignatureParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public FunctionSignatureParseException(Throwable cause) {
        super(cause);
    }

    public FunctionSignatureParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
