package com.licel.stringer.signing;

public class ZipAbortException extends Exception {
    private static final long serialVersionUID = 1L;

    public ZipAbortException() {
    }

    public ZipAbortException(String format, Object... args) {
        super(String.format(format, args));
    }

    public ZipAbortException(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }

    public ZipAbortException(Throwable cause) {
        super(cause);
    }
}
