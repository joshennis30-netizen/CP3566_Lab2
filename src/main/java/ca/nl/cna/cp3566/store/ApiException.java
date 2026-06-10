package ca.nl.cna.cp3566.store;

import java.util.Map;

/**
 * INFRASTRUCTURE (given — do not change).
 *
 * One exception type for every expected error. Your code throws one of these and
 * a single {@link ApiExceptionMapper} turns it into the uniform JSON body
 * {@code {"error": "...", "message": "..."}} with the right HTTP status. That is
 * why you never write {@code Response.status(409)...} by hand in your logic — you
 * just throw, and the mapper does the rest.
 *
 *   status   the HTTP status to send  (422, 404, 409, ...)
 *   error    a short stable machine code ("validation", "not_found", ...)
 *   fields   optional per-field messages, used by registration validation (422)
 *
 * Use the factory helpers so call sites read like the spec table in LAB2.docx:
 *   throw ApiException.notFound("No product with id " + id);
 *   throw ApiException.conflict("Not enough stock for ...");
 *   throw ApiException.unprocessable("Order must contain at least one item.");
 *   throw ApiException.validation(fieldMessages);   // registration
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String error;
    private final Map<String, String> fields;   // may be null

    public ApiException(int status, String error, String message) {
        this(status, error, message, null);
    }

    public ApiException(int status, String error, String message, Map<String, String> fields) {
        super(message);
        this.status = status;
        this.error = error;
        this.fields = fields;
    }

    public int status() { return status; }
    public String error() { return error; }
    public Map<String, String> fields() { return fields; }

    /** 422 Unprocessable Entity — understood but invalid. */
    public static ApiException unprocessable(String message) {
        return new ApiException(422, "validation", message);
    }

    /** 422 with a message per bad field (registration). */
    public static ApiException validation(Map<String, String> fields) {
        return new ApiException(422, "validation", "Some fields are invalid.", fields);
    }

    /** 404 Not Found — the referenced thing does not exist. */
    public static ApiException notFound(String message) {
        return new ApiException(404, "not_found", message);
    }

    /** 409 Conflict — clashes with current state (dup email, no stock). */
    public static ApiException conflict(String message) {
        return new ApiException(409, "conflict", message);
    }

    /** 500 — an unexpected server/database fault. */
    public static ApiException server(String message) {
        return new ApiException(500, "server_error", message);
    }
}
