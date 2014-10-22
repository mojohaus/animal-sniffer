package test;

public class SSOException extends Exception {

    /** 
     * 
     */
    public SSOException() {
        super();

    }

    /**
     * @param message
     * @param cause
     */
    protected SSOException(final String message, final Throwable cause) {
        super(message, cause);

    }

    /**
     * @param message
     */
    protected SSOException(final String message) {
        super(message);

    }

    /**
     * @param cause
     */
    protected SSOException(final Throwable cause) {
        super(cause);

    }

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

}
