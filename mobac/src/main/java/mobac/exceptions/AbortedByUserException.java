package mobac.exceptions;

import java.io.PrintStream;
import java.io.PrintWriter;

public class AbortedByUserException extends RuntimeException {

    public AbortedByUserException() {
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[0];
    }

    @Override
    public void printStackTrace(PrintStream s) {
    }

    @Override
    public void printStackTrace(PrintWriter s) {
    }
}
