package ca.nl.cna.cp3566.store;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * INFRASTRUCTURE (given — do not change).
 *
 * Re-skins the framework's own errors (unknown URL 404, wrong method 405, ...)
 * into the same { "error", "message" } JSON envelope, so every error a client
 * sees has one consistent shape.
 */
@Provider
public class WebExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException ex) {
        int status = ex.getResponse() != null ? ex.getResponse().getStatus() : 500;
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "error", codeFor(status),
                        "message", ex.getMessage() == null ? "" : ex.getMessage()))
                .build();
    }

    private static String codeFor(int status) {
        return switch (status) {
            case 400 -> "bad_request";
            case 404 -> "not_found";
            case 405 -> "method_not_allowed";
            case 415 -> "unsupported_media_type";
            default  -> "error";
        };
    }
}
