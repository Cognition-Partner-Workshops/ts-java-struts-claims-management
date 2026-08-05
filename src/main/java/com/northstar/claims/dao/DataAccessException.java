package com.northstar.claims.dao;

/**
 * Checked exception used when a DAO cannot complete a database operation.
 */
public class DataAccessException extends Exception {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(Throwable cause) {
        super(cause);
    }

    public String describe() {
        return getMessage() == null ? "Database access failure" : getMessage();
    }

    public boolean hasCause() {
        return getCause() != null;
    }

    public String rootMessage() {
        Throwable cause = getCause();
        if (cause == null) {
            return describe();
        }
        return cause.getMessage() == null ? describe() : cause.getMessage();
    }

    public boolean isRecoverable() {
        return hasCause();
    }

    public String category() {
        return "DAO";
    }
}
